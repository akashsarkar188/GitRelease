package com.akashsarkar188.gitrelease.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "github_tokens")
data class GithubToken(
    @PrimaryKey val accessToken: String,
    val username: String,
    val avatarUrl: String?,
    val email: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
