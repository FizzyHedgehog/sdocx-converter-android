package com.fizzyhedgehog.sdocxconverter.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fizzyhedgehog.sdocxconverter.R
import com.fizzyhedgehog.sdocxconverter.ui.theme.SamsungOrange

@Composable
fun DropZone(
    onSelectFilesClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = SamsungOrange.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelectFilesClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 14.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = null,
                tint = SamsungOrange,
                modifier = Modifier.size(if (isCompact) 36.dp else 48.dp)
            )
            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))
            Text(
                text = stringResource(R.string.drop_hint),
                style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.drop_subhint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
