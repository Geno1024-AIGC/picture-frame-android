package com.geno1024.pictureframe.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geno1024.pictureframe.EditorViewModel
import com.geno1024.pictureframe.R
import com.geno1024.pictureframe.io.ExportFormat
import com.geno1024.pictureframe.io.LoadedPhoto
import com.geno1024.pictureframe.model.EditorState
import com.geno1024.pictureframe.model.Look
import com.geno1024.pictureframe.model.Looks
import com.geno1024.pictureframe.model.Resolution
import com.geno1024.pictureframe.ui.components.PreviewCanvas
import com.geno1024.pictureframe.ui.components.SegmentedTabs
import com.geno1024.pictureframe.ui.update.UpdateDialog
import com.geno1024.pictureframe.ui.update.UpdateUiState
import com.geno1024.pictureframe.update.Mirrors
import com.geno1024.pictureframe.update.UpdateViewModel

private val TABS = listOf("镜框", "背景", "画布", "导出")

@Composable
fun EditorScreen(viewModel: EditorViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var format by rememberSaveable { mutableStateOf(ExportFormat.Jpeg) }
    var showUpdate by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val updateViewModel: UpdateViewModel = viewModel()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { updateViewModel.grantInstallPermission() }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Whether or not it was granted, the download itself carries on; the
        // notification is a convenience, not a requirement.
        updateViewModel.download()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.open(uri)
    }
    val pick: () -> Unit = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val state = viewModel.state
    val photo = viewModel.photo

    LaunchedEffect(viewModel.notice) {
        val message = viewModel.notice ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeNotice()
    }

    if (showUpdate) {
        UpdateDialog(
            currentRun = updateViewModel.currentRun,
            availableRun = updateViewModel.info?.runNumber ?: 0,
            infoLabel = updateViewModel.info?.let {
                "${it.versionName} · ${it.sizeBytes / 1024} KB · sha256 ${it.shortSha}"
            }.orEmpty(),
            state = UpdateUiState(
                checking = updateViewModel.checking,
                downloading = updateViewModel.downloading,
                progress = updateViewModel.progress,
                status = updateViewModel.status,
                error = updateViewModel.error,
                needsInstallPermission = updateViewModel.needsInstallPermission,
            ),
            mirrorOptions = Mirrors.PRESETS.map { it.label to it.prefix },
            selectedMirrorPrefix = updateViewModel.selectedMirror.prefix,
            onSelectMirror = updateViewModel::selectMirror,
            onCheck = updateViewModel::check,
            onDownload = {
                val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    updateViewModel.download()
                } else {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onGrantInstallPermission = {
                permissionLauncher.launch(updateViewModel.settingsIntentForPermission())
            },
            onDismiss = { showUpdate = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopBar(
                onReset = viewModel::reset,
                onPick = pick,
                onUpdate = { showUpdate = true },
                hasUpdate = updateViewModel.hasUpdate,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Stage(
                state = state,
                photo = photo,
                blurred = viewModel.blurred,
                loading = viewModel.loading,
                onPick = pick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (photo != null) {
                    LookStrip(onSelect = viewModel::applyLook)
                    SegmentedTabs(TABS, tab) { tab = it }
                    Box(modifier = Modifier.heightIn(max = 320.dp)) {
                        Box(Modifier.verticalScroll(rememberScrollState())) {
                            Options(
                                tab = tab,
                                state = state,
                                photo = photo,
                                format = format,
                                exporting = viewModel.exporting,
                                onEdit = viewModel::edit,
                                onResolution = { option -> viewModel.edit { it.copy(resolution = option) } },
                                onFormat = { format = it },
                                onExport = { viewModel.export(format) },
                            )
                        }
                    }
                } else {
                    Hint()
                }
            }
        }
    }
}

@Composable
private fun Options(
    tab: Int,
    state: EditorState,
    photo: LoadedPhoto,
    format: ExportFormat,
    exporting: Boolean,
    onEdit: ((EditorState) -> EditorState) -> Unit,
    onResolution: (Resolution) -> Unit,
    onFormat: (ExportFormat) -> Unit,
    onExport: () -> Unit,
) {
    when (tab) {
        0 -> FramePanel(state, onEdit)
        1 -> BackgroundPanel(state, onEdit)
        2 -> CanvasPanel(state, onEdit)
        else -> ExportPanel(
            state = state,
            format = format,
            photoAspect = photo.aspect,
            photoLongEdge = photo.longEdge,
            exporting = exporting,
            onResolution = onResolution,
            onFormat = onFormat,
            onExport = onExport,
        )
    }
}

@Composable
private fun TopBar(
    onReset: () -> Unit,
    onPick: () -> Unit,
    onUpdate: () -> Unit,
    hasUpdate: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            // Pinned so that adding another action, or a large font scale making
            // the title wrap, cannot change how tall the bar is.
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "相框",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        IconAction(
            icon = painterResource(R.drawable.ic_update),
            label = "检查更新",
            onClick = onUpdate,
            tint = if (hasUpdate) MaterialTheme.colorScheme.primary else null,
        )
        Spacer(Modifier.width(4.dp))
        IconAction(Icons.Filled.Add, "换一张照片", onPick)
        Spacer(Modifier.width(4.dp))
        IconAction(Icons.Filled.Refresh, "重置", onReset)
    }
}

@Composable
private fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun IconAction(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Stage(
    state: EditorState,
    photo: LoadedPhoto?,
    blurred: ImageBitmap?,
    loading: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            PreviewCanvas(
                state = state,
                photo = photo.image,
                blurred = blurred,
                photoAspect = photo.aspect,
                photoLongEdge = photo.longEdge,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        } else if (photo == null) {
            PickButton(onPick)
        }
    }
}

@Composable
private fun PickButton(onPick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            text = "选择一张照片",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LookStrip(onSelect: (Look) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Looks.all) { look ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(look) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = look.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Hint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("先选一张照片", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "然后挑选镜框与背景，随时可以换图或重置。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
