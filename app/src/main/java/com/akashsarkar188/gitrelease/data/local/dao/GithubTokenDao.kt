package com.akashsarkar188.gitrelease.data.local.dao

import androidx.room.*
import com.akashsarkar188.gitrelease.data.local.entity.GithubToken
import kotlinx.coroutines.flow.Flow

@Dao
interface GithubTokenDao {
    @Query("SELECT * FROM github_tokens ORDER BY addedAt DESC")
    fun getAllTokens(): Flow<List<GithubToken>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: GithubToken)

    @Delete
    suspend fun deleteToken(token: GithubToken)

    @Query("SELECT * FROM github_tokens WHERE accessToken = :token LIMIT 1")
    suspend fun getToken(token: String): GithubToken?
}
