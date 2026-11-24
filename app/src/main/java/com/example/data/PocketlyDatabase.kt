package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        ScheduledItemEntity::class,
        HoldingEntity::class,
        HoldingContributionEntity::class,
        HoldingValuePointEntity::class,
        ActivityItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PocketlyDatabase : RoomDatabase() {
    abstract fun dao(): PocketlyDao

    companion object {
        @Volatile
        private var INSTANCE: PocketlyDatabase? = null

        /**
         * Versioned migration scaffold so future schema upgrades preserve user financial history.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun getInstance(context: Context): PocketlyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PocketlyDatabase::class.java,
                    "pocketly_local.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
