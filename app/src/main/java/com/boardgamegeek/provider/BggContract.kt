package com.boardgamegeek.provider

import android.net.Uri

class BggContract {
    object Thumbnails {
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_THUMBNAILS).build()
        fun buildUri(fileName: String?): Uri = CONTENT_URI.buildUpon().appendPath(fileName).build()
    }

    object Avatars {
        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_AVATARS).build()
        fun buildUri(avatarFileName: String?): Uri = CONTENT_URI.buildUpon().appendPath(avatarFileName).build()
    }

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
            const val PLAYER_COUNT_RECOMMENDATION_PREFIX = "player_count_recommendation_"
            const val HERO_IMAGE_URL = "hero_image_url"
            const val ICON_COLOR = "ICON_COLOR"
            const val DARK_COLOR = "DARK_COLOR"
            const val WINS_COLOR = "WINS_COLOR"
            const val WINNABLE_PLAYS_COLOR = "WINNABLE_PLAYS_COLOR"
            const val ALL_PLAYS_COLOR = "ALL_PLAYS_COLOR"
            const val PLAYER_COUNTS_BEST = "player_counts_best"
            const val PLAYER_COUNTS_RECOMMENDED = "player_counts_recommended"
            const val PLAYER_COUNTS_NOT_RECOMMENDED = "player_count_nots_recommended"
            const val UPDATED = "updated"
            const val UPDATED_LIST = "updated_list"
            const val POLLS_COUNT = "polls_count"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_GAMES).build()
        val CONTENT_PLAYS_URI: Uri = CONTENT_URI.buildUpon().fragment(FRAGMENT_PLAYS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.game"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.game"
        const val DEFAULT_SORT = Columns.GAME_SORT_NAME + COLLATE_NOCASE + " ASC"

        fun buildGameUri(gameId: Int): Uri {
            return getUriBuilder(gameId).build()
        }

        fun buildRanksUri(gameId: Int): Uri {
            return getUriBuilder(gameId, PATH_RANKS).build()
        }

        fun buildRanksUri(gameId: Int, rankId: Int): Uri {
            return getUriBuilder(gameId, PATH_RANKS, rankId).build()
        }

        fun buildDesignersUri(gameId: Int, limitCount: Int = 0): Uri {
            return getLimitedUriBuilder(gameId, PATH_DESIGNERS, limitCount).build()
        }

        fun buildDesignersUri(rowId: Long): Uri {
            return getUriBuilder().appendPath(PATH_DESIGNERS).appendPath(rowId.toString()).build()
        }

        fun buildArtistsUri(gameId: Int, limitCount: Int = 0): Uri {
            return getLimitedUriBuilder(gameId, PATH_ARTISTS, limitCount).build()
        }

        fun buildArtistUri(rowId: Long): Uri {
            return getUriBuilder().appendPath(PATH_ARTISTS).appendPath(rowId.toString()).build()
        }

        fun buildPublishersUri(gameId: Int, limitCount: Int = 0): Uri {
            return getLimitedUriBuilder(gameId, PATH_PUBLISHERS, limitCount).build()
        }

        fun buildPublisherUri(rowId: Long): Uri {
            return getUriBuilder().appendPath(PATH_PUBLISHERS).appendPath(rowId.toString()).build()
        }

        fun buildMechanicsUri(gameId: Int, limitCount: Int = 0): Uri {
            return getLimitedUriBuilder(gameId, PATH_MECHANICS, limitCount).build()
        }

        fun buildMechanicUri(rowId: Long): Uri {
            return getUriBuilder().appendPath(PATH_MECHANICS).appendPath(rowId.toString()).build()
        }

        fun buildCategoriesUri(gameId: Int, limitCount: Int = 0): Uri {
            return getLimitedUriBuilder(gameId, PATH_CATEGORIES, limitCount).build()
        }

        fun buildCategoryUri(rowId: Long): Uri {
            return getUriBuilder().appendPath(PATH_CATEGORIES).appendPath(rowId.toString()).build()
        }

        fun buildExpansionsUri(gameId: Int, limitCount: Int = 0): Uri {
            return getLimitedUriBuilder(gameId, PATH_EXPANSIONS, limitCount).build()
        }

        fun buildExpansionUri(rowId: Long): Uri {
            return getUriBuilder().appendPath(PATH_EXPANSIONS).appendPath(rowId.toString()).build()
        }

        fun buildPollsUri(gameId: Int): Uri {
            return getUriBuilder(gameId, PATH_POLLS).build()
        }

        fun buildPollsUri(gameId: Int, pollName: String?): Uri {
            return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).build()
        }

        fun buildPollResultsUri(gameId: Int, pollName: String?): Uri {
            return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS).build()
        }

        fun buildPollResultsUri(gameId: Int, pollName: String?, key: String?): Uri {
            return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS).appendPath(key)
                .build()
        }

        fun buildPollResultsResultUri(gameId: Int, pollName: String?): Uri {
            return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS)
                .appendPath(PATH_POLL_RESULTS_RESULT).build()
        }

        fun buildPollResultsResultUri(gameId: Int, pollName: String?, key: String?): Uri {
            return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS).appendPath(key)
                .appendPath(PATH_POLL_RESULTS_RESULT).build()
        }

        fun buildPollResultsResultUri(gameId: Int, pollName: String?, key: String?, key2: String?): Uri {
            return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName)
                .appendPath(PATH_POLL_RESULTS).appendPath(key)
                .appendPath(PATH_POLL_RESULTS_RESULT).appendPath(key2).build()
        }

        fun buildSuggestedPlayerCountPollResultsUri(gameId: Int): Uri {
            return getUriBuilder(gameId, PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS).build()
        }

        fun buildSuggestedPlayerCountPollResultsUri(gameId: Int, playerCount: String?): Uri {
            return getUriBuilder(gameId, PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS).appendPath(playerCount).build()
        }

        fun buildColorsUri(gameId: Int): Uri {
            return getUriBuilder(gameId, PATH_COLORS).build()
        }

        fun buildColorsUri(gameId: Int, color: String?): Uri {
            return getUriBuilder(gameId, PATH_COLORS).appendPath(color).build()
        }

        private fun getUriBuilder(): Uri.Builder = CONTENT_URI.buildUpon()

        private fun getUriBuilder(gameId: Int): Uri.Builder {
            return CONTENT_URI.buildUpon().appendPath(gameId.toString())
        }

        private fun getUriBuilder(gameId: Int, path: String): Uri.Builder {
            return getLimitedUriBuilder(gameId, path, 0)
        }

        private fun getUriBuilder(gameId: Int, path: String, id: Int): Uri.Builder {
            return getUriBuilder(gameId, path).appendPath(id.toString())
        }

        fun buildPathUri(gameId: Int, path: String?): Uri {
            return CONTENT_URI.buildUpon().appendPath(gameId.toString()).appendPath(path).build()
        }

        fun buildPathUri(gameId: Int, path: String?, id: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(gameId.toString()).appendPath(path)
                .appendPath(id.toString()).build()
        }

        private fun getLimitedUriBuilder(gameId: Int, path: String, limit: Int = 0): Uri.Builder {
            val builder = CONTENT_URI.buildUpon().appendPath(gameId.toString()).appendPath(path)
            if (limit > 0) {
                builder.appendQueryParameter(QUERY_KEY_LIMIT, limit.toString())
            }
            return builder
        }

        fun getGameId(uri: Uri?): Int {
            // TODO use generic function
            return uri?.pathSegments?.let {
                if (it.size > 1 && it.getOrNull(0) == PATH_GAMES) it.getOrNull(1)?.toIntOrNull() else null
            } ?: INVALID_ID
        }

        fun getPollName(uri: Uri): String {
            return getPathValue(uri, PATH_POLLS)
        }

        fun getPollResultsKey(uri: Uri): String {
            return getPathValue(uri, PATH_POLL_RESULTS)
        }

        fun getPollResultsResultKey(uri: Uri): String {
            return getPathValue(uri, PATH_POLL_RESULTS_RESULT)
        }

        fun getPollPlayerCount(uri: Uri): String {
            return getPathValue(uri, PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
        }

        private fun getPathValue(uri: Uri, path: String): String {
            // TODO use find()
            if (path.isEmpty()) {
                return ""
            }
            var isNextValue = false
            for (segment in uri.pathSegments) {
                if (isNextValue) {
                    return segment
                }
                if (path == segment) {
                    isNextValue = true
                }
            }
            return ""
        }

        fun createRecommendedPlayerCountColumn(playerCount: String): String {
            return Columns.PLAYER_COUNT_RECOMMENDATION_PREFIX + playerCount
        }

        fun getRecommendedPlayerCountFromColumn(column: String): String? {
            if (column.startsWith(Columns.PLAYER_COUNT_RECOMMENDATION_PREFIX)) {
                val delimiter = Columns.PLAYER_COUNT_RECOMMENDATION_PREFIX.substring(Columns.PLAYER_COUNT_RECOMMENDATION_PREFIX.length - 1)
                val parts = column.split(delimiter.toRegex()).toTypedArray()
                return parts[parts.size - 1]
            }
            return null
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

        val CONTENT_URI: Uri = Games.CONTENT_URI.buildUpon().appendPath(PATH_RANKS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.rank"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.rank"
        const val DEFAULT_SORT = ("${Columns.GAME_RANK_TYPE} DESC,${Columns.GAME_RANK_VALUE},${Columns.GAME_RANK_FRIENDLY_NAME}")

        fun buildGameRankUri(gameRankId: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(gameRankId.toString()).build()
        }

        fun getRankId(uri: Uri) = uri.lastPathSegment?.toIntOrNull() ?: INVALID_ID
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

            //TODO
            const val UPDATED = "updated"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_DESIGNERS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.designer"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.designer"
        const val DEFAULT_SORT = "${Columns.DESIGNER_NAME}$COLLATE_NOCASE ASC"

        fun buildDesignerUri(designerId: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(designerId.toString()).build()
        }

        fun buildDesignerCollectionUri(designerId: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(designerId.toString()).appendPath(PATH_COLLECTION).build()
        }

        fun getDesignerId(uri: Uri) = uri.pathSegments.getOrNull(1)?.toIntOrNull() ?: INVALID_ID
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

            //TODO
            const val UPDATED = "updated"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ARTISTS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.artist"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.artist"
        const val DEFAULT_SORT = Columns.ARTIST_NAME + COLLATE_NOCASE + " ASC"

        fun buildArtistUri(artistId: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(artistId.toString()).build()
        }

        fun buildArtistCollectionUri(artistId: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(artistId.toString()).appendPath(PATH_COLLECTION).build()
        }

        fun getArtistId(uri: Uri) = uri.pathSegments.getOrNull(1)?.toIntOrNull() ?: INVALID_ID
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

            //TODO
            const val UPDATED = "updated"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PUBLISHERS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.publisher"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.publisher"
        const val DEFAULT_SORT = "${Columns.PUBLISHER_NAME}$COLLATE_NOCASE ASC"

        fun buildPublisherUri(publisherId: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(publisherId.toString()).build()
        }

        fun getPublisherId(uri: Uri): Int {
            return uri.pathSegments.getOrNull(1)?.toIntOrNull() ?: INVALID_ID
        }

        fun buildCollectionUri(publisherId: Int): Uri {
            return CONTENT_URI.buildUpon().appendPath(publisherId.toString()).appendPath(PATH_COLLECTION).build()
        }
    }

    object Mechanics {
        object Columns {
            const val MECHANIC_ID = "mechanic_id"
            const val MECHANIC_NAME = "mechanic_name"
            const val ITEM_COUNT = "item_count"
            const val UPDATED = "updated"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MECHANICS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.mechanic"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.mechanic"
        const val DEFAULT_SORT = "${Columns.MECHANIC_NAME}$COLLATE_NOCASE ASC"

        @Suppress("unused")
        fun buildMechanicUri(mechanicId: Int): Uri {
            return createMechanicUri(mechanicId).build()
        }

        private fun createMechanicUri(mechanicId: Int): Uri.Builder {
            return CONTENT_URI.buildUpon().appendPath(mechanicId.toString())
        }

        fun getMechanicId(uri: Uri): Int {
            return uri.pathSegments.getOrNull(1)?.toIntOrNull() ?: INVALID_ID
        }

        fun buildCollectionUri(mechanicId: Int): Uri {
            return createMechanicUri(mechanicId).appendPath(PATH_COLLECTION).build()
        }
    }

    object Categories {
        object Columns {
            const val CATEGORY_ID = "category_id"
            const val CATEGORY_NAME = "category_name"
            const val ITEM_COUNT = "item_count"
            const val UPDATED = "updated"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_CATEGORIES).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.category"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.category"
        const val DEFAULT_SORT = "${Columns.CATEGORY_NAME}$COLLATE_NOCASE ASC"

        @Suppress("unused")
        fun buildCategoryUri(categoryId: Int): Uri {
            return createCategoryUri(categoryId).build()
        }

        fun buildCollectionUri(categoryId: Int): Uri {
            return createCategoryUri(categoryId).appendPath(PATH_COLLECTION).build()
        }

        fun getCategoryId(uri: Uri): Int {
            return uri.pathSegments.getOrNull(1)?.toIntOrNull() ?: INVALID_ID
        }

        private fun createCategoryUri(categoryId: Int): Uri.Builder {
            return CONTENT_URI.buildUpon().appendPath(categoryId.toString())
        }
    }

    object GamesExpansions {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val EXPANSION_ID = "expansion_id"
            const val EXPANSION_NAME = "expansion_name"
            const val INBOUND = "inbound"
        }

        val CONTENT_URI: Uri = Games.CONTENT_URI.buildUpon().appendPath(PATH_EXPANSIONS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.expansion"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.expansion"
        const val DEFAULT_SORT = "${Columns.EXPANSION_NAME}$COLLATE_NOCASE ASC"
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

            // TODO use constants
            const val UPDATED = "updated"
            const val UPDATED_LIST = "updated_list"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_COLLECTION).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.collection"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.collection"
        const val DEFAULT_SORT = Columns.COLLECTION_SORT_NAME + COLLATE_NOCASE + " ASC"
        const val SORT_ACQUIRED_FROM = Columns.PRIVATE_INFO_ACQUIRED_FROM + COLLATE_NOCASE + " ASC"
        const val SORT_INVENTORY_LOCATION = Columns.PRIVATE_INFO_INVENTORY_LOCATION + COLLATE_NOCASE + " ASC"

        fun buildUri(id: Long): Uri {
            return CONTENT_URI.buildUpon().appendPath(id.toString()).build()
        }

        fun buildAcquiredFromUri(): Uri {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACQUIRED_FROM).build()
        }

        fun buildInventoryLocationUri(): Uri {
            return CONTENT_URI.buildUpon().appendPath(PATH_INVENTORY_LOCATION).build()
        }

        fun getId(uri: Uri): Long {
            return uri.pathSegments.getOrNull(1)?.toLongOrNull() ?: INVALID_ID.toLong()
        }
    }

    object Buddies {
        object Columns {
            const val BUDDY_ID = "buddy_id"
            const val BUDDY_NAME = "buddy_name"
            const val BUDDY_FIRSTNAME = "buddy_firtname" // TODO no "S"?!
            const val BUDDY_LASTNAME = "buddy_lastname"
            const val AVATAR_URL = "avatar_url"
            const val PLAY_NICKNAME = "play_nickname"
            const val BUDDY_FLAG = "buddy_flag"
            const val SYNC_HASH_CODE = "sync_hash_code"

            // TODO use constants
            const val UPDATED = "updated"
            const val UPDATED_LIST = "updated_list"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BUDDIES).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.buddy"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.buddy"
        const val DEFAULT_SORT = "${Columns.BUDDY_LASTNAME}$COLLATE_NOCASE ASC, ${Columns.BUDDY_FIRSTNAME}$COLLATE_NOCASE ASC"

        fun buildBuddyUri(buddyName: String?): Uri {
            return CONTENT_URI.buildUpon().appendPath(buddyName).build()
        }

        fun getBuddyName(uri: Uri): String { // TODO null-able?
            return uri.pathSegments[1]
        }
    }

    object PlayerColors {
        object Columns {
            const val PLAYER_NAME = "player_name"
            const val PLAYER_TYPE = "player_type"
            const val PLAYER_COLOR = "player_color"
            const val PLAYER_COLOR_SORT_ORDER = "player_color_sort"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYER_COLORS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.playercolor"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.playercolor"
        const val TYPE_USER = 1
        const val TYPE_PLAYER = 2
        const val DEFAULT_SORT = "${Columns.PLAYER_TYPE} ASC, ${Columns.PLAYER_NAME} ASC, ${Columns.PLAYER_COLOR_SORT_ORDER} ASC"

        fun buildUserUri(username: String?): Uri {
            return BASE_CONTENT_URI.buildUpon().appendPath(PATH_USERS).appendPath(username).appendPath(PATH_COLORS).build()
        }

        fun buildPlayerUri(playerName: String?): Uri {
            return BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYERS).appendPath(playerName).appendPath(PATH_COLORS).build()
        }

        fun buildUserUri(username: String?, sortOrder: Int): Uri {
            return buildUserUri(username).buildUpon().appendPath(sortOrder.toString()).build()
        }

        fun getUsername(uri: Uri?) = uri.getSegmentAfterPath(PATH_USERS)

        fun getPlayerName(uri: Uri?) = uri.getSegmentAfterPath(PATH_PLAYERS)

        fun getSortOrder(uri: Uri?) = uri.getSegmentAfterPath(PATH_COLORS)?.toIntOrNull() ?: 0

        private fun Uri?.getSegmentAfterPath(pathSegment: String, pathIndex: Int = 0): String? {
            return this?.pathSegments?.let {
                if (it.getOrNull(pathIndex) == pathSegment) it.getOrNull(pathIndex + 1) else null
            }
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

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepoll.playercount"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepoll.playercount"
        const val DEFAULT_SORT = "${Columns.SORT_INDEX} ASC"
    }

    object GamePolls {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val POLL_NAME = "poll_name"
            const val POLL_TITLE = "poll_title"
            const val POLL_TOTAL_VOTES = "poll_total_votes"
        }

        val CONTENT_URI: Uri = Games.CONTENT_URI.buildUpon().appendPath(PATH_POLLS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepoll"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepoll"
        const val DEFAULT_SORT = "${Columns.POLL_TITLE}$COLLATE_NOCASE ASC"
    }

    object GamePollResults {
        object Columns {
            const val POLL_ID = "poll_id"
            const val POLL_RESULTS_KEY = "pollresults_key"
            const val POLL_RESULTS_PLAYERS = "pollresults_players"
            const val POLL_RESULTS_SORT_INDEX = "pollresults_sortindex"
        }

        val CONTENT_URI: Uri = GamePolls.CONTENT_URI.buildUpon().appendPath(PATH_POLL_RESULTS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepollresult"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepollresult"
        const val DEFAULT_SORT = "${Columns.POLL_RESULTS_SORT_INDEX} ASC"
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

        val CONTENT_URI: Uri = GamePollResults.CONTENT_URI.buildUpon().appendPath(PATH_POLL_RESULTS_RESULT).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepollresultsresult"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepollresultsresult"
        const val DEFAULT_SORT = "${GamePollResults.Columns.POLL_RESULTS_SORT_INDEX} ASC, ${Columns.POLL_RESULTS_RESULT_SORT_INDEX} ASC"
    }

    object GameColors {
        object Columns {
            const val GAME_ID = Games.Columns.GAME_ID
            const val COLOR = "color"
        }

        val CONTENT_URI: Uri = Games.CONTENT_URI.buildUpon().appendPath(PATH_COLORS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamecolor"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamecolor"
        const val DEFAULT_SORT = Columns.COLOR + COLLATE_NOCASE + " ASC"
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
            const val SYNC_TIMESTAMP = "updated_list"
            const val SUM_QUANTITY = "sum_quantity"
            const val SUM_WINS = "sum_wins"
            const val MAX_DATE = "max_date"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYS).build()
        private val CONTENT_SIMPLE_URI: Uri = CONTENT_URI.buildUpon().fragment(FRAGMENT_SIMPLE).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.play"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.play"

        // TODO define table name in a better spot
        const val DEFAULT_SORT = "${Columns.DATE} DESC, plays.${Columns.PLAY_ID} DESC"

        /**
         * content://com.boardgamegeek/plays/#
         */
        fun buildPlayUri(internalId: Long): Uri {
            return CONTENT_SIMPLE_URI.buildUpon().appendPath(internalId.toString()).build()
        }

        fun buildPlayWithGameUri(internalId: Long): Uri {
            return CONTENT_URI.buildUpon().appendPath(internalId.toString()).build()
        }

        fun buildPlayerUri(): Uri {
            return CONTENT_URI.buildUpon()
                .appendPath(PATH_PLAYERS)
                .build()
        }

        fun buildPlayerUri(internalId: Long): Uri {
            return CONTENT_URI.buildUpon()
                .appendPath(internalId.toString())
                .appendPath(PATH_PLAYERS)
                .build()
        }

        fun buildPlayerUri(internalPlayId: Long, internalPlayerId: Long): Uri {
            return CONTENT_URI.buildUpon()
                .appendPath(internalPlayId.toString())
                .appendPath(PATH_PLAYERS)
                .appendPath(internalPlayerId.toString())
                .build()
        }

        fun buildLocationsUri(): Uri {
            return CONTENT_URI.buildUpon().appendPath(PATH_LOCATIONS).build()
        }

        fun buildPlayersUri(): Uri {
            return CONTENT_URI.buildUpon().appendPath(PATH_PLAYERS).build()
        }

        fun buildPlayersByPlayUri(): Uri {
            return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_PLAY).build()
        }

        fun buildPlayersByUniquePlayerUri(): Uri {
            return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_UNIQUE_PLAYER).build()
        }

        fun buildPlayersByUniqueUserUri(): Uri {
            return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_UNIQUE_USER).build()
        }

        fun buildPlayersByUniqueNameUri(): Uri {
            return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_UNIQUE_NAME).build()
        }

        fun buildPlayersByColor(): Uri {
            return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_COLOR).build()
        }

        fun getInternalId(uri: Uri) = uri.pathSegments[1].toLong()
    }

    object PlayPlayers {
        object Columns {
            @Suppress("ObjectPropertyName")
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
            const val COUNT = "count"
            const val DESCRIPTION = "description"
            const val UNIQUE_NAME = "unique_name"
        }

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.playplayer"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.playplayer"
        const val DEFAULT_SORT = "${Columns.START_POSITION} ASC, play_players.${Columns.NAME}$COLLATE_NOCASE ASC"
        const val SORT_BY_SUM_QUANTITY = "${Plays.Columns.SUM_QUANTITY} DESC, $DEFAULT_SORT"

        fun getPlayPlayerId(uri: Uri?) = uri?.lastPathSegment?.toLongOrNull() ?: INVALID_ID.toLong()
    }

    object PlayLocations {
        const val DEFAULT_SORT = "${Plays.Columns.LOCATION}$COLLATE_NOCASE ASC"
    }

    object CollectionViews {
        object Columns {
            const val NAME = "name"
            const val STARRED = "starred"
            const val SORT_TYPE = "sort_type"
            const val SELECTED_COUNT = "selected_count"
            const val SELECTED_TIMESTAMP = "selected_timestamp"
        }

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_COLLECTION_VIEWS).build()
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.collectionview"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.collectionview"
        const val DEFAULT_SORT = "${Columns.STARRED} DESC, ${Columns.NAME}$COLLATE_NOCASE ASC"

        fun buildViewUri(viewId: Long): Uri = build(viewId).build()

        // TODO move to next object
        fun buildViewFilterUri(viewId: Long): Uri = build2(viewId).build()

        fun buildViewFilterUri(viewId: Long, filterId: Long): Uri = build2(viewId).appendPath(filterId.toString()).build()

        private fun build(viewId: Long): Uri.Builder = CONTENT_URI.buildUpon().appendPath(viewId.toString())

        private fun build2(viewId: Long) = build(viewId).appendPath(PATH_FILTERS)

        fun getViewId(uri: Uri?) = uri?.pathSegments?.getOrNull(1)?.toIntOrNull() ?: INVALID_ID
    }

    object CollectionViewFilters {
        object Columns {
            const val VIEW_ID = "filter_id"
            const val TYPE = "type"
            const val DATA = "data"
        }

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.collectionviewfilter"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.collectionviewfilter"
        const val DEFAULT_SORT = "${CollectionViews.Columns.STARRED} DESC, ${CollectionViews.Columns.NAME}$COLLATE_NOCASE ASC, ${Columns.TYPE} ASC"

        fun getFilterType(uri: Uri?) = uri?.lastPathSegment?.toIntOrNull() ?: INVALID_ID
    }

    companion object {
        const val INVALID_ID = -1
        const val INVALID_URL = "N/A"

        const val CONTENT_AUTHORITY = "com.boardgamegeek"
        private val BASE_CONTENT_URI = Uri.parse("content://$CONTENT_AUTHORITY")

        const val COLLATE_NOCASE = " COLLATE NOCASE"

        // TODO prefix with COL_
        const val UPDATED = "updated"
        const val UPDATED_LIST = "updated_list"

        const val PATH_GAMES = "games"
        const val PATH_RANKS = "ranks"
        const val PATH_DESIGNERS = "designers"
        const val PATH_ARTISTS = "artists"
        const val PATH_PUBLISHERS = "publishers"
        const val PATH_MECHANICS = "mechanics"
        const val PATH_CATEGORIES = "categories"
        const val PATH_EXPANSIONS = "expansions"
        const val PATH_COLLECTION = "collection"
        const val PATH_BUDDIES = "buddies"
        const val PATH_USERS = "users"
        const val PATH_POLLS = "polls"
        const val PATH_POLL_RESULTS = "results"
        const val PATH_POLL_RESULTS_RESULT = "result"
        const val PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS = "suggestedplayercountpollresults"
        const val PATH_THUMBNAILS = "thumbnails"
        const val PATH_AVATARS = "avatars"
        const val PATH_COLORS = "colors"
        const val PATH_PLAYER_COLORS = "playercolors"
        const val PATH_ACQUIRED_FROM = "acquiredfrom"
        const val PATH_INVENTORY_LOCATION = "inventorylocation"
        const val PATH_PLAYS = "plays"
        const val PATH_PLAYERS = "players"
        const val PATH_LOCATIONS = "locations"
        const val PATH_COLLECTION_VIEWS = "collectionviews"
        const val PATH_FILTERS = "filters"

        const val QUERY_KEY_GROUP_BY = "groupby"
        const val QUERY_VALUE_NAME_NOT_USER = "namenotuser"
        const val QUERY_VALUE_UNIQUE_NAME = "uniquename"
        const val QUERY_VALUE_UNIQUE_PLAYER = "uniqueplayer"
        const val QUERY_VALUE_UNIQUE_USER = "uniqueuser"
        const val QUERY_VALUE_COLOR = "color"
        const val QUERY_VALUE_PLAY = "play"
        const val QUERY_KEY_HAVING = "having"
        const val QUERY_KEY_LIMIT = "limit"

        const val FRAGMENT_SIMPLE = "simple"
        const val FRAGMENT_PLAYS = "plays"

        const val POLL_TYPE_LANGUAGE_DEPENDENCE = "language_dependence"
        const val POLL_TYPE_SUGGESTED_PLAYER_AGE = "suggested_playerage"

        fun buildBasicUri(path: String?, id: Long): Uri? {
            return BASE_CONTENT_URI.buildUpon().appendPath(path).appendPath(id.toString()).build()
        }
    }
}
