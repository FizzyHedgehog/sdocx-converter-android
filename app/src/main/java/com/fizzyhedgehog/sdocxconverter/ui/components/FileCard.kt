package com.fizzyhedgehog.sdocxconverter.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun FileCard(
    item: SdocxFileItem,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sizeText = if (item.sizeBytes > 1024 * 1024) {
        String.format("%.1f MB", item.sizeBytes / (1024.0 * 1024.0))
    } else {
        String.format("%.1f KB", item.sizeBytes / 1024.0)
    }

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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = SamsungOrange,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
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
                                color = SamsungOrange
                            )
                        }
                        ConversionStatus.COMPLETED -> {
                            Text(
                                text = stringResource(R.string.status_completed),
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
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
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (item.status) {
                ConversionStatus.CONVERTING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = SamsungOrange
                    )
                }
                ConversionStatus.COMPLETED -> {
                    if (item.outputPdfUri != null) {
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, item.outputPdfUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "PDF 공유"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "공유",
                                tint = SamsungOrange
                            )
                        }
                    }
                }
                else -> {
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
}
