package com.darmix.darmixnight.update

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tag: String,
    val version: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String
)

object UpdateRepository {

    private const val OWNER = "OrlandoGalvanVargas"
    private const val REPO = "darmixnight"
    private const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    private const val CHECK_INTERVAL_MS = 30L * 60L * 1000L
    private const val TIMEOUT_MS = 15_000

    private const val FILE = "darmixnight_updates"
    private const val KEY_CHECKED_AT = "checked_at"
    private const val KEY_HAS_RELEASE = "has_release"
    private const val KEY_TAG = "tag"
    private const val KEY_TITLE = "title"
    private const val KEY_NOTES = "notes"
    private const val KEY_APK_URL = "apk_url"
    private const val KEY_APK_SIZE = "apk_size"
    private const val KEY_PAGE_URL = "page_url"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun installedVersion(context: Context): String = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: "0"
    } catch (_: Exception) {
        "0"
    }

    suspend fun latestNewerRelease(context: Context): ReleaseInfo? {
        val release = cachedOrFetched(context) ?: return null
        return if (isNewer(release.version, installedVersion(context))) release else null
    }

    private suspend fun cachedOrFetched(context: Context): ReleaseInfo? {
        val p = prefs(context)
        val checkedAt = p.getLong(KEY_CHECKED_AT, 0L)
        val fresh = System.currentTimeMillis() - checkedAt in 0 until CHECK_INTERVAL_MS
        if (fresh) return readCache(context)
        return try {
            val fetched = fetchLatest()
            writeCache(context, fetched)
            fetched
        } catch (_: Exception) {
            readCache(context)
        }
    }

    private suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DarmixNight-Android")
        }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parse(JSONObject(body))
                }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IllegalStateException("GitHub respondió ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: JSONObject): ReleaseInfo? {
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null
        val assets = json.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        var apkSize = 0L
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                apkSize = asset.optLong("size")
                break
            }
        }
        if (apkUrl.isNullOrBlank()) return null
        val tag = json.optString("tag_name")
        return ReleaseInfo(
            tag = tag,
            version = normalize(tag),
            title = json.optString("name").ifBlank { tag },
            notes = cleanNotes(json.optString("body")),
            apkUrl = apkUrl,
            apkSize = apkSize,
            pageUrl = json.optString("html_url")
        )
    }

    private fun writeCache(context: Context, release: ReleaseInfo?) {
        prefs(context).edit {
            putLong(KEY_CHECKED_AT, System.currentTimeMillis())
            putBoolean(KEY_HAS_RELEASE, release != null)
            if (release != null) {
                putString(KEY_TAG, release.tag)
                putString(KEY_TITLE, release.title)
                putString(KEY_NOTES, release.notes)
                putString(KEY_APK_URL, release.apkUrl)
                putLong(KEY_APK_SIZE, release.apkSize)
                putString(KEY_PAGE_URL, release.pageUrl)
            }
        }
    }

    private fun readCache(context: Context): ReleaseInfo? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_HAS_RELEASE, false)) return null
        val tag = p.getString(KEY_TAG, null) ?: return null
        val apkUrl = p.getString(KEY_APK_URL, null) ?: return null
        return ReleaseInfo(
            tag = tag,
            version = normalize(tag),
            title = p.getString(KEY_TITLE, tag) ?: tag,
            notes = p.getString(KEY_NOTES, "") ?: "",
            apkUrl = apkUrl,
            apkSize = p.getLong(KEY_APK_SIZE, 0L),
            pageUrl = p.getString(KEY_PAGE_URL, "") ?: ""
        )
    }

    private fun normalize(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").substringBefore("-").substringBefore("+")

    private fun parts(version: String): List<Int> =
        normalize(version).split(".").map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    fun isNewer(candidate: String, installed: String): Boolean {
        val a = parts(candidate)
        val b = parts(installed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun cleanNotes(markdown: String): String =
        markdown.lines()
            .map { line ->
                line.trim()
                    .replace(Regex("^#{1,6}\\s*"), "")
                    .replace(Regex("^[-*+]\\s+"), "• ")
                    .replace("**", "")
                    .replace("__", "")
                    .replace("`", "")
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
}
