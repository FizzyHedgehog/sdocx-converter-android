package com.fizzyhedgehog.sdocxconverter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ConversionHelper {

    const val PREFS_NAME = "sdocx_converter_prefs"
    private const val NOTIFICATION_CHANNEL_ID = "sdocx_conversion_channel"

    data class ConversionResult(
        val success: Boolean,
        val localPdfFile: File? = null,
        val fileProviderUri: Uri? = null,
        val pdfName: String = "",
        val pdfSizeBytes: Long = 0L,
        val isSavedToDevice: Boolean = false,
        val savedDeviceUri: Uri? = null,
        val errorMessage: String? = null
    )

    fun loadSettings(context: Context): ConverterSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ConverterSettings(
            penScale = prefs.getFloat("pen_scale", 1.5f).toDouble(),
            markerScale = prefs.getFloat("marker_scale", 1.0f).toDouble(),
            splitOption = prefs.getString("split_option", "") ?: "",
            autoSplit = prefs.getBoolean("auto_split", false),
            autoSave = prefs.getBoolean("auto_save", false),
            defaultSaveTreeUri = prefs.getString("default_save_tree_uri", null),
            defaultSaveTreeName = prefs.getString("default_save_tree_name", null),
            notifyOnCompletion = prefs.getBoolean("notify_on_completion", true),
            shareReceiveAction = ShareReceiveAction.fromKey(prefs.getString("share_receive_action", ShareReceiveAction.OPEN_APP.key)),
            autoCheckUpdate = prefs.getBoolean("auto_check_update", true)
        )
    }

    fun saveSettings(context: Context, newSettings: ConverterSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("pen_scale", newSettings.penScale.toFloat())
            putFloat("marker_scale", newSettings.markerScale.toFloat())
            putString("split_option", newSettings.splitOption)
            putBoolean("auto_split", newSettings.autoSplit)
            putBoolean("auto_save", newSettings.autoSave)
            putString("default_save_tree_uri", newSettings.defaultSaveTreeUri)
            putString("default_save_tree_name", newSettings.defaultSaveTreeName)
            putBoolean("notify_on_completion", newSettings.notifyOnCompletion)
            putString("share_receive_action", newSettings.shareReceiveAction.key)
            putBoolean("auto_check_update", newSettings.autoCheckUpdate)
            apply()
        }
    }

    fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
        var name = "note.sdocx"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}
        return Pair(name, size)
    }

    fun convertSdocxToPdf(
        context: Context,
        uri: Uri,
        originalName: String,
        settings: ConverterSettings,
        parentFolderTreeUri: Uri? = null,
        forceAutoSave: Boolean = false
    ): ConversionResult {
        val cacheDir = context.cacheDir
        val uniqueId = UUID.randomUUID().toString()
        val tempInput = File(cacheDir, "input_${uniqueId}.sdocx")
        val baseName = originalName.substringBeforeLast(".")
        val pdfName = "$baseName.pdf"
        val itemDir = File(File(context.filesDir, "converted_pdfs"), uniqueId).apply { mkdirs() }
        val finalLocalPdf = File(itemDir, pdfName)

        var conversionSucceeded = false
        try {
            // 1. Content URI -> 로컬 임시 파일 복사
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempInput).use { output ->
                    input.copyTo(output)
                }
            } ?: return ConversionResult(
                success = false,
                pdfName = pdfName,
                errorMessage = context.getString(R.string.error_read_failed)
            )

            // 2. Native Rust 변환 엔진 호출
            if (NativeConverter.isNativeAvailable()) {
                val exitCode = NativeConverter.convertNative(
                    inputPath = tempInput.absolutePath,
                    outputPath = finalLocalPdf.absolutePath,
                    penScale = settings.penScale,
                    markerScale = settings.markerScale,
                    splitOption = settings.splitOption,
                    autoSplit = settings.autoSplit
                )
                if (exitCode != 0 || !finalLocalPdf.exists()) {
                    return ConversionResult(
                        success = false,
                        pdfName = pdfName,
                        errorMessage = context.getString(R.string.error_native_engine, exitCode)
                    )
                }
            } else {
                return ConversionResult(
                    success = false,
                    pdfName = pdfName,
                    errorMessage = context.getString(R.string.error_native_not_loaded)
                )
            }

            // 3. FileProvider Content URI 생성
            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                finalLocalPdf
            )
            val pdfSizeBytes = finalLocalPdf.length()

            var isSaved = false
            var deviceUri: Uri? = null

            // 4. 자동 저장 처리 (forceAutoSave = true 이거나 settings.autoSave = true)
            if (forceAutoSave || settings.autoSave) {
                deviceUri = savePdfToDevice(
                    context = context,
                    pdfFile = finalLocalPdf,
                    pdfName = pdfName,
                    settings = settings,
                    parentFolderTreeUri = parentFolderTreeUri
                )
                isSaved = (deviceUri != null)
            }

            conversionSucceeded = true
            return ConversionResult(
                success = true,
                localPdfFile = finalLocalPdf,
                fileProviderUri = fileProviderUri,
                pdfName = pdfName,
                pdfSizeBytes = pdfSizeBytes,
                isSavedToDevice = isSaved,
                savedDeviceUri = deviceUri
            )
        } catch (e: Exception) {
            return ConversionResult(
                success = false,
                pdfName = pdfName,
                errorMessage = e.localizedMessage ?: context.getString(R.string.error_unknown)
            )
        } finally {
            if (tempInput.exists()) {
                tempInput.delete()
            }
            if (!conversionSucceeded && !finalLocalPdf.exists()) {
                itemDir.deleteRecursively()
            }
        }
    }

    fun savePdfToDevice(
        context: Context,
        pdfFile: File,
        pdfName: String,
        settings: ConverterSettings,
        parentFolderTreeUri: Uri? = null
    ): Uri? {
        // Case A: 원본 파일이 SAF 트리(폴더)에서 온 경우 원본 폴더에 저장
        if (parentFolderTreeUri != null) {
            val savedUri = savePdfToTreeUri(context, pdfFile, pdfName, parentFolderTreeUri)
            if (savedUri != null) return savedUri
        }

        // Case B: 사용자가 지정한 기본 저장 폴더가 있는 경우
        if (settings.defaultSaveTreeUri != null) {
            try {
                val targetTreeUri = Uri.parse(settings.defaultSaveTreeUri)
                val savedUri = savePdfToTreeUri(context, pdfFile, pdfName, targetTreeUri)
                if (savedUri != null) return savedUri
            } catch (_: Exception) {}
        }

        // Case C: 기본 다운로드 폴더 저장 (MediaStore)
        return savePdfToDownloads(context, pdfFile, pdfName)
    }

    fun savePdfToTreeUri(
        context: Context,
        srcFile: File,
        displayName: String,
        treeUri: Uri
    ): Uri? {
        try {
            val parentDoc = DocumentFile.fromTreeUri(context, treeUri)
            val targetDoc = parentDoc?.createFile("application/pdf", displayName)
            if (targetDoc != null) {
                context.contentResolver.openOutputStream(targetDoc.uri)?.use { out ->
                    srcFile.inputStream().use { inp -> inp.copyTo(out) }
                }
                return targetDoc.uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun savePdfToDownloads(context: Context, srcFile: File, displayName: String): Uri? {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { inp ->
                        inp.copyTo(out)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                return uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: 공용 Download 디렉토리 직접 파일 복사 시도
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists() || downloadDir.mkdirs()) {
                val destFile = File(downloadDir, displayName)
                srcFile.copyTo(destFile, overwrite = true)
                return try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
                } catch (_: Exception) {
                    Uri.fromFile(destFile)
                }
            }
        } catch (_: Exception) {}

        return null
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notify_sublabel)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendCompletionNotification(context: Context, completedCount: Int, singleName: String?) {
        try {
            createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val body = if (completedCount == 1 && singleName != null) {
                context.getString(R.string.notification_single_body, singleName)
            } else {
                context.getString(R.string.notification_multi_body, completedCount)
            }

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
