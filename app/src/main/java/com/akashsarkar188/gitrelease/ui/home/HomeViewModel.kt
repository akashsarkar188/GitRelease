package com.akashsarkar188.gitrelease.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.akashsarkar188.gitrelease.data.local.entity.TrackedApp
import com.akashsarkar188.gitrelease.data.manager.ApkDownloadService
import com.akashsarkar188.gitrelease.data.manager.UpdateManager
import com.akashsarkar188.gitrelease.data.remote.model.ReleaseAsset
import com.akashsarkar188.gitrelease.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HomeViewModel"
private const val DEFAULT_REPO_OWNER = "akashsarkar188"
private const val DEFAULT_REPO_NAME = "GitRelease"
private const val DEFAULT_AVATAR_URL = "https://avatars.githubusercontent.com/u/29357444?v=4"

/**
 * Status of a track compared to installed version
 */
enum class TrackStatus {
    INSTALLED,      // This exact version is installed
    UPDATE,         // Newer than installed
    OLD,            // Older than installed
    UNKNOWN         // Can't determine (package unknown)
}

/**
 * Represents a single release track (Pre-Release or Release)
 */
data class TrackInfo(
    val trackType: String,  // "Release" or "Pre-Release"
    val version: String,    // e.g., "1.0.1"
    val versionCode: String?, // From APK or tag
    val title: String?,     // Release title
    val changelog: String?, // Release body/notes
    val apkAsset: ReleaseAsset?,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val status: TrackStatus = TrackStatus.UNKNOWN
)

/**
 * Information about an installed package
 */
data class InstalledPackageInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Long?
)

/**
 * Represents the UI state for a tracked app with all its tracks
 */
data class AppUiState(
    val app: TrackedApp,
    val installedPackages: List<InstalledPackageInfo>,
    val tracks: List<TrackInfo>,
    val isUpToDate: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: AppRepository,
    private val updateManager: UpdateManager,
    private val downloadService: ApkDownloadService
) : ViewModel() {

    val trackedApps: LiveData<List<TrackedApp>> = repository.allTrackedApps.asLiveData()
    
    private val _appStates = MutableLiveData<List<AppUiState>>(emptyList())
    val appStates: LiveData<List<AppUiState>> = _appStates
    
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun refreshApps() {
        viewModelScope.launch {
            Log.d(TAG, "refreshApps() called")
            _isLoading.value = true
            
            withContext(Dispatchers.Default) {
                var apps = trackedApps.value ?: emptyList()
                
                // Seed default repo if missing.
                val hasDefaultRepo = apps.any { it.repoOwner.equals(DEFAULT_REPO_OWNER, ignoreCase = true) && 
                                               it.repoName.equals(DEFAULT_REPO_NAME, ignoreCase = true) }
                
                if (!hasDefaultRepo) {
                    Log.d(TAG, "Default repo missing, seeding: $DEFAULT_REPO_OWNER/$DEFAULT_REPO_NAME")
                    val defaultApp = TrackedApp(
                        repoOwner = DEFAULT_REPO_OWNER,
                        repoName = DEFAULT_REPO_NAME,
                        packageName = "com.akashsarkar188.gitrelease",
                        appName = "GitRelease",
                        ownerAvatarUrl = DEFAULT_AVATAR_URL
                    )
                    repository.addTrackedApp(defaultApp)
                    apps = apps + defaultApp
                }
                
                Log.d(TAG, "Refreshing ${apps.size} apps")
                
                val states = mutableListOf<AppUiState>()
                
                for (app in apps) {
                    Log.d(TAG, "Processing app: ${app.appName} (${app.fullRepoPath})")
                    
                    // Get all tracking package names
                    val pkgNames = app.trackPackageNames.values.distinct().toMutableList()
                    if (app.packageName.isNotBlank() && !pkgNames.contains(app.packageName)) {
                        pkgNames.add(app.packageName)
                    }

                    // Gather installed package info for all tracked packages
                    val installedPackages = pkgNames.mapNotNull { pkg ->
                        val versionName = updateManager.getCurrentVersion(pkg)
                        if (versionName != null) {
                            InstalledPackageInfo(
                                packageName = pkg,
                                versionName = versionName,
                                versionCode = updateManager.getVersionCode(pkg)
                            )
                        } else null
                    }

                    // Fetch all release tracks
                    val tracks = mutableListOf<TrackInfo>()
                    
                    try {
                        val releases = repository.getAllReleaseTracks(
                            app.repoOwner,
                            app.repoName,
                            app.accessToken
                        )
                        
                        Log.d(TAG, "  Found ${releases.size} tracks")
                        
                        // Find the highest version among all tracks (for "up to date" check)
                        var highestVersion: String? = null
                        
                        for (release in releases) {
                            val trackType = if (release.prerelease) "Pre-Release" else "Release"
                            val version = release.tagName.removePrefix("v")
                            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                            val versionCode = extractVersionCode(release.tagName)
                            
                            // Find the relevant package name for this track
                            val targetPkg = app.trackPackageNames[trackType]?.trim() ?: run {
                                // Only fallback to global packageName if NO tracks have been mapped yet
                                if (app.trackPackageNames.isEmpty()) app.packageName.trim() else ""
                            }
                            
                            val installedInfo = installedPackages.find { it.packageName == targetPkg }
                            val installedVersionName = installedInfo?.versionName?.trim() ?: ""
                            val installedVersionCode = installedInfo?.versionCode

                            // Determine track status: Prioritize Version Code if both available
                            val releaseVersionCode = versionCode?.toLongOrNull()
                            
                            val status = when {
                                targetPkg.isBlank() -> TrackStatus.UNKNOWN
                                installedInfo == null -> TrackStatus.UPDATE  // Not installed = available
                                releaseVersionCode != null && installedVersionCode != null -> {
                                    when {
                                        releaseVersionCode > installedVersionCode -> TrackStatus.UPDATE
                                        releaseVersionCode < installedVersionCode -> TrackStatus.OLD
                                        else -> TrackStatus.INSTALLED
                                    }
                                }
                                else -> {
                                    // Fallback to version name comparison
                                    val compResult = compareVersions(version, installedVersionName)
                                    when {
                                        compResult > 0 -> TrackStatus.UPDATE
                                        compResult < 0 -> TrackStatus.OLD
                                        else -> TrackStatus.INSTALLED
                                    }
                                }
                            }
                            
                            Log.d(TAG, "  Track: $trackType - version: '$version' (code: $releaseVersionCode), installed: '$installedVersionName' (code: $installedVersionCode), status: $status, pkg: '$targetPkg'")

                            // Track highest version
                            if (highestVersion == null || compareVersions(version, highestVersion) > 0) {
                                highestVersion = version
                            }
                            
                            tracks.add(TrackInfo(
                                trackType = trackType,
                                version = version,
                                versionCode = versionCode,
                                title = release.name ?: release.tagName,
                                changelog = release.body,
                                apkAsset = apkAsset,
                                publishedAt = release.publishedAt,
                                isPrerelease = release.prerelease,
                                status = status
                            ))
                        }
                        
                        // Check if up to date (installed version >= highest available)
                        val isUpToDate = installedPackages.isNotEmpty() && 
                                         highestVersion != null &&
                                         installedPackages.any { compareVersions(it.versionName ?: "", highestVersion) >= 0 }
                        
                        states.add(AppUiState(
                            app = app,
                            installedPackages = installedPackages,
                            tracks = tracks,
                            isUpToDate = isUpToDate
                        ))
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching tracks for ${app.appName}: ${e.message}", e)
                        states.add(AppUiState(
                            app = app,
                            installedPackages = installedPackages,
                            tracks = emptyList(),
                            errorMessage = e.message
                        ))
                    }
                }
                
                _appStates.postValue(states)
                _isLoading.postValue(false)
                _statusMessage.postValue("Checked ${apps.size} app(s)")
            }
            Log.d(TAG, "refreshApps() completed")
        }
    }
    
    /**
     * Try to extract version code from tag name (e.g., "v1.0.1(2)" -> "2")
     */
    private fun extractVersionCode(tagName: String): String? {
        val match = Regex("\\((\\d+)\\)").find(tagName)
        return match?.groupValues?.get(1)
    }
    
    /**
     * Compare two version strings.
     * Returns: positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val s1 = v1.trim().removePrefix("v").removePrefix("V")
        val s2 = v2.trim().removePrefix("v").removePrefix("V")
        
        if (s1 == s2) return 0
        if (s1.isEmpty()) return -1
        if (s2.isEmpty()) return 1
        
        val parts1 = s1.split(".").map { it.takeWhile { char -> char.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = s2.split(".").map { it.takeWhile { char -> char.isDigit() }.toIntOrNull() ?: 0 }
        
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        
        // If numeric parts are equal, string comparison for safety (e.g. 1.0.0 vs 1.0.0-beta)
        return s1.compareTo(s2, ignoreCase = true)
    }
    
    fun downloadTrack(app: TrackedApp, track: TrackInfo) {
        val apkAsset = track.apkAsset ?: run {
            Log.e(TAG, "downloadTrack called but no APK asset!")
            _statusMessage.value = "No APK available in ${track.trackType}"
            return
        }
        
        // For private repos (with token), use API URL
        // For public repos (no token), use browser download URL
        val downloadUrl = if (!app.accessToken.isNullOrBlank()) {
            Log.d(TAG, "Using API URL for authenticated download")
            apkAsset.apiUrl
        } else {
            Log.d(TAG, "Using browser URL for public download")
            apkAsset.browserDownloadUrl
        }
        
        Log.d(TAG, "Starting download: $downloadUrl")
        
        viewModelScope.launch {
            _statusMessage.value = "Downloading ${apkAsset.name}..."
            
            val result = downloadService.downloadApk(
                url = downloadUrl,
                fileName = apkAsset.name,
                token = app.accessToken,
                onProgress = { progress ->
                    Log.d(TAG, "Download progress: $progress%")
                }
            )
            
            when (result) {
                is ApkDownloadService.DownloadResult.Success -> {
                    Log.d(TAG, "Download complete: ${result.file.absolutePath}")
                    
                    // Detect APK info
                    val extractedPkg = updateManager.getPackageNameFromApk(result.file)
                    val extractedCode = updateManager.getVersionCode(result.file) ?: 0L
                    
                    if (extractedPkg != null) {
                        Log.d(TAG, "Extracted info: $extractedPkg (vCode: $extractedCode)")
                        
                        // Update app mapping in DB
                        val updatedMap = app.trackPackageNames.toMutableMap()
                        updatedMap[track.trackType] = extractedPkg
                        
                        var updatedApp = app.copy(trackPackageNames = updatedMap)
                        // Also update global packageName if currently blank
                        if (updatedApp.packageName.isBlank()) {
                            updatedApp = updatedApp.copy(packageName = extractedPkg)
                        }
                        repository.updateTrackedApp(updatedApp)

                        // Check for Downgrade
                        val installedCode = updateManager.getVersionCode(extractedPkg) ?: 0L
                        if (installedCode > 0 && extractedCode < installedCode) {
                            Log.d(TAG, "Downgrade detected: $extractedCode < $installedCode. Prompting uninstall.")
                            _statusMessage.value = "Downgrade detected! Please uninstall previous version first."
                            updateManager.promptUninstall(extractedPkg)
                        } else {
                            // Normal install or upgrade
                            Log.d(TAG, "Normal install/upgrade triggered")
                            _statusMessage.value = "Installing..."
                            updateManager.installApk(result.file)
                        }
                    } else {
                        Log.w(TAG, "Could not extract package name from APK, trying direct install")
                        updateManager.installApk(result.file)
                    }
                }
                is ApkDownloadService.DownloadResult.Error -> {
                    Log.e(TAG, "Download failed: ${result.message}")
                    _statusMessage.value = "Download failed: ${result.message}"
                }
            }
        }
    }
    
    fun deleteApp(app: TrackedApp) {
        if (app.repoOwner.equals(DEFAULT_REPO_OWNER, ignoreCase = true) && 
            app.repoName.equals(DEFAULT_REPO_NAME, ignoreCase = true)) {
            Log.w(TAG, "Attempted to delete default repo! Blocked.")
            _statusMessage.value = "Cannot remove the GitRelease repository."
            return
        }
        
        Log.d(TAG, "Deleting app: ${app.appName}")
        viewModelScope.launch {
            repository.deleteTrackedApp(app)
            _statusMessage.value = "Removed ${app.appName}"
        }
    }
    
    fun clearDownloads() {
        Log.d(TAG, "Clearing downloaded APKs")
        viewModelScope.launch {
            val count = updateManager.clearDownloadedApks()
            _statusMessage.value = "Deleted $count APK file(s)"
        }
    }
}
