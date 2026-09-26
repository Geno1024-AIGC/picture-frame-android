package com.geno1024.pictureframe.update

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geno1024.pictureframe.BuildConfig
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(Mirrors.PREFS, 0)

    var info by mutableStateOf<CanaryInfo?>(null)
        private set

    var selectedMirror by mutableStateOf(
        Mirrors.resolve(prefs.getString(Mirrors.KEY_SELECTED, Mirrors.DIRECT.prefix).orEmpty()),
    )
        private set

    var checking by mutableStateOf(false)
        private set

    var downloading by mutableStateOf(false)
        private set

    /** -1 means the mirror did not report a usable length. */
    var progress by mutableStateOf(-1f)
        private set

    /** Informational, shown in the normal content colour. */
    var status by mutableStateOf<String?>(null)
        private set

    /** A failure, shown in the error colour. */
    var error by mutableStateOf<String?>(null)
        private set

    /**
     * Only set once an APK is actually waiting to be installed, so the dialog
     * does not nag about the unknown-sources permission before the user has even
     * asked for an update.
     */
    var needsInstallPermission by mutableStateOf(false)
        private set

    /** A downloaded APK waiting on the install permission. */
    private var pending: File? = null

    private val notifier by lazy { UpdateNotifier(getApplication()) }

    val currentRun: Int get() = BuildConfig.CANARY_RUN_NUMBER

    val hasUpdate: Boolean get() = (info?.runNumber ?: 0) > currentRun

    init {
        // Check on launch so the top bar can badge a new build. The manifest is
        // a few hundred bytes, so this is cheap.
        check()
    }

    fun selectMirror(prefix: String) {
        selectedMirror = Mirrors.resolve(prefix)
        prefs.edit().putString(Mirrors.KEY_SELECTED, selectedMirror.prefix).apply()
    }

    fun check() {
        if (checking || downloading) return
        checking = true
        status = null
        error = null
        progress = -1f
        viewModelScope.launch {
            val mirrors = buildList {
                add(selectedMirror)
                addAll(Mirrors.PRESETS)
            }.distinctBy { it.prefix }
            when (val result = Updater.check(currentRun, mirrors)) {
                is UpdateResult.UpToDate -> {
                    info = null
                    status = "已经是最新版本"
                }

                is UpdateResult.Available -> {
                    info = result.info
                    status = "发现新版本 #${result.info.runNumber}"
                }

                is UpdateResult.Failed -> error = result.message
            }
            checking = false
        }
    }

    fun download() {
        val target = info ?: return
        if (downloading) return
        downloading = true
        status = null
        error = null
        progress = 0f
        viewModelScope.launch {
            val context = getApplication<Application>()
            val cache = File(context.cacheDir, "updates").apply { mkdirs() }
            // Try the chosen mirror first, then fall back through the presets so a
            // single dead domain does not block the update.
            val candidates = buildList {
                add(selectedMirror)
                addAll(Mirrors.PRESETS)
            }.distinctBy { it.prefix }
            var failure: String? = null
            var lastReported = -1
            try {
                for (mirror in candidates) {
                    val result = Updater.download(target, mirror, cache) { fraction ->
                        // Throttle: a 12 MB APK arrives in ~190 chunks, and writing
                        // state that often would recompose the dialog just as often.
                        val step = (fraction * 200).toInt()
                        if (step != lastReported) {
                            lastReported = step
                            progress = fraction
                            val percent = (fraction * 100).toInt()
                            if (notifier.shouldPost(percent)) {
                                notifier.downloading(target.runNumber, percent)
                            }
                        }
                    }
                    when (result) {
                        is DownloadResult.Success -> {
                            progress = 1f
                            downloading = false
                            promptInstall(result.file)
                            return@launch
                        }

                        is DownloadResult.Failed -> {
                            failure = result.message
                            if (mirror == selectedMirror) {
                                status = "${result.message}，正在尝试其他源…"
                            }
                        }
                    }
                }
                downloading = false
                progress = -1f
                status = null
                val message = failure ?: "所有下载源都失败了"
                error = message
                notifier.failed(message)
            } finally {
                // Also covers the activity being destroyed mid-download, which
                // cancels the scope and would otherwise strand an ongoing
                // notification that nothing is left to clear.
                notifier.done()
            }
        }
    }

    private fun promptInstall(file: File) {
        val context = getApplication<Application>()
        if (!ApkInstaller.canRequestInstall(context)) {
            // The APK is already on disk; remember it so granting the permission
            // does not force another download.
            pending = file
            needsInstallPermission = true
            status = "下载完成，需要先授权安装"
            return
        }
        launchInstaller(ApkInstaller.installIntent(context, file))
    }

    /** Called after the user returns from the unknown-sources settings screen. */
    fun grantInstallPermission() {
        val context = getApplication<Application>()
        needsInstallPermission = !ApkInstaller.canRequestInstall(context)
        if (needsInstallPermission) return
        val file = pending ?: cachedApk()
        if (file == null) {
            error = "请重新下载"
            return
        }
        pending = null
        status = null
        error = null
        launchInstaller(ApkInstaller.installIntent(context, file))
    }

    fun settingsIntentForPermission(): Intent = ApkInstaller.settingsIntent(getApplication())

    private fun launchInstaller(intent: Intent) {
        runCatching {
            getApplication<Application>().startActivity(intent)
        }.onFailure {
            error = "无法打开安装界面：${it.message}"
        }
    }

    private fun cachedApk(): File? {
        val run = info?.runNumber ?: return null
        val file = File(File(getApplication<Application>().cacheDir, "updates"), "canary-$run.apk")
        return file.takeIf { it.exists() }
    }
}
