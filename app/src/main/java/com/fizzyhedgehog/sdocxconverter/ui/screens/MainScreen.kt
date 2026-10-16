package com.fizzyhedgehog.sdocxconverter.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.fizzyhedgehog.sdocxconverter.ConversionStatus
import com.fizzyhedgehog.sdocxconverter.ConverterViewModel
import com.fizzyhedgehog.sdocxconverter.R
import com.fizzyhedgehog.sdocxconverter.UpdateInfo
import com.fizzyhedgehog.sdocxconverter.UpdateManager
import com.fizzyhedgehog.sdocxconverter.ui.components.AddSourceDialog
import com.fizzyhedgehog.sdocxconverter.ui.components.BatchSaveDialog
import com.fizzyhedgehog.sdocxconverter.ui.components.ConvertedPdfCard
import com.fizzyhedgehog.sdocxconverter.ui.components.DropZone
import com.fizzyhedgehog.sdocxconverter.ui.components.QueueFileCard
import com.fizzyhedgehog.sdocxconverter.ui.components.UpdateAvailableDialog
import com.fizzyhedgehog.sdocxconverter.ui.components.UpdateDownloadingDialog
import com.fizzyhedgehog.sdocxconverter.ui.components.openPdf
import com.fizzyhedgehog.sdocxconverter.ui.components.openPdfFolder
import com.fizzyhedgehog.sdocxconverter.ui.components.shareMultiplePdfs
import com.fizzyhedgehog.sdocxconverter.ui.components.sharePdf
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange
import com.fizzyhedgehog.sdocxconverter.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ConverterViewModel,
    onSelectFilesClick: () -> Unit,
    onSelectFolderClick: () -> Unit,
    onSaveSinglePdf: (Uri, String) -> Unit = { _, _ -> },
    onSaveBatchPdfs: (List<Uri>) -> Unit = {},
    onSelectDefaultSaveFolderClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val fileList by viewModel.fileList.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showBatchSaveDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showAddSourceDialog by remember { mutableStateOf(false) }

    var autoUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloadingAutoUpdate by remember { mutableStateOf(false) }
    var autoDownloadProgress by remember { mutableStateOf(0f) }
    var autoDownloadedBytes by remember { mutableStateOf(0L) }
    var autoTotalBytes by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    // 앱 시작 시 설정이 켜져 있는 경우에만 백그라운드에서 조용히 GitHub 최신 버전 확인
    LaunchedEffect(Unit) {
        val currentSettings = ConversionHelper.loadSettings(context)
        if (currentSettings.autoCheckUpdate) {
            val result = UpdateManager.checkUpdate(context)
            result.onSuccess { info ->
                if (info.hasUpdate) {
                    autoUpdateInfo = info
                }
            }
        }
    }

    val pendingItems = fileList.filter { it.status != ConversionStatus.COMPLETED }
    val completedItems = fileList.filter { it.status == ConversionStatus.COMPLETED }

    LaunchedEffect(completedItems) {
        if (completedItems.isEmpty()) {
            isSelectionMode = false
            selectedIds = emptySet()
        } else {
            selectedIds = selectedIds.filter { id -> completedItems.any { it.id == id } }.toSet()
        }
    }

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
                                Text(stringResource(R.string.btn_converting_progress), fontWeight = FontWeight.Bold)
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
                            onCardClick = { showAddSourceDialog = true },
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

                        // 2. 대기열 목록 및 우측 하단 동그라미 안 '+' 버튼
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 76.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(pendingItems, key = { it.id }) { item ->
                                    QueueFileCard(
                                        item = item,
                                        onRemoveClick = { viewModel.removeFile(item.id) }
                                    )
                                }
                            }

                            FloatingActionButton(
                                onClick = { showAddSourceDialog = true },
                                shape = CircleShape,
                                containerColor = SamsungOrange,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 8.dp, bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.action_add_source),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!isSelectionMode) {
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
                                            onClick = {
                                                isSelectionMode = true
                                                selectedIds = emptySet()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_select),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SamsungOrange
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        TextButton(
                                            onClick = { showClearCompletedDialog = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_clear_completed),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.items_selected_count, selectedIds.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SamsungOrange
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            selectedIds = if (selectedIds.size == completedItems.size) {
                                                emptySet()
                                            } else {
                                                completedItems.map { it.id }.toSet()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (selectedIds.size == completedItems.size) stringResource(R.string.btn_deselect_all) else stringResource(R.string.btn_select_all),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    TextButton(
                                        onClick = {
                                            isSelectionMode = false
                                            selectedIds = emptySet()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.btn_cancel),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        if (isSelectionMode && completedItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 윗줄: 다운로드에 저장 / 공유
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val uris = completedItems.filter { selectedIds.contains(it.id) }.mapNotNull { it.outputPdfUri }
                                            if (uris.isNotEmpty()) {
                                                viewModel.exportBatchPdfsToDownloads(uris, context)
                                                isSelectionMode = false
                                                selectedIds = emptySet()
                                            }
                                        },
                                        enabled = selectedIds.isNotEmpty(),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = SamsungOrange)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.btn_save_to_downloads), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val uris = completedItems.filter { selectedIds.contains(it.id) }.mapNotNull { it.outputPdfUri }
                                            if (uris.isNotEmpty()) shareMultiplePdfs(context, uris)
                                        },
                                        enabled = selectedIds.isNotEmpty(),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SamsungOrange),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.btn_share), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 아랫줄: 저장할 폴더 선택 (파일 모양) / 삭제
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val uris = completedItems.filter { selectedIds.contains(it.id) }.mapNotNull { it.outputPdfUri }
                                            if (uris.isNotEmpty()) {
                                                onSaveBatchPdfs(uris)
                                                isSelectionMode = false
                                                selectedIds = emptySet()
                                            }
                                        },
                                        enabled = selectedIds.isNotEmpty(),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = SamsungOrange)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.btn_save_to_custom_folder), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }

                                    FilledTonalButton(
                                        onClick = { showDeleteSelectedDialog = true },
                                        enabled = selectedIds.isNotEmpty(),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.btn_delete_selected), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
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
                                val isSelected = selectedIds.contains(item.id)
                                ConvertedPdfCard(
                                    item = item,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onToggleSelect = {
                                        selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                                    },
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
                                    onRemoveClick = {
                                        selectedIds = selectedIds - item.id
                                        viewModel.removeFile(item.id)
                                    }
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
                        onCardClick = { showAddSourceDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .defaultMinSize(minHeight = 160.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showAddSourceDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.action_add_source),
                                        tint = SamsungOrange
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (!isSelectionMode) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.title_completed),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SuccessGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${completedItems.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                isSelectionMode = true
                                                selectedIds = emptySet()
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_select),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SamsungOrange
                                            )
                                        }
                                        TextButton(
                                            onClick = { showClearCompletedDialog = true },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_clear_completed),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.items_selected_count, selectedIds.size),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SamsungOrange
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                selectedIds = if (selectedIds.size == completedItems.size) {
                                                    emptySet()
                                                } else {
                                                    completedItems.map { it.id }.toSet()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (selectedIds.size == completedItems.size) stringResource(R.string.btn_deselect_all) else stringResource(R.string.btn_select_all),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                isSelectionMode = false
                                                selectedIds = emptySet()
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_cancel),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }

                            if (isSelectionMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // 윗줄: 다운로드에 저장 / 공유
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                val uris = completedItems.filter { selectedIds.contains(it.id) }.mapNotNull { it.outputPdfUri }
                                                if (uris.isNotEmpty()) {
                                                    viewModel.exportBatchPdfsToDownloads(uris, context)
                                                    isSelectionMode = false
                                                    selectedIds = emptySet()
                                                }
                                            },
                                            enabled = selectedIds.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp), tint = SamsungOrange)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.btn_save_to_downloads), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val uris = completedItems.filter { selectedIds.contains(it.id) }.mapNotNull { it.outputPdfUri }
                                                if (uris.isNotEmpty()) shareMultiplePdfs(context, uris)
                                            },
                                            enabled = selectedIds.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SamsungOrange),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.btn_share), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // 아랫줄: 저장할 폴더 선택 (파일 모양) / 삭제
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val uris = completedItems.filter { selectedIds.contains(it.id) }.mapNotNull { it.outputPdfUri }
                                                if (uris.isNotEmpty()) {
                                                    onSaveBatchPdfs(uris)
                                                    isSelectionMode = false
                                                    selectedIds = emptySet()
                                                }
                                            },
                                            enabled = selectedIds.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp), tint = SamsungOrange)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.btn_save_to_custom_folder), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = { showDeleteSelectedDialog = true },
                                            enabled = selectedIds.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            ),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.btn_delete_selected), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(completedItems, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        ConvertedPdfCard(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                            },
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
                            onRemoveClick = {
                                selectedIds = selectedIds - item.id
                                viewModel.removeFile(item.id)
                            }
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
            onSave = { viewModel.updateSettings(context, it) },
            onSelectDefaultSaveFolderClick = onSelectDefaultSaveFolderClick
        )
    }

    if (showBatchSaveDialog) {
        val uris = completedItems.filter { selectedIds.contains(it.id) }.mapNotNull { it.outputPdfUri }
        BatchSaveDialog(
            selectedCount = uris.size,
            onDismiss = { showBatchSaveDialog = false },
            onSaveToDownloads = {
                showBatchSaveDialog = false
                viewModel.exportBatchPdfsToDownloads(uris, context)
                isSelectionMode = false
                selectedIds = emptySet()
            },
            onSaveToCustomFolder = {
                showBatchSaveDialog = false
                onSaveBatchPdfs(uris)
                isSelectionMode = false
                selectedIds = emptySet()
            },
            defaultSaveTreeName = settings.defaultSaveTreeName,
            onSaveToDefaultFolder = if (settings.defaultSaveTreeUri != null) {
                {
                    showBatchSaveDialog = false
                    val targetTree = Uri.parse(settings.defaultSaveTreeUri)
                    viewModel.exportBatchPdfsToFolder(uris, targetTree, context)
                    isSelectionMode = false
                    selectedIds = emptySet()
                }
            } else null
        )
    }

    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.clear_completed_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.clear_completed_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCompletedDialog = false
                        viewModel.clearCompleted()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.btn_dialog_confirm_select),
                        color = SamsungOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedDialog = false }) {
                    Text(stringResource(R.string.btn_dialog_cancel))
                }
            }
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_selected_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.delete_selected_dialog_message, selectedIds.size))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedDialog = false
                        val idsToDelete = selectedIds.toList()
                        idsToDelete.forEach { id -> viewModel.removeFile(id) }
                        selectedIds = emptySet()
                        isSelectionMode = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.btn_dialog_confirm_select),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(stringResource(R.string.btn_dialog_cancel))
                }
            }
        )
    }

    if (showAddSourceDialog) {
        AddSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onSelectFiles = onSelectFilesClick,
            onSelectFolder = onSelectFolderClick
        )
    }

    autoUpdateInfo?.let { updateInfo ->
        UpdateAvailableDialog(
            updateInfo = updateInfo,
            onDismiss = { autoUpdateInfo = null },
            onUpdateClick = {
                val apkUrl = updateInfo.apkDownloadUrl
                if (!apkUrl.isNullOrEmpty()) {
                    isDownloadingAutoUpdate = true
                    autoDownloadProgress = 0f
                    scope.launch {
                        val dlResult = UpdateManager.downloadApk(
                            context = context,
                            downloadUrl = apkUrl,
                            versionName = updateInfo.latestVersion,
                            onProgress = { p, d, t ->
                                autoDownloadProgress = p
                                autoDownloadedBytes = d
                                autoTotalBytes = t
                            }
                        )
                        isDownloadingAutoUpdate = false
                        dlResult.onSuccess { apkFile ->
                            autoUpdateInfo = null
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

    if (isDownloadingAutoUpdate) {
        UpdateDownloadingDialog(
            progress = autoDownloadProgress,
            downloadedBytes = autoDownloadedBytes,
            totalBytes = autoTotalBytes
        )
    }
}
