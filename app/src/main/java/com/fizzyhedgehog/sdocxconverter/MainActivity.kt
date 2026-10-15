package com.fizzyhedgehog.sdocxconverter

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.fizzyhedgehog.sdocxconverter.ui.screens.MainScreen
import com.fizzyhedgehog.sdocxconverter.ui.theme.SDOCXConverterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ConverterViewModel by viewModels()

    private var pendingSinglePdfSourceUri: Uri? = null
    private var pendingBatchSourceUris: List<Uri> = emptyList()

    private val selectFilesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addUris(uris, this)
        }
    }

    private val selectFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.addFolderTreeUri(treeUri, this)
        }
    }

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri: Uri? ->
        val srcUri = pendingSinglePdfSourceUri
        if (destUri != null && srcUri != null) {
            viewModel.exportSinglePdf(srcUri, destUri, this)
        }
        pendingSinglePdfSourceUri = null
    }

    private val exportBatchFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        val sourceUris = pendingBatchSourceUris
        if (treeUri != null && sourceUris.isNotEmpty()) {
            try {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.exportBatchPdfsToFolder(sourceUris, treeUri, this)
        }
        pendingBatchSourceUris = emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 인텐트로 전달된 .sdocx 파일/폴더 처리 (공유하기 또는 연결 프로그램)
        handleIntent(intent)

        setContent {
            SDOCXConverterTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSelectFilesClick = {
                        selectFilesLauncher.launch(arrayOf("*/*"))
                    },
                    onSelectFolderClick = {
                        selectFolderLauncher.launch(null)
                    },
                    onSaveSinglePdf = { srcUri, fileName ->
                        pendingSinglePdfSourceUri = srcUri
                        createDocumentLauncher.launch(fileName)
                    },
                    onSaveBatchPdfs = { uris ->
                        pendingBatchSourceUris = uris
                        exportBatchFolderLauncher.launch(null)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    val mime = contentResolver.getType(uri)
                    if (mime == "vnd.android.document/directory" || DocumentsContract.isTreeUri(uri)) {
                        viewModel.addFolderTreeUri(uri, this)
                    } else {
                        viewModel.addUris(listOf(uri), this)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    val mime = contentResolver.getType(uri)
                    if (mime == "vnd.android.document/directory" || DocumentsContract.isTreeUri(uri)) {
                        viewModel.addFolderTreeUri(uri, this)
                    } else {
                        viewModel.addUris(listOf(uri), this)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    val fileUris = mutableListOf<Uri>()
                    for (u in uris) {
                        val mime = contentResolver.getType(u)
                        if (mime == "vnd.android.document/directory" || DocumentsContract.isTreeUri(u)) {
                            viewModel.addFolderTreeUri(u, this)
                        } else {
                            fileUris.add(u)
                        }
                    }
                    if (fileUris.isNotEmpty()) {
                        viewModel.addUris(fileUris, this)
                    }
                }
            }
        }
    }
}
