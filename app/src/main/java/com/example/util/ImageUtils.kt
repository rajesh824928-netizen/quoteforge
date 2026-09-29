package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    private const val TAG = "ImageUtils"

    /**
     * Copies a picked media Uri into app-internal storage so the app retains
     * permanent read access across device reboots and app updates without permissions.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri, prefix: String = "brand"): String? {
        return try {
            val directory = File(context.filesDir, "branding").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "${prefix}_${System.currentTimeMillis()}.png"
            val destinationFile = File(directory, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream for $sourceUri")
                return null
            }

            val outputStream = FileOutputStream(destinationFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            destinationFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image from $sourceUri", e)
            null
        }
    }

    /**
     * Safely loads a Bitmap from either an absolute file path or a content Uri.
     */
    fun loadBitmap(context: Context, pathOrUri: String?): Bitmap? {
        if (pathOrUri.isNullOrBlank()) return null
        return try {
            if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("android.resource://")) {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                val cleanPath = pathOrUri.removePrefix("file://")
                val file = File(cleanPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from $pathOrUri", e)
            null
        }
    }
}
