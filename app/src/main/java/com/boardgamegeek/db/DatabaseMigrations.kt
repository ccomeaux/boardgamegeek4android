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

    val MIGRATION_62_63 = object : Migration(62, 63) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.dropTable("game_poll_results_result")
            database.dropTable("game_poll_results")
            database.dropTable("game_polls")
            database.execSQL("CREATE TABLE IF NOT EXISTS `game_poll_age_results` (`game_id` INTEGER NOT NULL, `value` INTEGER NOT NULL, `votes` INTEGER NOT NULL, PRIMARY KEY(`game_id`, `value`), FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_game_poll_age_results_game_id` ON `game_poll_age_results` (`game_id`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `game_poll_language_results` (`game_id` INTEGER NOT NULL, `level` INTEGER NOT NULL, `votes` INTEGER NOT NULL, PRIMARY KEY(`game_id`, `level`), FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_game_poll_language_results_game_id` ON `game_poll_language_results` (`game_id`)")
        }
    }

    private fun SupportSQLiteDatabase.dropTable(tableName: String) = execSQL("DROP TABLE $tableName")
}
