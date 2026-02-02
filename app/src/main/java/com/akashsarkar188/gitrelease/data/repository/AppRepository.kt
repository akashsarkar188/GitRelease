package com.akashsarkar188.gitrelease.data.repository

import android.util.Log
import com.akashsarkar188.gitrelease.data.local.dao.TrackedAppDao
import com.akashsarkar188.gitrelease.data.local.entity.TrackedApp
import com.akashsarkar188.gitrelease.data.remote.NetworkModule
import com.akashsarkar188.gitrelease.data.remote.model.ReleaseResponse
import com.akashsarkar188.gitrelease.data.remote.model.RepoDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response

private const val TAG = "AppRepository"

class AppRepository(private val trackedAppDao: TrackedAppDao) {

    private val gitHubApi = NetworkModule.gitHubApi

    val allTrackedApps: Flow<List<TrackedApp>> = trackedAppDao.getAllApps()

    suspend fun getRepoDetails(owner: String, repo: String, token: String? = null): Response<RepoDetails> = 
        withContext(Dispatchers.IO) {
            val authHeader = token?.let { "Bearer $it" }
            gitHubApi.getRepoDetails(owner, repo, authHeader)
        }

    /**
     * Gets the latest release. Tries /releases/latest first,
     * falls back to /releases and returns the first one.
     */
    suspend fun getLatestRelease(owner: String, repo: String, token: String? = null): Response<ReleaseResponse> =
        withContext(Dispatchers.IO) {
            val authHeader = token?.let { "Bearer $it" }
            
            // Try latest first
            val latestResponse = gitHubApi.getLatestRelease(owner, repo, authHeader)
            if (latestResponse.isSuccessful) {
                Log.d(TAG, "Got latest release directly")
                return@withContext latestResponse
            }
            
            // If 404, try getting all releases
            if (latestResponse.code() == 404) {
                Log.d(TAG, "No /releases/latest, trying /releases")
                val allResponse = gitHubApi.getAllReleases(owner, repo, authHeader)
                if (allResponse.isSuccessful) {
                    val releases = allResponse.body()
                    if (!releases.isNullOrEmpty()) {
                        Log.d(TAG, "Found ${releases.size} releases, using first one")
                        // Create a fake successful Response with the first release
                        return@withContext Response.success(releases.first())
                    } else {
                        Log.d(TAG, "Releases list is empty")
                    }
                } else {
                    Log.e(TAG, "getAllReleases failed: ${allResponse.code()}")
                }
            }
            
            // Return original response (which is the error)
            latestResponse
        }
    
    /**
     * Gets all release tracks: latest stable release and latest pre-release
     * Returns a list with up to 2 releases (prerelease and stable)
     */
    suspend fun getAllReleaseTracks(owner: String, repo: String, token: String? = null): List<ReleaseResponse> =
        withContext(Dispatchers.IO) {
            val authHeader = token?.let { "Bearer $it" }
            val tracks = mutableListOf<ReleaseResponse>()
            
            try {
                val allResponse = gitHubApi.getAllReleases(owner, repo, authHeader)
                if (allResponse.isSuccessful) {
                    val releases = allResponse.body() ?: emptyList()
                    Log.d(TAG, "Fetched ${releases.size} total releases")
                    
                    // Find latest stable release (prerelease = false)
                    val latestStable = releases.firstOrNull { !it.prerelease }
                    if (latestStable != null) {
                        Log.d(TAG, "Found stable release: ${latestStable.tagName}")
                        tracks.add(latestStable)
                    }
                    
                    // Find latest pre-release (prerelease = true)
                    val latestPrerelease = releases.firstOrNull { it.prerelease }
                    if (latestPrerelease != null) {
                        Log.d(TAG, "Found pre-release: ${latestPrerelease.tagName}")
                        tracks.add(latestPrerelease)
                    }
                } else {
                    Log.e(TAG, "getAllReleaseTracks failed: ${allResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllReleaseTracks error: ${e.message}", e)
            }
            
            tracks
        }

    suspend fun addTrackedApp(app: TrackedApp) = withContext(Dispatchers.IO) {
        trackedAppDao.insertApp(app)
    }
    
    suspend fun deleteTrackedApp(app: TrackedApp) = withContext(Dispatchers.IO) {
        trackedAppDao.deleteApp(app)
    }

    suspend fun updateTrackedApp(app: TrackedApp) = withContext(Dispatchers.IO) {
        trackedAppDao.updateApp(app)
    }
}


