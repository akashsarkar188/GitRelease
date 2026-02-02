package com.akashsarkar188.gitrelease.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akashsarkar188.gitrelease.auth.TokenManager
import com.akashsarkar188.gitrelease.data.local.entity.TrackedApp
import com.akashsarkar188.gitrelease.data.repository.AppRepository
import kotlinx.coroutines.launch

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
    object NavigateBack : SettingsEvent()
}

class SettingsViewModel(
    private val repository: AppRepository,
    private val context: Context
) : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status
    
    private val _event = MutableLiveData<SettingsEvent?>()
    val event: LiveData<SettingsEvent?> = _event

    fun addRepository(url: String) {
        val (owner, repo) = parseRepoUrl(url) ?: run {
            _status.value = "Invalid URL format. Use 'owner/repo' or full GitHub URL"
            return
        }

        viewModelScope.launch {
            _status.value = "Checking repository..."
            
            // Get saved token (if any)
            val token = TokenManager.getToken(context)
            
            try {
                // Try with token first (works for both public & private)
                val response = repository.getRepoDetails(owner, repo, token)
                
                if (response.isSuccessful) {
                    val details = response.body()!!
                    val app = TrackedApp(
                        repoOwner = owner,
                        repoName = repo,
                        packageName = "",  // Auto-detected after first download
                        appName = details.name,
                        ownerAvatarUrl = details.owner.avatarUrl,
                        accessToken = token // Store token with the app
                    )
                    repository.addTrackedApp(app)
                    _status.value = "Added: ${details.fullName}"
                    _event.value = SettingsEvent.NavigateBack
                } else if (response.code() == 404 || response.code() == 401) {
                    if (token.isNullOrBlank()) {
                        _status.value = "Private repo. Add your GitHub token first ↑"
                    } else {
                        _status.value = "Access denied. Check your token has 'repo' scope."
                    }
                } else {
                    _status.value = "Error: ${response.message()}"
                }
            } catch (e: Exception) {
                _status.value = "Network Error: ${e.localizedMessage}"
            }
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


