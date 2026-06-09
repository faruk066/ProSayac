package com.prosayac.app.util.log

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Writes the log text to a file in the cache exports directory.
     * Returns the absolute path of the exported file, or null on failure.
     */
    fun exportToFile(text: String): String? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()

            val fileName = "sistem_gunlukleri_${System.currentTimeMillis()}.txt"
            val file = File(exportDir, fileName)
            file.writeText(text, Charsets.UTF_8)
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Shares a file at the given path via Android share sheet.
     * Returns true if the share intent was launched successfully.
     */
    fun shareFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (!file.exists()) return false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Günlükleri Paylaş"))
            true
        } catch (_: Exception) {
            false
        }
    }
}
