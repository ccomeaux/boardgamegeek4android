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
            Timber.i("Recreating tables with not null internal ID columns and correct schemas")

            db.execSQL("ALTER TABLE `designers` RENAME TO `designers_old`")
            db.execSQL("CREATE TABLE designers (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `designer_id` INTEGER NOT NULL, `designer_name` TEXT NOT NULL, `designer_description` TEXT, `designer_image_url` TEXT, `designer_thumbnail_url` TEXT, `designer_hero_image_url` TEXT, `designer_images_updated_timestamp` INTEGER, `whitmore_score` INTEGER, `designer_stats_updated_timestamp` INTEGER, `updated` INTEGER)")
            db.execSQL("INSERT INTO `designers` (`_id`, `designer_id`, `designer_name`, `designer_description`, `designer_image_url`, `designer_thumbnail_url`, `designer_hero_image_url`, `designer_images_updated_timestamp`, `whitmore_score`, `designer_stats_updated_timestamp`, `updated`) SELECT `_id`, `designer_id`, `designer_name`, `designer_description`, `designer_image_url`, `designer_thumbnail_url`, `designer_hero_image_url`, `designer_images_updated_timestamp`, `whitmore_score`, `designer_stats_updated_timestamp`, `updated` FROM `designers_old`")
            db.dropTable("designers_old")

            db.execSQL("ALTER TABLE `artists` RENAME TO `artists_old`")
            db.execSQL("CREATE TABLE artists (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `artist_id` INTEGER NOT NULL, `artist_name` TEXT NOT NULL, `artist_description` TEXT, `artist_image_url` TEXT, `artist_thumbnail_url` TEXT, `artist_hero_image_url` TEXT, `artist_images_updated_timestamp` INTEGER, `whitmore_score` INTEGER, `artist_stats_updated_timestamp` INTEGER, `updated` INTEGER)")
            db.execSQL("INSERT INTO `artists` (`_id`, `artist_id`, `artist_name`, `artist_description`, `artist_image_url`, `artist_thumbnail_url`, `artist_hero_image_url`, `artist_images_updated_timestamp`, `whitmore_score`, `artist_stats_updated_timestamp`, `updated`) SELECT `_id`, `artist_id`, `artist_name`, `artist_description`, `artist_image_url`, `artist_thumbnail_url`, `artist_hero_image_url`, `artist_images_updated_timestamp`, `whitmore_score`, `artist_stats_updated_timestamp`, `updated` FROM `artists_old`")
            db.dropTable("artists_old")

            db.execSQL("ALTER TABLE `publishers` RENAME TO `publishers_old`")
            db.execSQL("CREATE TABLE publishers (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `publisher_id` INTEGER NOT NULL, `publisher_name` TEXT NOT NULL, `publisher_sort_name` TEXT, `publisher_description` TEXT, `publisher_image_url` TEXT, `publisher_thumbnail_url` TEXT, `publisher_hero_image_url` TEXT, `whitmore_score` INTEGER, `publisher_stats_updated_timestamp` INTEGER, `updated` INTEGER)")
            db.execSQL("INSERT INTO `publishers` (`_id`, `publisher_id`, `publisher_name`, `publisher_sort_name`, `publisher_description`, `publisher_image_url`, `publisher_thumbnail_url`, `publisher_hero_image_url`, `whitmore_score`, `publisher_stats_updated_timestamp`, `updated`) SELECT `_id`, `publisher_id`, `publisher_name`, `publisher_sort_name`, `publisher_description`, `publisher_image_url`, `publisher_thumbnail_url`, `publisher_hero_image_url`, `whitmore_score`, `publisher_stats_updated_timestamp`, `updated` FROM `publishers_old`")
            db.dropTable("publishers_old")

            db.execSQL("ALTER TABLE `categories` RENAME TO `categories_old`")
            db.execSQL("CREATE TABLE categories (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category_id` INTEGER NOT NULL, `category_name` TEXT NOT NULL)")
            db.execSQL("INSERT INTO `categories` (`_id`, `category_id`, `category_name`) SELECT `_id`, `category_id`, `category_name` FROM `categories_old`")
            db.dropTable("categories_old")

            db.execSQL("ALTER TABLE `mechanics` RENAME TO `mechanics_old`")
            db.execSQL("CREATE TABLE mechanics (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mechanic_id` INTEGER NOT NULL, `mechanic_name` TEXT NOT NULL)")
            db.execSQL("INSERT INTO `mechanics` (`_id`, `mechanic_id`, `mechanic_name`) SELECT `_id`, `mechanic_id`, `mechanic_name` FROM `mechanics_old`")
            db.dropTable("mechanics_old")

            db.execSQL("ALTER TABLE `game_colors` RENAME TO `game_colors_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `game_colors` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `color` TEXT NOT NULL, FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("INSERT INTO `game_colors` (`_id`, `game_id`, `color`) SELECT `_id`, `game_id`, `color` FROM `game_colors_old`")
            db.dropTable("game_colors_old")

            db.execSQL("ALTER TABLE `game_ranks` RENAME TO `game_ranks_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `game_ranks` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `gamerank_id` INTEGER NOT NULL, `gamerank_type` TEXT NOT NULL, `gamerank_name` TEXT NOT NULL, `gamerank_friendly_name` TEXT NOT NULL, `gamerank_value` INTEGER NOT NULL, `gamerank_bayes_average` REAL NOT NULL, FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("INSERT INTO `game_ranks` (`_id`, `game_id`, `gamerank_id`, `gamerank_type`, `gamerank_name`, `gamerank_friendly_name`, `gamerank_value`, `gamerank_bayes_average`) SELECT `_id`, `game_id`, `gamerank_id`, `gamerank_type`, `gamerank_name`, `gamerank_friendly_name`, `gamerank_value`, `gamerank_bayes_average` FROM `game_ranks_old`")
            db.dropTable("game_ranks_old")

            db.execSQL("ALTER TABLE `game_suggested_player_count_poll_results` RENAME TO `game_suggested_player_count_poll_results_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `game_suggested_player_count_poll_results` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `player_count` TEXT NOT NULL, `sort_index` INTEGER, `best_vote_count` INTEGER, `recommended_vote_count` INTEGER, `not_recommended_vote_count` INTEGER, `recommendation` INTEGER, FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("INSERT INTO `game_suggested_player_count_poll_results` (`_id`, `game_id`, `player_count`, `sort_index`, `best_vote_count`, `recommended_vote_count`, `not_recommended_vote_count`, `recommendation`) SELECT `_id`, `game_id`, `player_count`, `sort_index`, `best_vote_count`, `recommended_vote_count`, `not_recommended_vote_count`, `recommendation` FROM `game_suggested_player_count_poll_results_old`")
            db.dropTable("game_suggested_player_count_poll_results_old")

            db.execSQL("ALTER TABLE `games_artists` RENAME TO `games_artists_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `games_artists` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `artist_id` INTEGER NOT NULL, FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artist_id`) REFERENCES `artists`(`artist_id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
            db.execSQL("INSERT INTO `games_artists` (`_id`, `game_id`, `artist_id`) SELECT `_id`, `game_id`, `artist_id` FROM `games_artists_old`")
            db.dropTable("games_artists_old")

            db.execSQL("ALTER TABLE `games_designers` RENAME TO `games_designers_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `games_designers` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `designer_id` INTEGER NOT NULL)")
            db.execSQL("INSERT INTO `games_designers` (`_id`, `game_id`, `designer_id`) SELECT `_id`, `game_id`, `designer_id` FROM `games_designers_old`")
            db.dropTable("games_designers_old")

            db.execSQL("ALTER TABLE `games_publishers` RENAME TO `games_publishers_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `games_publishers` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `publisher_id` INTEGER NOT NULL)")
            db.execSQL("INSERT INTO `games_publishers` (`_id`, `game_id`, `publisher_id`) SELECT `_id`, `game_id`, `publisher_id` FROM `games_publishers_old`")
            db.dropTable("games_publishers_old")

            db.execSQL("ALTER TABLE `games_categories` RENAME TO `games_categories_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `games_categories` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL)")
            db.execSQL("INSERT INTO `games_categories` (`_id`, `game_id`, `category_id`) SELECT `_id`, `game_id`, `category_id` FROM `games_categories_old`")
            db.dropTable("games_categories_old")

            db.execSQL("ALTER TABLE `games_mechanics` RENAME TO `games_mechanics_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `games_mechanics` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `mechanic_id` INTEGER NOT NULL)")
            db.execSQL("INSERT INTO `games_mechanics` (`_id`, `game_id`, `mechanic_id`) SELECT `_id`, `game_id`, `mechanic_id` FROM `games_mechanics_old`")
            db.dropTable("games_mechanics_old")

            db.execSQL("ALTER TABLE `games_expansions` RENAME TO `games_expansions_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `games_expansions` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `game_id` INTEGER NOT NULL, `expansion_id` INTEGER NOT NULL, `expansion_name` TEXT NOT NULL, `inbound` INTEGER)")
            db.execSQL("INSERT INTO `games_expansions` (`_id`, `game_id`, `expansion_id`, `expansion_name`, `inbound`) SELECT `_id`, `game_id`, `expansion_id`, `expansion_name`, `inbound` FROM `games_expansions_old`")
            db.dropTable("games_expansions_old")

            db.execSQL("ALTER TABLE `plays` RENAME TO `plays_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `plays` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `updated_list` INTEGER NOT NULL, `play_id` INTEGER, `date` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `length` INTEGER NOT NULL, `incomplete` INTEGER NOT NULL, `no_win_stats` INTEGER NOT NULL, `location` TEXT, `comments` TEXT, `start_time` INTEGER, `player_count` INTEGER, `sync_hash_code` INTEGER, `item_name` TEXT NOT NULL, `object_id` INTEGER NOT NULL, `delete_timestamp` INTEGER, `update_timestamp` INTEGER, `dirty_timestamp` INTEGER)")
            db.execSQL("INSERT INTO `plays` (`_id`, `updated_list`, `play_id`, `date`, `quantity`, `length`, `incomplete`, `no_win_stats`, `location`, `comments`, `start_time`, `player_count`, `sync_hash_code`, `item_name`, `object_id`, `delete_timestamp`, `update_timestamp`, `dirty_timestamp`) SELECT `_id`, IFNULL(`updated_list`, 0), `play_id`, IFNULL(`date`, ''), IFNULL(`quantity`, 0), IFNULL(`length`, 0), IFNULL(`incomplete`, 0), IFNULL(`no_win_stats`, 0), `location`, `comments`, `start_time`, `player_count`, `sync_hash_code`, IFNULL(`item_name`, ''), IFNULL(`object_id`, 0), `delete_timestamp`, `update_timestamp`, `dirty_timestamp` FROM `plays_old`")
            db.dropTable("plays_old")

            db.execSQL("ALTER TABLE `play_players` RENAME TO `play_players_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `play_players` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `_play_id` INTEGER NOT NULL, `user_name` TEXT, `user_id` INTEGER, `name` TEXT, `start_position` TEXT, `color` TEXT, `score` TEXT, `new` INTEGER, `rating` REAL, `win` INTEGER)")
            db.execSQL("INSERT INTO `play_players` (`_id`, `_play_id`, `user_name`, `user_id`, `name`, `start_position`, `color`, `score`, `new`, `rating`, `win`) SELECT `_id`, `_play_id`, `user_name`, `user_id`, `name`, `start_position`, `color`, `score`, `new`, `rating`, `win` FROM `play_players_old`")
            db.dropTable("play_players_old")

            db.execSQL("ALTER TABLE `player_colors` RENAME TO `player_colors_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `player_colors` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `player_type` INTEGER NOT NULL, `player_name` TEXT NOT NULL, `player_color` TEXT NOT NULL, `player_color_sort` INTEGER NOT NULL)")
            db.execSQL("INSERT INTO `player_colors` (`_id`, `player_type`, `player_name`, `player_color`, `player_color_sort`) SELECT `_id`, `player_type`, `player_name`, `player_color`, `player_color_sort` FROM `player_colors_old`")
            db.dropTable("player_colors_old")
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
            Timber.i("Recreating collection_filters and collection_filters_details to fix AUTOINCREMENT")
            db.execSQL("ALTER TABLE `collection_filters` RENAME TO `collection_filters_old`")
            db.execSQL("ALTER TABLE `collection_filters_details` RENAME TO `collection_filters_details_old`")

            db.execSQL("CREATE TABLE IF NOT EXISTS `collection_filters` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `starred` INTEGER, `sort_type` INTEGER, `selected_count` INTEGER, `selected_timestamp` INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collection_filters_details` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `filter_id` INTEGER NOT NULL, `type` INTEGER, `data` TEXT, FOREIGN KEY(`filter_id`) REFERENCES `collection_filters`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")

            db.execSQL("INSERT INTO `collection_filters` (`_id`, `name`, `starred`, `sort_type`, `selected_count`, `selected_timestamp`) SELECT `_id`, `name`, `starred`, `sort_type`, `selected_count`, `selected_timestamp` FROM `collection_filters_old`")
            db.execSQL("INSERT INTO `collection_filters_details` (`_id`, `filter_id`, `type`, `data`) SELECT `_id`, `filter_id`, `type`, `data` FROM `collection_filters_details_old`")

            db.execSQL("DROP TABLE `collection_filters_details_old`")
            db.execSQL("DROP TABLE `collection_filters_old`")

            Timber.i("Recreating games to fix types and NOT NULL constraints")
            db.execSQL("ALTER TABLE `games` RENAME TO `games_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `games` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `updated` INTEGER, `updated_list` INTEGER NOT NULL, `game_id` INTEGER NOT NULL, `game_name` TEXT NOT NULL, `game_sort_name` TEXT NOT NULL, `year_published` INTEGER, `image_url` TEXT, `thumbnail_url` TEXT, `min_players` INTEGER, `max_players` INTEGER, `playing_time` INTEGER, `min_playing_time` INTEGER, `max_playing_time` INTEGER, `num_of_plays` INTEGER NOT NULL DEFAULT 0, `age` INTEGER, `description` TEXT, `subtype` TEXT, `usersrated` INTEGER, `average` REAL, `bayes_average` REAL, `standard_deviation` REAL, `median` REAL, `number_owned` INTEGER, `number_trading` INTEGER, `number_wanting` INTEGER, `number_wishing` INTEGER, `number_commenting` INTEGER, `number_weighting` INTEGER, `average_weight` REAL, `last_viewed` INTEGER, `starred` INTEGER, `updated_plays` INTEGER, `custom_player_sort` INTEGER, `game_rank` INTEGER, `suggested_player_count_poll_vote_total` INTEGER, `hero_image_url` TEXT, `ICON_COLOR` INTEGER, `DARK_COLOR` INTEGER, `WINS_COLOR` INTEGER, `WINNABLE_PLAYS_COLOR` INTEGER, `ALL_PLAYS_COLOR` INTEGER, `player_counts_best` TEXT, `player_counts_recommended` TEXT, `player_count_nots_recommended` TEXT)")
            db.execSQL("INSERT INTO `games` (`_id`, `updated`, `updated_list`, `game_id`, `game_name`, `game_sort_name`, `year_published`, `image_url`, `thumbnail_url`, `min_players`, `max_players`, `playing_time`, `min_playing_time`, `max_playing_time`, `num_of_plays`, `age`, `description`, `subtype`, `usersrated`, `average`, `bayes_average`, `standard_deviation`, `median`, `number_owned`, `number_trading`, `number_wanting`, `number_wishing`, `number_commenting`, `number_weighting`, `average_weight`, `last_viewed`, `starred`, `updated_plays`, `custom_player_sort`, `game_rank`, `suggested_player_count_poll_vote_total`, `hero_image_url`, `ICON_COLOR`, `DARK_COLOR`, `WINS_COLOR`, `WINNABLE_PLAYS_COLOR`, `ALL_PLAYS_COLOR`, `player_counts_best`, `player_counts_recommended`, `player_count_nots_recommended`) SELECT `_id`, `updated`, `updated_list`, `game_id`, `game_name`, `game_sort_name`, `year_published`, `image_url`, `thumbnail_url`, `min_players`, `max_players`, `playing_time`, `min_playing_time`, `max_playing_time`, `num_of_plays`, `age`, `description`, `subtype`, `usersrated`, `average`, `bayes_average`, `standard_deviation`, `median`, `number_owned`, `number_trading`, `number_wanting`, `number_wishing`, `number_commenting`, `number_weighting`, `average_weight`, `last_viewed`, `starred`, `updated_plays`, `custom_player_sort`, `game_rank`, `suggested_player_count_poll_vote_total`, `hero_image_url`, `ICON_COLOR`, `DARK_COLOR`, `WINS_COLOR`, `WINNABLE_PLAYS_COLOR`, `ALL_PLAYS_COLOR`, `player_counts_best`, `player_counts_recommended`, `player_count_nots_recommended` FROM `games_old`")
            db.execSQL("DROP TABLE `games_old`")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_games_game_id` ON `games` (`game_id`)")

            Timber.i("Recreating collection to fix AUTOINCREMENT")
            db.execSQL("ALTER TABLE `collection` RENAME TO `collection_old`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collection` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `updated` INTEGER, `updated_list` INTEGER, `game_id` INTEGER NOT NULL, `collection_id` INTEGER NOT NULL, `collection_name` TEXT NOT NULL, `collection_sort_name` TEXT NOT NULL, `own` INTEGER NOT NULL, `previously_owned` INTEGER NOT NULL, `for_trade` INTEGER NOT NULL, `want` INTEGER NOT NULL, `want_to_play` INTEGER NOT NULL, `want_to_buy` INTEGER NOT NULL, `wishlist` INTEGER NOT NULL, `wishlist_priority` INTEGER, `preordered` INTEGER NOT NULL, `comment` TEXT, `last_modified` INTEGER, `price_paid_currency` TEXT, `price_paid` REAL, `current_value_currency` TEXT, `current_value` REAL, `quantity` INTEGER, `acquisition_date` TEXT, `acquired_from` TEXT, `private_comment` TEXT, `conditiontext` TEXT, `wantpartslist` TEXT, `haspartslist` TEXT, `wishlistcomment` TEXT, `collection_year_published` INTEGER, `rating` REAL, `collection_thumbnail_url` TEXT, `collection_image_url` TEXT, `status_dirty_timestamp` INTEGER, `rating_dirty_timestamp` INTEGER, `comment_dirty_timestamp` INTEGER, `private_info_dirty_timestamp` INTEGER, `collection_dirty_timestamp` INTEGER, `collection_delete_timestamp` INTEGER, `wishlist_comment_dirty_timestamp` INTEGER, `trade_condition_dirty_timestamp` INTEGER, `want_parts_dirty_timestamp` INTEGER, `has_parts_dirty_timestamp` INTEGER, `collection_hero_image_url` TEXT, `inventory_location` TEXT, FOREIGN KEY(`game_id`) REFERENCES `games`(`game_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("INSERT INTO `collection` (`_id`, `updated`, `updated_list`, `game_id`, `collection_id`, `collection_name`, `collection_sort_name`, `own`, `previously_owned`, `for_trade`, `want`, `want_to_play`, `want_to_buy`, `wishlist`, `wishlist_priority`, `preordered`, `comment`, `last_modified`, `price_paid_currency`, `price_paid`, `current_value_currency`, `current_value`, `quantity`, `acquisition_date`, `acquired_from`, `private_comment`, `conditiontext`, `wantpartslist`, `haspartslist`, `wishlistcomment`, `collection_year_published`, `rating`, `collection_thumbnail_url`, `collection_image_url`, `status_dirty_timestamp`, `rating_dirty_timestamp`, `comment_dirty_timestamp`, `private_info_dirty_timestamp`, `collection_dirty_timestamp`, `collection_delete_timestamp`, `wishlist_comment_dirty_timestamp`, `trade_condition_dirty_timestamp`, `want_parts_dirty_timestamp`, `has_parts_dirty_timestamp`, `collection_hero_image_url`, `inventory_location`) SELECT `_id`, `updated`, `updated_list`, `game_id`, `collection_id`, `collection_name`, `collection_sort_name`, `own`, `previously_owned`, `for_trade`, `want`, `want_to_play`, `want_to_buy`, `wishlist`, `wishlist_priority`, `preordered`, `comment`, `last_modified`, `price_paid_currency`, `price_paid`, `current_value_currency`, `current_value`, `quantity`, `acquisition_date`, `acquired_from`, `private_comment`, `conditiontext`, `wantpartslist`, `haspartslist`, `wishlistcomment`, `collection_year_published`, `rating`, `collection_thumbnail_url`, `collection_image_url`, `status_dirty_timestamp`, `rating_dirty_timestamp`, `comment_dirty_timestamp`, `private_info_dirty_timestamp`, `collection_dirty_timestamp`, `collection_delete_timestamp`, `wishlist_comment_dirty_timestamp`, `trade_condition_dirty_timestamp`, `want_parts_dirty_timestamp`, `has_parts_dirty_timestamp`, `collection_hero_image_url`, `inventory_location` FROM `collection_old`")
            db.execSQL("DROP TABLE `collection_old`")

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
