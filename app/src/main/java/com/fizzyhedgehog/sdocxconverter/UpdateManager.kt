package com.fizzyhedgehog.sdocxconverter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val apkSize: Long = 0L
)

object UpdateManager {

    private const val GITHUB_REPO = "FizzyHedgehog/sdocx-converter-android"
    private const val API_URL_LATEST = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val API_URL_RELEASES = "https://api.github.com/repos/$GITHUB_REPO/releases"

    /**
     * Compare semantic versions (e.g. "1.1.0" > "1.0.0").
     * Returns true if latestVersion is strictly newer than currentVersion.
     */
    fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val cleanLatest = latestTag.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V").substringBefore("-")

        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * Query GitHub Releases API for updates. Supports stable and pre-release channels.
     */
    suspend fun checkUpdate(
        context: Context,
        includePrerelease: Boolean? = null,
        bypassVersionCheck: Boolean? = null
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(ConversionHelper.PREFS_NAME, Context.MODE_PRIVATE)
            val isDev = prefs.getBoolean("developer_mode_enabled", false)
            val usePrerelease = includePrerelease ?: (isDev && prefs.getBoolean("dev_prerelease_channel", true))
            val bypass = bypassVersionCheck ?: (isDev && prefs.getBoolean("dev_bypass_version_check", false))

            val targetApiUrl = if (usePrerelease) API_URL_RELEASES else API_URL_LATEST
            val url = URL(targetApiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SDOCXConverter-Android")
            }

            if (conn.responseCode != 200) {
                return@withContext Result.failure(Exception("GitHub API HTTP ${conn.responseCode}"))
            }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }

            val targetRelease = if (usePrerelease) {
                val releasesArray = JSONArray(responseText)
                var found: JSONObject? = null
                for (i in 0 until releasesArray.length()) {
                    val rel = releasesArray.getJSONObject(i)
                    if (rel.optBoolean("draft", false)) continue
                    val assets = rel.optJSONArray("assets") ?: continue
                    var hasApk = false
                    for (j in 0 until assets.length()) {
                        if (assets.getJSONObject(j).optString("name", "").endsWith(".apk", ignoreCase = true)) {
                            hasApk = true
                            break
                        }
                    }
                    if (hasApk) {
                        found = rel
                        break
                    }
                }
                found ?: if (releasesArray.length() > 0) releasesArray.getJSONObject(0) else return@withContext Result.failure(Exception("No releases found"))
            } else {
                JSONObject(responseText)
            }

            val tagName = targetRelease.optString("tag_name", "")
            val isPrerelease = targetRelease.optBoolean("prerelease", false)
            val rawTitle = targetRelease.optString("name", tagName)
            val title = if (isPrerelease && !rawTitle.contains("Pre-release", ignoreCase = true)) {
                "[Pre-release] $rawTitle"
            } else {
                rawTitle
            }
            val body = targetRelease.optString("body", "")

            val assets = targetRelease.optJSONArray("assets")
            var apkUrl: String? = null
            var apkSize = 0L
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val pInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
            val currentVersion = pInfo?.versionName ?: "1.0.0"
            val hasUpdate = (!apkUrl.isNullOrEmpty()) && (bypass || isNewerVersion(tagName, currentVersion))

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = tagName.removePrefix("v").removePrefix("V"),
                    currentVersion = currentVersion,
                    releaseTitle = title,
                    releaseNotes = body,
                    apkDownloadUrl = apkUrl,
                    apkSize = apkSize
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download the latest APK file with progress reporting.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        versionName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "apk_updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }
            val apkFile = File(updateDir, "SDOCX_Converter_${versionName}.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            var curUrl = downloadUrl
            var redirectCount = 0
            var conn: HttpURLConnection

            while (true) {
                val url = URL(curUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "SDOCXConverter-Android")
                }

                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308) && redirectCount < 8) {
                    val location = conn.getHeaderField("Location") ?: break
                    conn.disconnect()
                    curUrl = location
                    redirectCount++
                } else {
                    break
                }
            }

            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else -1f
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launch the Android Package Installer to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Check if the app has permission to install unknown apps on Android 8.0+.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Open system settings screen to allow installation of unknown apps from this app.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
