package com.rohit.smartshare.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        FileEntity::class,
        BucketEntity::class,
        ShareEntity::class
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun fileDao(): FileDao
    abstract fun bucketDao(): BucketDao
    abstract fun shareDao(): ShareDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS buckets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        ownerId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shares (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bucketId INTEGER NOT NULL,
                        shareCode TEXT NOT NULL,
                        expiryTime INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
