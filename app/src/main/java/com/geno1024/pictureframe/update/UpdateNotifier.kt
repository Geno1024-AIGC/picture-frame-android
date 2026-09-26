package com.geno1024.pictureframe.update

import android.Manifest
import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Mirrors download progress into a system notification.
 *
 * The dialog already shows a bar, but it is easy to dismiss it and a 12 MB APK
 * over a proxy is not instant, so the progress also belongs where the user can
 * leave the app and still see it.
 */
class UpdateNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /**
     * Remembers the last posted percentage so the throttle in the view model
     * keeps the shade from reposting on every chunk of a ~190 chunk download.
     */
    private var lastPercent = -1

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL,
                "应用更新",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "下载安装新版本时显示进度"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun downloading(runNumber: Int, percent: Int?) {
        // Advance the throttle even when nothing can be posted, otherwise every
        // chunk re-enters this and gets rejected again.
        lastPercent = percent ?: -1
        if (!canPost()) return
        val indeterminate = percent == null
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载更新 #$runNumber")
            .setContentText(if (indeterminate) "正在连接…" else "$percent%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent ?: 0, indeterminate)
            .build()
        post(NOTIFICATION_ID, notification)
    }

    /**
     * Clears the ongoing notification. Android removes it on tap anyway, and the
     * installer takes over the screen from here.
     */
    fun done() {
        manager.cancel(NOTIFICATION_ID)
        lastPercent = -1
    }

    fun failed(message: String) {
        if (!canPost()) return
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("更新下载失败")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        post(FAILURE_ID, notification)
        manager.cancel(NOTIFICATION_ID)
        lastPercent = -1
    }

    /** Post the same percentage twice and the shade still animates, so skip it. */
    fun shouldPost(percent: Int?): Boolean {
        if (percent == lastPercent) return false
        return true
    }

    /**
     * The explicit check is what notify() needs; areNotificationsEnabled() also
     * reflects the user having switched notifications off in settings, which
     * would suppress the bar even on a version that requires no grant.
     */
    private fun canPost(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun post(id: Int, notification: Notification) {
        // Spelled out here rather than behind canPost() so lint can see the
        // guard; a denied grant is a normal outcome, not an error worth
        // surfacing, since the dialog still shows the same progress.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching { manager.notify(id, notification) }
    }

    private companion object {
        const val CHANNEL = "updates"
        const val NOTIFICATION_ID = 1001
        const val FAILURE_ID = 1002
    }
}
