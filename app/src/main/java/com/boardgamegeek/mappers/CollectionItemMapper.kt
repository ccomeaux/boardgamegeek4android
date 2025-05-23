package com.boardgamegeek.mappers

import android.graphics.Color
import com.boardgamegeek.db.model.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.sortName
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.CollectionItemRemote
import com.boardgamegeek.model.GameSubtype
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
    collectionName = name.orEmpty(),
    collectionSortName = name.sortName(sortindex),
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
    collectionName = name.orEmpty(),
    collectionSortName = name.sortName(sortindex),
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
    gameName = if (originalname.isNullOrBlank()) name.orEmpty() else originalname.orEmpty(),
    collectionId = collid.toIntOrNull() ?: BggContract.INVALID_ID,
    collectionName = name.orEmpty(),
    sortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else originalname.orEmpty(),
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

fun CollectionItemRemote.mapToCollectionGameForInsert(updatedTimestamp: Long, internalId: Long = 0L) = CollectionGameForInsert(
    internalId = internalId,
    gameId = objectid,
    gameName = if (originalname.isNullOrBlank()) name.orEmpty() else originalname.orEmpty(),
    gameSortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else originalname,
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

fun CollectionItemRemote.mapToCollectionGameForUpdate(updatedTimestamp: Long, internalId: Long = 0L) = CollectionGameForUpdate(
    internalId = internalId,
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
    return this.item.mapToModel().addGame(this.game)
}

fun CollectionItemWithGameAndLastPlayedEntity.mapToModel(): CollectionItem {
    val playDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return this.item.mapToModel()
        .addGame(this.game)
        .copy(lastPlayDate = lastPlayedDate?.toMillis(playDateFormat) ?: 0L)
}

fun CollectionItemEntity.mapToModel(): CollectionItem {
    val acquisitionDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return CollectionItem(
        internalId = internalId,
        gameId = gameId,
        collectionId = collectionId,
        collectionName = collectionName,
        sortName = collectionSortName,
        collectionYearPublished = collectionYearPublished ?: CollectionItem.YEAR_UNKNOWN,
        imageUrl = collectionImageUrl.orEmpty(),
        thumbnailUrl = collectionThumbnailUrl.orEmpty(),
        heroImageUrl = collectionHeroImageUrl.orEmpty(),
        rating = rating ?: CollectionItem.UNRATED,
        own = statusOwn,
        previouslyOwned = statusPreviouslyOwned,
        forTrade = statusForTrade,
        wantInTrade = statusWant,
        wantToPlay = statusWantToPlay,
        wantToBuy = statusWantToBuy,
        wishList = statusWishlist,
        wishListPriority = statusWishlistPriority ?: CollectionItem.WISHLIST_PRIORITY_UNKNOWN,
        preOrdered = statusPreordered,
        lastModifiedDate = lastModified ?: 0L,
        pricePaidCurrency = privateInfoPricePaidCurrency.orEmpty(),
        pricePaid = privateInfoPricePaid ?: 0.0,
        currentValueCurrency = privateInfoCurrentValueCurrency.orEmpty(),
        currentValue = privateInfoCurrentValue ?: 0.0,
        quantity = privateInfoQuantity ?: 1,
        acquisitionDate = privateInfoAcquisitionDate.toMillis(acquisitionDateFormat),
        acquiredFrom = privateInfoAcquiredFrom.orEmpty(),
        privateComment = privateInfoComment.orEmpty(),
        inventoryLocation = privateInfoInventoryLocation.orEmpty(),
        comment = comment.orEmpty(),
        conditionText = condition.orEmpty(),
        wantPartsList = wantpartsList.orEmpty(),
        hasPartsList = haspartsList.orEmpty(),
        wishListComment = wishlistComment.orEmpty(),
        syncTimestamp = updatedTimestamp ?: 0L,
        deleteTimestamp = collectionDeleteTimestamp ?: 0L,
        dirtyTimestamp = collectionDirtyTimestamp ?: 0L,
        statusDirtyTimestamp = statusDirtyTimestamp ?: 0L,
        ratingDirtyTimestamp = ratingDirtyTimestamp ?: 0L,
        commentDirtyTimestamp = commentDirtyTimestamp ?: 0L,
        privateInfoDirtyTimestamp = privateInfoDirtyTimestamp ?: 0L,
        wishListCommentDirtyTimestamp = wishlistCommentDirtyTimestamp ?: 0L,
        tradeConditionDirtyTimestamp = tradeConditionDirtyTimestamp ?: 0L,
        hasPartsDirtyTimestamp = hasPartsDirtyTimestamp ?: 0L,
        wantPartsDirtyTimestamp = wantPartsDirtyTimestamp ?: 0L,
    )
}

private fun CollectionItem.addGame(game: GameEntity): CollectionItem {
    return this.copy(
        gameName = game.gameName,
        gameYearPublished = game.yearPublished ?: CollectionItem.YEAR_UNKNOWN,
        gameImageUrl = game.imageUrl.orEmpty(),
        gameThumbnailUrl = game.thumbnailUrl.orEmpty(),
        gameHeroImageUrl = game.heroImageUrl.orEmpty(),
        averageRating = game.average ?: CollectionItem.UNRATED,
        lastViewedDate = game.lastViewedTimestamp ?: 0L,
        numberOfPlays = game.numberOfPlays,
        winsColor = game.winsColor ?: Color.TRANSPARENT,
        winnablePlaysColor = game.winnablePlaysColor ?: Color.TRANSPARENT,
        allPlaysColor = game.allPlaysColor ?: Color.TRANSPARENT,
        playingTime = game.playingTime ?: 0,
        minimumAge = game.minimumAge ?: 0,
        rank = game.gameRank ?: GameSubtype.RANK_UNKNOWN,
        geekRating = game.bayesAverage ?: CollectionItem.UNRATED,
        averageWeight = game.averageWeight ?: CollectionItem.UNWEIGHTED,
        isFavorite = game.isStarred ?: false,
        arePlayersCustomSorted = game.customPlayerSort ?: false,
        minPlayerCount = game.minPlayers ?: 0,
        maxPlayerCount = game.maxPlayers ?: 0,
        subtype = game.subtype.fromDatabaseToSubtype(),
        bestPlayerCounts = game.playerCountsBest.splitFromDatabase(),
        recommendedPlayerCounts = game.playerCountsRecommended.splitFromDatabase(),
        numberOfUsersOwned = game.numberOfUsersOwned ?: 0,
        numberOfUsersWanting = game.numberOfUsersWanting ?: 0,
        numberOfUsersRating = game.numberOfRatings ?: 0,
        numberOfUsersWishing = game.numberOfUsersWishListing ?: 0,
        standardDeviation = game.standardDeviation ?: 0.0,
    )
}
