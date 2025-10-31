package com.boardgamegeek.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggDatabase
import com.boardgamegeek.util.TableBuilder
import timber.log.Timber

object DatabaseMigrations {
    val MIGRATION_56_57 = object : Migration(56, 57) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Adding player count columns to the games table")
            db.execSQL("ALTER TABLE ${BggDatabase.Tables.GAMES} ADD COLUMN ${BggContract.Games.Columns.PLAYER_COUNTS_BEST} ${TableBuilder.ColumnType.TEXT}")
            db.execSQL("ALTER TABLE ${BggDatabase.Tables.GAMES} ADD COLUMN ${BggContract.Games.Columns.PLAYER_COUNTS_RECOMMENDED} ${TableBuilder.ColumnType.TEXT}")
            db.execSQL("ALTER TABLE ${BggDatabase.Tables.GAMES} ADD COLUMN ${BggContract.Games.Columns.PLAYER_COUNTS_NOT_RECOMMENDED} ${TableBuilder.ColumnType.TEXT}")
            db.execSQL("UPDATE ${BggDatabase.Tables.GAMES} SET ${BggContract.Games.Columns.UPDATED_LIST}=0, ${BggContract.Games.Columns.UPDATED}=0, ${BggContract.Games.Columns.UPDATED_PLAYS}=0")
        }
    }

    val MIGRATION_57_58 = object : Migration(57, 58) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Replacing table 'buddies' with 'users'")
            db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`username` TEXT NOT NULL PRIMARY KEY, `first_name` TEXT, `last_name` TEXT, `avatar_url` TEXT, `play_nickname` TEXT, `buddy_flag` INTEGER, `sync_hash_code` INTEGER, `updated_list_timestamp` INTEGER, `updated_detail_timestamp` INTEGER)")
            db.execSQL(
                """
                INSERT INTO users (username, first_name, last_name, avatar_url, play_nickname, buddy_flag, sync_hash_code, updated_detail_timestamp, updated_list_timestamp)
                SELECT buddy_name, buddy_firtname, buddy_lastname, avatar_url, play_nickname, buddy_flag, sync_hash_code, updated, updated_list FROM buddies
                """.trimIndent()
            )
            db.dropTable("buddies")
        }
    }

    val MIGRATION_58_59 = object : Migration(58, 59) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Skipping recreating tables with not null internal ID columns")
        }
    }

    val MIGRATION_59_60 = object : Migration(59, 60) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Adding missing indexes on games and artists")
            db.execSQL("CREATE UNIQUE INDEX index_games_game_id ON games(game_id)")
            db.execSQL("CREATE UNIQUE INDEX index_artists_artist_id ON artists(artist_id)")
        }
    }

    val MIGRATION_60_61 = object : Migration(60, 61) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Initializing Room database from version 60...")
        }
    }

    val MIGRATION_61_62 = object : Migration(61, 62) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Adding missing indexes on designers, publishers, categories, and mechanics")
            db.execSQL("DELETE FROM designers")
            db.execSQL("CREATE UNIQUE INDEX index_designers_designer_id ON designers(designer_id)")
            db.execSQL("DELETE FROM publishers")
            db.execSQL("CREATE UNIQUE INDEX index_publishers_publisher_id ON publishers(publisher_id)")
            db.execSQL("DELETE FROM categories")
            db.execSQL("CREATE UNIQUE INDEX index_categories_category_id ON categories(category_id)")
            db.execSQL("DELETE FROM mechanics")
            db.execSQL("CREATE UNIQUE INDEX index_mechanics_mechanic_id ON mechanics(mechanic_id)")
        }
    }

    val MIGRATION_62_63 = object : Migration(62, 63) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Recreating the game poll tables")
            db.dropTable("game_poll_results_result")
            db.dropTable("game_poll_results")
            db.dropTable("game_polls")
            db.execSQL("CREATE TABLE IF NOT EXISTS `game_poll_age_results` (`game_id` INTEGER NOT NULL, `value` INTEGER NOT NULL, `votes` INTEGER NOT NULL, PRIMARY KEY(`game_id`, `value`), FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_poll_age_results_game_id` ON `game_poll_age_results` (`game_id`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `game_poll_language_results` (`game_id` INTEGER NOT NULL, `level` INTEGER NOT NULL, `votes` INTEGER NOT NULL, PRIMARY KEY(`game_id`, `level`), FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_poll_language_results_game_id` ON `game_poll_language_results` (`game_id`)")
        }
    }

    val MIGRATION_63_64 = object : Migration(63, 64) {
        override fun migrate(db: SupportSQLiteDatabase) {
            Timber.i("Adding missing indexes to several tables")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_filters_details_filter_id` ON `collection_filters_details` (`filter_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_colors_game_id` ON `game_colors` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_ranks_game_id` ON `game_ranks` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_suggested_player_count_poll_results_game_id` ON `game_suggested_player_count_poll_results` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_artists_game_id` ON `games_artists` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_artists_artist_id` ON `games_artists` (`artist_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_designers_game_id` ON `games_designers` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_designers_designer_id` ON `games_designers` (`designer_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_publishers_game_id` ON `games_publishers` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_publishers_publisher_id` ON `games_publishers` (`publisher_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_categories_game_id` ON `games_categories` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_categories_category_id` ON `games_categories` (`category_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_mechanics_game_id` ON `games_mechanics` (`game_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_mechanics_mechanic_id` ON `games_mechanics` (`mechanic_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_game_id` ON `collection` (`game_id`)")
        }
    }

    private fun SupportSQLiteDatabase.dropTable(tableName: String) = execSQL("DROP TABLE $tableName")
}
