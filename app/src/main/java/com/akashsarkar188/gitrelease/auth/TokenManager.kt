package com.akashsarkar188.gitrelease.auth

import android.content.Context

/**
 * Simple token manager using SharedPreferences.
 * User creates a Personal Access Token at github.com/settings/tokens
 * with 'repo' scope, then pastes it here.
 */
object TokenManager {
    private const val PREFS_NAME = "debug_helper_prefs"
    private const val KEY_GITHUB_TOKEN = "github_pat"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GITHUB_TOKEN, token)
            .apply()
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GITHUB_TOKEN, null)
    }

    fun hasToken(context: Context): Boolean {
        return !getToken(context).isNullOrBlank()
    }

    fun clearToken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_GITHUB_TOKEN)
            .apply()
    }
}
