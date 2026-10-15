package com.fizzyhedgehog.sdocxconverter

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareHandlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUris = extractUrisFromIntent(intent)
        if (targetUris.isEmpty()) {
            finish()
            return
        }

        val settings = ConversionHelper.loadSettings(this)

        when (settings.shareReceiveAction) {
            ShareReceiveAction.OPEN_APP -> {
                // 기본 모드: 메인 앱 화면을 열고 인텐트 전달
                val forwardIntent = Intent(this, MainActivity::class.java).apply {
                    action = intent.action
                    type = intent.type
                    putExtras(intent)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(forwardIntent)
                finish()
            }

            ShareReceiveAction.AUTO_SAVE -> {
                // 모드 A: 무음 백그라운드 자동 저장
                handleSilentAutoSave(targetUris, settings)
            }

            ShareReceiveAction.RE_SHARE -> {
                // 모드 B: 백그라운드 변환 후 즉시 시스템 PDF 공유창 팝업
                handleInstantReShare(targetUris, settings)
            }
        }
    }

    private fun extractUrisFromIntent(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()

        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) listOf(uri) else emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.filterNotNull() ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun handleSilentAutoSave(uris: List<Uri>, settings: ConverterSettings) {
        Toast.makeText(this, getString(R.string.toast_background_converting), Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            var successCount = 0
            var lastName: String? = null
            var lastError: String? = null

            for (uri in uris) {
                val (name, _) = ConversionHelper.queryFileInfo(this@ShareHandlerActivity, uri)
                val result = ConversionHelper.convertSdocxToPdf(
                    context = this@ShareHandlerActivity,
                    uri = uri,
                    originalName = name,
                    settings = settings,
                    forceAutoSave = true
                )
                if (result.success) {
                    successCount++
                    lastName = result.pdfName
                } else {
                    lastError = result.errorMessage
                }
            }

            withContext(Dispatchers.Main) {
                if (successCount > 0) {
                    val msg = if (successCount == 1 && lastName != null) {
                        getString(R.string.toast_background_save_complete, lastName)
                    } else {
                        getString(R.string.toast_background_save_multi_complete, successCount)
                    }
                    Toast.makeText(this@ShareHandlerActivity, msg, Toast.LENGTH_LONG).show()

                    if (settings.notifyOnCompletion) {
                        ConversionHelper.sendCompletionNotification(this@ShareHandlerActivity, successCount, lastName)
                    }
                } else {
                    val errMsg = lastError ?: getString(R.string.status_error)
                    Toast.makeText(
                        this@ShareHandlerActivity,
                        getString(R.string.toast_background_convert_error, errMsg),
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
        }
    }

    private fun handleInstantReShare(uris: List<Uri>, settings: ConverterSettings) {
        Toast.makeText(this, getString(R.string.toast_background_converting), Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val convertedUris = mutableListOf<Uri>()
            var lastError: String? = null

            for (uri in uris) {
                val (name, _) = ConversionHelper.queryFileInfo(this@ShareHandlerActivity, uri)
                val result = ConversionHelper.convertSdocxToPdf(
                    context = this@ShareHandlerActivity,
                    uri = uri,
                    originalName = name,
                    settings = settings,
                    forceAutoSave = false
                )
                if (result.success && result.fileProviderUri != null) {
                    convertedUris.add(result.fileProviderUri)
                } else {
                    lastError = result.errorMessage
                }
            }

            withContext(Dispatchers.Main) {
                if (convertedUris.isNotEmpty()) {
                    if (convertedUris.size == 1) {
                        val singleUri = convertedUris[0]
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, singleUri)
                            clipData = ClipData.newUri(contentResolver, "PDF", singleUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
                    } else {
                        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "application/pdf"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(convertedUris))
                            val clip = ClipData.newUri(contentResolver, "PDF", convertedUris[0])
                            for (i in 1 until convertedUris.size) {
                                clip.addItem(ClipData.Item(convertedUris[i]))
                            }
                            clipData = clip
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_multi_title)))
                    }
                } else {
                    val errMsg = lastError ?: getString(R.string.status_error)
                    Toast.makeText(
                        this@ShareHandlerActivity,
                        getString(R.string.toast_background_convert_error, errMsg),
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
        }
    }
}
