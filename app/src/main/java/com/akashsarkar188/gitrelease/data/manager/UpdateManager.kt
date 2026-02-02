package com.akashsarkar188.gitrelease.data.manager

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "UpdateManager"

class UpdateManager(private val context: Context) {

    suspend fun getCurrentVersion(packageName: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "getCurrentVersion called for package: '$packageName'")
        if (packageName.isBlank() || packageName == "unknown") {
            Log.d(TAG, "Package name is blank or unknown, skipping check")
            return@withContext null
        }
        try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            Log.d(TAG, "Found package '$packageName': versionName=${pInfo.versionName}, versionCode=${pInfo.longVersionCode}")
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package NOT found: '$packageName' - ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error querying package '$packageName': ${e.message}", e)
            null
        }
    }
    
    /**
     * Get version info string including version name and code
     * Returns format: "1.0.1 (code: 123)"
     */
    suspend fun getVersionInfo(packageName: String): String? = withContext(Dispatchers.IO) {
        if (packageName.isBlank() || packageName == "unknown") {
            return@withContext null
        }
        try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            "${pInfo.versionName} (code: ${pInfo.longVersionCode})"
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get just the version code as Long
     */
    suspend fun getVersionCode(packageName: String): Long? = withContext(Dispatchers.IO) {
        if (packageName.isBlank() || packageName == "unknown") {
            return@withContext null
        }
        try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.longVersionCode
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract package name from an APK file
     */
    suspend fun getPackageNameFromApk(apkFile: File): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Extracting package name from APK: ${apkFile.absolutePath}")
        try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            val packageName = packageInfo?.packageName
            Log.d(TAG, "Extracted package name: $packageName")
            packageName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract package name from APK: ${e.message}", e)
            null
        }
    }

    /**
     * Get version code from an APK file
     */
    suspend fun getVersionCode(apkFile: File): Long? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            packageInfo?.longVersionCode
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract version code from APK: ${e.message}", e)
            null
        }
    }
    fun downloadApk(url: String, fileName: String, token: String? = null): Long {
        Log.d(TAG, "Starting download: $url")
        Log.d(TAG, "Token present: ${token != null}")
        
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading Update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        // Add auth header for private repos
        if (!token.isNullOrBlank()) {
            request.addRequestHeader("Authorization", "Bearer $token")
            request.addRequestHeader("Accept", "application/octet-stream")
            Log.d(TAG, "Added Authorization header")
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
    }

    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Prompts the user to uninstall a package
     */
    fun promptUninstall(packageName: String) {
        Log.d(TAG, "Prompting uninstall for package: $packageName")
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getDownloadFile(fileName: String): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
    }

    /**
     * Clears all APK files from the Downloads folder that were downloaded by this app
     */
    suspend fun clearDownloadedApks(): Int = withContext(Dispatchers.IO) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var deletedCount = 0
        
        downloadDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                Log.d(TAG, "Deleting: ${file.name}")
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        
        Log.d(TAG, "Deleted $deletedCount APK files")
        deletedCount
    }
}

