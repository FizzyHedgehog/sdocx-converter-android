package com.fizzyhedgehog.sdocxconverter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fizzyhedgehog.sdocxconverter.ConversionHelper
import com.fizzyhedgehog.sdocxconverter.ConverterSettings
import com.fizzyhedgehog.sdocxconverter.R
import com.fizzyhedgehog.sdocxconverter.ShareReceiveAction
import com.fizzyhedgehog.sdocxconverter.UpdateInfo
import com.fizzyhedgehog.sdocxconverter.UpdateManager
import com.fizzyhedgehog.sdocxconverter.ui.components.UpdateAvailableDialog
import com.fizzyhedgehog.sdocxconverter.ui.components.UpdateDownloadingDialog
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    currentSettings: ConverterSettings,
    onDismiss: () -> Unit,
    onSave: (ConverterSettings) -> Unit,
    onSelectDefaultSaveFolderClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVersionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
    var penScale by remember { mutableStateOf(currentSettings.penScale.toFloat()) }
    var markerScale by remember { mutableStateOf(currentSettings.markerScale.toFloat()) }
    var autoSave by remember { mutableStateOf(currentSettings.autoSave) }
    var defaultSaveTreeUri by remember { mutableStateOf(currentSettings.defaultSaveTreeUri) }
    var defaultSaveTreeName by remember { mutableStateOf(currentSettings.defaultSaveTreeName) }
    var notifyOnCompletion by remember { mutableStateOf(currentSettings.notifyOnCompletion) }
    var shareReceiveAction by remember { mutableStateOf(currentSettings.shareReceiveAction) }
    var autoCheckUpdate by remember { mutableStateOf(currentSettings.autoCheckUpdate) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(0L) }

    val prefs = remember(context) {
        context.getSharedPreferences(ConversionHelper.PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }
    var developerModeEnabled by remember {
        mutableStateOf(prefs.getBoolean("developer_mode_enabled", false))
    }
    var devPrereleaseChannel by remember {
        mutableStateOf(prefs.getBoolean("dev_prerelease_channel", true))
    }
    var devBypassVersionCheck by remember {
        mutableStateOf(prefs.getBoolean("dev_bypass_version_check", false))
    }

    var devTapCount by remember { mutableIntStateOf(0) }
    var lastDevTapTime by remember { mutableLongStateOf(0L) }
    var showPinDialog by remember { mutableStateOf(false) }
    var currentDevToast by remember { mutableStateOf<Toast?>(null) }

    // 외부에서 defaultSaveTreeUri가 갱신되면 동기화
    LaunchedEffect(currentSettings.defaultSaveTreeUri, currentSettings.defaultSaveTreeName) {
        defaultSaveTreeUri = currentSettings.defaultSaveTreeUri
        defaultSaveTreeName = currentSettings.defaultSaveTreeName
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.title_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 펜 굵기 배율 (기본 1.5x)
                Text(
                    text = "${stringResource(R.string.pen_scale_label)}: ${String.format("%.1fx", penScale)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = penScale,
                    onValueChange = { penScale = it },
                    valueRange = 0.5f..3.0f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = SamsungOrange,
                        activeTrackColor = SamsungOrange
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 형광펜 굵기 배율 (기본 1.0x)
                Text(
                    text = "${stringResource(R.string.marker_scale_label)}: ${String.format("%.1fx", markerScale)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = markerScale,
                    onValueChange = { markerScale = it },
                    valueRange = 0.5f..3.0f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = SamsungOrange,
                        activeTrackColor = SamsungOrange
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))

                // 2. 저장 위치 설정
                Text(
                    text = stringResource(R.string.section_storage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_save_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.auto_save_sublabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = autoSave,
                        onCheckedChange = { autoSave = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SamsungOrange,
                            checkedTrackColor = SamsungOrange.copy(alpha = 0.5f)
                        )
                    )
                }

                if (autoSave) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.save_folder_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val defaultFolderFallback = stringResource(R.string.specified_folder)
                            val currentFolderText = if (defaultSaveTreeUri != null) {
                                defaultSaveTreeName ?: defaultFolderFallback
                            } else {
                                stringResource(R.string.downloads_folder_label)
                            }
                            Text(
                                text = currentFolderText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (defaultSaveTreeUri != null) MaterialTheme.colorScheme.onSurface else SamsungOrange
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        defaultSaveTreeUri = null
                                        defaultSaveTreeName = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = if (defaultSaveTreeUri == null) {
                                        ButtonDefaults.buttonColors(containerColor = SamsungOrange)
                                    } else {
                                        ButtonDefaults.filledTonalButtonColors()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_save_folder_downloads), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onSelectDefaultSaveFolderClick,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_select_another_folder), style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.saf_downloads_warning),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))

                // 3. 외부 공유 수신 시 동작 설정
                Text(
                    text = stringResource(R.string.section_share_action),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.section_share_action_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 옵션 1: 기본 동작 (앱 열기)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { shareReceiveAction = ShareReceiveAction.OPEN_APP }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (shareReceiveAction == ShareReceiveAction.OPEN_APP),
                        onClick = { shareReceiveAction = ShareReceiveAction.OPEN_APP },
                        colors = RadioButtonDefaults.colors(selectedColor = SamsungOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.share_action_open_app_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.share_action_open_app_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 옵션 2: 백그라운드 무음 자동 저장
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { shareReceiveAction = ShareReceiveAction.AUTO_SAVE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (shareReceiveAction == ShareReceiveAction.AUTO_SAVE),
                        onClick = { shareReceiveAction = ShareReceiveAction.AUTO_SAVE },
                        colors = RadioButtonDefaults.colors(selectedColor = SamsungOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.share_action_auto_save_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.share_action_auto_save_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 옵션 3: 변환 후 즉시 PDF 재공유
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { shareReceiveAction = ShareReceiveAction.RE_SHARE }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (shareReceiveAction == ShareReceiveAction.RE_SHARE),
                        onClick = { shareReceiveAction = ShareReceiveAction.RE_SHARE },
                        colors = RadioButtonDefaults.colors(selectedColor = SamsungOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.share_action_re_share_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.share_action_re_share_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))

                // 4. 알림 설정
                Text(
                    text = stringResource(R.string.section_notification),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notify_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.notify_sublabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = notifyOnCompletion,
                        onCheckedChange = { notifyOnCompletion = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SamsungOrange,
                            checkedTrackColor = SamsungOrange.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // 5. 소프트웨어 업데이트 (Software Update)
                Text(
                    text = stringResource(R.string.section_software_update),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Upper row: Current version + [업데이트 확인] button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (developerModeEnabled) {
                                            currentDevToast?.cancel()
                                            currentDevToast = Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_dev_already_enabled),
                                                Toast.LENGTH_SHORT
                                            ).also { it.show() }
                                            return@clickable
                                        }

                                        val now = System.currentTimeMillis()
                                        if (now - lastDevTapTime > 3000L) {
                                            devTapCount = 0
                                        }
                                        lastDevTapTime = now
                                        devTapCount++

                                        val remaining = 7 - devTapCount
                                        if (remaining in 1..3) {
                                            currentDevToast?.cancel()
                                            currentDevToast = Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_dev_remaining, remaining),
                                                Toast.LENGTH_SHORT
                                            ).also { it.show() }
                                        } else if (remaining <= 0) {
                                            devTapCount = 0
                                            currentDevToast?.cancel()
                                            showPinDialog = true
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.current_version_format, appVersionName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (availableUpdate != null) {
                                    Text(
                                        text = stringResource(R.string.update_available_version, availableUpdate?.latestVersion ?: "", appVersionName),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SamsungOrange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = {
                                    if (!isCheckingUpdate && !isDownloadingUpdate) {
                                        isCheckingUpdate = true
                                        scope.launch {
                                            val result = UpdateManager.checkUpdate(context)
                                            isCheckingUpdate = false
                                            result.onSuccess { info ->
                                                if (info.hasUpdate) {
                                                    availableUpdate = info
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.latest_version_toast, info.currentVersion),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }.onFailure { err ->
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.update_check_failed),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isCheckingUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = SamsungOrange
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.checking_updates),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SamsungOrange
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.check_for_updates),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SamsungOrange,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Lower row: Checkbox + Description (Matching user's image)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { autoCheckUpdate = !autoCheckUpdate }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = autoCheckUpdate,
                                onCheckedChange = { autoCheckUpdate = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SamsungOrange,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.opt_auto_check_update),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.opt_auto_check_update_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // 6. 오픈소스 및 저작권 정보
                Text(
                    text = stringResource(R.string.title_about),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.license_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (developerModeEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 7. 개발자 옵션
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = null,
                                tint = SamsungOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.section_developer),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SamsungOrange
                            )
                        }
                        Switch(
                            checked = developerModeEnabled,
                            onCheckedChange = { isEnabled ->
                                developerModeEnabled = isEnabled
                                prefs.edit().putBoolean("developer_mode_enabled", isEnabled).apply()
                                if (!isEnabled) {
                                    Toast.makeText(context, context.getString(R.string.toast_dev_disabled), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SamsungOrange,
                                checkedTrackColor = SamsungOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pre-release 채널 스위치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dev_prerelease_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.dev_prerelease_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = devPrereleaseChannel,
                            onCheckedChange = { checked ->
                                devPrereleaseChannel = checked
                                prefs.edit().putBoolean("dev_prerelease_channel", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SamsungOrange,
                                checkedTrackColor = SamsungOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 동일 버전 재설치 허용 (Bypass Version Check)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dev_bypass_version_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.dev_bypass_version_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = devBypassVersionCheck,
                            onCheckedChange = { checked ->
                                devBypassVersionCheck = checked
                                prefs.edit().putBoolean("dev_bypass_version_check", checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SamsungOrange,
                                checkedTrackColor = SamsungOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 강제 업데이트 시뮬레이션 버튼
                    OutlinedButton(
                        onClick = {
                            availableUpdate = UpdateInfo(
                                hasUpdate = true,
                                latestVersion = context.getString(R.string.dev_simulated_version),
                                currentVersion = appVersionName,
                                releaseTitle = context.getString(R.string.dev_simulated_title),
                                releaseNotes = context.getString(R.string.dev_simulated_notes),
                                apkDownloadUrl = "https://github.com/FizzyHedgehog/sdocx-converter-android/releases/download/v1.0.0/SDOCX_Converter.apk",
                                apkSize = 3894720L
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.dev_simulate_update_btn), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            penScale = penScale.toDouble(),
                            markerScale = markerScale.toDouble(),
                            splitOption = currentSettings.splitOption,
                            autoSplit = currentSettings.autoSplit,
                            autoSave = autoSave,
                            defaultSaveTreeUri = defaultSaveTreeUri,
                            defaultSaveTreeName = defaultSaveTreeName,
                            notifyOnCompletion = notifyOnCompletion,
                            shareReceiveAction = shareReceiveAction,
                            autoCheckUpdate = autoCheckUpdate
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_ok), color = SamsungOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )

    availableUpdate?.let { updateInfo ->
        UpdateAvailableDialog(
            updateInfo = updateInfo,
            onDismiss = { availableUpdate = null },
            onUpdateClick = {
                val apkUrl = updateInfo.apkDownloadUrl
                if (!apkUrl.isNullOrEmpty()) {
                    isDownloadingUpdate = true
                    downloadProgress = 0f
                    scope.launch {
                        val dlResult = UpdateManager.downloadApk(
                            context = context,
                            downloadUrl = apkUrl,
                            versionName = updateInfo.latestVersion,
                            onProgress = { p, d, t ->
                                downloadProgress = p
                                downloadedBytes = d
                                totalBytes = t
                            }
                        )
                        isDownloadingUpdate = false
                        dlResult.onSuccess { apkFile ->
                            availableUpdate = null
                            UpdateManager.installApk(context, apkFile)
                        }.onFailure { err ->
                            Toast.makeText(
                                context,
                                context.getString(R.string.download_failed, err.message ?: ""),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )
    }

    if (isDownloadingUpdate) {
        UpdateDownloadingDialog(
            progress = downloadProgress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes
        )
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_pin_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_pin_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text(stringResource(R.string.dialog_pin_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (pinInput == "3208") {
                                    developerModeEnabled = true
                                    prefs.edit().putBoolean("developer_mode_enabled", true).apply()
                                    showPinDialog = false
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_dev_enabled),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    pinError = true
                                }
                            }
                        ),
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text(stringResource(R.string.dialog_pin_wrong), color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == "3208") {
                            developerModeEnabled = true
                            prefs.edit().putBoolean("developer_mode_enabled", true).apply()
                            showPinDialog = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_dev_enabled),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SamsungOrange)
                ) {
                    Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
