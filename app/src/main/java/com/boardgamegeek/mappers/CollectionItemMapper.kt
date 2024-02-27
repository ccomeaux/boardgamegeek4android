package com.boardgamegeek.mappers

import android.graphics.Color
import com.boardgamegeek.db.model.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.sortName
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.extensions.toSubtype
import com.boardgamegeek.io.model.CollectionItemRemote
import com.boardgamegeek.model.GameRank
import com.boardgamegeek.provider.BggContract
import okhttp3.FormBody
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

fun CollectionItemRemote.mapForInsert(updatedTimestamp: Long) = CollectionItemForInsert(
    internalId = 0L,
    updatedTimestamp = updatedTimestamp,
    updatedListTimestamp = updatedTimestamp,
    gameId = objectid,
    collectionId = collid.toIntOrNull() ?: BggContract.INVALID_ID,
    collectionName = name,
    collectionSortName = if (name.isNullOrBlank()) name.sortName(sortindex) else name,
    statusOwn = own?.equals("1") ?: false,
    statusPreviouslyOwned = prevowned?.equals("1") ?: false,
    statusForTrade = fortrade?.equals("1") ?: false,
    statusWant = want?.equals("1") ?: false,
    statusWantToPlay = wanttoplay?.equals("1") ?: false,
    statusWantToBuy = wanttobuy?.equals("1") ?: false,
    statusWishlist = wishlist?.equals("1") ?: false,
    statusWishlistPriority = if (wishlist?.equals("1") == true) wishlistpriority else CollectionItem.WISHLIST_PRIORITY_UNKNOWN,
    statusPreordered = preordered?.equals("1") ?: false,
    comment = comment.orEmpty(),
    lastModified = lastmodified.toMillis(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)),
    privateInfoPricePaidCurrency = pp_currency.orEmpty(),
    privateInfoPricePaid = pricepaid?.toDoubleOrNull() ?: 0.0,
    privateInfoCurrentValueCurrency = cv_currency.orEmpty(),
    privateInfoCurrentValue = currvalue?.toDoubleOrNull() ?: 0.0,
    privateInfoQuantity = quantity?.toIntOrNull() ?: 1,
    privateInfoAcquisitionDate = acquisitiondate,
    privateInfoAcquiredFrom = acquiredfrom.orEmpty(),
    privateInfoComment = privatecomment.orEmpty(),
    condition = conditiontext.orEmpty(),
    wantpartsList = wantpartslist.orEmpty(),
    haspartsList = haspartslist.orEmpty(),
    wishlistComment = wishlistcomment.orEmpty(),
    collectionYearPublished = yearpublished?.toIntOrNull() ?: CollectionItem.YEAR_UNKNOWN,
    rating = stats?.rating?.toDoubleOrNull() ?: 0.0,
    collectionThumbnailUrl = thumbnail.orEmpty(),
    collectionImageUrl = image.orEmpty(),
    privateInfoInventoryLocation = inventorylocation.orEmpty(),
    collectionDirtyTimestamp = 0L,
    statusDirtyTimestamp = 0L,
)

fun CollectionItemRemote.mapForUpdate(internalId: Long, updatedTimestamp: Long) = CollectionItemForUpdate(
    internalId = internalId,
    updatedTimestamp = updatedTimestamp,
    updatedListTimestamp = updatedTimestamp,
    gameId = objectid,
    collectionId = collid.toIntOrNull() ?: BggContract.INVALID_ID,
    collectionName = name,
    collectionSortName = if (name.isNullOrBlank()) name.sortName(sortindex) else name,
    lastModified = lastmodified.toMillis(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)),
    collectionYearPublished = yearpublished?.toIntOrNull() ?: CollectionItem.YEAR_UNKNOWN,
    collectionThumbnailUrl = thumbnail.orEmpty(),
    collectionImageUrl = image.orEmpty(),
)

fun GameEntity.mapForInsert(
    statuses: List<String>,
    wishListPriority: Int,
    timestamp: Long
) = CollectionItemForInsert(
    internalId = 0L,
    updatedTimestamp = null,
    updatedListTimestamp = null,
    gameId = gameId,
    collectionId = BggContract.INVALID_ID,
    collectionName = gameName,
    collectionSortName = gameSortName,
    statusOwn = statuses.contains(BggContract.Collection.Columns.STATUS_OWN),
    statusPreviouslyOwned = statuses.contains(BggContract.Collection.Columns.STATUS_PREVIOUSLY_OWNED),
    statusForTrade = statuses.contains(BggContract.Collection.Columns.STATUS_FOR_TRADE),
    statusWant = statuses.contains(BggContract.Collection.Columns.STATUS_WANT),
    statusWantToPlay = statuses.contains(BggContract.Collection.Columns.STATUS_WANT_TO_PLAY),
    statusWantToBuy = statuses.contains(BggContract.Collection.Columns.STATUS_WANT_TO_BUY),
    statusWishlist = statuses.contains(BggContract.Collection.Columns.STATUS_WISHLIST),
    statusWishlistPriority = wishListPriority,
    statusPreordered = statuses.contains(BggContract.Collection.Columns.STATUS_PREORDERED),
    comment = null,
    collectionImageUrl = imageUrl,
    collectionThumbnailUrl = thumbnailUrl,
    collectionYearPublished = yearPublished,
    condition = null,
    haspartsList = null,
    wantpartsList = null,
    lastModified = System.currentTimeMillis(),
    privateInfoCurrentValueCurrency = null,
    privateInfoCurrentValue = null,
    privateInfoPricePaidCurrency = null,
    privateInfoPricePaid = null,
    privateInfoQuantity = null,
    privateInfoAcquiredFrom = null,
    privateInfoAcquisitionDate = null,
    privateInfoComment = null,
    privateInfoInventoryLocation = null,
    rating = null,
    wishlistComment = null,
    collectionDirtyTimestamp = timestamp,
    statusDirtyTimestamp = timestamp,
)

fun CollectionItemRemote.mapToCollectionItem() = CollectionItem(
    gameId = objectid,
    gameName = if (originalname.isNullOrBlank()) name else originalname,
    collectionId = collid.toIntOrNull() ?: BggContract.INVALID_ID,
    collectionName = name,
    sortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else name,
    gameYearPublished = yearpublished?.toIntOrNull() ?: CollectionItem.YEAR_UNKNOWN,
    collectionYearPublished = yearpublished?.toIntOrNull() ?: CollectionItem.YEAR_UNKNOWN,
    imageUrl = image.orEmpty(),
    thumbnailUrl = thumbnail.orEmpty(),
    rating = stats?.rating?.toDoubleOrNull() ?: 0.0,
    numberOfPlays = numplays,
    comment = comment.orEmpty(),
    wantPartsList = wantpartslist.orEmpty(),
    conditionText = conditiontext.orEmpty(),
    hasPartsList = haspartslist.orEmpty(),
    wishListComment = wishlistcomment.orEmpty(),
    own = own?.equals("1") ?: false,
    previouslyOwned = prevowned?.equals("1") ?: false,
    forTrade = fortrade?.equals("1") ?: false,
    wantInTrade = want?.equals("1") ?: false,
    wantToPlay = wanttoplay?.equals("1") ?: false,
    wantToBuy = wanttobuy?.equals("1") ?: false,
    wishList = wishlist?.equals("1") ?: false,
    wishListPriority = wishlistpriority,
    preOrdered = preordered?.equals("1") ?: false,
    lastModifiedDate = lastmodified.toMillis(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)),
    pricePaidCurrency = pp_currency.orEmpty(),
    pricePaid = pricepaid?.toDoubleOrNull() ?: 0.0,
    currentValueCurrency = cv_currency.orEmpty(),
    currentValue = currvalue?.toDoubleOrNull() ?: 0.0,
    quantity = quantity?.toIntOrNull() ?: 1,
    acquisitionDate = acquisitiondate.toMillis(SimpleDateFormat("yyyy-MM-dd", Locale.US)),
    acquiredFrom = acquiredfrom.orEmpty(),
    privateComment = privatecomment.orEmpty(),
    inventoryLocation = inventorylocation.orEmpty()
)

fun CollectionItemRemote.mapToCollectionGame(updatedTimestamp: Long, internalId: Long = 0L) = CollectionGameForUpsert(
    internalId = internalId,
    gameId = objectid,
    gameName = if (originalname.isNullOrBlank()) name else originalname,
    gameSortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else name,
    yearPublished = yearpublished?.toIntOrNull() ?: CollectionItem.YEAR_UNKNOWN,
    imageUrl = image.orEmpty(),
    thumbnailUrl = thumbnail.orEmpty(),
    minPlayers = stats?.minplayers ?: 0,
    maxPlayers = stats?.maxplayers ?: 0,
    playingTime = stats?.playingtime ?: 0,
    minPlayingTime = stats?.minplaytime ?: 0,
    maxPlayingTime = stats?.maxplaytime ?: 0,
    numberOfUsersOwned = stats?.numowned?.toIntOrNull() ?: 0,
    numberOfRatings = stats?.usersrated?.toIntOrNull() ?: 0,
    average = stats?.average?.toDoubleOrNull() ?: 0.0,
    bayesAverage = stats?.bayesaverage?.toDoubleOrNull() ?: 0.0,
    standardDeviation = stats?.stddev?.toDoubleOrNull() ?: 0.0,
    median = stats?.median?.toDoubleOrNull() ?: 0.0,
    numberOfPlays = numplays,
    updatedList = updatedTimestamp,
)

fun CollectionItem.mapToFormBodyForDeletion(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "delete")
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForInsert(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "additem")
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForStatusUpdate(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", "status")
        .add("own", if (own) "1" else "0")
        .add("prevowned", if (previouslyOwned) "1" else "0")
        .add("fortrade", if (forTrade) "1" else "0")
        .add("want", if (wantInTrade) "1" else "0")
        .add("wanttobuy", if (wantToBuy) "1" else "0")
        .add("wanttoplay", if (wantToPlay) "1" else "0")
        .add("preordered", if (preOrdered) "1" else "0")
        .add("wishlist", if (wishList) "1" else "0")
        .add("wishlistpriority", wishListPriority.toString())
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForRatingUpdate(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", "rating")
        .add("rating", rating.toString())
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForPrivateInfoUpdate(): FormBody {
    fun Double.formatCurrency(currencyFormat: DecimalFormat = DecimalFormat("0.00")): String {
        return if (this == 0.0) "" else currencyFormat.format(this)
    }

    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", "ownership")
        .add("pp_currency", pricePaidCurrency)
        .add("pricepaid", pricePaid.formatCurrency())
        .add("cv_currency", currentValueCurrency)
        .add("currvalue", currentValue.formatCurrency())
        .add("quantity", quantity.toString())
        .add("acquisitiondate", acquisitionDate.asDateForApi())
        .add("acquiredfrom", acquiredFrom)
        .add("privatecomment", privateComment)
        .add("invlocation", inventoryLocation)
        .build()
}

fun CollectionItem.mapToFormBodyForCommentUpdate() = mapToFormBodyForTextUpdate("comment", comment)

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForWishlistCommentUpdate() = mapToFormBodyForTextUpdate("wishlistcomment", wishListComment)

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForTradeConditionUpdate() = mapToFormBodyForTextUpdate("conditiontext", conditionText)

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForWantPartsUpdate() = mapToFormBodyForTextUpdate("wantpartslist", wantPartsList)

@Suppress("SpellCheckingInspection")
fun CollectionItem.mapToFormBodyForHasPartsUpdate() = mapToFormBodyForTextUpdate("haspartslist", hasPartsList)

@Suppress("SpellCheckingInspection")
private fun CollectionItem.mapToFormBodyForTextUpdate(fieldName: String, value: String): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", fieldName)
        .add("value", value)
        .build()
}

@Suppress("SpellCheckingInspection")
private fun CollectionItem.mapToFormBodyBuilder(): FormBody.Builder {
    val builder = FormBody.Builder()
        .add("ajax", "1")
        .add("objecttype", "thing")
        .add("objectid", gameId.toString())
    if (collectionId != BggContract.INVALID_ID) builder.add("collid", collectionId.toString())
    return builder
}

fun CollectionItemWithGameEntity.mapToModel(): CollectionItem {
    val acquisitionDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val playDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return CollectionItem(
        internalId = item.internalId,
        gameId = item.gameId,
        gameName = game.gameName,
        collectionId = item.collectionId,
        collectionName = item.collectionName,
        sortName = item.collectionSortName,
        gameYearPublished = game.yearPublished ?: CollectionItem.YEAR_UNKNOWN,
        collectionYearPublished = item.collectionYearPublished ?: CollectionItem.YEAR_UNKNOWN,
        imageUrl = item.collectionImageUrl.orEmpty(),
        thumbnailUrl = item.collectionThumbnailUrl.orEmpty(),
        heroImageUrl = item.collectionHeroImageUrl.orEmpty(),
        gameImageUrl = game.imageUrl.orEmpty(),
        gameThumbnailUrl = game.thumbnailUrl.orEmpty(),
        gameHeroImageUrl = game.heroImageUrl.orEmpty(),
        averageRating = game.average ?: CollectionItem.UNRATED,
        rating = item.rating ?: CollectionItem.UNRATED,
        own = item.statusOwn,
        previouslyOwned = item.statusPreviouslyOwned,
        forTrade = item.statusForTrade,
        wantInTrade = item.statusWant,
        wantToPlay = item.statusWantToPlay,
        wantToBuy = item.statusWantToBuy,
        wishList = item.statusWishlist,
        wishListPriority = item.statusWishlistPriority ?: CollectionItem.WISHLIST_PRIORITY_UNKNOWN,
        preOrdered = item.statusPreordered,
        lastModifiedDate = item.lastModified ?: 0L,
        lastViewedDate = game.lastViewedTimestamp ?: 0L,
        numberOfPlays = game.numberOfPlays,
        pricePaidCurrency = item.privateInfoPricePaidCurrency.orEmpty(),
        pricePaid = item.privateInfoPricePaid ?: 0.0,
        currentValueCurrency = item.privateInfoCurrentValueCurrency.orEmpty(),
        currentValue = item.privateInfoCurrentValue ?: 0.0,
        quantity = item.privateInfoQuantity ?: 1,
        acquisitionDate = item.privateInfoAcquisitionDate.toMillis(acquisitionDateFormat),
        acquiredFrom = item.privateInfoAcquiredFrom.orEmpty(),
        privateComment = item.privateInfoComment.orEmpty(),
        inventoryLocation = item.privateInfoInventoryLocation.orEmpty(),
        comment = item.comment.orEmpty(),
        conditionText = item.condition.orEmpty(),
        wantPartsList = item.wantpartsList.orEmpty(),
        hasPartsList = item.haspartsList.orEmpty(),
        wishListComment = item.wishlistComment.orEmpty(),
        syncTimestamp = item.updatedTimestamp ?: 0L,
        deleteTimestamp = item.collectionDeleteTimestamp ?: 0L,
        dirtyTimestamp = item.collectionDirtyTimestamp ?: 0L,
        statusDirtyTimestamp = item.statusDirtyTimestamp ?: 0L,
        ratingDirtyTimestamp = item.ratingDirtyTimestamp ?: 0L,
        commentDirtyTimestamp = item.commentDirtyTimestamp ?: 0L,
        privateInfoDirtyTimestamp = item.privateInfoDirtyTimestamp ?: 0L,
        wishListCommentDirtyTimestamp = item.wishlistCommentDirtyTimestamp ?: 0L,
        tradeConditionDirtyTimestamp = item.tradeConditionDirtyTimestamp ?: 0L,
        hasPartsDirtyTimestamp = item.hasPartsDirtyTimestamp ?: 0L,
        wantPartsDirtyTimestamp = item.wantPartsDirtyTimestamp ?: 0L,
        winsColor = game.winsColor ?: Color.TRANSPARENT,
        winnablePlaysColor = game.winnablePlaysColor ?: Color.TRANSPARENT,
        allPlaysColor = game.allPlaysColor ?: Color.TRANSPARENT,
        playingTime = game.playingTime ?: 0,
        minimumAge = game.minimumAge ?: 0,
        rank = game.gameRank ?: GameRank.RANK_UNKNOWN,
        geekRating = game.bayesAverage ?: CollectionItem.UNRATED,
        averageWeight = game.averageWeight ?: CollectionItem.UNWEIGHTED,
        isFavorite = game.isStarred ?: false,
        lastPlayDate = lastPlayedDate?.toMillis(playDateFormat) ?: 0L,
        arePlayersCustomSorted = game.customPlayerSort ?: false,
        minPlayerCount = game.minPlayers ?: 0,
        maxPlayerCount = game.maxPlayers ?: 0,
        subtype = game.subtype.toSubtype(),
        bestPlayerCounts = game.playerCountsBest.splitFromDatabase(),
        recommendedPlayerCounts = game.playerCountsRecommended.splitFromDatabase(),
        numberOfUsersOwned = game.numberOfUsersOwned ?: 0,
        numberOfUsersWanting = game.numberOfUsersWanting ?: 0,
        numberOfUsersRating = game.numberOfRatings ?: 0,
        standardDeviation = game.standardDeviation ?: 0.0,
    )
}
