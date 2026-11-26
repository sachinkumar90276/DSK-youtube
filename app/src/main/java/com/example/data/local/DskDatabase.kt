package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ProfileEntity::class,
        AdminRoleEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        VideoEntity::class,
        ShortEntity::class,
        CommentEntity::class,
        CommentReplyEntity::class,
        LikeEntity::class,
        DislikeEntity::class,
        SubscriptionEntity::class,
        PlaylistEntity::class,
        PlaylistVideoEntity::class,
        WatchHistoryEntity::class,
        WatchLaterEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DskDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun channelDao(): ChannelDao
    abstract fun videoDao(): VideoDao
    abstract fun shortDao(): ShortDao
    abstract fun commentDao(): CommentDao
    abstract fun interactionDao(): InteractionDao
    abstract fun libraryDao(): LibraryDao
    abstract fun systemDao(): SystemDao

    companion object {
        @Volatile
        private var INSTANCE: DskDatabase? = null

        fun getInstance(context: Context): DskDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DskDatabase::class.java,
                    "dsk_youtube.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(SeedCategoriesCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedCategoriesCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = INSTANCE ?: return@launch
                seedCategoriesOnly(database)
            }
        }
    }
}

private suspend fun seedCategoriesOnly(db: DskDatabase) {
    // Only seed real YouTube discovery categories — NO fake videos, NO fake channels, NO demo data.
    val categories = listOf(
        "Music", "Gaming", "Movies", "Entertainment", "News",
        "Live", "Comedy", "Technology", "Education", "Sports", "Travel"
    )
    categories.forEachIndexed { index, name ->
        db.systemDao().upsertCategory(
            CategoryEntity(
                id = "cat_${name.lowercase()}",
                name = name,
                sortOrder = index + 1
            )
        )
    }
}
