package com.prosayac.app.util.excel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.prosayac.app.data.local.dao.ReadingWithMeter
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

class ExcelExporter @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    suspend fun export(readings: List<ReadingWithMeter>, binaAdi: String): Boolean {
        return try {
            // 1. DYNAMIC NAMING
            val safeName = binaAdi.replace(Regex("[^a-zA-Z0-9]"), "_")
            val timeStamp = SimpleDateFormat("ddMMyy_HHmm", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "${safeName}_${timeStamp}.xls")

            // ── Style definitions ─────────────────────────────────────────────────
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
            val workbook = Workbook.createWorkbook(file)
            val sheet = workbook.createSheet("Okumalar", 0)

            // ── Header Row ─────────────────────────────────────────────────────
            val headers = arrayOf(
                "Tarih / Saat",
                "Bina / Blok",
                "Sayaç No",
                "Sayaç Tipi",
                "Okunan Endeks",
                "Birim"
            )
            for ((i, h) in headers.withIndex()) {
                sheet.addCell(Label(i, 0, h, headerFormat))
            }

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

            // ── Data Rows ───────────────────────────────────────────────────────
            for ((rowIdx, r) in readings.withIndex()) {
                val row = rowIdx + 1

                // 1. Tarih / Saat
                sheet.addCell(Label(0, row, dateFormat.format(Date(r.readingDate)), dateFormatStyle))

                // 2. Bina / Blok (buildingName + flatNumber combined)
                val buildingText = buildString {
                    append(r.buildingName.ifBlank { "" })
                    if (r.flatNumber.isNotBlank()) {
                        if (isNotEmpty()) append(" / ")
                        append(r.flatNumber)
                    }
                }.ifBlank { "-" }
                sheet.addCell(Label(1, row, buildingText, dataFormat))

                // 3. Sayaç No
                sheet.addCell(Label(2, row, r.serialNumber.ifBlank { "-" }, dataFormat))

                // 4. Sayaç Tipi
                sheet.addCell(Label(3, row, r.meterType, dataFormat))

                // 5. Okunan Endeks (NUMERIC)
                val numericValue = r.readingValue
                    .replace(",", ".")
                    .toDoubleOrNull()
                if (numericValue != null) {
                    sheet.addCell(Number(4, row, numericValue, numberFormat))
                } else {
                    sheet.addCell(Label(4, row, r.readingValue.ifBlank { "-" }, dataFormat))
                }

                // 6. Birim (m³ or kWh based on meter type)
                val unit = when {
                    r.meterType.contains("Su", ignoreCase = true) -> "m³"
                    r.meterType.contains("Isı", ignoreCase = true) -> "kWh"
                    else -> ""
                }
                sheet.addCell(Label(5, row, unit, dataFormat))
            }

            // ── Set column widths ───────────────────────────────────────────────
            val columnWidths = intArrayOf(20, 25, 18, 18, 18, 10)
            for (i in 0 until 6) {
                sheet.setColumnView(i, columnWidths[i])
            }

            // ── Write and close ───────────────────────────────────────────────
            workbook.write()
            workbook.close()

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

            context.startActivity(Intent.createChooser(intent, "Dosyayı Paylaş"))
            true

        } catch (e: IOException) {
            Toast.makeText(context, "Dışa aktarma başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            false
        } catch (e: jxl.write.WriteException) {
            Toast.makeText(context, "Dışa aktarma başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "Dışa aktarma başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}