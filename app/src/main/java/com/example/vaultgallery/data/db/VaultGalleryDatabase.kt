package com.example.vaultgallery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaItem::class,
        RecentItem::class,
        FavoriteItem::class,
        Bookmark::class,
        AppSettings::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VaultGalleryDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun recentItemDao(): RecentItemDao
    abstract fun favoriteItemDao(): FavoriteItemDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        private const val DATABASE_NAME = "vault_gallery.db"

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_items_mediaType_isAccessible_dateModified` " +
                        "ON `media_items` (`mediaType`, `isAccessible`, `dateModified`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_items_bucketId_isAccessible_dateModified` " +
                        "ON `media_items` (`bucketId`, `isAccessible`, `dateModified`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_media_items_source_mediaType_lastSyncedAt` " +
                        "ON `media_items` (`source`, `mediaType`, `lastSyncedAt`)"
                )
            }
        }

        fun create(context: Context): VaultGalleryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VaultGalleryDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
