package com.akashsarkar188.gitrelease.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.openid.appauth.*
import org.json.JSONObject

/**
 * Manages GitHub OAuth authentication using AppAuth library.
 * 
 * Setup Instructions:
 * 1. Create a GitHub OAuth App at: https://github.com/settings/developers
 * 2. Set Authorization callback URL to: github.akashsarkar188.debughelper://oauth
 * 3. Enter your Client ID in the app's Settings screen
 */
class GitHubOAuthManager(private val context: Context) {

    private val authService = AuthorizationService(context)
    private var authState: AuthState? = null
    
    private val authorizationEndpoint = Uri.parse("https://github.com/login/oauth/authorize")
    private val tokenEndpoint = Uri.parse("https://github.com/login/oauth/access_token")
    
    private val serviceConfig = AuthorizationServiceConfiguration(
        authorizationEndpoint,
        tokenEndpoint
    )

    companion object {
        private const val PREFS_NAME = "debug_helper_prefs"
        private const val KEY_CLIENT_ID = "github_client_id"
        
        fun saveClientId(context: Context, clientId: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CLIENT_ID, clientId)
                .apply()
        }
        
        fun getClientId(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CLIENT_ID, null)
        }
        
        fun hasClientId(context: Context): Boolean {
            val id = getClientId(context)
            return !id.isNullOrBlank() && id != "YOUR_GITHUB_CLIENT_ID"
        }
    }

    /**
     * Gets the OAuth Client ID from SharedPreferences or fallback to config file
     */
    private fun getClientIdOrFallback(): String {
        // First check SharedPreferences
        val savedId = getClientId(context)
        if (!savedId.isNullOrBlank() && savedId != "YOUR_GITHUB_CLIENT_ID") {
            return savedId
        }
        
        // Fallback to config file
        return try {
            val inputStream = context.resources.openRawResource(
                context.resources.getIdentifier("auth_config", "raw", context.packageName)
            )
            val json = inputStream.bufferedReader().use { it.readText() }
            JSONObject(json).getString("client_id")
        } catch (e: Exception) {
            "YOUR_GITHUB_CLIENT_ID"
        }
    }

    /**
     * Creates authorization intent to start OAuth flow
     */
    fun createAuthIntent(): Intent {
        val clientId = getClientIdOrFallback()
        val redirectUri = Uri.parse("github.akashsarkar188.debughelper://oauth")
        
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
            .setScope("repo")
            .build()

        return authService.getAuthorizationRequestIntent(authRequest)
    }

    /**
     * Handles the OAuth callback response
     */
    fun handleAuthResponse(
        intent: Intent,
        onSuccess: (accessToken: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response != null) {
            val tokenRequest = response.createTokenExchangeRequest()
            
            authService.performTokenRequest(tokenRequest) { tokenResponse, tokenException ->
                if (tokenResponse != null) {
                    val accessToken = tokenResponse.accessToken
                    if (accessToken != null) {
                        authState = AuthState(response, tokenResponse, tokenException)
                        onSuccess(accessToken)
                    } else {
                        onError("No access token in response")
                    }
                } else {
                    onError(tokenException?.errorDescription ?: "Token exchange failed")
                }
            }
        } else {
            onError(exception?.errorDescription ?: "Authorization failed")
        }
    }

    fun dispose() {
        authService.dispose()
    }
}

