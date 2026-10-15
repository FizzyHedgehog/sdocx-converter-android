package com.fizzyhedgehog.sdocxconverter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fizzyhedgehog.sdocxconverter.ConversionStatus
import com.fizzyhedgehog.sdocxconverter.ConverterViewModel
import com.fizzyhedgehog.sdocxconverter.R
import com.fizzyhedgehog.sdocxconverter.ui.components.ConvertedPdfCard
import com.fizzyhedgehog.sdocxconverter.ui.components.DropZone
import com.fizzyhedgehog.sdocxconverter.ui.components.MultiShareDialog
import com.fizzyhedgehog.sdocxconverter.ui.components.QueueFileCard
import com.fizzyhedgehog.sdocxconverter.ui.components.openPdf
import com.fizzyhedgehog.sdocxconverter.ui.components.openPdfFolder
import com.fizzyhedgehog.sdocxconverter.ui.components.shareMultiplePdfs
import com.fizzyhedgehog.sdocxconverter.ui.components.sharePdf
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange
import com.fizzyhedgehog.sdocxconverter.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ConverterViewModel,
    onSelectFilesClick: () -> Unit,
    onSelectFolderClick: () -> Unit,
    onSaveSinglePdf: (Uri, String) -> Unit = { _, _ -> },
    onSaveBatchPdfs: (List<Uri>) -> Unit = {}
) {
    val context = LocalContext.current
    val fileList by viewModel.fileList.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showMultiShareDialog by remember { mutableStateOf(false) }

    val pendingItems = fileList.filter { it.status != ConversionStatus.COMPLETED }
    val completedItems = fileList.filter { it.status == ConversionStatus.COMPLETED }

    val configuration = LocalConfiguration.current
    val isTabletOrDeX = configuration.screenWidthDp >= 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (fileList.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.btn_clear)
                            )
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.title_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (pendingItems.isNotEmpty() || isConverting) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.convertAll(context) },
                            enabled = !isConverting && pendingItems.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SamsungOrange
                            )
                        ) {
                            if (isConverting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("변환 진행 중…", fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = "${stringResource(R.string.btn_convert_all)} (${pendingItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isTabletOrDeX) {
            // 태블릿 / 삼성 DeX 모드:
            // 좌측(변환 대기열 상단 + 파일 추가 하단 48%) + 구분선 + 우측(변환 완료 파일 52%)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // [좌측 패널]: 처음에 파일 추가만 있고, 파일이 들어가면 위에는 대기열, 아래는 파일 추가
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                ) {
                    if (pendingItems.isEmpty()) {
                        // 대기 중인 파일이 없을 때: 파일 추가 드롭존이 좌측 전체를 시원하게 차지
                        DropZone(
                            onSelectFilesClick = onSelectFilesClick,
                            onSelectFolderClick = onSelectFolderClick,
                            isCompact = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 대기 중인 파일이 있을 때:
                        // 1. 위에는 변환 대기열 목록
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.title_queue)} (${pendingItems.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { viewModel.clearQueue() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_clear_queue),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(pendingItems, key = { it.id }) { item ->
                                QueueFileCard(
                                    item = item,
                                    onRemoveClick = { viewModel.removeFile(item.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. 아래는 파일 추가 (컴팩트 드롭존)
                        DropZone(
                            onSelectFilesClick = onSelectFilesClick,
                            onSelectFolderClick = onSelectFolderClick,
                            isCompact = true
                        )
                    }
                }

                // 중앙 세로 구분선
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )

                // [우측 패널]: 변환 완료 파일 전용 영역
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.title_completed),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (completedItems.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${completedItems.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (completedItems.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { showMultiShareDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SamsungOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.btn_multi_share),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SamsungOrange
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                TextButton(
                                    onClick = { openPdfFolder(context) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.btn_open_folder),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                TextButton(
                                    onClick = { viewModel.clearCompleted() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_clear_completed),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    if (completedItems.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.empty_completed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.empty_completed_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(completedItems, key = { it.id }) { item ->
                                ConvertedPdfCard(
                                    item = item,
                                    onOpenPdf = { item.outputPdfUri?.let { openPdf(context, it) } },
                                    onOpenFolder = { openPdfFolder(context) },
                                    onSavePdf = {
                                        item.outputPdfUri?.let { uri ->
                                            val name = item.outputPdfName ?: (item.name.substringBeforeLast(".") + ".pdf")
                                            onSaveSinglePdf(uri, name)
                                        }
                                    },
                                    onShare = {
                                        item.outputPdfUri?.let {
                                            sharePdf(context, it, item.outputPdfName ?: item.name)
                                        }
                                    },
                                    onRemoveClick = { viewModel.removeFile(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 스마트폰 모드: 세로 1단 레이아웃
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    DropZone(
                        onSelectFilesClick = onSelectFilesClick,
                        onSelectFolderClick = onSelectFolderClick,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }

                if (pendingItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.title_queue)} (${pendingItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { viewModel.clearQueue() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_clear_queue),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    items(pendingItems, key = { it.id }) { item ->
                        QueueFileCard(
                            item = item,
                            onRemoveClick = { viewModel.removeFile(item.id) }
                        )
                    }
                }

                if (completedItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.title_completed)} (${completedItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Row {
                                TextButton(
                                    onClick = { showMultiShareDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = SamsungOrange
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(R.string.btn_multi_share),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SamsungOrange
                                    )
                                }
                                TextButton(
                                    onClick = { openPdfFolder(context) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(R.string.btn_open_folder),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.clearCompleted() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_clear_completed),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    items(completedItems, key = { it.id }) { item ->
                        ConvertedPdfCard(
                            item = item,
                            onOpenPdf = { item.outputPdfUri?.let { openPdf(context, it) } },
                            onOpenFolder = { openPdfFolder(context) },
                            onSavePdf = {
                                item.outputPdfUri?.let { uri ->
                                    val name = item.outputPdfName ?: (item.name.substringBeforeLast(".") + ".pdf")
                                    onSaveSinglePdf(uri, name)
                                }
                            },
                            onShare = {
                                item.outputPdfUri?.let {
                                    sharePdf(context, it, item.outputPdfName ?: item.name)
                                }
                            },
                            onRemoveClick = { viewModel.removeFile(item.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentSettings = settings,
            onDismiss = { showSettings = false },
            onSave = { viewModel.updateSettings(it) }
        )
    }

    if (showMultiShareDialog) {
        MultiShareDialog(
            completedItems = completedItems,
            onDismiss = { showMultiShareDialog = false },
            onShare = { selectedUris ->
                shareMultiplePdfs(context, selectedUris)
            },
            onSaveToFolder = { selectedUris ->
                onSaveBatchPdfs(selectedUris)
            }
        )
    }
}
