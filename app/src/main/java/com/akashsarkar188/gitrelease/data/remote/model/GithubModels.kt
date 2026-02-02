package com.akashsarkar188.gitrelease.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RepoOwner(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String
)

@JsonClass(generateAdapter = true)
data class RepoDetails(
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "private") val isPrivate: Boolean,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "owner") val owner: RepoOwner
)

@JsonClass(generateAdapter = true)
data class ReleaseResponse(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String?,
    @Json(name = "body") val body: String?,
    @Json(name = "prerelease") val prerelease: Boolean,
    @Json(name = "published_at") val publishedAt: String,
    @Json(name = "assets") val assets: List<ReleaseAsset>
)

@JsonClass(generateAdapter = true)
data class ReleaseAsset(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "url") val apiUrl: String,  // API URL for authenticated downloads
    @Json(name = "browser_download_url") val browserDownloadUrl: String,  // Public URL
    @Json(name = "content_type") val contentType: String?,
    @Json(name = "size") val size: Long
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String,
    @Json(name = "email") val email: String?,
    @Json(name = "name") val name: String?
)
