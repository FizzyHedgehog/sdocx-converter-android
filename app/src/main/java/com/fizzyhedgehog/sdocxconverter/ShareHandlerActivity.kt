package com.fizzyhedgehog.sdocxconverter

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                // 모드 A: 무음 백그라운드 자동 저장 (0.5초 정밀 알림)
                handleSilentAutoSave(targetUris, settings)
            }

            ShareReceiveAction.RE_SHARE -> {
                // 모드 B: 백그라운드 변환 후 즉시 시스템 PDF 공유창 팝업 (0.5초 정밀 알림)
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

    private fun setupToastUi(messageState: MutableState<String?>) {
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val msg = messageState.value
                AnimatedVisibility(
                    visible = msg != null,
                    enter = fadeIn(animationSpec = tween(100)),
                    exit = fadeOut(animationSpec = tween(100))
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xDD232427),
                        shadowElevation = 4.dp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = msg.orEmpty(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    private fun handleSilentAutoSave(uris: List<Uri>, settings: ConverterSettings) {
        val toastMessage = mutableStateOf<String?>(getString(R.string.toast_background_converting))
        setupToastUi(toastMessage)

        // 1차 '변환 중' 알림은 정확히 1000ms(1초) 동안만 표시된 후 사라지도록 자동 종료 타이머 설정
        val dismissConvertingJob = lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            if (toastMessage.value == getString(R.string.toast_background_converting)) {
                toastMessage.value = null
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
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

            // 변환이 1000ms보다 빨리 끝난 경우, 1차 '변환 중' 알림이 1초 동안은 보이도록 최소 표시 시간 보장
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1000) {
                delay(1000 - elapsed)
            }
            dismissConvertingJob.cancel()

            withContext(Dispatchers.Main) {
                if (successCount > 0) {
                    val msg = if (successCount == 1 && lastName != null) {
                        getString(R.string.toast_background_save_complete, lastName)
                    } else {
                        getString(R.string.toast_background_save_multi_complete, successCount)
                    }
                    toastMessage.value = msg

                    if (settings.notifyOnCompletion) {
                        ConversionHelper.sendCompletionNotification(this@ShareHandlerActivity, successCount, lastName)
                    }

                    // 사용자 지침: 2차 '저장 완료' 알림 정확히 1초(1000ms) 유지 후 종료
                    delay(1000)
                    toastMessage.value = null
                    delay(100)
                } else {
                    val errMsg = lastError ?: getString(R.string.status_error)
                    toastMessage.value = getString(R.string.toast_background_convert_error, errMsg)
                    delay(1000)
                    toastMessage.value = null
                    delay(100)
                }
                finish()
            }
        }
    }

    private fun handleInstantReShare(uris: List<Uri>, settings: ConverterSettings) {
        val toastMessage = mutableStateOf<String?>(getString(R.string.toast_background_converting))
        setupToastUi(toastMessage)

        // 1차 '변환 중' 알림은 정확히 1000ms(1초) 동안만 표시된 후 사라지도록 자동 종료 타이머 설정
        val dismissConvertingJob = lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            if (toastMessage.value == getString(R.string.toast_background_converting)) {
                toastMessage.value = null
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
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

            // 변환이 1000ms보다 빨리 끝난 경우, 1차 '변환 중' 알림이 1초 동안은 보이도록 최소 표시 시간 보장
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1000) {
                delay(1000 - elapsed)
            }
            dismissConvertingJob.cancel()

            withContext(Dispatchers.Main) {
                toastMessage.value = null
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
                    toastMessage.value = getString(R.string.toast_background_convert_error, errMsg)
                    delay(1000)
                    toastMessage.value = null
                    delay(100)
                }
                finish()
            }
        }
    }
}
