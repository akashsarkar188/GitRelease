package com.akashsarkar188.gitrelease.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "ApkDownloadService"
private const val CHANNEL_ID = "apk_download_channel"
private const val NOTIFICATION_ID = 1001

/**
 * Custom APK download service using OkHttp.
 * Supports authentication for private GitHub repos.
 */
class ApkDownloadService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Use app's external files directory (no permission needed, accessible via FileProvider)
    private val downloadDir: File by lazy {
        File(context.getExternalFilesDir(null), "apk_downloads").apply {
            if (!exists()) mkdirs()
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress for APK updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    /**
     * Downloads APK file with authentication support.
     * Shows progress in notification.
     */
    suspend fun downloadApk(
        url: String,
        fileName: String,
        token: String?,
        onProgress: (Int) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting download: $url")
        Log.d(TAG, "Token present: ${!token.isNullOrBlank()}")
        Log.d(TAG, "Download dir: ${downloadDir.absolutePath}")
        
        showProgressNotification(fileName, 0)
        
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/octet-stream")
            
            // Add auth header for private repos
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
                Log.d(TAG, "Added Authorization header")
            }
            
            val request = requestBuilder.build()
            Log.d(TAG, "Executing request to: ${request.url}")
            
            val response = okHttpClient.newCall(request).execute()
            
            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response message: ${response.message}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "Download failed: ${response.code} - $errorBody")
                showErrorNotification(fileName, "HTTP ${response.code}")
                return@withContext DownloadResult.Error("HTTP ${response.code}: ${response.message}")
            }
            
            val body = response.body
            if (body == null) {
                Log.e(TAG, "Response body is null")
                showErrorNotification(fileName, "Empty response")
                return@withContext DownloadResult.Error("Empty response body")
            }
            
            val contentLength = body.contentLength()
            Log.d(TAG, "Content length: $contentLength bytes")
            
            // Save to app's external files directory
            val file = File(downloadDir, fileName)
            Log.d(TAG, "Saving to: ${file.absolutePath}")
            
            // Delete existing file if present
            if (file.exists()) {
                file.delete()
            }
            
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var lastProgress = 0
                
                body.byteStream().use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        
                        if (contentLength > 0) {
                            val progress = ((bytesRead * 100) / contentLength).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                                showProgressNotification(fileName, progress)
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "Download complete: ${file.absolutePath}")
            Log.d(TAG, "File size: ${file.length()} bytes")
            
            // Auto-launch install dialog and dismiss notification
            launchInstallIntent(file)
            notificationManager.cancel(NOTIFICATION_ID)
            
            DownloadResult.Success(file)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download exception: ${e.message}", e)
            showErrorNotification(fileName, e.message ?: "Unknown error")
            DownloadResult.Error(e.message ?: "Download failed")
        }
    }

    private fun showProgressNotification(fileName: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $fileName")
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompleteNotification(fileName: String, file: File) {
        // Create install intent
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText("Tap to install $fileName")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(fileName: String, errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download Failed")
            .setContentText("$fileName: $errorMessage")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Launch the install intent for the APK file
     */
    private fun launchInstallIntent(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d(TAG, "Launching install intent for: ${file.name}")
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch install intent: ${e.message}", e)
        }
    }
    
    /**
     * Returns all downloaded APK files
     */
    suspend fun getDownloadedApks(): List<File> = withContext(Dispatchers.IO) {
        downloadDir.listFiles()?.filter { it.extension == "apk" } ?: emptyList()
    }
    
    /**
     * Clears all downloaded APKs
     */
    suspend fun clearDownloadedApks(): Int = withContext(Dispatchers.IO) {
        var count = 0
        downloadDir.listFiles()?.forEach { file ->
            if (file.extension == "apk" && file.delete()) {
                count++
            }
        }
        Log.d(TAG, "Deleted $count APK files")
        count
    }
}
