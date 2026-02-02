package com.akashsarkar188.gitrelease.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.akashsarkar188.gitrelease.data.local.dao.TrackedAppDao
import com.akashsarkar188.gitrelease.data.local.dao.GithubTokenDao
import com.akashsarkar188.gitrelease.data.local.entity.TrackedApp
import com.akashsarkar188.gitrelease.data.local.entity.GithubToken

import androidx.room.TypeConverters

@Database(entities = [TrackedApp::class, GithubToken::class], version = 5, exportSchema = false)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao
    abstract fun githubTokenDao(): GithubTokenDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2: add ownerAvatarUrl column
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tracked_apps ADD COLUMN ownerAvatarUrl TEXT")
            }
        }

        // Migration from version 2 to 3: add trackPackageNames column
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tracked_apps ADD COLUMN trackPackageNames TEXT NOT NULL DEFAULT '{}'")
            }
        }

        // Migration from version 3 to 4: add github_tokens table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS github_tokens (
                        accessToken TEXT NOT NULL, 
                        username TEXT NOT NULL, 
                        avatarUrl TEXT, 
                        email TEXT, 
                        addedAt INTEGER NOT NULL, 
                        PRIMARY KEY(accessToken)
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 4 to 5: add unique index to tracked_apps and clean duplicates
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Find duplicates and keep only the one with the highest ID (most recent)
                database.execSQL("""
                    DELETE FROM tracked_apps 
                    WHERE id NOT IN (
                        SELECT MAX(id) 
                        FROM tracked_apps 
                        GROUP BY repoOwner, repoName
                    )
                """.trimIndent())
                
                // 2. Add the unique index
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tracked_apps_repoOwner_repoName ON tracked_apps(repoOwner, repoName)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debug_helper_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
