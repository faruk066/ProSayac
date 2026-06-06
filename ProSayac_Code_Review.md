# ProSayaç — Deep Code Review

**Scope:** 38 Kotlin files, ~7,100 LOC. Architecture: Jetpack Compose + MVVM + Hilt + Room + DataStore + USB-serial M-Bus.
**Type:** Read-only senior-engineer audit. No files were modified during analysis.

> **Note:** There is **no Firebase/Firestore** anywhere in this project (`grep` for `firebase|firestore` → no matches). The app is fully offline (Room + DataStore + USB serial). Section 5 is therefore N/A. The manifest declares `INTERNET`/`ACCESS_NETWORK_STATE` "for future sync" and `isSynced` flags exist, but no network layer is implemented.

---

## 1. Bugs & Crashes

### 1.1 — `connect()` performs blocking USB I/O on the main thread
**`MBusSerialManager.kt:145-212`** + **`ConnectionViewModel.kt:42-46`** — **HIGH**

`ConnectionViewModel.connect()` does `viewModelScope.launch { serialManager.connect(...) }`. `viewModelScope` defaults to `Dispatchers.Main.immediate`, and `connect()` is **not** a `suspend` function — it calls `usbManager.openDevice()`, `port.open()`, `port.setParameters()` and prober scans synchronously on the main thread.

**Impact:** USB enumeration/open can take hundreds of ms → jank and ANR risk on the connect screen, especially with a slow/flaky adapter.
**Fix:** Make `connect()` a `suspend fun … = withContext(Dispatchers.IO){…}` (as `sendReadRequest` already does), and only flip `_connectionState` back on the main thread.

### 1.2 — USB-permission retry loses the user's baud rate
**`MBusSerialManager.kt:214-222`** — **MEDIUM**

`connect(config)` stores no config; when permission must be requested it returns early, and `onPermissionGranted()` calls `connect()` with the **default** `SerialConfig()` (2400). The user's selected baud (`ConnectionViewModel.connect(baudRate)`) is silently discarded on first-time permission grant.
**Fix:** Cache the requested `SerialConfig` in a field and reuse it in `onPermissionGranted`.

### 1.3 — `decodeInt32` sign-extension is a no-op (dead/misleading code)
**`MBusProtocolHandler.kt:273-276`** — **LOW (correctness OK, code is wrong-looking)**

```kotlin
if ((v and 0x80000000.toInt()) != 0) {
    v = v - 0x100000000.toInt()   // 0x100000000.toInt() == 0  → subtracts nothing
}
```
`0x100000000` is a `Long`; `.toInt()` truncates the low 32 bits to **0**, so the subtraction does nothing. It happens to be harmless because `v` (built via `shl`/`or` into an `Int`) is already a correct two's-complement signed value — but the block is dead code that implies a fix that isn't happening.
**Fix:** Delete the `if` block; `v.toDouble()` is already correct.

### 1.4 — String-matched error classification is brittle
**`MetersViewModel.kt:294`** — **LOW**

Timeout detection is `result.errorMessage.contains("Cihaz Yanıt Vermedi")`. Any rewording of the message in `pollMeter` (`MBusProtocolHandler.kt:380`) silently reclassifies timeouts as generic errors. Also the log at **`:297`** says "5 saniyede" while the actual timeout is 1500 ms (`:372`).
**Fix:** Return a typed result (sealed class / enum `PollOutcome`) instead of matching localized strings.

### 1.5 — `LazyVerticalGrid` value/status maps mutate the whole `UiState` per meter
**`MetersViewModel.kt:368-378`** — **LOW (functional, perf below in §4)**

`updateMeterStatus`/`updateMeterReadingValue` copy the entire map and the entire `MetersUiState` on every single meter event. Functionally fine; see §4.3 for the perf angle.

---

## 2. M-Bus Protocol Logic

### 2.1 — Medium-byte (water vs heat) is read from the WRONG offset
**`MBusProtocolHandler.kt:58-60` and `308-312`** — **HIGH (latent; mitigated downstream)**

```kotlin
val medium = bytes[6].toInt() and 0xFF   // claims this is the "Measurement Medium"
isWaterMeter = medium == 0x06 || medium == 0x07
```
In an M-Bus long frame (`68 L L 68 C A CI …`), **`bytes[6]` is the CI field** (typically `0x72`/`0x76`), not the medium. The actual *Measurement Medium* byte lives in the fixed data header at **`bytes[14]`** (after ID[7-10], Manufacturer[11-12], Version[13]). So `isWaterMeter` is almost always `false` regardless of meter type — the auto-detection is effectively non-functional, and the code comment is incorrect.

**Impact:** `parseRspUD.readingValue` (`:316-324`) picks energy-vs-volume from this wrong flag, and the volume-branch early `break` at `:150` never fires for real water meters. It's **mitigated** only because `MetersViewModel` (`:279`) re-selects value by the DB `meterType` and treats `readingValue` as a mere "non-null = success" sentinel. Anyone reusing `parseData`/`parseRspUD` directly gets wrong results.
**Fix:** Read medium at `bytes[14]` (guard `bytes.size > 14`), and rename `MEDIUM_*` usage accordingly. Verify against the CI value at `bytes[6]`.

### 2.2 — Targeted addressing breaks when the serial isn't exactly 8 hex digits
**`MBusSerialManager.kt:282-339`** — **HIGH**

```kotlin
if (!targetSerial.isNullOrEmpty() && targetSerial.length >= 8) {
    val b1 = targetSerial.substring(6, 8).toInt(16)
    …
} else {
    write(byteArrayOf(0x10, 0x5B, 0xFE, 0x59, 0x16)) // BROADCAST short-frame read
}
```
Two failure modes:
- **Leading-zero loss (data-integrity):** Excel numeric serials are stringified by `formatNumericValue` (`ExcelParser.kt:604-613`) which strips leading zeros — `00123456` becomes `"123456"` (length 6). That fails `length >= 8`, so the code falls back to a **broadcast read** instead of selecting the target. On a multi-meter bus this reads *whatever responds*, mis-assigning readings.
- **`'O'` vs `'0'` / non-hex (crash path):** `substring(...).toInt(16)` throws `NumberFormatException` for any non-hex char (the classic letter-O-for-zero). It propagates out of `sendReadRequest` → caught in `pollMeter` (`:386`) → the meter just "fails", with no actionable message.

**Fix:** Normalize the serial before use: strip whitespace, map ambiguous `O→0` if your meter IDs are numeric, left-pad to 8 digits, and validate it's hex. Reject/skip with a clear error rather than silently broadcasting. Critically, **don't strip leading zeros from serials on Excel import** (treat serial columns as text).

### 2.3 — Only DIF data types `0x04` and `0x0C` are decoded
**`MBusProtocolHandler.kt:137-141`** — **MEDIUM**

```kotlin
when (dataType) {
    0x04 -> decodeInt32(...)            // 32-bit int
    0x0C -> decodeBcdIntForParse(...)   // 8-digit BCD
    else -> continue                    // silently skipped
}
```
Real meters frequently encode energy/volume as `0x02` (16-bit), `0x03` (24-bit), `0x06` (48-bit BCD), or `0x07` (64-bit). `dataLength()` even computes their lengths (`:242-257`) — but the value decode drops them, so those frames yield "Enerji/Volume değeri bulunamadı."
**Fix:** Add decoders for `0x01/0x02/0x03` (LE signed ints), `0x06`, `0x07` (BCD), reusing `decodeBcdIntForParse` with the appropriate byte length.

### 2.4 — No checksum / stop-byte validation of the response frame
**`MBusProtocolHandler.kt:57-78`** — **MEDIUM**

`parseData` checks the start delimiter (`0x68`) and a minimum length, but never validates the M-Bus **checksum** (sum of C..end-of-user-data mod 256) or the trailing `0x16` stop byte. (`waitForRspUdFrame` validates the `68 L L 68` header and length, but not the checksum either.) A corrupted-but-well-framed payload will be parsed as a real reading.
**Fix:** After accumulating `L+6` bytes, verify `bytes[L+4] == checksum(bytes[4 .. L+3])` and `bytes[L+5] == 0x16`; reject on mismatch.

### 2.5 — `decodeBcd` produces lowercase, fixed-width hex; match against DB is fragile
**`MBusProtocolHandler.kt:194-202`** + **`MetersViewModel.kt:262-263`** — **MEDIUM**

`decodeBcd` emits lowercase, always-8-char output (e.g. `"0a123456"` for an invalid BCD nibble). The strict-match `it.serialNumber == result.meterId` is a case-sensitive exact compare against the DB serial, which may differ in width/case (see §2.2 leading-zero loss). When it fails, the code silently falls back to the loop meter — masking real mismatches.
**Fix:** Normalize both sides (uppercase, zero-pad to 8) before comparing; log when fallback occurs so silent mis-assignment is visible.

---

## 3. Architecture & Design

### 3.1 — Excel/XLSX I/O lives inside the ViewModels
**`MetersViewModel.kt:30-31, 124` (POI imports) and `ReadingsViewModel.kt:72-220`** — **MEDIUM**

`MetersViewModel` imports `org.apache.poi…WorkbookFactory`/`Row`, and `ReadingsViewModel.exportToXlsx()` contains ~150 lines of POI workbook construction. This is data/IO logic bleeding into the presentation layer — it belongs behind the repository or a dedicated `ExcelExporter`/use-case. It also makes the VMs untestable without POI and an Android `Context`.
**Fix:** Move XLSX export into an injected `ExcelExporter` (return a `File`/`Uri`), keep the VM orchestration only.

### 3.2 — `confirmBuildingName` parses Excel on the Main dispatcher
**`MetersViewModel.kt:116-124`** — **MEDIUM**

`viewModelScope.launch { … excelParser.parse(context, uri, …) }` runs the full POI parse (open stream, build workbook, iterate rows) on `Dispatchers.Main`. Large sheets will block the UI. (Contrast with `ReadingsViewModel.exportToXlsx`, which correctly wraps in `withContext(Dispatchers.IO)`.)
**Fix:** Wrap `excelParser.parse(...)` in `withContext(Dispatchers.IO)`.

### 3.3 — `onFormatPicked` ignores the chosen format
**`MetersViewModel.kt:176-189`** — **MEDIUM (functional bug + dead param)**

When the header matches multiple templates, the user picks a format — but `onFormatPicked(format)` never passes `format` to a re-parse. It just re-opens the building-name dialog and `confirmBuildingName` re-calls `excelParser.parse(...)` **without `forceFormat`**, so it hits the ambiguity again → potential loop / the user's choice is meaningless. `ExcelParser.parse` already accepts `forceFormat` (`:58`) but it's never supplied anywhere.
**Fix:** Thread the selected `ExcelFormat` through state and pass it as `forceFormat` to `parse`.

### 3.4 — DI provides `MBusSerialManager` twice
**`DatabaseModule.kt:44-48`** + **`MBusSerialManager.kt:27-30`** — **LOW**

`MBusSerialManager` is `@Singleton @Inject constructor(...)` *and* explicitly `@Provides`-ed in `DatabaseModule`. Redundant; the `@Provides` wins but the constructor binding is dead. Also `DatabaseModule` (DB concerns) is the wrong home for a serial-manager provider.
**Fix:** Delete the `@Provides fun provideSerialManager`; rely on the constructor binding.

### 3.5 — All six ViewModels instantiated eagerly at app root
**`MainActivity.kt:103-108`** — **LOW**

Every screen's VM is `hiltViewModel()`-created up front in `ProSayacMainApp`, so Dashboard/Meters/Readings all start collecting Room flows and run `init{}` immediately even for unseen screens. Minor memory/CPU; also couples all VMs to one composable scope.
**Fix:** Create each VM inside its `composable(route){}` block.

---

## 4. Performance

### 4.1 — `LoggerService.log` is O(n) per call → O(n²) under heavy hardware logging
**`LoggerService.kt:46-56`** — **MEDIUM**

Each log copies the full list (`_logs.value.toMutableList()`), appends, and (over 10k) does `removeAt(0)` in a `repeat` loop (also O(n)). The HARDWARE path logs prolifically (every send, every "garbage byte" at `MBusSerialManager.kt:446/455`, every frame step). During a multi-meter poll this is a measurable CPU/GC drain, and it emits a new full-list `StateFlow` value each time, re-rendering `LogsScreen`.
**Fix:** Use an `ArrayDeque` with bounded capacity, or batch emissions; consider gating HARDWARE byte-level logs behind a debug flag.

### 4.2 — Per-byte/echo buffer manipulation uses `removeAt(0)` on `ArrayList`
**`MBusSerialManager.kt:564-624`** — **LOW**

`dataBuffer`/`lastSentBytes` are `mutableListOf` (ArrayList); `removeAt(0)` (`:582`) and repeated index access are O(n). At 2400 baud this is negligible in practice, but it's the wrong data structure (use `ArrayDeque`).

### 4.3 — Reading progress copies entire `MetersUiState` + maps per meter
**`MetersViewModel.kt:368-378`** — **LOW**

Two full-state copies per meter event (`status` then `value`). With hundreds of meters this re-emits `uiState` continuously, recomposing the whole grid. Consider a dedicated lightweight `SnapshotStateMap` for live statuses outside the main `UiState`.

### 4.4 — No `Context`/Activity leak in VMs
Positive: all VMs that need `Context` inject `@ApplicationContext` (`MetersViewModel.kt:66`, `ReadingsViewModel.kt:43`, `LogsViewModel.kt:27`). No Activity references retained. ✅

---

## 5. Firebase / Firestore
**N/A** — there is no Firebase/Firestore dependency or code anywhere in the project. If/when sync is added, the `markAsSynced`/`isSynced` plumbing is ready, but you'll need listener-lifecycle and error handling that don't exist yet.

---

## 6. Data & State Management

### 6.1 — `fallbackToDestructiveMigration()` on a field-collection DB
**`DatabaseModule.kt:23-29`** (DB version 4) — **HIGH (data loss)**

Any schema change wipes the entire local DB — including **unsynced readings collected in the field** (and there's no sync yet, so the DB is the *only* copy). `exportSchema = true` is set, so the schemas exist to write migrations.
**Fix:** Write `Migration` objects for each version bump; never ship destructive migration for user-collected data. At minimum gate it to debug builds.

### 6.2 — Excel parsing uses `physicalNumberOfRows`/`physicalNumberOfCells` as bounds
**`ExcelParser.kt:236, 251, 322, 337, 410, 430`** — **HIGH (silent data loss)**

`for (rowIndex in 1 until sheet.physicalNumberOfRows)` is wrong: `physicalNumberOfRows` is the **count of non-empty rows**, not the last row index. If the sheet has any blank row in the middle (common in exported templates), the loop terminates early and **drops trailing meters**. The same applies to `headerRow.physicalNumberOfCells` for header-column scanning (a blank header cell mid-row truncates detection).
**Fix:** Iterate `1..sheet.lastRowNum` (use `getRow()` null-checks you already have), and scan headers `0 until headerRow.lastCellNum`.

### 6.3 — `formatNumericValue` strips leading zeros from serials
**`ExcelParser.kt:604-613`** — **HIGH (couples to §2.2)**

Numeric cells are formatted as plain integers, destroying leading zeros (`00123456 → "123456"`). For serial numbers this breaks targeted M-Bus addressing (§2.2) and DB matching (§2.5).
**Fix:** Read the *serial* column as text (POI: read raw string, or format with a fixed width), independent of the generic numeric formatter.

### 6.4 — DataStore vs SerialConfig are disconnected
**`UserPreferences.kt` / `SettingsViewModel.kt` / `ConnectionViewModel.kt`** — **MEDIUM**

`UserPreferences` persists baud/dataBits/stopBits/parity, and `AppPreferences.parity` defaults to `0` (`UserPreferences.kt:22`) — but `SerialConfig.parity` defaults to `UsbSerialPort.PARITY_EVEN` (=2) per the M-Bus spec (`SerialConfig.kt:18`). `ConnectionViewModel.connect` only forwards `baudRate` and ignores stored data/stop/parity entirely. So the persisted serial settings are never actually applied, and the DataStore default (parity 0 = NONE) contradicts the M-Bus requirement.
**Fix:** Build `SerialConfig` from `AppPreferences` and fix the default parity to EVEN; or remove the unused persisted fields.

### 6.5 — `autoSyncEnabled` / `offlineMode` are stored but unused
**`UserPreferences.kt:17-18`** — **LOW (dead state)** — no consumer; misleads users via Settings toggles that do nothing.

---

## 7. Code Quality

- **7.1 — Magic strings everywhere — MEDIUM.** Status values `"Read"/"Unread"/"Skipped"` and poll states `"polling"/"success"/"timeout"/"error"` are bare literals across `MeterDao.kt:28-31`, `MetersViewModel.kt:213/242/290/299/307`, `MetersScreen.kt:458-478`, and meter types `"Isı Sayacı"/"Sıcak Su Sayacı"` are duplicated in `Meter.kt:7`, `ExcelParser.kt:46-47`, and matched by substring `contains("Su")`/`contains("Isı")` in three places. One typo → silent filter/branch failure. Introduce `enum class MeterStatus`/`MeterType` (or central `const`).
- **7.2 — Duplicated `formatReadingValue`** in `MetersScreen.kt:429` and `ReadingsScreen.kt:37` (identical) — DRY violation. Extract to a shared util. **LOW.**
- **7.3 — `parseData` is doing too much** (frame validation + ID decode + DIF/VIF walk + unit scaling + logging) in one ~130-line function. Split header-parse, record-iteration, and value-scaling. **LOW.**
- **7.4 — Silent `catch {}` swallowing** in `SettingsViewModel.deleteAllData` (`:71-73`), `LogsViewModel.exportLogs/shareExport` (`:61/94`). "Delete all data" failing silently is bad UX for a destructive action. **MEDIUM.**
- **7.5 — Duplicated comment header / leftover** `ExcelParser.kt:210-214` (TELEGRAM header printed twice). Dead-code `receivedData` channel (`MBusSerialManager.kt:40-41`) is never consumed. **LOW.**
- **7.6 — Input validation:** building name is validated (non-blank) but Excel-derived serials/flat numbers aren't (length, charset). Meter type code accepts only 4/6; everything else → row error (good), but `safeGetCellAsInt` returning `0` on parse failure conflates "missing" with "zero." **LOW.**

---

## 8. Room Database

- **8.1 — No migrations / destructive fallback — HIGH.** Covered in §6.1.
- **8.2 — `OnConflictStrategy.REPLACE` on readings — MEDIUM.** `ReadingDao.insertReading` (`:83`) uses REPLACE. Since `ReadingEntity.id` autogenerates, this is fine for new rows, but REPLACE on a FK-child with `onDelete = CASCADE` can trigger cascade side-effects on conflict; verify no unintended deletes. For an append-only readings log, prefer `ABORT`/`IGNORE`.
- **8.3 — Missing indices for common queries — MEDIUM.** `meters` is filtered by `status` and `meter_type` (`MeterDao.kt:16-23`) and counted by status repeatedly for the dashboard, but has **no index** on `status`/`meter_type`. `readings` is queried by `reading_date` ranges and grouped by date (`ReadingDao.kt:31, 56-81`) with **no index** on `reading_date`. Add `@Entity(indices=[Index("status"), Index("meter_type")])` and `Index("reading_date")`.
- **8.4 — `strftime(reading_date/1000,'unixepoch')` is UTC — MEDIUM.** Daily/monthly grouping (`ReadingDao.kt:56-81`) buckets by **UTC** day/month, while the UI formats dates in `Locale.getDefault()` timezone. Readings near midnight land in the wrong day/month bucket on the dashboard charts. Apply a `'localtime'` modifier or pass a tz offset.
- **8.5 — Entity integrity is otherwise good.** FK with `CASCADE` + index on `meter_id` is correct (`ReadingEntity.kt:10-21`). ✅

---

## 9. Kotlin Specifics

- **9.1 — `combine` of 5 flows collected forever inside a child launch, gated by `delay(200)` — MEDIUM.** `DashboardViewModel.kt:61-87`: the donut chart reads a `_uiState.value` **snapshot** 200 ms after launching the counts collector, racing the first emission. On a slow device counts may still be 0 → donut shows empty on first load. Also charts are computed **once** (`loadChartData`) and never recomputed when counts change, so the donut/"synced" numbers go stale after new reads. Replace `delay` with a proper `combine(... ).map{…}.stateIn(...)`, and recompute donut reactively.
- **9.2 — `catch { e -> }` ignores the throwable — LOW.** `MetersViewModel.kt:84`, `ReadingsViewModel.kt:57`: the `Flow.catch` blocks discard `e` (not logged). Harder to diagnose load failures.
- **9.3 — Sealed-class exhaustiveness — OK.** `NavRoute` and `ConnectionState` `when`s are exhaustive; no issues.
- **9.4 — `waitForRspUdFrame` resume-after-resume guarded — OK but messy.** `MBusSerialManager.kt:490-504`: after `cont.resume`, the loop keeps iterating and re-checking `cont.isActive`; correct but awkward (the author even comments on the non-local-break limitation). Consider restructuring so the callback returns early once the frame completes.

---

## 10. Security / Threading

- **10.1 — Shared-mutable callbacks accessed across threads without synchronization — MEDIUM (threading).** `accumulatorCallback`, `e5Callback`, `rawResponseCallback` (`MBusSerialManager.kt:53-60`) are plain `var`s. They're **read** inside `onNewData`'s `synchronized(dataBuffer)` block (IO thread) but **assigned** from coroutine threads in `waitForRspUdFrame`/`waitForE5` **outside** any common lock (`:437, 349, 372`). This is a data-race / visibility hazard: a set callback may not be observed, or a stale one invoked. Make them `@Volatile` and/or assign under the same `dataBuffer` lock.
- **10.2 — Singleton `cleanup()` is one-shot vs Activity lifecycle — MEDIUM.** `MainActivity.onDestroy` (`:88-91`) calls `serialManager.cleanup()`, which sets `cleanupCalled = true` and unregisters the USB receiver permanently. The manager is a process-scoped `@Singleton`; if the process survives and a new `MainActivity` is created, the singleton is already "cleaned" and won't re-register the receiver (`registerUsbReceiver` is only called in `init{}`). USB attach/detach + permission callbacks then stop working until process death. `configChanges` covers rotation, so this mainly bites on backgrounded-then-restored sessions. Tie USB lifecycle to a process/`ProcessLifecycleOwner` or re-init on connect, not Activity destroy.
- **10.3 — `android:allowBackup="true"` backs up the meter/reading DB — LOW.** `AndroidManifest.xml:17`. Customer/flat/owner data and readings can be auto-backed-up to the cloud. Consider `allowBackup="false"` or `fullBackupContent` rules excluding the DB.
- **10.4 — No hardcoded secrets — OK.** No API keys/credentials anywhere. ✅ FileProvider is `exported=false` with proper grant flags and `cache-path` covers the export dir (`file_paths.xml`). ✅

---

## Summary Table by Severity

| # | Issue | File:Line | Severity |
|---|-------|-----------|----------|
| 6.1/8.1 | `fallbackToDestructiveMigration` → field data loss | DatabaseModule.kt:28 | **HIGH** |
| 6.2 | `physicalNumberOfRows/Cells` truncates Excel import | ExcelParser.kt:236,251,322,337,410,430 | **HIGH** |
| 2.2/6.3 | Serial leading-zero loss + non-hex crash → wrong/broadcast read | MBusSerialManager.kt:282-339; ExcelParser.kt:604 | **HIGH** |
| 1.1 | Blocking USB I/O on main thread | MBusSerialManager.kt:145; ConnectionViewModel.kt:42 | **HIGH** |
| 2.1 | Medium byte read at wrong offset (bytes[6]≠medium) | MBusProtocolHandler.kt:58-60,308 | **HIGH** (mitigated) |
| 2.3 | Only DIF 0x04/0x0C decoded; others dropped | MBusProtocolHandler.kt:137-141 | MEDIUM |
| 2.4 | No checksum / stop-byte validation | MBusProtocolHandler.kt:57-78 | MEDIUM |
| 2.5 | Fragile DB↔frame serial matching | MetersViewModel.kt:262 | MEDIUM |
| 3.2 | Excel parse on Main dispatcher | MetersViewModel.kt:116-124 | MEDIUM |
| 3.3 | `onFormatPicked` ignores chosen format (re-ambiguity loop) | MetersViewModel.kt:176-189 | MEDIUM |
| 1.2 | Permission retry loses baud rate | MBusSerialManager.kt:214-222 | MEDIUM |
| 4.1 | O(n²) logging under heavy HW logs | LoggerService.kt:46-56 | MEDIUM |
| 6.4 | DataStore serial config never applied; wrong parity default | UserPreferences.kt:22; ConnectionViewModel.kt:40 | MEDIUM |
| 8.3 | Missing indices (status, meter_type, reading_date) | MeterDao/ReadingDao | MEDIUM |
| 8.4 | UTC date bucketing in chart queries | ReadingDao.kt:56-81 | MEDIUM |
| 9.1 | Dashboard `delay(200)` race + stale charts | DashboardViewModel.kt:87 | MEDIUM |
| 10.1 | Cross-thread callbacks not synchronized/volatile | MBusSerialManager.kt:53-60 | MEDIUM |
| 10.2 | One-shot singleton cleanup vs lifecycle | MainActivity.kt:88; MBusSerialManager.kt:90 | MEDIUM |
| 7.1 | Magic strings (status/type/poll-state) | many | MEDIUM |
| 7.4 | Silent catch on destructive/delete actions | SettingsViewModel.kt:71 | MEDIUM |
| 3.1 | POI/XLSX logic inside ViewModels | Meters/ReadingsViewModel | MEDIUM |
| 8.2 | REPLACE conflict strategy on FK child | ReadingDao.kt:83 | MEDIUM |
| 1.3 | Dead sign-extension in decodeInt32 | MBusProtocolHandler.kt:273-276 | LOW |
| 1.4 | String-matched error classification | MetersViewModel.kt:294 | LOW |
| 3.4 | Duplicate DI binding for SerialManager | DatabaseModule.kt:44 | LOW |
| 3.5 | All VMs eagerly created at root | MainActivity.kt:103 | LOW |
| 7.2 | Duplicated `formatReadingValue` | Meters/ReadingsScreen | LOW |
| 6.5 | autoSync/offlineMode stored but unused | UserPreferences.kt:17 | LOW |
| 10.3 | allowBackup=true on PII DB | AndroidManifest.xml:17 | LOW |
| 4.2/4.3 | removeAt(0) on ArrayList; full-state copies | MBusSerialManager / MetersViewModel | LOW |

**Totals:** 5 HIGH · 16 MEDIUM · ~9 LOW. (Section 5 Firebase: N/A.)

---

## Top 5 to Fix First

1. **Excel row/column truncation (6.2, `physicalNumberOfRows`).** *Why first:* it silently drops meters during import — the user never sees the missing rows, and every downstream reading is then incomplete. Cheap one-line fixes (`lastRowNum`/`lastCellNum`), huge correctness payoff.
2. **Serial leading-zero loss + non-hex handling (2.2/6.3).** *Why:* directly corrupts the core function of the app — targeted M-Bus reads. Leading-zero stripping silently downgrades targeted reads to broadcast (wrong meter), and `'O'`/non-hex throws. Fix: treat serial as text, normalize+validate before addressing.
3. **Destructive migration (6.1).** *Why:* a single schema bump erases all field-collected readings with no remote backup. One bad release = total data loss. Add real migrations before any further entity change ships.
4. **Blocking USB connect on main thread (1.1).** *Why:* ANR on the most-used action (connect), and it's a 1-line dispatcher fix. High user-visible reliability impact.
5. **Medium-byte offset + missing DIF decoders + no checksum (2.1/2.3/2.4).** *Why:* these are the protocol-correctness core. Even though the VM currently masks the medium bug via DB type, the parser is the reusable source of truth and will produce wrong/empty readings for any meter using 16/24/48-bit encodings or sending corrupted-but-framed data.

---

## Overall Code Health Score: **6.5 / 10**

**Justification:**
- **Strengths (+):** Clean MVVM separation in most places; correct Hilt singleton scoping; proper `@ApplicationContext` usage (no Activity leaks); reactive Room `Flow`s; atomic import via `@Transaction`; sensible USB-serial state machine; thoughtful echo-cancellation and frame-accumulator logic; FK + index on readings; no secrets; correct VIF exponent math; defensive try/catch in the parser; good use of `collectAsStateWithLifecycle` for theme.
- **Weaknesses (−):** The two things that *must* be bulletproof in this domain — **Excel ingestion** and **M-Bus addressing/parsing** — both have HIGH-severity correctness bugs that fail *silently*. Destructive DB migration is a latent catastrophe for field data. Threading around the serial callbacks is racy. Pervasive magic strings make the status/type state machine fragile. Several persisted settings (serial config, autoSync) are wired but never applied — indicating drift between intent and implementation.

It's a competent, well-structured app that is **one bad import or schema bump away from silently losing or corrupting data**. The architecture is sound enough that the fixes are mostly localized rather than structural — hence above-average, but not yet trustworthy for unattended field use.

---

## Recommended Refactoring Roadmap (priority order)

1. **Data-integrity hardening (sprint 1).** Fix Excel `lastRowNum`/`lastCellNum`; read serial columns as text (no zero-strip); replace destructive migration with real `Migration`s; add `OnConflictStrategy.ABORT`/`IGNORE` for readings.
2. **M-Bus correctness (sprint 1-2).** Correct medium offset (`bytes[14]`) with size guards; add DIF decoders for 0x01/0x02/0x03/0x06/0x07; add checksum + `0x16` validation in the accumulator; normalize+validate serials before addressing; return a typed `PollOutcome` instead of string-matched errors.
3. **Threading/lifecycle (sprint 2).** Move `connect()` to `Dispatchers.IO`; make the serial callbacks `@Volatile` and assign under the `dataBuffer` lock; decouple USB lifecycle from `Activity.onDestroy`; cache `SerialConfig` across permission retries.
4. **Domain-type safety (sprint 2-3).** Introduce `enum class MeterStatus`/`MeterType`/`PollState` and a single `MeterUnit` mapper; eliminate magic strings and the duplicated `formatReadingValue`/`contains("Su")` logic.
5. **Layering cleanup (sprint 3).** Extract `ExcelImporter`/`ExcelExporter` behind the repository; move POI out of ViewModels; wrap Excel parse in IO; wire `forceFormat` through `onFormatPicked`.
6. **Dashboard & queries (sprint 3).** Replace `delay(200)` with reactive `combine(...).stateIn`; recompute charts on data change; add DB indices; fix UTC→local date bucketing.
7. **Settings reconciliation & polish (sprint 4).** Apply persisted serial config (or remove unused fields); fix default parity to EVEN; surface delete/export failures to the user; bound `LoggerService` with `ArrayDeque` and gate verbose HW logs; set `allowBackup=false` (or backup rules) for the PII DB.
