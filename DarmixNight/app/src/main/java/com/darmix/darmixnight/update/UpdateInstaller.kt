package com.darmix.darmixnight.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

object UpdateInstaller {

    private const val TIMEOUT_MS = 20_000
    private const val BUFFER = 64 * 1024

    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    private fun updatesDir(context: Context): File =
        File(context.cacheDir, "updates").apply { mkdirs() }

    fun apkFile(context: Context, release: ReleaseInfo): File =
        File(updatesDir(context), "darmixnight-${release.version}.apk")

    fun clearOldDownloads(context: Context, keep: ReleaseInfo?) {
        val keepName = keep?.let { apkFile(context, it).name }
        updatesDir(context).listFiles()?.forEach { if (it.name != keepName) it.delete() }
    }

    suspend fun download(
        context: Context,
        release: ReleaseInfo,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val target = apkFile(context, release)
        if (target.exists() && release.apkSize > 0 && target.length() == release.apkSize) {
            onProgress(target.length(), target.length())
            return@withContext target
        }
        val partial = File(target.parentFile, target.name + ".part")
        partial.delete()
        val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "DarmixNight-Android")
            setRequestProperty("Accept", "application/octet-stream")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("La descarga respondió ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            var downloaded = 0L
            var lastReported = -1
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else -1
                        if (percent != lastReported) {
                            lastReported = percent
                            onProgress(downloaded, total)
                        }
                    }
                }
            }
            if (total > 0 && downloaded != total) {
                throw IllegalStateException("La descarga quedó incompleta")
            }
            target.delete()
            if (!partial.renameTo(target)) throw IllegalStateException("No se pudo guardar la actualización")
            target
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    suspend fun install(context: Context, apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("darmixnight.apk", 0, apk.length()).use { output ->
                apk.inputStream().use { it.copyTo(output, BUFFER) }
                session.fsync(output)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, UpdateInstallReceiver::class.java).setPackage(context.packageName),
                flags
            )
            session.commit(callback.intentSender)
        }
    }
}
