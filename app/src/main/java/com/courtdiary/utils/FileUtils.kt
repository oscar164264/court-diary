package com.courtdiary.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.courtdiary.model.CaseDocument
import java.io.File

object FileUtils {

    /**
     * Copies the file at [sourceUri] into app-internal storage under
     * filesDir/case_documents/{caseId}/ and returns a [CaseDocument] ready to insert.
     * Returns null if the copy fails.
     */
    fun copyToAppStorage(context: Context, sourceUri: Uri, caseId: Int): CaseDocument? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(sourceUri) ?: "application/octet-stream"
            val rawName = getFileName(context, sourceUri) ?: "document_${System.currentTimeMillis()}"

            val dir = File(context.filesDir, "case_documents/$caseId")
            dir.mkdirs()

            val destFile = uniqueFile(dir, rawName)

            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            CaseDocument(
                caseId = caseId,
                fileName = destFile.name,
                filePath = destFile.absolutePath,
                mimeType = mimeType,
                fileSize = destFile.length()
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Returns a FileProvider URI so other apps can open the file. */
    fun getFileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }

    // ── Helpers ──────────────────────────────────────────────

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name
    }

    /** Appends _1, _2 … to the filename if a file with that name already exists. */
    private fun uniqueFile(dir: File, fileName: String): File {
        var file = File(dir, fileName)
        if (!file.exists()) return file
        val dotIndex = fileName.lastIndexOf('.')
        val base = if (dotIndex >= 0) fileName.substring(0, dotIndex) else fileName
        val ext  = if (dotIndex >= 0) fileName.substring(dotIndex) else ""
        var n = 1
        while (file.exists()) { file = File(dir, "${base}_$n$ext"); n++ }
        return file
    }
}
