package io.github.dospe.exifcsv

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dospe.exifcsv.csv.CsvBuilder
import io.github.dospe.exifcsv.csv.CsvOptions
import io.github.dospe.exifcsv.csv.PhotoRecord
import io.github.dospe.exifcsv.data.MediaAccess
import io.github.dospe.exifcsv.data.MediaPermissions
import io.github.dospe.exifcsv.data.MediaRepository
import io.github.dospe.exifcsv.data.PhotoItem
import io.github.dospe.exifcsv.exif.ExifReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Progress(val done: Int, val total: Int)

data class ExportResult(val records: List<PhotoRecord>) {
    val withGps: Int get() = records.count { it.hasGps }
    val withDate: Int get() = records.count { it.dateTime != null }
}

enum class UiMessage { SAVED, SAVE_FAILED, CANCELLED, SHARE_FAILED }

data class UiState(
    val access: MediaAccess = MediaAccess.NONE,
    val locationAccess: Boolean = false,
    val loading: Boolean = false,
    val photos: List<PhotoItem> = emptyList(),
    val selected: Set<Uri> = emptySet(),
    val progress: Progress? = null,
    val result: ExportResult? = null,
    val decimalComma: Boolean = false,
    val message: UiMessage? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = MediaRepository(context)
    private val exifReader = ExifReader(context)
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(UiState(decimalComma = prefs.getBoolean(KEY_DECIMAL_COMMA, false)))
    val state: StateFlow<UiState> = _state

    private var exportJob: Job? = null

    init {
        refreshAccess()
    }

    /** Re-reads permission state; loads the gallery when access appeared (e.g. granted in system settings). */
    fun refreshAccess() {
        val access = MediaPermissions.access(context)
        val hadAccess = _state.value.access != MediaAccess.NONE
        _state.update { it.copy(access = access, locationAccess = MediaPermissions.hasLocationAccess(context)) }
        if (access != MediaAccess.NONE && !hadAccess) loadPhotos()
    }

    /** Called after the permission dialog closes: on Android 14+ the user may have picked more photos. */
    fun onPermissionResult() {
        val access = MediaPermissions.access(context)
        _state.update { it.copy(access = access, locationAccess = MediaPermissions.hasLocationAccess(context)) }
        if (access != MediaAccess.NONE) loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val photos = runCatching { repository.loadPhotos() }.getOrDefault(emptyList())
            _state.update { s ->
                val uris: Set<Uri> = photos.mapTo(HashSet<Uri>()) { it.uri }
                s.copy(loading = false, photos = photos, selected = s.selected.filterTo(LinkedHashSet<Uri>()) { it in uris })
            }
        }
    }

    fun toggle(uri: Uri) = _state.update { s ->
        s.copy(selected = if (uri in s.selected) s.selected - uri else s.selected + uri)
    }

    fun selectAll() = _state.update { s -> s.copy(selected = s.photos.mapTo(LinkedHashSet<Uri>()) { it.uri }) }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    fun setDecimalComma(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DECIMAL_COMMA, enabled).apply()
        _state.update { it.copy(decimalComma = enabled) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun closeResult() = _state.update { it.copy(result = null) }

    fun cancelExport() {
        exportJob?.cancel()
    }

    /** Reads EXIF of the photos selected in the gallery grid. */
    fun exportSelected() {
        val s = _state.value
        val items = s.photos.filter { it.uri in s.selected }.map { it.uri to it.name }
        export(items)
    }

    /** Reads EXIF of documents returned by the system file picker. */
    fun exportDocuments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val items = uris.map { it to repository.displayName(it) }
            export(items)
        }
    }

    private fun export(items: List<Pair<Uri, String>>) {
        if (items.isEmpty()) return
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _state.update { it.copy(progress = Progress(0, items.size), result = null) }
            val records = ArrayList<PhotoRecord>(items.size)
            try {
                withContext(Dispatchers.IO) {
                    items.forEachIndexed { index, (uri, name) ->
                        ensureActive()
                        records.add(exifReader.read(uri, name))
                        _state.update { it.copy(progress = Progress(index + 1, items.size)) }
                    }
                }
                _state.update { it.copy(progress = null, result = ExportResult(records)) }
            } catch (e: CancellationException) {
                _state.update { it.copy(progress = null, message = UiMessage.CANCELLED) }
                throw e
            }
        }
    }

    fun csvText(): String? {
        val result = _state.value.result ?: return null
        return CsvBuilder.build(result.records, CsvOptions(decimalComma = _state.value.decimalComma))
    }

    fun suggestedFileName(): String =
        "exif_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date()) + ".csv"

    /** Writes the CSV to a document URI obtained from the "Save as" dialog. */
    fun saveTo(uri: Uri) {
        val text = csvText() ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    // "wt" truncates an existing file; not every provider supports it, so fall back to "w".
                    val stream = runCatching { resolver().openOutputStream(uri, "wt") }.getOrNull()
                        ?: resolver().openOutputStream(uri)
                    requireNotNull(stream).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            _state.update { it.copy(message = if (ok) UiMessage.SAVED else UiMessage.SAVE_FAILED) }
        }
    }

    /** Writes the CSV into the cache dir and returns a FileProvider URI for the share sheet. */
    suspend fun prepareShareUri(): Uri? = withContext(Dispatchers.IO) {
        val text = csvText() ?: return@withContext null
        runCatching {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, suggestedFileName())
            file.writeText(text, Charsets.UTF_8)
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        }.getOrNull()
    }

    fun reportShareFailed() = _state.update { it.copy(message = UiMessage.SHARE_FAILED) }

    private fun resolver() = context.contentResolver

    private companion object {
        const val KEY_DECIMAL_COMMA = "decimal_comma"
    }
}
