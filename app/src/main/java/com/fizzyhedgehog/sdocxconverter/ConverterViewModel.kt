package com.fizzyhedgehog.sdocxconverter

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
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

    fun addUris(uris: List<Uri>, context: Context) {
        val current = _fileList.value.toMutableList()
        val resolver = context.contentResolver

        for (uri in uris) {
            // 중복 방지
            if (current.any { it.uri == uri }) continue

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
                    Toast.makeText(context, "폴더를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val foundItems = mutableListOf<SdocxFileItem>()
            scanFolderRecursively(rootDoc, foundItems, maxDepth = 10, currentDepth = 0)

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
                    for (item in foundItems) {
                        if (current.none { it.uri == item.uri }) {
                            current.add(item)
                            addedCount++
                        }
                    }
                    _fileList.value = current
                    Toast.makeText(
                        context,
                        context.getString(R.string.folder_added_hint, addedCount),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun scanFolderRecursively(
        directory: DocumentFile,
        result: MutableList<SdocxFileItem>,
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
                scanFolderRecursively(file, result, maxDepth, currentDepth + 1)
            } else if (file.isFile) {
                val name = file.name ?: ""
                if (name.endsWith(".sdocx", ignoreCase = true)) {
                    result.add(
                        SdocxFileItem(
                            uri = file.uri,
                            name = name,
                            sizeBytes = file.length(),
                            status = ConversionStatus.PENDING
                        )
                    )
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

    fun updateSettings(newSettings: ConverterSettings) {
        _settings.value = newSettings
    }

    fun convertAll(context: Context) {
        if (_isConverting.value) return

        viewModelScope.launch {
            _isConverting.value = true
            val items = _fileList.value
            val currentSettings = _settings.value

            for (item in items) {
                if (item.status == ConversionStatus.COMPLETED) continue

                updateItemStatus(item.id, ConversionStatus.CONVERTING)

                val result = withContext(Dispatchers.IO) {
                    processConversion(item, currentSettings, context)
                }

                if (result.success) {
                    updateItemCompleted(item.id, result.uri, result.pdfName, result.pdfSizeBytes)
                } else {
                    updateItemError(item.id, result.errorMessage ?: "변환 실패")
                }
            }
            _isConverting.value = false
        }
    }

    private data class ConversionResult(
        val success: Boolean,
        val uri: Uri? = null,
        val pdfName: String = "",
        val pdfSizeBytes: Long = 0L,
        val errorMessage: String? = null
    )

    private fun processConversion(
        item: SdocxFileItem,
        settings: ConverterSettings,
        context: Context
    ): ConversionResult {
        val cacheDir = context.cacheDir
        val tempInput = File(cacheDir, "input_${item.id}.sdocx")
        val baseName = item.name.substringBeforeLast(".")
        val pdfName = "$baseName.pdf"
        val tempOutput = File(cacheDir, pdfName)

        try {
            // 1. Content URI -> 임시 로컬 파일 복사
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(tempInput).use { output ->
                    input.copyTo(output)
                }
            } ?: return ConversionResult(false, null, pdfName, 0L, "파일을 읽을 수 없습니다.")

            // 2. Native Rust 변환 엔진 호출
            if (NativeConverter.isNativeAvailable()) {
                val exitCode = NativeConverter.convertNative(
                    inputPath = tempInput.absolutePath,
                    outputPath = tempOutput.absolutePath,
                    penScale = settings.penScale,
                    markerScale = settings.markerScale,
                    splitOption = settings.splitOption,
                    autoSplit = settings.autoSplit
                )
                if (exitCode != 0 || !tempOutput.exists()) {
                    return ConversionResult(false, null, pdfName, 0L, "변환 엔진 오류 (코드: $exitCode)")
                }
            } else {
                // 네이티브 모듈 로드 전 시뮬레이션 / 안전 플레이스홀더
                return ConversionResult(false, null, pdfName, 0L, "네이티브 변환 엔진(libsdocx2pdf.so)이 탑재되지 않았습니다.")
            }

            // 3. MediaStore를 통해 '다운로드' 폴더로 저장
            val pdfSizeBytes = tempOutput.length()
            val savedUri = savePdfToDownloads(context, tempOutput, pdfName)
            tempInput.delete()
            tempOutput.delete()

            return ConversionResult(
                success = true,
                uri = savedUri,
                pdfName = pdfName,
                pdfSizeBytes = pdfSizeBytes
            )
        } catch (e: Exception) {
            tempInput.delete()
            tempOutput.delete()
            return ConversionResult(false, null, pdfName, 0L, e.localizedMessage ?: "알 수 없는 오류")
        }
    }

    private fun savePdfToDownloads(context: Context, srcFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SDOCX_PDF")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return null
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

    private fun updateItemStatus(id: String, status: ConversionStatus) {
        _fileList.value = _fileList.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
    }

    private fun updateItemCompleted(id: String, outputUri: Uri?, outputName: String, outputSize: Long) {
        _fileList.value = _fileList.value.map {
            if (it.id == id) {
                it.copy(
                    status = ConversionStatus.COMPLETED,
                    outputPdfUri = outputUri,
                    outputPdfName = outputName,
                    outputPdfSizeBytes = outputSize
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
