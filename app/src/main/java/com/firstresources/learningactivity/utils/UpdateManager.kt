package com.firstresources.learningactivity.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.firstresources.learningactivity.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val GITHUB_REPO_API = "https://api.github.com/repos/ZikrulCahyadi/Learning_Activity/releases/latest"

    fun checkForUpdates(context: Context, showNoUpdateToast: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(GITHUB_REPO_API)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "LearningActivity-Android")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "").trim()
                    val bodyNotes = json.optString("body", "Pembaruan versi baru telah tersedia.")

                    var apkDownloadUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkDownloadUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                    }

                    if (apkDownloadUrl != null && isNewerVersion(context, tagName)) {
                        withContext(Dispatchers.Main) {
                            if (context is Activity && (context.isFinishing || context.isDestroyed)) return@withContext
                            CustomDialog.showConfirmation(
                                context = context,
                                title = "Pembaruan Tersedia ($tagName)",
                                message = "Versi baru aplikasi sudah tersedia!\n\nCatatan:\n" + bodyNotes + "\n\nApakah Anda ingin mengunduh dan memperbarui sekarang?",
                                positiveText = "Update Sekarang",
                                negativeText = "Nanti Saja",
                                iconRes = R.drawable.ic_assignment,
                                type = CustomDialog.Type.INFO,
                                isCancelable = true,
                                onPositive = {
                                    downloadAndInstallApk(context, apkDownloadUrl, tagName)
                                }
                            )
                        }
                    } else if (showNoUpdateToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Aplikasi sudah dalam versi terbaru.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (showNoUpdateToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tidak ada pembaruan ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (showNoUpdateToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal memeriksa pembaruan: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isNewerVersion(context: Context, tagVersion: String): Boolean {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = pInfo.versionName ?: "1.0"

            val cleanTag = tagVersion.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

            val tagParts = cleanTag.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(tagParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val t = tagParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (t > c) return true
                if (t < c) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun downloadAndInstallApk(context: Context, downloadUrl: String, versionTag: String) {
        try {
            Toast.makeText(context, "Mulai mengunduh versi $versionTag...", Toast.LENGTH_LONG).show()

            val fileName = "LearningActivity_$versionTag.apk"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDir, fileName)

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Mengunduh Learning Activity $versionTag")
                setDescription("Sedang mengunduh berkas instalasi...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            // Already unregistered
                        }
                        installApk(context, destinationFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengunduh pembaruan: " + e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Silakan aktifkan izin instalasi untuk aplikasi ini.",
                        Toast.LENGTH_LONG
                    ).show()
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka installer: " + e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }
}