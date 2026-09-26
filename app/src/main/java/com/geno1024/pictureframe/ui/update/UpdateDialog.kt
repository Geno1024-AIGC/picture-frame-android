package com.geno1024.pictureframe.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geno1024.pictureframe.ui.components.ChoiceChip
import com.geno1024.pictureframe.ui.components.OutlineAction
import com.geno1024.pictureframe.ui.components.PrimaryAction

data class UpdateUiState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    /** -1 when the mirror did not report a usable content length. */
    val progress: Float = -1f,
    val status: String? = null,
    val error: String? = null,
    val needsInstallPermission: Boolean = false,
)

@Composable
fun UpdateDialog(
    currentRun: Int,
    availableRun: Int,
    infoLabel: String,
    state: UpdateUiState,
    mirrorOptions: List<Pair<String, String>>,
    selectedMirrorPrefix: String,
    onSelectMirror: (String) -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onGrantInstallPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    val busy = state.checking || state.downloading
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("检查更新") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "当前构建 #$currentRun" +
                        if (availableRun > 0) "，最新 #$availableRun" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (infoLabel.isNotEmpty()) {
                    Text(
                        text = infoLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (state.needsInstallPermission) {
                    Text(
                        text = "需要先允许安装未知来源应用。APK 已下载完成，授权后即可直接安装。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryAction("去授权", onClick = onGrantInstallPermission)
                        OutlineAction("稍后", onClick = onDismiss)
                    }
                }

                if (state.progress > 0f) {
                    Text(
                        text = "正在下载… ${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Both live here rather than in a snackbar, because the dialog
                // draws on top of the scaffold's host and would hide it.
                state.status?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "下载源",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    mirrorOptions.forEach { (label, prefix) ->
                        ChoiceChip(
                            label = label,
                            selected = prefix == selectedMirrorPrefix,
                            onClick = { onSelectMirror(prefix) },
                        )
                    }
                    var custom by remember(selectedMirrorPrefix) {
                        mutableStateOf(selectedMirrorPrefix)
                    }
                    OutlinedTextField(
                        value = custom,
                        onValueChange = {
                            custom = it
                            onSelectMirror(it)
                        },
                        label = { Text("自定义前缀") },
                        placeholder = { Text("https://gh-proxy.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (availableRun > currentRun) {
                    PrimaryAction(
                        text = "下载安装",
                        onClick = onDownload,
                        enabled = !busy,
                    )
                } else {
                    PrimaryAction(
                        text = if (state.checking) "检查中…" else "检查",
                        onClick = onCheck,
                        enabled = !state.checking,
                    )
                }
                OutlineAction("关闭", onClick = onDismiss, enabled = !busy)
            }
        },
    )
}
