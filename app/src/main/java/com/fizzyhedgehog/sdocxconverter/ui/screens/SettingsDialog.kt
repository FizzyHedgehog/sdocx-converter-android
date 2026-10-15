package com.fizzyhedgehog.sdocxconverter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fizzyhedgehog.sdocxconverter.ConverterSettings
import com.fizzyhedgehog.sdocxconverter.R
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange

@Composable
fun SettingsDialog(
    currentSettings: ConverterSettings,
    onDismiss: () -> Unit,
    onSave: (ConverterSettings) -> Unit,
    onSelectDefaultSaveFolderClick: () -> Unit = {}
) {
    val context = LocalContext.current
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
                                "📂 ${defaultSaveTreeName ?: defaultFolderFallback}"
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

                // 5. 오픈소스 및 저작권 정보
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
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_version_label, appVersionName),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
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
                            notifyOnCompletion = notifyOnCompletion
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
}
