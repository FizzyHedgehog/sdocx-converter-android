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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ConverterViewModel : ViewModel() {

    private val _fileList = MutableStateFlow<List<SdocxFileItem>>(emptyList())
    val fileList: StateFlow<List<SdocxFileItem>> = _fileList.asStateFlow()

    private val _settings = MutableStateFlow(ConverterSettings())
    val settings: StateFlow<ConverterSettings> = _settings.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    fun initSettings(context: Context) {
        _settings.value = ConversionHelper.loadSettings(context)
    }

    fun updateSettings(context: Context, newSettings: ConverterSettings) {
        _settings.value = newSettings
        ConversionHelper.saveSettings(context, newSettings)
    }

    fun updateSettings(newSettings: ConverterSettings) {
        _settings.value = newSettings
    }

    fun addUris(uris: List<Uri>, context: Context) {
        val current = _fileList.value.toMutableList()
        val resolver = context.contentResolver
        val alreadyCompletedNames = mutableListOf<String>()
        val alreadyQueuedNames = mutableListOf<String>()

        for (uri in uris) {
            var name = "note.sdocx"
            var size = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            // 이미 변환 완료된 파일인지 확인
            val isCompleted = current.any { it.status == ConversionStatus.COMPLETED && (it.uri == uri || it.name == name) }
            if (isCompleted) {
                alreadyCompletedNames.add(name)
                continue
            }

            // 이미 대기열에 추가된 파일인지 확인
            val isPending = current.any { it.uri == uri || it.name == name }
            if (isPending) {
                alreadyQueuedNames.add(name)
                continue
            }

            current.add(
                SdocxFileItem(
                    uri = uri,
                    name = name,
                    sizeBytes = size,
                    status = ConversionStatus.PENDING
                )
            )
        }
        _fileList.value = current

        // 중복 파일 선택 시 팝업/토스트 알림 표시
        if (alreadyCompletedNames.isNotEmpty()) {
            val namesStr = if (alreadyCompletedNames.size <= 2) {
                alreadyCompletedNames.joinToString(", ")
            } else {
                "${alreadyCompletedNames.first()} +${alreadyCompletedNames.size - 1}"
            }
            Toast.makeText(
                context,
                context.getString(R.string.file_already_completed, namesStr),
                Toast.LENGTH_LONG
            ).show()
        } else if (alreadyQueuedNames.isNotEmpty()) {
            val namesStr = if (alreadyQueuedNames.size <= 2) {
                alreadyQueuedNames.joinToString(", ")
            } else {
                "${alreadyQueuedNames.first()} +${alreadyQueuedNames.size - 1}"
            }
            Toast.makeText(
                context,
                context.getString(R.string.file_already_queued, namesStr),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun addFolderTreeUri(treeUri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootDoc = try {
                DocumentFile.fromTreeUri(context, treeUri)
            } catch (e: Exception) {
                null
            }

            if (rootDoc == null || !rootDoc.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val foundItems = mutableListOf<SdocxFileItem>()
            scanFolderRecursively(rootDoc, foundItems, parentTreeUri = treeUri, maxDepth = 10, currentDepth = 0)

            withContext(Dispatchers.Main) {
                if (foundItems.isEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.folder_empty_hint),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val current = _fileList.value.toMutableList()
                    var addedCount = 0
                    val alreadyCompleted = mutableListOf<String>()

                    for (item in foundItems) {
                        val isDone = current.any { it.status == ConversionStatus.COMPLETED && (it.uri == item.uri || it.name == item.name) }
                        if (isDone) {
                            alreadyCompleted.add(item.name)
                            continue
                        }
                        if (current.none { it.uri == item.uri || it.name == item.name }) {
                            current.add(item)
                            addedCount++
                        }
                    }
                    _fileList.value = current

                    if (addedCount > 0) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.folder_added_hint, addedCount),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (alreadyCompleted.isNotEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.file_already_completed, "${alreadyCompleted.size} files"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun scanFolderRecursively(
        directory: DocumentFile,
        result: MutableList<SdocxFileItem>,
        parentTreeUri: Uri,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth) return

        val children = try {
            directory.listFiles()
        } catch (_: Exception) {
            emptyArray()
        }

        for (file in children) {
            if (file.isDirectory) {
                scanFolderRecursively(file, result, parentTreeUri, maxDepth, currentDepth + 1)
            } else if (file.isFile) {
                val name = file.name ?: ""
                if (name.endsWith(".sdocx", ignoreCase = true)) {
                    result.add(
                        SdocxFileItem(
                            uri = file.uri,
                            name = name,
                            sizeBytes = file.length(),
                            status = ConversionStatus.PENDING,
                            parentFolderTreeUri = directory.uri // 파일이 위치한 원본 폴더 URI 기록
                        )
                    )
                }
            }
        }
    }

    fun exportSinglePdf(srcUri: Uri, destUri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            try {
                context.contentResolver.openInputStream(srcUri)?.use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                        success = true
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                if (success) {
                    _fileList.value = _fileList.value.map {
                        if (it.outputPdfUri == srcUri) {
                            it.copy(isSavedToDevice = true, savedDeviceUri = destUri)
                        } else it
                    }
                    Toast.makeText(context, context.getString(R.string.save_success_single), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportBatchPdfsToFolder(sourceUris: List<Uri>, targetTreeUri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetDir = try {
                DocumentFile.fromTreeUri(context, targetTreeUri)
            } catch (e: Exception) {
                null
            }

            if (targetDir == null || !targetDir.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            var savedCount = 0
            val savedUris = mutableSetOf<Uri>()
            for (srcUri in sourceUris) {
                val item = _fileList.value.find { it.outputPdfUri == srcUri }
                val fileName = item?.outputPdfName ?: (item?.name?.substringBeforeLast(".")?.plus(".pdf") ?: "document.pdf")

                try {
                    val targetFile = targetDir.createFile("application/pdf", fileName) ?: continue
                    context.contentResolver.openInputStream(srcUri)?.use { input ->
                        context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                            input.copyTo(output)
                            savedCount++
                            savedUris.add(srcUri)
                        }
                    }
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                if (savedCount > 0) {
                    _fileList.value = _fileList.value.map {
                        if (it.outputPdfUri in savedUris) it.copy(isSavedToDevice = true) else it
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.save_success_batch, savedCount),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportBatchPdfsToDownloads(sourceUris: List<Uri>, context: Context, subFolder: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            var savedCount = 0
            val savedUris = mutableSetOf<Uri>()

            for (srcUri in sourceUris) {
                val item = _fileList.value.find { it.outputPdfUri == srcUri }
                val fileName = item?.outputPdfName ?: (item?.name?.substringBeforeLast(".")?.plus(".pdf") ?: "document.pdf")

                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val relPath = if (subFolder.isEmpty()) {
                                Environment.DIRECTORY_DOWNLOADS
                            } else {
                                "${Environment.DIRECTORY_DOWNLOADS}/$subFolder"
                            }
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (targetUri != null) {
                        resolver.openOutputStream(targetUri)?.use { out ->
                            resolver.openInputStream(srcUri)?.use { inp ->
                                inp.copyTo(out)
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(targetUri, contentValues, null, null)
                        }

                        savedCount++
                        savedUris.add(srcUri)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                if (savedCount > 0) {
                    _fileList.value = _fileList.value.map {
                        if (it.outputPdfUri in savedUris) it.copy(isSavedToDevice = true) else it
                    }
                    val downloadsLabel = context.getString(R.string.btn_save_folder_downloads)
                    val folderLabel = if (subFolder.isEmpty()) downloadsLabel else "$downloadsLabel($subFolder)"
                    Toast.makeText(
                        context,
                        context.getString(R.string.folder_saved_success, savedCount, folderLabel),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun removeFile(id: String) {
        _fileList.value = _fileList.value.filter { it.id != id }
    }

    fun clearQueue() {
        _fileList.value = _fileList.value.filter { it.status == ConversionStatus.COMPLETED }
    }

    fun clearCompleted() {
        _fileList.value = _fileList.value.filter { it.status != ConversionStatus.COMPLETED }
    }

    fun clearAll() {
        _fileList.value = emptyList()
    }

    fun convertAll(context: Context) {
        if (_isConverting.value) return

        viewModelScope.launch {
            _isConverting.value = true
            val items = _fileList.value
            val currentSettings = _settings.value
            var successCount = 0
            var lastName: String? = null

            for (item in items) {
                if (item.status == ConversionStatus.COMPLETED) continue

                updateItemStatus(item.id, ConversionStatus.CONVERTING)

                val result = withContext(Dispatchers.IO) {
                    ConversionHelper.convertSdocxToPdf(
                        context = context,
                        uri = item.uri,
                        originalName = item.name,
                        settings = currentSettings,
                        parentFolderTreeUri = item.parentFolderTreeUri,
                        forceAutoSave = currentSettings.autoSave
                    )
                }

                if (result.success) {
                    successCount++
                    lastName = result.pdfName
                    updateItemCompleted(
                        id = item.id,
                        outputUri = result.fileProviderUri,
                        outputName = result.pdfName,
                        outputSize = result.pdfSizeBytes,
                        isSaved = result.isSavedToDevice,
                        deviceUri = result.savedDeviceUri
                    )
                } else {
                    updateItemError(item.id, result.errorMessage ?: context.getString(R.string.status_error))
                }
            }
            _isConverting.value = false

            // 변환 완료 후 알림 발송 (설정에서 알림이 켜진 경우)
            if (successCount > 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.btn_all_completed),
                        Toast.LENGTH_SHORT
                    ).show()

                    if (currentSettings.notifyOnCompletion) {
                        ConversionHelper.sendCompletionNotification(context, successCount, lastName)
                    }
                }
            }
        }
    }

    private fun updateItemStatus(id: String, status: ConversionStatus) {
        _fileList.value = _fileList.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
    }

    private fun updateItemCompleted(
        id: String,
        outputUri: Uri?,
        outputName: String,
        outputSize: Long,
        isSaved: Boolean,
        deviceUri: Uri?
    ) {
        _fileList.value = _fileList.value.map {
            if (it.id == id) {
                it.copy(
                    status = ConversionStatus.COMPLETED,
                    outputPdfUri = outputUri,
                    outputPdfName = outputName,
                    outputPdfSizeBytes = outputSize,
                    isSavedToDevice = isSaved,
                    savedDeviceUri = deviceUri
                )
            } else it
        }
    }

    private fun updateItemError(id: String, error: String) {
        _fileList.value = _fileList.value.map {
            if (it.id == id) it.copy(status = ConversionStatus.ERROR, errorMessage = error) else it
        }
    }
}
