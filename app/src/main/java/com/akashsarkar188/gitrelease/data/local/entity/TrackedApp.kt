package com.akashsarkar188.gitrelease.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracked_apps",
    indices = [Index(value = ["repoOwner", "repoName"], unique = true)]
)
data class TrackedApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoOwner: String,
    val repoName: String,
    val packageName: String,
    val appName: String,
    val ownerAvatarUrl: String? = null,  // GitHub owner avatar URL
    val accessToken: String? = null,
    val lastCheckedVersion: String? = null,
    val trackPackageNames: Map<String, String> = emptyMap() // TrackType -> PackageName mapping
) {
    val fullRepoPath: String
        get() = "$repoOwner/$repoName"
}
