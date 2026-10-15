package com.fizzyhedgehog.sdocxconverter.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.fizzyhedgehog.sdocxconverter.ConversionStatus
import com.fizzyhedgehog.sdocxconverter.R
import com.fizzyhedgehog.sdocxconverter.SdocxFileItem
import com.fizzyhedgehog.sdocxconverter.ui.theme.ErrorRed
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange
import com.fizzyhedgehog.sdocxconverter.ui.theme.SuccessGreen

fun formatFileSize(bytes: Long): String {
    return if (bytes > 1024 * 1024) {
        String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    } else {
        String.format("%.1f KB", bytes / 1024.0)
    }
}

fun openPdf(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.btn_open_pdf)))
        }
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.open_pdf_error), Toast.LENGTH_SHORT).show()
    }
}

fun openPdfFolder(context: Context) {
    var opened = false
    val folderPath = "/storage/emulated/0/Download/SDOCX_PDF"

    // 1. 삼성 갤럭시 '내 파일' 전용 인텐트 (갤럭시 탭 최우선 지원)
    try {
        val samsungIntent = Intent("samsung.myfiles.intent.action.VIEW_PATH").apply {
            putExtra("samsung.myfiles.intent.extra.START_PATH", folderPath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(samsungIntent)
        opened = true
    } catch (_: Exception) {}

    // 2. SAF 특정 폴더(SDOCX_PDF) 열기
    if (!opened) {
        try {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FSDOCX_PDF")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            opened = true
        } catch (_: Exception) {}
    }

    // 3. 다운로드 앱 열기
    if (!opened) {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            opened = true
        } catch (_: Exception) {}
    }

    // 4. 일반 MediaStore 다운로드 URI
    if (!opened) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(MediaStore.Downloads.EXTERNAL_CONTENT_URI, "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            opened = true
        } catch (_: Exception) {}
    }

    if (!opened) {
        Toast.makeText(context, context.getString(R.string.toast_pdf_folder_hint), Toast.LENGTH_SHORT).show()
    }
}

fun sharePdf(context: Context, uri: Uri, title: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    } catch (e: Exception) {
        Toast.makeText(context, "공유 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun shareMultiplePdfs(context: Context, uris: List<Uri>, title: String = "PDF 파일 일괄 공유") {
    if (uris.isEmpty()) {
        Toast.makeText(context, "공유할 파일이 선택되지 않았습니다.", Toast.LENGTH_SHORT).show()
        return
    }

    if (uris.size == 1) {
        sharePdf(context, uris.first(), title)
        return
    }

    try {
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    } catch (e: Exception) {
        Toast.makeText(context, "다중 공유 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun QueueFileCard(
    item: SdocxFileItem,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeText = formatFileSize(item.sizeBytes)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = "SDOCX",
                    tint = SamsungOrange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    when (item.status) {
                        ConversionStatus.PENDING -> {
                            Text(
                                text = stringResource(R.string.status_pending),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ConversionStatus.CONVERTING -> {
                            Text(
                                text = stringResource(R.string.status_converting),
                                style = MaterialTheme.typography.labelSmall,
                                color = SamsungOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        ConversionStatus.ERROR -> {
                            Text(
                                text = item.errorMessage ?: stringResource(R.string.status_error),
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed
                            )
                        }
                        ConversionStatus.COMPLETED -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (item.status == ConversionStatus.CONVERTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = SamsungOrange
                )
            } else {
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ConvertedPdfCard(
    item: SdocxFileItem,
    onOpenPdf: () -> Unit,
    onOpenFolder: () -> Unit,
    onShare: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSavePdf: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val pdfDisplayName = item.outputPdfName ?: (item.name.substringBeforeLast(".") + ".pdf")
    val sizeText = formatFileSize(if (item.outputPdfSizeBytes > 0) item.outputPdfSizeBytes else item.sizeBytes)
    val savedStatusText = if (item.isSavedToDevice) " · 저장됨" else ""
    var showUnsavedDialog by remember { mutableStateOf(false) }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("기기 미저장", fontWeight = FontWeight.Bold) },
            text = { Text("기기에 저장되어 있지 않습니다. 저장할 위치를 정해주세요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onSavePdf?.invoke()
                    }
                ) {
                    Text("저장 위치 정하기", color = SamsungOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) {
                    onToggleSelect()
                } else {
                    onOpenPdf()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectionMode && isSelected) {
                SamsungOrange.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SamsungOrange,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdfDisplayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.status_completed),
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "·  $sizeText$savedStatusText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            if (!isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (item.isSavedToDevice) {
                                onOpenFolder()
                            } else {
                                showUnsavedDialog = true
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = stringResource(R.string.btn_open_folder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onSavePdf != null) {
                        IconButton(
                            onClick = onSavePdf,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SaveAlt,
                                contentDescription = stringResource(R.string.btn_save_single),
                                tint = SamsungOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.btn_share),
                            tint = SamsungOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileCard(
    item: SdocxFileItem,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (item.status == ConversionStatus.COMPLETED) {
        ConvertedPdfCard(
            item = item,
            onOpenPdf = { item.outputPdfUri?.let { openPdf(context, it) } },
            onOpenFolder = { openPdfFolder(context) },
            onShare = { item.outputPdfUri?.let { sharePdf(context, it, item.outputPdfName ?: item.name) } },
            onRemoveClick = onRemoveClick,
            modifier = modifier
        )
    } else {
        QueueFileCard(
            item = item,
            onRemoveClick = onRemoveClick,
            modifier = modifier
        )
    }
}
