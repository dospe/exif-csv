@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.dospe.exifcsv.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dospe.exifcsv.ExportResult
import io.github.dospe.exifcsv.MainViewModel
import io.github.dospe.exifcsv.Progress
import io.github.dospe.exifcsv.R
import io.github.dospe.exifcsv.UiMessage
import io.github.dospe.exifcsv.UiState
import io.github.dospe.exifcsv.csv.CsvBuilder
import io.github.dospe.exifcsv.csv.PhotoRecord
import io.github.dospe.exifcsv.data.MediaAccess
import io.github.dospe.exifcsv.data.MediaPermissions
import io.github.dospe.exifcsv.data.PhotoItem
import kotlinx.coroutines.launch

@Composable
fun ExifCsvApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.onPermissionResult() }

    val documentsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.exportDocuments(uris) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> if (uri != null) viewModel.saveTo(uri) }

    val messageTexts = mapOf(
        UiMessage.SAVED to stringResource(R.string.msg_saved),
        UiMessage.SAVE_FAILED to stringResource(R.string.msg_save_failed),
        UiMessage.CANCELLED to stringResource(R.string.msg_cancelled),
        UiMessage.SHARE_FAILED to stringResource(R.string.msg_share_failed),
    )
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        viewModel.consumeMessage()
        snackbarHostState.showSnackbar(messageTexts.getValue(message))
    }

    val requestPermission = { permissionLauncher.launch(MediaPermissions.required()) }
    val pickDocuments = { documentsLauncher.launch(arrayOf("image/*")) }

    val result = state.result
    if (result != null) {
        ResultScreen(
            result = result,
            decimalComma = state.decimalComma,
            snackbarHostState = snackbarHostState,
            onBack = viewModel::closeResult,
            onToggleDecimalComma = viewModel::setDecimalComma,
            onSave = { saveLauncher.launch(viewModel.suggestedFileName()) },
            onShare = {
                scope.launch {
                    val uri = viewModel.prepareShareUri()
                    val started = uri != null && runCatching {
                        context.startActivity(Intent.createChooser(shareIntent(uri), null))
                    }.isSuccess
                    if (!started) viewModel.reportShareFailed()
                }
            },
        )
    } else {
        GalleryScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onRequestPermission = requestPermission,
            onPickDocuments = pickDocuments,
            onToggle = viewModel::toggle,
            onSelectAll = viewModel::selectAll,
            onClearSelection = viewModel::clearSelection,
            onReload = viewModel::loadPhotos,
            onExport = viewModel::exportSelected,
            onToggleDecimalComma = viewModel::setDecimalComma,
        )
    }

    state.progress?.let { progress ->
        ProgressDialog(progress = progress, onCancel = viewModel::cancelExport)
    }
}

private fun shareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/csv"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

@Composable
private fun GalleryScreen(
    state: UiState,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onPickDocuments: () -> Unit,
    onToggle: (Uri) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onReload: () -> Unit,
    onExport: () -> Unit,
    onToggleDecimalComma: (Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasAccess = state.access != MediaAccess.NONE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (state.photos.isNotEmpty()) {
                        val allSelected = state.selected.size == state.photos.size
                        TextButton(onClick = if (allSelected) onClearSelection else onSelectAll) {
                            Text(stringResource(if (allSelected) R.string.action_clear_selection else R.string.action_select_all))
                        }
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_pick_files)) },
                            onClick = { menuOpen = false; onPickDocuments() },
                        )
                        if (hasAccess) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_reload)) },
                                onClick = { menuOpen = false; onReload() },
                            )
                        }
                        if (state.access == MediaAccess.PARTIAL) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_select_more_photos)) },
                                onClick = { menuOpen = false; onRequestPermission() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.setting_decimal_comma)) },
                            trailingIcon = { Checkbox(checked = state.decimalComma, onCheckedChange = null) },
                            onClick = { onToggleDecimalComma(!state.decimalComma) },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (hasAccess) {
                BottomAppBar {
                    Text(
                        text = stringResource(R.string.selected_count, state.selected.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                    )
                    Button(
                        onClick = onExport,
                        enabled = state.selected.isNotEmpty() && state.progress == null,
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_create_csv))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                !hasAccess -> PermissionContent(onRequestPermission, onPickDocuments)
                state.loading && state.photos.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.photos.isEmpty() -> EmptyContent(
                    partial = state.access == MediaAccess.PARTIAL,
                    onRequestPermission = onRequestPermission,
                    onPickDocuments = onPickDocuments,
                )
                else -> Column(Modifier.fillMaxSize()) {
                    if (state.access == MediaAccess.PARTIAL) {
                        InfoBanner(
                            text = stringResource(R.string.banner_partial),
                            actionText = stringResource(R.string.action_select_more_photos),
                            onAction = onRequestPermission,
                        )
                    }
                    if (!state.locationAccess) {
                        InfoBanner(
                            text = stringResource(R.string.banner_no_location),
                            actionText = stringResource(R.string.action_fix),
                            onAction = onRequestPermission,
                            warning = true,
                        )
                    }
                    PhotoGrid(
                        photos = state.photos,
                        selected = state.selected,
                        onToggle = onToggle,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<PhotoItem>,
    selected: Set<Uri>,
    onToggle: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(photos, key = { it.uri.toString() }) { photo ->
            val isSelected = photo.uri in selected
            val shape = RoundedCornerShape(4.dp)
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(shape)
                    .toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(photo.uri) },
                    ),
            ) {
                PhotoThumbnail(uri = photo.uri, modifier = Modifier.fillMaxSize())
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            .border(3.dp, MaterialTheme.colorScheme.primary, shape),
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(50)),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(onRequestPermission: () -> Unit, onPickDocuments: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permission_text),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.action_grant_access))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onPickDocuments) {
            Text(stringResource(R.string.action_pick_files))
        }
    }
}

@Composable
private fun EmptyContent(partial: Boolean, onRequestPermission: () -> Unit, onPickDocuments: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(if (partial) R.string.empty_partial_text else R.string.empty_text),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (partial) {
            Button(onClick = onRequestPermission) { Text(stringResource(R.string.action_select_more_photos)) }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onPickDocuments) { Text(stringResource(R.string.action_pick_files)) }
    }
}

@Composable
private fun InfoBanner(text: String, actionText: String, onAction: () -> Unit, warning: Boolean = false) {
    val container = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val content = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (warning) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null,
            tint = content,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAction) { Text(actionText, color = content) }
    }
}

@Composable
private fun ProgressDialog(progress: Progress, onCancel: () -> Unit) {
    val fraction = if (progress.total == 0) 0f else progress.done.toFloat() / progress.total
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.progress_title)) },
        text = {
            Column {
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.progress_text, progress.done, progress.total))
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ResultScreen(
    result: ExportResult,
    decimalComma: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToggleDecimalComma: (Boolean) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_share))
                    }
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Done, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_save_csv))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SummaryCard(result = result, decimalComma = decimalComma, onToggleDecimalComma = onToggleDecimalComma)
            }
            items(result.records) { record ->
                RecordRow(record = record, decimalComma = decimalComma)
            }
        }
    }
}

@Composable
private fun SummaryCard(result: ExportResult, decimalComma: Boolean, onToggleDecimalComma: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.summary_photos, result.records.size), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.summary_gps, result.withGps), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.summary_date, result.withDate), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.setting_decimal_comma),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = decimalComma, onCheckedChange = onToggleDecimalComma)
            }
        }
    }
}

@Composable
private fun RecordRow(record: PhotoRecord, decimalComma: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = record.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = record.dateTime ?: stringResource(R.string.no_date),
            style = MaterialTheme.typography.bodyMedium,
            color = if (record.dateTime == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val gpsText = if (record.hasGps) {
            CsvBuilder.formatNumber(record.latitude, 6, decimalComma) + "  " +
                CsvBuilder.formatNumber(record.longitude, 6, decimalComma) +
                (record.altitude?.let { "  (" + CsvBuilder.formatNumber(it, 1, decimalComma) + " m)" } ?: "")
        } else {
            stringResource(R.string.no_gps)
        }
        Text(
            text = gpsText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (record.hasGps) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
    }
}
