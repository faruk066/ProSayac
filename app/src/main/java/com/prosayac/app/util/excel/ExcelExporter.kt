package com.prosayac.app.util.excel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.prosayac.app.domain.model.Meter
import jxl.Workbook
import jxl.write.Label
import jxl.write.Number
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExcelExporter @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    suspend fun export(meters: List<Meter>, binaAdi: String): Boolean {
        // 1. DYNAMIC NAMING
        val safeName = binaAdi.replace(Regex("[^a-zA-Z0-9]"), "_")
        val timeStamp = SimpleDateFormat("ddMMyy_HHmm", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "${safeName}_${timeStamp}.xls")
        var workbook: Workbook? = null
        return try {

            // ── Style definitions ─────────────────────────────────────────────────
            val titleFont = WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD)
            val titleFormat = WritableCellFormat(titleFont)

            val headerFont = WritableFont(WritableFont.ARIAL, 11, WritableFont.BOLD)
            val headerFormat = WritableCellFormat(headerFont).apply {
                setBackground(Colour.GRAY_25)
                setBorder(Border.ALL, BorderLineStyle.THIN)
            }

            val dataFormat = WritableCellFormat().apply {
                setBorder(Border.ALL, BorderLineStyle.THIN)
            }

            val numberFormat = WritableCellFormat().apply {
                setBorder(Border.ALL, BorderLineStyle.THIN)
            }

            val dateFormatStyle = WritableCellFormat().apply {
                setBorder(Border.ALL, BorderLineStyle.THIN)
            }

            // ── Create workbook and sheet ─────────────────────────────────────
            workbook = Workbook.createWorkbook(file)
            val sheet = workbook!!.createSheet("Okumalar", 0)

            // ── Title Row ──────────────────────────────────────────────────────
            sheet.addCell(Label(0, 0, "Site/Apartman Adı: $binaAdi", titleFormat))

            // ── Header Row ─────────────────────────────────────────────────────
            val headers = arrayOf(
                "Daire No",
                "Isı Sayaç No",
                "Isı Enerji (kWh)",
                "Su Sayaç No",
                "Su Hacim (m³)",
                "Son Okuma Tarihi"
            )
            for ((i, h) in headers.withIndex()) {
                sheet.addCell(Label(i, 1, h, headerFormat))
            }

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

            // ── Group by flat number and sort naturally ─────────────────────────
            val grouped = meters.groupBy { it.flatNumber.ifBlank { "0" } }
            val sortedFlats = grouped.keys.sortedWith(NATURAL_FLAT_COMPARATOR)

            var row = 2
            for (flat in sortedFlats) {
                val flatMeters = grouped[flat]!!
                val heatMeter = flatMeters.firstOrNull { it.meterType.contains("Isı", ignoreCase = true) }
                val waterMeter = flatMeters.firstOrNull { it.meterType.contains("Su", ignoreCase = true) }

                // Col 0: Daire No
                sheet.addCell(Label(0, row, flat, dataFormat))

                // Col 1: Isı Sayaç No
                sheet.addCell(
                    Label(1, row,
                        if (heatMeter != null) heatMeter.serialNumber else "Seri No Bulunamadı",
                        dataFormat)
                )

                // Col 2: Isı Enerji (kWh)
                if (heatMeter != null && !heatMeter.lastReading.isNullOrBlank()) {
                    val value = heatMeter.lastReading.replace(",", ".").toDoubleOrNull()
                    if (value != null) {
                        sheet.addCell(Number(2, row, value, numberFormat))
                    } else {
                        sheet.addCell(Label(2, row, "0", dataFormat))
                    }
                } else {
                    sheet.addCell(Label(2, row, "0", dataFormat))
                }

                // Col 3: Su Sayaç No
                sheet.addCell(
                    Label(3, row,
                        if (waterMeter != null) waterMeter.serialNumber else "Seri No Bulunamadı",
                        dataFormat)
                )

                // Col 4: Su Hacim (m³)
                if (waterMeter != null && !waterMeter.lastReading.isNullOrBlank()) {
                    val value = waterMeter.lastReading.replace(",", ".").toDoubleOrNull()
                    if (value != null) {
                        sheet.addCell(Number(4, row, value, numberFormat))
                    } else {
                        sheet.addCell(Label(4, row, "Okunamadı", dataFormat))
                    }
                } else {
                    sheet.addCell(Label(4, row, "Okunamadı", dataFormat))
                }

                // Col 5: Son Okuma Tarihi
                val maxDate = listOfNotNull(heatMeter?.lastReadingDate, waterMeter?.lastReadingDate)
                    .maxOrNull()
                if (maxDate != null) {
                    sheet.addCell(Label(5, row, dateFormat.format(Date(maxDate)), dateFormatStyle))
                } else {
                    sheet.addCell(Label(5, row, "-", dataFormat))
                }

                row++
            }

            // ── Set column widths ───────────────────────────────────────────────
            val columnWidths = intArrayOf(12, 18, 18, 18, 18, 20)
            for (i in 0 until 6) {
                sheet.setColumnView(i, columnWidths[i])
            }

            // ── Write and close ───────────────────────────────────────────────
            workbook!!.write()

            // 2. FILE PROVIDER EXPORT
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // FLAG_ACTIVITY_NEW_TASK MUST be on the chooser intent when using ApplicationContext
            val chooser = Intent.createChooser(intent, "Dosyayı Paylaş")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true

        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Dışa aktarma başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            }
            false
        } catch (e: jxl.write.WriteException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Dışa aktarma başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            }
            false
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Dışa aktarma başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            }
            false
        } finally {
            try { workbook?.close() } catch (_: Exception) {}
        }
    }

    companion object {
        private val NATURAL_FLAT_COMPARATOR = Comparator<String> { a, b ->
            val aNum = a.toIntOrNull()
            val bNum = b.toIntOrNull()
            if (aNum != null && bNum != null) aNum.compareTo(bNum)
            else a.compareTo(b)
        }
    }
}
