package com.boardgamegeek.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

object DatabaseMigrations {
    val MIGRATION_60_61 = object : Migration(60, 61) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Timber.i("Initializing Room database...")
        }
    }

    val MIGRATION_61_62 = object : Migration(61, 62) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Timber.i("Adding missing indexes to the games credits tables")
            database.execSQL("DELETE FROM designers")
            database.execSQL("CREATE UNIQUE INDEX index_designers_designer_id ON designers(designer_id)")
            database.execSQL("DELETE FROM publishers")
            database.execSQL("CREATE UNIQUE INDEX index_publishers_publisher_id ON publishers(publisher_id)")
            database.execSQL("DELETE FROM categories")
            database.execSQL("CREATE UNIQUE INDEX index_categories_category_id ON categories(category_id)")
            database.execSQL("DELETE FROM mechanics")
            database.execSQL("CREATE UNIQUE INDEX index_mechanics_mechanic_id ON mechanics(mechanic_id)")
        }
    }
}