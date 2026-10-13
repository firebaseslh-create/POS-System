package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.entity.Product
import java.io.ByteArrayOutputStream
import java.io.File

object ProductImageHelper {

    fun encodeImageFileOrUriToDataUrl(context: Context, uriStr: String, maxSizePx: Int = 360, quality: Int = 78): String? {
        return try {
            val bytes = readRawBytesFromUriOrFile(context, uriStr) ?: return null
            if (bytes.isEmpty()) return null
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxSizePx * 2 || boundsOpts.outHeight / sampleSize > maxSizePx * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
            val w = original.width
            val h = original.height
            val scaled = if (w > maxSizePx || h > maxSizePx) {
                val ratio = minOf(maxSizePx.toFloat() / w, maxSizePx.toFloat() / h)
                val targetW = (w * ratio).toInt().coerceAtLeast(1)
                val targetH = (h * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (scaled !== original) {
                scaled.recycle()
            }
            original.recycle()
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Throwable) {
            null
        }
    }

    fun readRawBytesFromUriOrFile(context: Context, uriStr: String): ByteArray? {
        val trimmed = uriStr.trim()
        if (trimmed.isBlank()) return null
        return try {
            when {
                trimmed.startsWith("data:image", ignoreCase = true) -> {
                    val base64Part = trimmed.substringAfter("base64,", "").trim()
                    if (base64Part.isNotBlank()) Base64.decode(base64Part, Base64.DEFAULT) else null
                }
                trimmed.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(trimmed).path ?: trimmed.removePrefix("file://")
                    val file = File(path)
                    if (file.exists()) file.readBytes() else null
                }
                trimmed.startsWith("/") -> {
                    val file = File(trimmed)
                    if (file.exists()) file.readBytes() else null
                }
                trimmed.startsWith("content://", ignoreCase = true) -> {
                    context.contentResolver.openInputStream(Uri.parse(trimmed))?.use { it.readBytes() }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun saveDataUrlToInternalStorage(context: Context, dataUrl: String, prefix: String = "prod_restored"): String? {
        val trimmed = dataUrl.trim()
        if (!trimmed.startsWith("data:image", ignoreCase = true)) return null
        return try {
            val base64Part = trimmed.substringAfter("base64,", "").trim()
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            if (bytes.isEmpty()) return null
            val dir = File(context.filesDir, "product_images")
            if (!dir.exists()) dir.mkdirs()
            val hash = base64Part.hashCode().toUInt().toString(16)
            val file = File(dir, "${prefix}_$hash.jpg")
            if (!file.exists() || file.length() == 0L) {
                file.writeBytes(bytes)
            }
            Uri.fromFile(file).toString()
        } catch (_: Exception) {
            null
        }
    }

    fun normalizeImportedImageUri(context: Context?, rawUri: String?): String {
        val raw = rawUri?.trim() ?: return ""
        if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return ""

        // 1. Handle =IMAGE("...") formula from Google Sheets if present
        var cleaned = raw
        if (cleaned.startsWith("=IMAGE(", ignoreCase = true)) {
            val match = Regex("=IMAGE\\(\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(cleaned)
            if (match != null) {
                cleaned = match.groupValues[1].trim()
            }
        }

        // 2. Handle base64 data:image URI -> materialize into local internal storage file so Coil & offline mode work seamlessly
        if (cleaned.startsWith("data:image", ignoreCase = true)) {
            if (context != null) {
                val savedFileUri = saveDataUrlToInternalStorage(context, cleaned)
                if (!savedFileUri.isNullOrBlank()) return savedFileUri
            }
            return cleaned
        }

        // 3. Convert Google Drive sharing/view links into direct image display URLs for Coil
        val driveId = extractGoogleDriveFileId(cleaned)
        if (!driveId.isNullOrBlank()) {
            return "https://drive.google.com/uc?export=view&id=$driveId"
        }

        // 4. Check if it is a file:// or local path from a previous install that no longer exists on disk
        if (cleaned.startsWith("file://", ignoreCase = true) || cleaned.startsWith("/")) {
            val path = if (cleaned.startsWith("file://", ignoreCase = true)) {
                Uri.parse(cleaned).path ?: cleaned.removePrefix("file://")
            } else {
                cleaned
            }
            val f = File(path)
            if (f.exists() && f.length() > 0L) {
                return Uri.fromFile(f).toString()
            }
            // Check if a file with the same name exists in current context.filesDir/product_images
            if (context != null) {
                val altFile = File(File(context.filesDir, "product_images"), f.name)
                if (altFile.exists() && altFile.length() > 0L) {
                    return Uri.fromFile(altFile).toString()
                }
            }
            // Non-existent local file from old APK install cannot be loaded directly
            return ""
        }

        return cleaned
    }

    fun extractGoogleDriveFileId(url: String): String? {
        if (!url.contains("drive.google.com") && !url.contains("docs.google.com")) return null
        // Match /file/d/<ID> or /d/<ID>
        val matchD = Regex("/d/([a-zA-Z0-9_-]{15,})").find(url)
        if (matchD != null) return matchD.groupValues[1]
        // Match id=<ID>
        val matchId = Regex("[?&]id=([a-zA-Z0-9_-]{15,})").find(url)
        if (matchId != null) return matchId.groupValues[1]
        return null
    }

    fun resolveImageModelForCoil(context: Context, imageUri: String): Any? {
        val trimmed = imageUri.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("data:image", ignoreCase = true)) {
            val saved = saveDataUrlToInternalStorage(context, trimmed)
            if (!saved.isNullOrBlank()) {
                val path = Uri.parse(saved).path
                if (path != null) return File(path)
                return saved
            }
            return try {
                val base64Part = trimmed.substringAfter("base64,", "").trim()
                Base64.decode(base64Part, Base64.DEFAULT)
            } catch (_: Exception) {
                trimmed
            }
        }

        val driveId = extractGoogleDriveFileId(trimmed)
        if (!driveId.isNullOrBlank()) {
            return "https://drive.google.com/uc?export=view&id=$driveId"
        }

        if (trimmed.startsWith("android.resource://", ignoreCase = true)) {
            return Uri.parse(trimmed)
        }

        if (trimmed.startsWith("file://", ignoreCase = true)) {
            val path = Uri.parse(trimmed).path ?: trimmed.removePrefix("file://")
            val file = File(path)
            if (file.exists()) return file
        } else if (trimmed.startsWith("/")) {
            val file = File(trimmed)
            if (file.exists()) return file
        }

        return trimmed
    }

    fun isLocalOnlyImageUri(uri: String): Boolean {
        val t = uri.trim()
        return t.startsWith("file://", ignoreCase = true) ||
            t.startsWith("content://", ignoreCase = true) ||
            t.startsWith("/")
    }

    fun prepareProductImageForCloudSync(context: Context?, product: Product): String {
        val raw = product.imageUri.trim()
        if (raw.isBlank()) return ""
        val driveId = extractGoogleDriveFileId(raw)
        if (!driveId.isNullOrBlank()) {
            return "https://drive.google.com/uc?export=view&id=$driveId"
        }
        if (raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true) ||
            raw.startsWith("data:image", ignoreCase = true)
        ) {
            return raw
        }
        if (context != null && isLocalOnlyImageUri(raw)) {
            // Convert local file/content URI to compact base64 data URL (<= 38,000 chars so it fits inside a Google Sheets cell too)
            val dataUrl = encodeImageFileOrUriToDataUrl(context, raw, maxSizePx = 300, quality = 72)
            if (!dataUrl.isNullOrBlank() && dataUrl.length < 45000) {
                return dataUrl
            }
        }
        return raw
    }
}
