package com.fizzyhedgehog.sdocxconverter

import android.net.Uri

enum class ConversionStatus {
    PENDING,
    CONVERTING,
    COMPLETED,
    ERROR
}

data class SdocxFileItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val sizeBytes: Long = 0,
    val status: ConversionStatus = ConversionStatus.PENDING,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val outputPdfUri: Uri? = null,
    val outputPdfName: String? = null,
    val outputPdfSizeBytes: Long = 0L,
    val parentFolderTreeUri: Uri? = null,
    val isSavedToDevice: Boolean = false,
    val savedDeviceUri: Uri? = null
)

enum class ShareReceiveAction(val key: String) {
    OPEN_APP("open_app"),
    AUTO_SAVE("auto_save"),
    RE_SHARE("re_share");

    companion object {
        fun fromKey(key: String?): ShareReceiveAction {
            return entries.firstOrNull { it.key == key } ?: OPEN_APP
        }
    }
}

data class ConverterSettings(
    val penScale: Double = 1.5,
    val markerScale: Double = 1.0,
    val splitOption: String = "",
    val autoSplit: Boolean = false,
    val autoSave: Boolean = false,
    val defaultSaveTreeUri: String? = null,
    val defaultSaveTreeName: String? = null,
    val notifyOnCompletion: Boolean = true,
    val shareReceiveAction: ShareReceiveAction = ShareReceiveAction.OPEN_APP
)

object NativeConverter {
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("sdocx2pdf")
            isLoaded = true
        } catch (e: Throwable) {
            isLoaded = false
        }
    }

    fun isNativeAvailable(): Boolean = isLoaded

    external fun convertNative(
        inputPath: String,
        outputPath: String,
        penScale: Double,
        markerScale: Double,
        splitOption: String,
        autoSplit: Boolean
    ): Int
}
