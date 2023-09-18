package com.boardgamegeek.db

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.CollectionItemLocal
import com.boardgamegeek.db.model.GameLocal
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class CollectionDao(private val context: Context) {
    private val resolver = context.contentResolver

    suspend fun load(internalId: Long) = withContext(Dispatchers.IO) {
        if (internalId != INVALID_ID.toLong()) {
            loadPairs(Collection.buildUri(internalId))
        } else null
    }

    suspend fun loadAll() = loadPairs()

    suspend fun loadByGame(gameId: Int) =
        loadPairs(selection = "collection.${Collection.Columns.GAME_ID}=?", selectionArgs = arrayOf(gameId.toString()))

    suspend fun loadItemsPendingDeletion() = loadPairs(selection = Collection.Columns.COLLECTION_DELETE_TIMESTAMP.greaterThanZero())

    suspend fun loadItemsPendingInsert() =
        loadPairs(selection = "${Collection.Columns.COLLECTION_DIRTY_TIMESTAMP.greaterThanZero()} AND ${Collection.Columns.COLLECTION_ID.whereNullOrBlank()}")

    suspend fun loadItemsPendingUpdate(): List<Pair<GameLocal, CollectionItemLocal>> {
        val columns = listOf(
            Collection.Columns.STATUS_DIRTY_TIMESTAMP,
            Collection.Columns.RATING_DIRTY_TIMESTAMP,
            Collection.Columns.COMMENT_DIRTY_TIMESTAMP,
            Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP,
            Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
            Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP,
            Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP,
            Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP,
        ).map { it.greaterThanZero() }
        return loadPairs(selection = "${columns.joinTo(" OR ")}")
    }

    private suspend fun loadPairs(
        uri: Uri = Collection.CONTENT_URI,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortBy: SortType = SortType.NAME,
    ) = withContext(Dispatchers.IO) {
        val sortByName = Games.Columns.GAME_SORT_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.RATING -> Collection.Columns.RATING.descending()
                .plus(", ${Games.Columns.STARRED}").descending()
                .plus(", $sortByName")
        }
        resolver.loadList(
            uri,
            arrayOf(
                Games.Columns.GAME_ID,
                Games.Columns.GAME_NAME,
                Games.Columns.DESCRIPTION,
                Games.Columns.SUBTYPE,
                Games.Columns.THUMBNAIL_URL,
                Games.Columns.IMAGE_URL, // 5
                Games.Columns.YEAR_PUBLISHED,
                Games.Columns.MIN_PLAYERS,
                Games.Columns.MAX_PLAYERS,
                Games.Columns.PLAYING_TIME,
                Games.Columns.MIN_PLAYING_TIME, // 10
                Games.Columns.MAX_PLAYING_TIME,
                Games.Columns.MINIMUM_AGE,
                Games.Columns.HERO_IMAGE_URL,
                Games.Columns.STATS_AVERAGE,
                Games.Columns.STATS_USERS_RATED, // 15
                Games.Columns.STATS_NUMBER_COMMENTS,
                Games.Columns.UPDATED,
                Games.Columns.UPDATED_PLAYS,
                Games.Columns.GAME_RANK,
                Games.Columns.STATS_STANDARD_DEVIATION, // 20
                Games.Columns.STATS_BAYES_AVERAGE,
                Games.Columns.STATS_AVERAGE_WEIGHT,
                Games.Columns.STATS_NUMBER_WEIGHTS,
                Games.Columns.STATS_NUMBER_OWNED,
                Games.Columns.STATS_NUMBER_TRADING, // 25
                Games.Columns.STATS_NUMBER_WANTING,
                Games.Columns.STATS_NUMBER_WISHING,
                Games.Columns.CUSTOM_PLAYER_SORT,
                Games.Columns.STARRED,
                BaseColumns._ID, // 30
                Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
                Games.Columns.ICON_COLOR,
                Games.Columns.DARK_COLOR,
                Games.Columns.WINS_COLOR,
                Games.Columns.WINNABLE_PLAYS_COLOR, // 35
                Games.Columns.ALL_PLAYS_COLOR,
                Games.Columns.GAME_SORT_NAME,
                Games.Columns.STATS_MEDIAN,
                Games.Columns.NUM_PLAYS,
                Games.Columns.UPDATED_LIST, //40
                Games.Columns.LAST_VIEWED,
                Games.Columns.PLAYER_COUNTS_BEST,
                Games.Columns.PLAYER_COUNTS_RECOMMENDED,
                Games.Columns.PLAYER_COUNTS_NOT_RECOMMENDED,
                Collection.Columns.COLLECTION_ID, // 45
                Collection.Columns.COLLECTION_ID,
                Collection.Columns.COLLECTION_NAME,
                Collection.Columns.COLLECTION_SORT_NAME,
                Collection.Columns.STATUS_OWN,
                Collection.Columns.STATUS_PREVIOUSLY_OWNED, // 50
                Collection.Columns.STATUS_FOR_TRADE,
                Collection.Columns.STATUS_WANT,
                Collection.Columns.STATUS_WANT_TO_PLAY,
                Collection.Columns.STATUS_WANT_TO_BUY,
                Collection.Columns.STATUS_WISHLIST, // 55
                Collection.Columns.STATUS_WISHLIST_PRIORITY,
                Collection.Columns.STATUS_PREORDERED,
                Collection.Columns.COMMENT,
                Collection.Columns.LAST_MODIFIED,
                Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY, // 60
                Collection.Columns.PRIVATE_INFO_PRICE_PAID,
                Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
                Collection.Columns.PRIVATE_INFO_CURRENT_VALUE,
                Collection.Columns.PRIVATE_INFO_QUANTITY,
                Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE, // 65
                Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM,
                Collection.Columns.PRIVATE_INFO_COMMENT,
                Collection.Columns.CONDITION,
                Collection.Columns.WANTPARTS_LIST,
                Collection.Columns.HASPARTS_LIST, // 70
                Collection.Columns.WISHLIST_COMMENT,
                Collection.Columns.COLLECTION_YEAR_PUBLISHED,
                Collection.Columns.RATING,
                Collection.Columns.COLLECTION_THUMBNAIL_URL,
                Collection.Columns.COLLECTION_IMAGE_URL, // 75
                Collection.Columns.STATUS_DIRTY_TIMESTAMP,
                Collection.Columns.RATING_DIRTY_TIMESTAMP,
                Collection.Columns.COMMENT_DIRTY_TIMESTAMP,
                Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP,
                Collection.Columns.COLLECTION_DIRTY_TIMESTAMP, // 80
                Collection.Columns.COLLECTION_DELETE_TIMESTAMP,
                Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
                Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP,
                Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP,
                Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP, // 85
                Collection.Columns.COLLECTION_HERO_IMAGE_URL,
                Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION,
                Plays.Columns.MAX_DATE,
            ),
            selection,
            selectionArgs,
        ) {
            val game = GameLocal(
                internalId = it.getLong(30),
                gameId = it.getInt(0),
                gameName = it.getString(1).orEmpty(),
                description = it.getStringOrNull(2).orEmpty(),
                subtype = it.getStringOrNull(3),
                thumbnailUrl = it.getStringOrNull(4),
                imageUrl = it.getStringOrNull(5),
                yearPublished = it.getIntOrNull(6),
                minPlayers = it.getIntOrNull(7) ?: 0,
                maxPlayers = it.getIntOrNull(8) ?: 0,
                playingTime = it.getIntOrNull(9) ?: 0,
                minPlayingTime = it.getIntOrNull(10),
                maxPlayingTime = it.getIntOrNull(11),
                minimumAge = it.getIntOrNull(12),
                heroImageUrl = it.getStringOrNull(13),
                average = it.getDoubleOrNull(14),
                numberOfRatings = it.getIntOrNull(15),
                numberOfComments = it.getIntOrNull(16),
                gameRank = it.getIntOrNull(19),
                standardDeviation = it.getDoubleOrNull(20),
                bayesAverage = it.getDoubleOrNull(21),
                averageWeight = it.getDoubleOrNull(22),
                numberOfUsersWeighting = it.getIntOrNull(23),
                numberOfUsersOwned = it.getIntOrNull(24),
                numberOfUsersTrading = it.getIntOrNull(25),
                numberOfUsersWanting = it.getIntOrNull(26),
                numberOfUsersWishListing = it.getIntOrNull(27),
                updated = it.getLongOrNull(17),
                updatedPlays = it.getLongOrNull(18),
                customPlayerSort = it.getBoolean(28),
                isStarred = it.getBoolean(29),
                suggestedPlayerCountPollVoteTotal = it.getIntOrNull(31) ?: 0,
                iconColor = it.getIntOrNull(32),
                darkColor = it.getIntOrNull(33),
                winsColor = it.getIntOrNull(34),
                winnablePlaysColor = it.getIntOrNull(35),
                allPlaysColor = it.getIntOrNull(36),
                gameSortName = it.getString(37),
                median = it.getDoubleOrNull(38),
                numberOfPlays = it.getIntOrNull(39),
                updatedList = it.getLong(40),
                lastViewedTimestamp = it.getLongOrNull(41),
                playerCountsBest = it.getStringOrNull(42),
                playerCountsRecommended = it.getStringOrNull(43),
                playerCountsNotRecommended = it.getStringOrNull(44),
                lastPlayDate = it.getStringOrNull(88),
            )
            val item = CollectionItemLocal(
                internalId = INVALID_ID.toLong(),
                updatedTimestamp = it.getLong(17),
                updatedListTimestamp = it.getLong(40),
                gameId = it.getInt(0),
                collectionId = it.getInt(46),
                collectionName = it.getString(47),
                collectionSortName = it.getString(48),
                statusOwn = it.getInt(49),
                statusPreviouslyOwned = it.getInt(50),
                statusForTrade = it.getInt(51),
                statusWant = it.getInt(52),
                statusWantToPlay = it.getInt(53),
                statusWantToBuy = it.getInt(54),
                statusWishlist = it.getInt(55),
                statusWishlistPriority = it.getIntOrNull(56),
                statusPreordered = it.getInt(57),
                comment = it.getStringOrNull(58),
                lastModified = it.getLongOrNull(59),
                privateInfoPricePaidCurrency = it.getStringOrNull(60),
                privateInfoPricePaid = it.getDoubleOrNull(61),
                privateInfoCurrentValueCurrency = it.getStringOrNull(62),
                privateInfoCurrentValue = it.getDoubleOrNull(63),
                privateInfoQuantity = it.getIntOrNull(64),
                privateInfoAcquisitionDate = it.getStringOrNull(65),
                privateInfoAcquiredFrom = it.getStringOrNull(66),
                privateInfoComment = it.getStringOrNull(67),
                condition = it.getStringOrNull(68),
                wantpartsList = it.getStringOrNull(69),
                haspartsList = it.getStringOrNull(70),
                wishlistComment = it.getStringOrNull(71),
                collectionYearPublished = it.getIntOrNull(72),
                rating = it.getDoubleOrNull(73),
                collectionThumbnailUrl = it.getStringOrNull(74),
                collectionImageUrl = it.getStringOrNull(75),
                statusDirtyTimestamp = it.getLongOrNull(76),
                ratingDirtyTimestamp = it.getLongOrNull(77),
                commentDirtyTimestamp = it.getLongOrNull(78),
                privateInfoDirtyTimestamp = it.getLongOrNull(79),
                collectionDirtyTimestamp = it.getLongOrNull(80),
                collectionDeleteTimestamp = it.getLongOrNull(81),
                wishlistCommentDirtyTimestamp = it.getLongOrNull(82),
                tradeConditionDirtyTimestamp = it.getLongOrNull(83),
                wantPartsDirtyTimestamp = it.getLongOrNull(84),
                hasPartsDirtyTimestamp = it.getLongOrNull(85),
                collectionHeroImageUrl = it.getStringOrNull(86),
                privateInfoInventoryLocation = it.getStringOrNull(87),
            )
            game to item
        }
    }

    enum class SortType {
        NAME, RATING
    }

    suspend fun loadCollectionForArtist(artistId: Int, sortBy: SortType = SortType.RATING) =
        loadPairs(Artists.buildArtistCollectionUri(artistId), sortBy = sortBy)

    suspend fun loadCollectionForDesigner(designerId: Int, sortBy: SortType = SortType.RATING) =
        loadPairs(Designers.buildDesignerCollectionUri(designerId), sortBy = sortBy)

    suspend fun loadCollectionForPublisher(publisherId: Int, sortBy: SortType = SortType.RATING) =
        loadPairs(Publishers.buildCollectionUri(publisherId), sortBy = sortBy)

    suspend fun loadCollectionForCategory(categoryId: Int, sortBy: SortType = SortType.RATING) =
        loadPairs(Categories.buildCollectionUri(categoryId), sortBy = sortBy)

    suspend fun loadCollectionForMechanic(mechanicId: Int, sortBy: SortType = SortType.RATING) =
        loadPairs(Mechanics.buildCollectionUri(mechanicId), sortBy = sortBy)

    suspend fun loadUnupdatedItems(gamesPerFetch: Int = 0) = withContext(Dispatchers.IO) {
        val games = mutableMapOf<Int, String>()
        val limit = if (gamesPerFetch > 0) " LIMIT $gamesPerFetch" else ""
        context.contentResolver.loadList(
            Collection.CONTENT_URI,
            arrayOf(Games.Columns.GAME_ID, Games.Columns.GAME_NAME),
            "collection.${Collection.Columns.UPDATED}".whereZeroOrNull(),
            null,
            "collection.${Collection.Columns.UPDATED_LIST} ASC$limit"
        ) {
            games[it.getInt(0)] = it.getString(1)
        }
        games.toMap()
    }

    suspend fun loadAcquiredFrom(): List<String> = withContext(Dispatchers.IO) {
        resolver.queryStrings(Collection.buildAcquiredFromUri(), Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM).filterNot { it.isBlank() }
    }

    suspend fun loadInventoryLocation(): List<String> = withContext(Dispatchers.IO) {
        resolver.queryStrings(Collection.buildInventoryLocationUri(), Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION).filterNot { it.isBlank() }
    }

    suspend fun updateHeroImageUrl(internalId: Long, url: String) =
        update(internalId, contentValuesOf(Collection.Columns.COLLECTION_HERO_IMAGE_URL to url))

    suspend fun updateRating(internalId: Long, rating: Double): Int =
        update(
            internalId, contentValuesOf(
                Collection.Columns.RATING to rating,
                Collection.Columns.RATING_DIRTY_TIMESTAMP to System.currentTimeMillis()
            )
        )

    suspend fun updateStatuses(internalId: Long, statuses: List<String>, wishlistPriority: Int): Int {
        val values = contentValuesOf(
            Collection.Columns.STATUS_DIRTY_TIMESTAMP to System.currentTimeMillis(),
            Collection.Columns.STATUS_OWN to statuses.contains(Collection.Columns.STATUS_OWN),
            Collection.Columns.STATUS_PREVIOUSLY_OWNED to statuses.contains(Collection.Columns.STATUS_PREVIOUSLY_OWNED),
            Collection.Columns.STATUS_PREORDERED to statuses.contains(Collection.Columns.STATUS_PREORDERED),
            Collection.Columns.STATUS_FOR_TRADE to statuses.contains(Collection.Columns.STATUS_FOR_TRADE),
            Collection.Columns.STATUS_WANT to statuses.contains(Collection.Columns.STATUS_WANT),
            Collection.Columns.STATUS_WANT_TO_BUY to statuses.contains(Collection.Columns.STATUS_WANT_TO_BUY),
            Collection.Columns.STATUS_WANT_TO_PLAY to statuses.contains(Collection.Columns.STATUS_WANT_TO_PLAY),
            Collection.Columns.STATUS_WISHLIST to statuses.contains(Collection.Columns.STATUS_WISHLIST),
        )
        if (statuses.contains(Collection.Columns.STATUS_WISHLIST)) {
            values.put(Collection.Columns.STATUS_WISHLIST_PRIORITY, wishlistPriority.coerceIn(1..5))
        }
        return update(internalId, values)
    }

    suspend fun updatePrivateInfo(
        internalId: Long, priceCurrency: String?,
        price: Double?,
        currentValueCurrency: String?,
        currentValue: Double?,
        quantity: Int?,
        acquisitionDate: Long?,
        acquiredFrom: String?,
        inventoryLocation: String?,
    ): Int = update(
        internalId, contentValuesOf(
            Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP to System.currentTimeMillis(),
            Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY to priceCurrency,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID to price,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY to currentValueCurrency,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE to currentValue,
            Collection.Columns.PRIVATE_INFO_QUANTITY to quantity,
            Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE to acquisitionDate.asDateForApi(),
            Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM to acquiredFrom,
            Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION to inventoryLocation
        )
    )

    suspend fun markAsDeleted(internalId: Long) =
        update(internalId, contentValuesOf(Collection.Columns.COLLECTION_DELETE_TIMESTAMP to System.currentTimeMillis()))

    suspend fun resetTimestamps(internalId: Long) =
        update(
            internalId, contentValuesOf(
                Collection.Columns.COLLECTION_DIRTY_TIMESTAMP to 0,
                Collection.Columns.STATUS_DIRTY_TIMESTAMP to 0,
                Collection.Columns.COMMENT_DIRTY_TIMESTAMP to 0,
                Collection.Columns.RATING_DIRTY_TIMESTAMP to 0,
                Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP to 0,
                Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP to 0,
                Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP to 0,
                Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP to 0,
                Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP to 0,
            )
        )

    suspend fun updateComment(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.COMMENT, Collection.Columns.COMMENT_DIRTY_TIMESTAMP)

    suspend fun updatePrivateComment(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.PRIVATE_INFO_COMMENT, Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP)

    suspend fun updateWishlistComment(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.WISHLIST_COMMENT, Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP)

    suspend fun updateCondition(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.CONDITION, Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP)

    suspend fun updateHasParts(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.HASPARTS_LIST, Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP)

    suspend fun updateWantParts(internalId: Long, text: String): Int =
        updateText(internalId, text, Collection.Columns.WANTPARTS_LIST, Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP)

    private suspend fun updateText(internalId: Long, text: String, textColumn: String, timestampColumn: String): Int =
        update(
            internalId, contentValuesOf(
                textColumn to text,
                timestampColumn to System.currentTimeMillis()
            )
        )

    suspend fun clearDirtyTimestamp(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.COLLECTION_DIRTY_TIMESTAMP)

    suspend fun clearStatusTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.STATUS_DIRTY_TIMESTAMP)

    suspend fun clearRatingTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.RATING_DIRTY_TIMESTAMP)

    suspend fun clearCommentTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.COMMENT_DIRTY_TIMESTAMP)

    suspend fun clearTradeConditionTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP)

    suspend fun clearPrivateInfoTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP)

    suspend fun clearWishListTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP)

    suspend fun clearWantPartsTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP)

    suspend fun clearHasPartsTimestampColumn(internalId: Long) = clearTimestampColumn(internalId, Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP)

    private suspend fun clearTimestampColumn(internalId: Long, timestampColumn: String) = update(internalId, contentValuesOf(timestampColumn to 0))

    private suspend fun update(internalId: Long, values: ContentValues): Int = withContext(Dispatchers.IO) {
        if (internalId != INVALID_ID.toLong()) {
            resolver.update(Collection.buildUri(internalId), values, null, null)
        } else 0
    }

    suspend fun delete(internalId: Long) = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Collection.buildUri(internalId), null, null)
    }

    /**
     * Remove all collection items belonging to a game, except the ones in the specified list.
     *
     * @param gameId                 delete collection items with this game ID.
     * @param protectedCollectionIds list of collection IDs not to delete.
     * @return the number or rows deleted.
     */
    suspend fun delete(gameId: Int, protectedCollectionIds: List<Int> = emptyList()): Int = withContext(Dispatchers.IO) {
        // determine the collection IDs that are no longer in the collection
        val collectionIdsToDelete = resolver.queryInts(
            Collection.CONTENT_URI,
            Collection.Columns.COLLECTION_ID,
            "collection.${Collection.Columns.GAME_ID}=?",
            arrayOf(gameId.toString()),
            valueIfNull = INVALID_ID,
        ).toMutableList()
        collectionIdsToDelete.removeAll(protectedCollectionIds.toSet())
        collectionIdsToDelete.removeAll(setOf(INVALID_ID))
        // delete them
        var numberOfDeletedRows = 0
        if (collectionIdsToDelete.isNotEmpty()) {
            for (collectionId in collectionIdsToDelete) {
                numberOfDeletedRows += resolver.delete(
                    Collection.CONTENT_URI,
                    "${Collection.Columns.COLLECTION_ID}=?",
                    arrayOf(collectionId.toString())
                )
            }
        }
        numberOfDeletedRows
    }

    suspend fun deleteUnupdatedItems(timestamp: Long): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(
            Collection.CONTENT_URI,
            "${Collection.Columns.UPDATED_LIST}<?",
            arrayOf(timestamp.toString())
        ).also { count ->
            Timber.d("Deleted $count old collection items")
        }
    }

    suspend fun saveItem(
        item: CollectionItemEntity,
        game: CollectionItemGameEntity,
        updatedTimestamp: Long,
        includeStats: Boolean = true,
        includePrivateInfo: Boolean = true,
        isBrief: Boolean = false
    ): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var internalId = INVALID_ID.toLong()
        val candidate = SyncCandidate.find(resolver, item.collectionId, item.gameId)
        if (candidate.dirtyTimestamp != NOT_DIRTY) {
            Timber.i("Local copy of the collection item is dirty, skipping sync.")
        } else {
            upsertGame(item.gameId, toGameValues(game, includeStats, isBrief, updatedTimestamp), isBrief)
            internalId = upsertItem(
                toCollectionValues(item, includeStats, includePrivateInfo, isBrief, updatedTimestamp),
                isBrief,
                candidate
            )
            Timber.i(
                "Saved collection item '%s' [ID=%s, collection ID=%s]",
                item.gameName,
                item.gameId,
                item.collectionId
            )
        }
        item.collectionId to internalId
    }

    private fun toGameValues(
        game: CollectionItemGameEntity,
        includeStats: Boolean,
        isBrief: Boolean,
        updatedTimestamp: Long
    ): ContentValues {
        val values = ContentValues()
        values.put(Games.Columns.UPDATED_LIST, updatedTimestamp)
        values.put(Games.Columns.GAME_ID, game.gameId)
        values.put(Games.Columns.GAME_NAME, game.gameName)
        values.put(Games.Columns.GAME_SORT_NAME, game.sortName)
        if (!isBrief) {
            values.put(Games.Columns.NUM_PLAYS, game.numberOfPlays)
        }
        if (includeStats) {
            values.put(Games.Columns.MIN_PLAYERS, game.minNumberOfPlayers)
            values.put(Games.Columns.MAX_PLAYERS, game.maxNumberOfPlayers)
            values.put(Games.Columns.PLAYING_TIME, game.playingTime)
            values.put(Games.Columns.MIN_PLAYING_TIME, game.minPlayingTime)
            values.put(Games.Columns.MAX_PLAYING_TIME, game.maxPlayingTime)
            values.put(Games.Columns.STATS_NUMBER_OWNED, game.numberOwned)
            values.put(Games.Columns.STATS_AVERAGE, game.average)
            values.put(Games.Columns.STATS_BAYES_AVERAGE, game.bayesAverage)
            if (!isBrief) {
                values.put(Games.Columns.STATS_USERS_RATED, game.numberOfUsersRated)
                values.put(Games.Columns.STATS_STANDARD_DEVIATION, game.standardDeviation)
                values.put(Games.Columns.STATS_MEDIAN, game.median)
            }
        }
        return values
    }

    private fun upsertGame(gameId: Int, values: ContentValues, isBrief: Boolean) {
        val uri = Games.buildGameUri(gameId)
        if (resolver.rowExists(uri)) {
            values.remove(Games.Columns.GAME_ID)
            if (isBrief) {
                values.remove(Games.Columns.GAME_NAME)
                values.remove(Games.Columns.GAME_SORT_NAME)
            }
            resolver.update(uri, values, null, null)
        } else {
            resolver.insert(Games.CONTENT_URI, values)
        }
    }

    private fun toCollectionValues(
        item: CollectionItemEntity,
        includeStats: Boolean,
        includePrivateInfo: Boolean,
        isBrief: Boolean,
        updatedTimestamp: Long
    ): ContentValues {
        val values = ContentValues()
        if (!isBrief && includePrivateInfo && includeStats) {
            values.put(Collection.Columns.UPDATED, updatedTimestamp)
        }
        values.put(Collection.Columns.UPDATED_LIST, updatedTimestamp)
        values.put(Collection.Columns.GAME_ID, item.gameId)
        if (item.collectionId != INVALID_ID) {
            values.put(Collection.Columns.COLLECTION_ID, item.collectionId)
        }
        values.put(Collection.Columns.COLLECTION_NAME, item.collectionName)
        values.put(Collection.Columns.COLLECTION_SORT_NAME, item.sortName)
        values.put(Collection.Columns.STATUS_OWN, item.own)
        values.put(Collection.Columns.STATUS_PREVIOUSLY_OWNED, item.previouslyOwned)
        values.put(Collection.Columns.STATUS_FOR_TRADE, item.forTrade)
        values.put(Collection.Columns.STATUS_WANT, item.wantInTrade)
        values.put(Collection.Columns.STATUS_WANT_TO_PLAY, item.wantToPlay)
        values.put(Collection.Columns.STATUS_WANT_TO_BUY, item.wantToBuy)
        values.put(Collection.Columns.STATUS_WISHLIST, item.wishList)
        values.put(Collection.Columns.STATUS_WISHLIST_PRIORITY, item.wishListPriority)
        values.put(Collection.Columns.STATUS_PREORDERED, item.preOrdered)
        values.put(Collection.Columns.LAST_MODIFIED, item.lastModifiedDate)
        if (!isBrief) {
            values.put(Collection.Columns.COLLECTION_YEAR_PUBLISHED, item.collectionYearPublished)
            values.put(Collection.Columns.COLLECTION_IMAGE_URL, item.imageUrl)
            values.put(Collection.Columns.COLLECTION_THUMBNAIL_URL, item.thumbnailUrl)
            values.put(Collection.Columns.COMMENT, item.comment)
            values.put(Collection.Columns.CONDITION, item.conditionText)
            values.put(Collection.Columns.WANTPARTS_LIST, item.wantPartsList)
            values.put(Collection.Columns.HASPARTS_LIST, item.hasPartsList)
            values.put(Collection.Columns.WISHLIST_COMMENT, item.wishListComment)
            if (includePrivateInfo) {
                values.put(Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY, item.pricePaidCurrency)
                values.put(Collection.Columns.PRIVATE_INFO_PRICE_PAID, item.pricePaid)
                values.put(Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, item.currentValueCurrency)
                values.put(Collection.Columns.PRIVATE_INFO_CURRENT_VALUE, item.currentValue)
                values.put(Collection.Columns.PRIVATE_INFO_QUANTITY, item.quantity)
                values.put(Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE, item.acquisitionDate.asDateForApi())
                values.put(Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM, item.acquiredFrom)
                values.put(Collection.Columns.PRIVATE_INFO_COMMENT, item.privateComment)
                values.put(Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION, item.inventoryLocation)
            }
        }
        if (includeStats) {
            values.put(Collection.Columns.RATING, item.rating)
        }
        return values
    }

    suspend fun addNewCollectionItem(
        gameId: Int,
        game: GameLocal?,
        statuses: List<String>,
        wishListPriority: Int,
        timestamp: Long = System.currentTimeMillis(),
    ): Long {
        return if (gameId != INVALID_ID) {
            val values = contentValuesOf(
                Collection.Columns.GAME_ID to gameId,
                Collection.Columns.COLLECTION_DIRTY_TIMESTAMP to timestamp,
                Collection.Columns.STATUS_DIRTY_TIMESTAMP to timestamp,
            )
            values.putStatusValue(statuses, Collection.Columns.STATUS_OWN)
            values.putStatusValue(statuses, Collection.Columns.STATUS_PREORDERED)
            values.putStatusValue(statuses, Collection.Columns.STATUS_FOR_TRADE)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WANT)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WANT_TO_PLAY)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WANT_TO_BUY)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WISHLIST)
            values.putStatusValue(statuses, Collection.Columns.STATUS_PREVIOUSLY_OWNED)
            if (statuses.contains(Collection.Columns.STATUS_WISHLIST)) {
                values.put(Collection.Columns.STATUS_WISHLIST, 1)
                values.put(Collection.Columns.STATUS_WISHLIST_PRIORITY, wishListPriority)
            } else {
                values.put(Collection.Columns.STATUS_WISHLIST, 0)
            }
            game?.let {
                values.put(Collection.Columns.COLLECTION_NAME, it.gameName)
                values.put(Collection.Columns.COLLECTION_SORT_NAME, it.gameSortName)
                values.put(Collection.Columns.COLLECTION_YEAR_PUBLISHED, it.yearPublished)
                values.put(Collection.Columns.COLLECTION_IMAGE_URL, it.imageUrl)
                values.put(Collection.Columns.COLLECTION_THUMBNAIL_URL, it.thumbnailUrl)
                values.put(Collection.Columns.COLLECTION_HERO_IMAGE_URL, it.heroImageUrl)
                it.gameName
            }
            upsertItem(values)
        } else INVALID_ID.toLong()
    }

    private fun ContentValues.putStatusValue(statuses: List<String>, statusColumn: String) {
        put(statusColumn, if (statuses.contains(statusColumn)) 1 else 0)
    }

    private suspend fun upsertItem(
        values: ContentValues,
        isBrief: Boolean = false,
        candidate: SyncCandidate = SyncCandidate()
    ): Long =
        withContext(Dispatchers.IO) {
            if (candidate.internalId != INVALID_ID.toLong()) {
                removeDirtyValues(values, candidate)
                val uri = Collection.buildUri(candidate.internalId)
                if (!isBrief) maybeDeleteThumbnail(values, uri)
                resolver.update(uri, values, null, null)
                candidate.internalId
            } else {
                val url = resolver.insert(Collection.CONTENT_URI, values)
                url?.lastPathSegment?.toLongOrNull() ?: INVALID_ID.toLong()
            }
        }

    private fun removeDirtyValues(values: ContentValues, candidate: SyncCandidate) {
        removeValuesIfDirty(
            values, candidate.statusDirtyTimestamp,
            Collection.Columns.STATUS_OWN,
            Collection.Columns.STATUS_PREVIOUSLY_OWNED,
            Collection.Columns.STATUS_FOR_TRADE,
            Collection.Columns.STATUS_WANT,
            Collection.Columns.STATUS_WANT_TO_BUY,
            Collection.Columns.STATUS_WISHLIST,
            Collection.Columns.STATUS_WANT_TO_PLAY,
            Collection.Columns.STATUS_PREORDERED,
            Collection.Columns.STATUS_WISHLIST_PRIORITY,
        )
        removeValuesIfDirty(values, candidate.ratingDirtyTimestamp, Collection.Columns.RATING)
        removeValuesIfDirty(values, candidate.commentDirtyTimestamp, Collection.Columns.COMMENT)
        removeValuesIfDirty(
            values, candidate.privateInfoDirtyTimestamp,
            Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM,
            Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE,
            Collection.Columns.PRIVATE_INFO_COMMENT,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY,
            Collection.Columns.PRIVATE_INFO_QUANTITY,
            Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION,
        )
        removeValuesIfDirty(values, candidate.wishListCommentDirtyTimestamp, Collection.Columns.WISHLIST_COMMENT)
        removeValuesIfDirty(values, candidate.tradeConditionDirtyTimestamp, Collection.Columns.CONDITION)
        removeValuesIfDirty(values, candidate.wantPartsDirtyTimestamp, Collection.Columns.WANTPARTS_LIST)
        removeValuesIfDirty(values, candidate.hasPartsDirtyTimestamp, Collection.Columns.HASPARTS_LIST)
    }

    private fun removeValuesIfDirty(values: ContentValues, dirtyFlag: Long, vararg columns: String) {
        if (dirtyFlag != NOT_DIRTY) columns.forEach { values.remove(it) }
    }

    private fun maybeDeleteThumbnail(values: ContentValues, uri: Uri) {
        values.getAsString(Collection.Columns.COLLECTION_THUMBNAIL_URL)?.let { newThumbnailUrl ->
            val oldThumbnailUrl = resolver.queryString(uri, Collection.Columns.COLLECTION_THUMBNAIL_URL).orEmpty()
            if (newThumbnailUrl != oldThumbnailUrl) {
                val thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl)
                if (thumbnailFileName.isNotBlank()) {
                    resolver.delete(Thumbnails.buildUri(thumbnailFileName), null, null)
                }
            }
        }
    }

    class SyncCandidate(
        val internalId: Long = INVALID_ID.toLong(),
        val dirtyTimestamp: Long = 0,
        val statusDirtyTimestamp: Long = 0,
        val ratingDirtyTimestamp: Long = 0,
        val commentDirtyTimestamp: Long = 0,
        val privateInfoDirtyTimestamp: Long = 0,
        val wishListCommentDirtyTimestamp: Long = 0,
        val tradeConditionDirtyTimestamp: Long = 0,
        val wantPartsDirtyTimestamp: Long = 0,
        val hasPartsDirtyTimestamp: Long = 0,
    ) {
        companion object {
            private val projection = arrayOf(
                BaseColumns._ID,
                Collection.Columns.COLLECTION_DIRTY_TIMESTAMP,
                Collection.Columns.STATUS_DIRTY_TIMESTAMP,
                Collection.Columns.RATING_DIRTY_TIMESTAMP,
                Collection.Columns.COMMENT_DIRTY_TIMESTAMP,
                Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP,
                Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
                Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP,
                Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP,
                Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP,
            )

            fun find(resolver: ContentResolver, collectionId: Int, gameId: Int): SyncCandidate {
                if (collectionId != INVALID_ID) {
                    resolver.query(
                        Collection.CONTENT_URI,
                        projection,
                        "${Collection.Columns.COLLECTION_ID}=?",
                        arrayOf(collectionId.toString()),
                        null
                    )?.use {
                        if (it.moveToFirst()) return fromCursor(it)
                    }
                }
                resolver.query(
                    Collection.CONTENT_URI,
                    projection,
                    "collection.${Collection.Columns.GAME_ID}=? AND ${Collection.Columns.COLLECTION_ID.whereNullOrBlank()}",
                    arrayOf(gameId.toString()),
                    null
                )?.use {
                    if (it.moveToFirst()) return fromCursor(it)
                }
                return SyncCandidate()
            }

            private fun fromCursor(cursor: Cursor): SyncCandidate {
                return SyncCandidate(
                    cursor.getLongOrNull(0) ?: INVALID_ID.toLong(),
                    cursor.getLongOrNull(1) ?: 0L,
                    cursor.getLongOrNull(2) ?: 0L,
                    cursor.getLongOrNull(3) ?: 0L,
                    cursor.getLongOrNull(4) ?: 0L,
                    cursor.getLongOrNull(5) ?: 0L,
                    cursor.getLongOrNull(6) ?: 0L,
                    cursor.getLongOrNull(7) ?: 0L,
                    cursor.getLongOrNull(8) ?: 0L,
                    cursor.getLongOrNull(9) ?: 0L,
                )
            }
        }
    }

    companion object {
        private const val NOT_DIRTY = 0L
    }
}
