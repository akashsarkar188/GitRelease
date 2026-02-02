package com.akashsarkar188.gitrelease.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.akashsarkar188.gitrelease.data.local.entity.GithubToken
import com.akashsarkar188.gitrelease.data.local.entity.TrackedApp
import com.akashsarkar188.gitrelease.data.repository.AppRepository
import com.akashsarkar188.gitrelease.data.remote.model.RepoDetails
import com.akashsarkar188.gitrelease.data.remote.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
    object NavigateBack : SettingsEvent()
}

class SettingsViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status
    
    private val _event = MutableLiveData<SettingsEvent?>()
    val event: LiveData<SettingsEvent?> = _event

    val tokens = repository.allGithubTokens.asLiveData()

    fun addRepository(url: String) {
        val (owner, repo) = parseRepoUrl(url) ?: run {
            _status.value = "Invalid URL format. Use 'owner/repo' or full GitHub URL"
            return
        }

        viewModelScope.launch {
            _status.value = "Checking repository..."
            
            val availableTokens = repository.allGithubTokens.first()
            
            try {
                // Try each token until one works (or try with no token if none work)
                var success = false
                var trackedApp: TrackedApp? = null

                // Try with tokens first (most likely to succeed for private/rate-limited)
                for (tokenEntity in availableTokens) {
                    val response = repository.getRepoDetails(owner, repo, tokenEntity.accessToken)
                    if (response.isSuccessful) {
                        val details = response.body()!!
                        trackedApp = TrackedApp(
                            repoOwner = owner,
                            repoName = repo,
                            packageName = "",
                            appName = details.name,
                            ownerAvatarUrl = details.owner.avatarUrl,
                            accessToken = tokenEntity.accessToken
                        )
                        success = true
                        break
                    }
                }

                // If no tokens worked, try unauthenticated
                if (!success) {
                    val response = repository.getRepoDetails(owner, repo, null)
                    if (response.isSuccessful) {
                        val details = response.body()!!
                        trackedApp = TrackedApp(
                            repoOwner = owner,
                            repoName = repo,
                            packageName = "",
                            appName = details.name,
                            ownerAvatarUrl = details.owner.avatarUrl,
                            accessToken = null
                        )
                        success = true
                    }
                }

                if (success && trackedApp != null) {
                    repository.addTrackedApp(trackedApp)
                    _status.value = "Added: ${trackedApp.repoOwner}/${trackedApp.repoName}"
                    _event.value = SettingsEvent.NavigateBack
                } else {
                    _status.value = "Repo not found or access denied. Try adds a token ↑"
                }
            } catch (e: Exception) {
                _status.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun addToken(tokenString: String) {
        viewModelScope.launch {
            _status.value = "Validating token..."
            try {
                val response = repository.getUserProfile(tokenString)
                if (response.isSuccessful) {
                    val profile = response.body()!!
                    val token = GithubToken(
                        accessToken = tokenString,
                        username = profile.login,
                        avatarUrl = profile.avatarUrl,
                        email = profile.email ?: profile.name // Fallback to name if email is private
                    )
                    repository.addGithubToken(token)
                    _status.value = "Added token for ${profile.login}"
                } else {
                    _status.value = "Invalid token: ${response.code()}"
                }
            } catch (e: Exception) {
                _status.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun deleteToken(token: GithubToken) {
        viewModelScope.launch {
            repository.deleteGithubToken(token)
            _status.value = "Token removed"
        }
    }

    fun clearEvent() {
        _event.value = null
    }

    private fun parseRepoUrl(input: String): Pair<String, String>? {
        if (!input.contains("github.com") && input.contains("/")) {
            val parts = input.split("/")
            if (parts.size == 2) return parts[0] to parts[1]
        }
        try {
            val uri = Uri.parse(input)
            val path = uri.path?.trim('/') ?: return null
            val parts = path.split("/")
            if (parts.size >= 2) return parts[0] to parts[1]
        } catch (e: Exception) {
            return null
        }
        return null
    }
}


