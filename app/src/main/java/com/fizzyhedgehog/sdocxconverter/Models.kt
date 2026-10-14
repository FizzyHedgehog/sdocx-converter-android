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
    val outputPdfUri: Uri? = null
)

data class ConverterSettings(
    val penScale: Double = 1.0,
    val markerScale: Double = 1.0,
    val splitOption: String = "",
    val autoSplit: Boolean = false,
    val keepOriginalInDownloads: Boolean = true
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
