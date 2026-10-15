package com.fizzyhedgehog.sdocxconverter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var penScale by remember { mutableStateOf(currentSettings.penScale.toFloat()) }
    var markerScale by remember { mutableStateOf(currentSettings.markerScale.toFloat()) }
    var splitOption by remember { mutableStateOf(currentSettings.splitOption) }
    var autoSplit by remember { mutableStateOf(currentSettings.autoSplit) }
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

                // 2. 페이지 분할 모드 (맥북 버전과 동일한 선택지)
                Text(
                    text = stringResource(R.string.split_mode_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val splitOptions = listOf(
                    "" to stringResource(R.string.split_mode_none),
                    "iso-portrait" to stringResource(R.string.split_mode_portrait),
                    "iso-landscape" to stringResource(R.string.split_mode_landscape)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    splitOptions.forEach { (optionKey, optionLabel) ->
                        val isSelected = splitOption == optionKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { splitOption = optionKey }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { splitOption = optionKey },
                                colors = RadioButtonDefaults.colors(selectedColor = SamsungOrange)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = optionLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 지능형 자동 페이지 분할 토글
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_split_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.auto_split_sublabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = autoSplit,
                        onCheckedChange = { autoSplit = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SamsungOrange,
                            checkedTrackColor = SamsungOrange.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))

                // 3. 저장 위치 설정
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
                            val currentFolderText = defaultSaveTreeName ?: stringResource(R.string.save_folder_source_priority)
                            Text(
                                text = currentFolderText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onSelectDefaultSaveFolderClick,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_select_save_folder), style = MaterialTheme.typography.labelSmall)
                                }
                                if (defaultSaveTreeUri != null) {
                                    TextButton(
                                        onClick = {
                                            defaultSaveTreeUri = null
                                            defaultSaveTreeName = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(stringResource(R.string.btn_reset_save_folder), style = MaterialTheme.typography.labelSmall)
                                    }
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            penScale = penScale.toDouble(),
                            markerScale = markerScale.toDouble(),
                            splitOption = splitOption,
                            autoSplit = autoSplit,
                            autoSave = autoSave,
                            defaultSaveTreeUri = defaultSaveTreeUri,
                            defaultSaveTreeName = defaultSaveTreeName,
                            notifyOnCompletion = notifyOnCompletion
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("확인", color = SamsungOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
