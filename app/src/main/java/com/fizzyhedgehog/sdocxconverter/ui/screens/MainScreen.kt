package com.fizzyhedgehog.sdocxconverter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
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
import com.fizzyhedgehog.sdocxconverter.ui.components.QueueFileCard
import com.fizzyhedgehog.sdocxconverter.ui.components.openPdf
import com.fizzyhedgehog.sdocxconverter.ui.components.openPdfFolder
import com.fizzyhedgehog.sdocxconverter.ui.components.sharePdf
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange
import com.fizzyhedgehog.sdocxconverter.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ConverterViewModel,
    onSelectFilesClick: () -> Unit
) {
    val context = LocalContext.current
    val fileList by viewModel.fileList.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

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
            if (fileList.isNotEmpty()) {
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
                                containerColor = SamsungOrange,
                                disabledContainerColor = if (pendingItems.isEmpty() && completedItems.isNotEmpty()) {
                                    SuccessGreen.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
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
                            } else if (pendingItems.isNotEmpty()) {
                                Text(
                                    text = "${stringResource(R.string.btn_convert_all)} (${pendingItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.btn_all_completed)} (${completedItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isTabletOrDeX) {
            // 태블릿 / 삼성 DeX 모드: 좌측(드롭존 42%) + 우측 2단 분할(대기열 50% / 완료파일 50%)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 좌측 패널: 드롭존 및 파일 선택
                Box(modifier = Modifier.weight(0.42f)) {
                    DropZone(
                        onSelectFilesClick = onSelectFilesClick,
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                // 우측 패널: 상단(변환 대기열) + 하단(변환 완료 파일)
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. 상단: 변환 대기열 (Top 50%)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
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
                            if (pendingItems.isNotEmpty()) {
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
                        }

                        if (pendingItems.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = stringResource(R.string.empty_queue),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(pendingItems, key = { it.id }) { item ->
                                    QueueFileCard(
                                        item = item,
                                        onRemoveClick = { viewModel.removeFile(item.id) }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    // 2. 하단: 변환 완료 파일 (Bottom 50%)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
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
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (completedItems.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = stringResource(R.string.empty_completed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(completedItems, key = { it.id }) { item ->
                                    ConvertedPdfCard(
                                        item = item,
                                        onOpenPdf = { item.outputPdfUri?.let { openPdf(context, it) } },
                                        onOpenFolder = { openPdfFolder(context) },
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
                    DropZone(onSelectFilesClick = onSelectFilesClick)
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
}
