package com.fizzyhedgehog.sdocxconverter.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fizzyhedgehog.sdocxconverter.ConversionHelper
import com.fizzyhedgehog.sdocxconverter.R
import com.fizzyhedgehog.sdocxconverter.SdocxFileItem
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MergeDialogMode {
    SHARE,
    SAVE_TO_DOWNLOADS
}

/**
 * 2개 이상의 문서를 선택했을 때 '개별 처리' vs '하나로 병합'을 선택하는 다이얼로그
 */
@Composable
fun MultiFileChoiceDialog(
    selectedCount: Int,
    isSaveMode: Boolean = false,
    onDismiss: () -> Unit,
    onIndividualAction: () -> Unit,
    onMergeAction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                if (isSaveMode) Icons.Rounded.Download else Icons.Rounded.Share,
                contentDescription = null,
                tint = SamsungOrange,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(
                    if (isSaveMode) R.string.save_choice_dialog_title else R.string.share_choice_dialog_title
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(
                    if (isSaveMode) R.string.save_choice_dialog_message else R.string.share_choice_dialog_message,
                    selectedCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onMergeAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SamsungOrange)
            ) {
                Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (isSaveMode) R.string.btn_merge_and_save else R.string.btn_merge_and_share
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onIndividualAction,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (isSaveMode) Icons.Rounded.Download else Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (isSaveMode) R.string.btn_save_separately else R.string.btn_share_separately
                    )
                )
            }
        }
    )
}

/**
 * 하나의 PDF로 병합하기 전, 순서를 정렬하고 파일명을 입력받는 다이얼로그
 */
@Composable
fun PdfReorderMergeDialog(
    initialItems: List<SdocxFileItem>,
    mode: MergeDialogMode = MergeDialogMode.SHARE,
    onDismiss: () -> Unit,
    onMergeCompleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var items by remember { mutableStateOf(initialItems) }
    var outputName by remember {
        val firstTitle = initialItems.firstOrNull()?.outputPdfName?.substringBeforeLast(".")
            ?: initialItems.firstOrNull()?.name?.substringBeforeLast(".")
            ?: "document"
        val defaultName = if (initialItems.size > 1) {
            "${firstTitle}_외_${initialItems.size - 1}개_합본.pdf"
        } else {
            "${firstTitle}_합본.pdf"
        }
        mutableStateOf(defaultName)
    }
    var isMerging by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isMerging) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = SamsungOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reorder_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.reorder_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 출력 파일명 입력 필드
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text(stringResource(R.string.output_pdf_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isMerging
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 순서 변경 리스트
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            ReorderItemRow(
                                index = index,
                                totalCount = items.size,
                                item = item,
                                enabled = !isMerging,
                                onMoveUp = {
                                    if (index > 0) {
                                        val mutable = items.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index - 1]
                                        mutable[index - 1] = temp
                                        items = mutable
                                    }
                                },
                                onMoveDown = {
                                    if (index < items.size - 1) {
                                        val mutable = items.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index + 1]
                                        mutable[index + 1] = temp
                                        items = mutable
                                    }
                                }
                            )
                        }
                    }
                }

                if (isMerging) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = SamsungOrange,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.toast_merging_pdf),
                            style = MaterialTheme.typography.bodySmall,
                            color = SamsungOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isMerging) return@Button
                    isMerging = true
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            ConversionHelper.mergePdfItems(
                                context = context,
                                items = items,
                                customOutputName = outputName.trim()
                            )
                        }
                        isMerging = false
                        if (result != null) {
                            if (mode == MergeDialogMode.SHARE) {
                                sharePdf(context, result.uri, result.name)
                            } else {
                                val savedUri = withContext(Dispatchers.IO) {
                                    ConversionHelper.savePdfToDownloads(context, result.file, result.name)
                                }
                                if (savedUri != null) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_merge_saved_downloads),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_merge_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            onMergeCompleted()
                            onDismiss()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_merge_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = !isMerging && items.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SamsungOrange)
            ) {
                Text(
                    text = stringResource(
                        if (mode == MergeDialogMode.SHARE) R.string.btn_confirm_merge_and_share
                        else R.string.btn_confirm_merge_and_save
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isMerging
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun ReorderItemRow(
    index: Int,
    totalCount: Int,
    item: SdocxFileItem,
    enabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SamsungOrange.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SamsungOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.outputPdfName ?: item.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onMoveUp,
                enabled = enabled && index > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = "Move Up",
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled && index > 0) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = enabled && index < totalCount - 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowDownward,
                    contentDescription = "Move Down",
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled && index < totalCount - 1) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                )
            }
        }
    }
}
