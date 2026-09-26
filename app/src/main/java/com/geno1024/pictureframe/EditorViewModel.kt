package com.geno1024.pictureframe

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geno1024.pictureframe.io.ExportFormat
import com.geno1024.pictureframe.io.LoadedPhoto
import com.geno1024.pictureframe.io.loadPhoto
import com.geno1024.pictureframe.io.saveToGallery
import com.geno1024.pictureframe.model.EditorState
import com.geno1024.pictureframe.model.Look
import com.geno1024.pictureframe.model.Looks
import com.geno1024.pictureframe.render.blurredBackdrop
import com.geno1024.pictureframe.render.renderToBitmap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(EditorState())
        private set

    var photo by mutableStateOf<LoadedPhoto?>(null)
        private set

    var blurred by mutableStateOf<ImageBitmap?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    var exporting by mutableStateOf(false)
        private set

    var notice by mutableStateOf<String?>(null)

    private var blurredFor = -1f

    fun open(uri: Uri) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            val loaded = loadPhoto(getApplication(), uri)
            loading = false
            if (loaded == null) {
                notice = "无法读取这张照片"
                return@launch
            }
            photo = loaded
            blurredFor = -1f
            syncBlurred()
        }
    }

    fun edit(transform: (EditorState) -> EditorState) {
        val next = transform(state)
        if (next == state) return
        state = next
        syncBlurred()
    }

    fun applyLook(look: Look) = edit { Looks.apply(it, look) }

    fun reset() {
        state = EditorState()
        syncBlurred()
    }

    fun consumeNotice() {
        notice = null
    }

    fun export(format: ExportFormat) {
        val current = photo ?: return
        if (exporting) return
        exporting = true
        viewModelScope.launch {
            val result = runCatching {
                val bitmap = renderToBitmap(
                    state = state,
                    photo = current.image,
                    blurred = blurred,
                    photoAspect = current.aspect,
                    photoLongEdge = current.longEdge,
                )
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                saveToGallery(
                    context = getApplication(),
                    bitmap = bitmap,
                    format = format,
                    displayName = "frame_$stamp.${format.extension}",
                )
            }
            exporting = false
            notice = if (result.getOrNull() != null) "已保存到相册" else "保存失败"
        }
    }

    private fun syncBlurred() {
        val strength = state.background.blurStrength
        if (strength == blurredFor) return
        val source = photo ?: return
        blurredFor = strength
        blurred = blurredBackdrop(source.image, strength)
    }
}
