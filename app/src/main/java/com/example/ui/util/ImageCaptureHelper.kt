package com.example.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageCaptureHelper {

    fun createTempImageUri(context: Context): Pair<Uri, File> {
        val imageFolder = File(context.filesDir, "product_images")
        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFile = File(imageFolder, "IMG_PROD_${timeStamp}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        return Pair(uri, imageFile)
    }

    fun savePickedImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val imageFolder = File(context.filesDir, "product_images")
            if (!imageFolder.exists()) {
                imageFolder.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetFile = File(imageFolder, "IMG_UPLOAD_${timeStamp}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
