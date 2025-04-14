@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.provider

import android.net.Uri
import androidx.core.net.toUri

class BggContract {
    object Games {
        object Columns {
            const val GAME_ID = "game_id"
            const val GAME_NAME = "game_name"
            const val GAME_SORT_NAME = "game_sort_name"
            const val YEAR_PUBLISHED = "year_published"
            const val IMAGE_URL = "image_url"
            const val THUMBNAIL_URL = "thumbnail_url"
            const val MIN_PLAYERS = "min_players"
            const val MAX_PLAYERS = "max_players"
            const val PLAYING_TIME = "playing_time"
            const val MIN_PLAYING_TIME = "min_playing_time"
            const val MAX_PLAYING_TIME = "max_playing_time"
            const val NUM_PLAYS = "num_of_plays"
            const val MINIMUM_AGE = "age"
            const val DESCRIPTION = "description"
            const val SUBTYPE = "subtype"
            const val STATS_USERS_RATED = "usersrated"
            const val STATS_AVERAGE = "average"
            const val STATS_BAYES_AVERAGE = "bayes_average"
            const val STATS_STANDARD_DEVIATION = "standard_deviation"
            const val STATS_MEDIAN = "median"
            const val STATS_NUMBER_OWNED = "number_owned"
            const val STATS_NUMBER_TRADING = "number_trading"
            const val STATS_NUMBER_WANTING = "number_wanting"
            const val STATS_NUMBER_WISHING = "number_wishing"
            const val STATS_NUMBER_COMMENTS = "number_commenting"
            const val STATS_NUMBER_WEIGHTS = "number_weighting"
            const val STATS_AVERAGE_WEIGHT = "average_weight"
            const val LAST_VIEWED = "last_viewed"
            const val STARRED = "starred"
            const val UPDATED_PLAYS = "updated_plays"
            const val CUSTOM_PLAYER_SORT = "custom_player_sort"
            const val GAME_RANK = "game_rank"
            const val SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL = "suggested_player_count_poll_vote_total"
            const val HERO_IMAGE_URL = "hero_image_url"
            const val ICON_COLOR = "ICON_COLOR"
            const val DARK_COLOR = "DARK_COLOR"
            const val WINS_COLOR = "WINS_COLOR"
            const val WINNABLE_PLAYS_COLOR = "WINNABLE_PLAYS_COLOR"
            const val ALL_PLAYS_COLOR = "ALL_PLAYS_COLOR"
            const val PLAYER_COUNTS_BEST = "player_counts_best"
            const val PLAYER_COUNTS_RECOMMENDED = "player_counts_recommended"
            const val PLAYER_COUNTS_NOT_RECOMMENDED = "player_count_nots_recommended"
            const val UPDATED = COL_UPDATED
            const val UPDATED_LIST = COL_UPDATED_LIST
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_GAMES).build()

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.game"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.game"

        fun getGameId(uri: Uri): Int {
            val index = uri.pathSegments.indexOf(PATH_GAMES)
            return if (index == -1)
                INVALID_ID
            else
                uri.pathSegments.getOrNull(index + 1)?.toIntOrNull() ?: INVALID_ID
        }
    }

    object GameRanks {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val GAME_RANK_ID = "gamerank_id"
            const val GAME_RANK_TYPE = "gamerank_type"
            const val GAME_RANK_NAME = "gamerank_name"
            const val GAME_RANK_FRIENDLY_NAME = "gamerank_friendly_name"
            const val GAME_RANK_VALUE = "gamerank_value"
            const val GAME_RANK_BAYES_AVERAGE = "gamerank_bayes_average"
        }
    }

    object Designers {
        object Columns {
            const val DESIGNER_ID = "designer_id"
            const val DESIGNER_NAME = "designer_name"
            const val DESIGNER_DESCRIPTION = "designer_description"
            const val DESIGNER_IMAGE_URL = "designer_image_url"
            const val DESIGNER_THUMBNAIL_URL = "designer_thumbnail_url"
            const val DESIGNER_HERO_IMAGE_URL = "designer_hero_image_url"
            const val DESIGNER_IMAGES_UPDATED_TIMESTAMP = "designer_images_updated_timestamp"
            const val WHITMORE_SCORE = "whitmore_score"
            const val DESIGNER_STATS_UPDATED_TIMESTAMP = "designer_stats_updated_timestamp"
            const val ITEM_COUNT = "item_count"
            const val UPDATED = COL_UPDATED
        }
    }

    object Artists {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val ARTIST_ID = "artist_id"
            const val ARTIST_NAME = "artist_name"
            const val ARTIST_DESCRIPTION = "artist_description"
            const val ARTIST_IMAGE_URL = "artist_image_url"
            const val ARTIST_THUMBNAIL_URL = "artist_thumbnail_url"
            const val ARTIST_HERO_IMAGE_URL = "artist_hero_image_url"
            const val ARTIST_IMAGES_UPDATED_TIMESTAMP = "artist_images_updated_timestamp"
            const val WHITMORE_SCORE = "whitmore_score"
            const val ARTIST_STATS_UPDATED_TIMESTAMP = "artist_stats_updated_timestamp"
            const val ITEM_COUNT = "item_count"
            const val UPDATED = COL_UPDATED
        }
    }

    object Publishers {
        object Columns {
            const val PUBLISHER_ID = "publisher_id"
            const val PUBLISHER_NAME = "publisher_name"
            const val PUBLISHER_DESCRIPTION = "publisher_description"
            const val PUBLISHER_IMAGE_URL = "publisher_image_url"
            const val PUBLISHER_THUMBNAIL_URL = "publisher_thumbnail_url"
            const val PUBLISHER_HERO_IMAGE_URL = "publisher_hero_image_url"
            const val PUBLISHER_SORT_NAME = "publisher_sort_name"
            const val WHITMORE_SCORE = "whitmore_score"
            const val PUBLISHER_STATS_UPDATED_TIMESTAMP = "publisher_stats_updated_timestamp"
            const val ITEM_COUNT = "item_count"
            const val UPDATED = COL_UPDATED
        }
    }

    object Mechanics {
        object Columns {
            const val MECHANIC_ID = "mechanic_id"
            const val MECHANIC_NAME = "mechanic_name"
            const val ITEM_COUNT = "item_count"
        }
    }

    object Categories {
        object Columns {
            const val CATEGORY_ID = "category_id"
            const val CATEGORY_NAME = "category_name"
            const val ITEM_COUNT = "item_count"
        }
    }

    object GamesExpansions {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val EXPANSION_ID = "expansion_id"
            const val EXPANSION_NAME = "expansion_name"
            const val INBOUND = "inbound"
        }
    }

    object Collection {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val COLLECTION_ID = "collection_id"
            const val COLLECTION_NAME = "collection_name"
            const val COLLECTION_SORT_NAME = "collection_sort_name"
            const val STATUS_OWN = "own"
            const val STATUS_PREVIOUSLY_OWNED = "previously_owned"
            const val STATUS_FOR_TRADE = "for_trade"
            const val STATUS_WANT = "want"
            const val STATUS_WANT_TO_PLAY = "want_to_play"
            const val STATUS_WANT_TO_BUY = "want_to_buy"
            const val STATUS_WISHLIST = "wishlist"
            const val STATUS_WISHLIST_PRIORITY = "wishlist_priority"
            const val STATUS_PREORDERED = "preordered"
            const val COLLECTION_YEAR_PUBLISHED = "collection_year_published"
            const val RATING = "rating"
            const val COMMENT = "comment"
            const val CONDITION = "conditiontext"
            const val WANTPARTS_LIST = "wantpartslist"
            const val HASPARTS_LIST = "haspartslist"
            const val WISHLIST_COMMENT = "wishlistcomment"
            const val COLLECTION_THUMBNAIL_URL = "collection_thumbnail_url"
            const val COLLECTION_IMAGE_URL = "collection_image_url"
            const val LAST_MODIFIED = "last_modified"
            const val PRIVATE_INFO_PRICE_PAID_CURRENCY = "price_paid_currency"
            const val PRIVATE_INFO_PRICE_PAID = "price_paid"
            const val PRIVATE_INFO_CURRENT_VALUE_CURRENCY = "current_value_currency"
            const val PRIVATE_INFO_CURRENT_VALUE = "current_value"
            const val PRIVATE_INFO_QUANTITY = "quantity"
            const val PRIVATE_INFO_ACQUISITION_DATE = "acquisition_date"
            const val PRIVATE_INFO_ACQUIRED_FROM = "acquired_from"
            const val PRIVATE_INFO_COMMENT = "private_comment"
            const val STATUS_DIRTY_TIMESTAMP = "status_dirty_timestamp"
            const val RATING_DIRTY_TIMESTAMP = "rating_dirty_timestamp"
            const val COMMENT_DIRTY_TIMESTAMP = "comment_dirty_timestamp"
            const val PRIVATE_INFO_DIRTY_TIMESTAMP = "private_info_dirty_timestamp"
            const val COLLECTION_DIRTY_TIMESTAMP = "collection_dirty_timestamp"
            const val COLLECTION_DELETE_TIMESTAMP = "collection_delete_timestamp"
            const val WISHLIST_COMMENT_DIRTY_TIMESTAMP = "wishlist_comment_dirty_timestamp"
            const val TRADE_CONDITION_DIRTY_TIMESTAMP = "trade_condition_dirty_timestamp"
            const val WANT_PARTS_DIRTY_TIMESTAMP = "want_parts_dirty_timestamp"
            const val HAS_PARTS_DIRTY_TIMESTAMP = "has_parts_dirty_timestamp"
            const val COLLECTION_HERO_IMAGE_URL = "collection_hero_image_url"
            const val PRIVATE_INFO_INVENTORY_LOCATION = "inventory_location"
            const val UPDATED = COL_UPDATED
            const val UPDATED_LIST = COL_UPDATED_LIST
        }

        // val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_COLLECTION).build()

        fun getCollectionId(uri: Uri): Int {
            val index = uri.pathSegments.indexOf(PATH_COLLECTION)
            return if (index == -1)
                INVALID_ID
            else
                uri.pathSegments.getOrNull(index + 1)?.toIntOrNull() ?: INVALID_ID
        }
    }

    object Users {
        object Columns {
            const val USERNAME = "username"
            const val FIRST_NAME = "first_name"
            const val LAST_NAME = "last_name"
            const val AVATAR_URL = "avatar_url"
            const val PLAY_NICKNAME = "play_nickname"
            const val BUDDY_FLAG = "buddy_flag"
            const val SYNC_HASH_CODE = "sync_hash_code"
            const val UPDATED_DETAIL_TIMESTAMP = "updated_detail_timestamp"
            const val UPDATED_LIST_TIMESTAMP = "updated_list_timestamp"
        }
    }

    object PlayerColors {
        object Columns {
            const val PLAYER_NAME = "player_name"
            const val PLAYER_TYPE = "player_type"
            const val PLAYER_COLOR = "player_color"
            const val PLAYER_COLOR_SORT_ORDER = "player_color_sort"
        }
    }

    object GameSuggestedPlayerCountPollPollResults {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val PLAYER_COUNT = "player_count"
            const val SORT_INDEX = "sort_index"
            const val BEST_VOTE_COUNT = "best_vote_count"
            const val RECOMMENDED_VOTE_COUNT = "recommended_vote_count"
            const val NOT_RECOMMENDED_VOTE_COUNT = "not_recommended_vote_count"
            const val RECOMMENDATION = "recommendation"
        }
    }

    object GamePolls {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val POLL_NAME = "poll_name"
            const val POLL_TITLE = "poll_title"
            const val POLL_TOTAL_VOTES = "poll_total_votes"
        }
    }

    object GamePollResults {
        object Columns {
            const val POLL_ID = "poll_id"
            const val POLL_RESULTS_KEY = "pollresults_key"
            const val POLL_RESULTS_PLAYERS = "pollresults_players"
            const val POLL_RESULTS_SORT_INDEX = "pollresults_sortindex"
        }
    }

    object GamePollResultsResult {
        object Columns {
            const val POLL_RESULTS_ID = "pollresults_id"
            const val POLL_RESULTS_RESULT_KEY = "pollresultsresult_key"
            const val POLL_RESULTS_RESULT_LEVEL = "pollresultsresult_level"
            const val POLL_RESULTS_RESULT_VALUE = "pollresultsresult_value"
            const val POLL_RESULTS_RESULT_VOTES = "pollresultsresult_votes"
            const val POLL_RESULTS_RESULT_SORT_INDEX = "pollresultsresult_sortindex"
        }
    }

    object GameColors {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val COLOR = "color"
        }
    }

    object Plays {
        object Columns {
            const val PLAY_ID = "play_id"
            const val DATE = "date"
            const val QUANTITY = "quantity"
            const val LENGTH = "length"
            const val INCOMPLETE = "incomplete"
            const val NO_WIN_STATS = "no_win_stats"
            const val LOCATION = "location"
            const val COMMENTS = "comments"
            const val START_TIME = "start_time"
            const val PLAYER_COUNT = "player_count"
            const val SYNC_HASH_CODE = "sync_hash_code"
            const val ITEM_NAME = "item_name"
            const val OBJECT_ID = "object_id"
            const val DELETE_TIMESTAMP = "delete_timestamp"
            const val UPDATE_TIMESTAMP = "update_timestamp"
            const val DIRTY_TIMESTAMP = "dirty_timestamp"
            const val SYNC_TIMESTAMP = COL_UPDATED_LIST
        }
    }

    object PlayPlayers {
        object Columns {
            @Suppress("ConstPropertyName")
            const val _PLAY_ID = "_play_id"
            const val USER_NAME = "user_name"
            const val USER_ID = "user_id"
            const val NAME = "name"
            const val START_POSITION = "start_position"
            const val COLOR = "color"
            const val SCORE = "score"
            const val NEW = "new"
            const val RATING = "rating"
            const val WIN = "win"
        }
    }

    object CollectionViews {
        object Columns {
            const val NAME = "name"
            const val STARRED = "starred"
            const val SORT_TYPE = "sort_type"
            const val SELECTED_COUNT = "selected_count"
            const val SELECTED_TIMESTAMP = "selected_timestamp"
        }
    }

    object CollectionViewFilters {
        object Columns {
            const val VIEW_ID = "filter_id"
            const val TYPE = "type"
            const val DATA = "data"
        }
    }

    companion object {
        const val INVALID_ID = -1
        const val INVALID_URL = "N/A"

        const val CONTENT_AUTHORITY = "com.boardgamegeek"
        private val BASE_CONTENT_URI = "content://$CONTENT_AUTHORITY".toUri()

        const val COLLATE_NOCASE = " COLLATE NOCASE"

        const val COL_UPDATED = "updated"
        const val COL_UPDATED_LIST = "updated_list"

        const val PATH_GAMES = "games"
        const val PATH_COLLECTION = "collection"
        const val PATH_THUMBNAILS = "thumbnails"
        const val PATH_AVATARS = "avatars"
    }
}
