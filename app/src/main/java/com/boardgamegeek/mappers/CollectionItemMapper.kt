package com.boardgamegeek.mappers

import android.graphics.Color
import com.boardgamegeek.db.model.CollectionItemLocal
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.CollectionItemGameEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.sortName
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import okhttp3.FormBody
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

fun CollectionItem.mapToEntities(): Pair<CollectionItemEntity, CollectionItemGameEntity> {
    val item = CollectionItemEntity(
        gameId = objectid,
        gameName = if (originalname.isNullOrBlank()) name else originalname,
        collectionId = collid.toIntOrNull() ?: BggContract.INVALID_ID,
        collectionName = name,
        sortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else name,
        gameYearPublished = yearpublished?.toIntOrNull() ?: CollectionItemEntity.YEAR_UNKNOWN,
        collectionYearPublished = yearpublished?.toIntOrNull() ?: CollectionItemEntity.YEAR_UNKNOWN,
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

    val game = CollectionItemGameEntity(
        gameId = objectid,
        gameName = if (originalname.isNullOrBlank()) name else originalname,
        sortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else name,
        yearPublished = yearpublished?.toIntOrNull() ?: CollectionItemGameEntity.YEAR_UNKNOWN,
        imageUrl = image.orEmpty(),
        thumbnailUrl = thumbnail.orEmpty(),
        minNumberOfPlayers = stats?.minplayers ?: 0,
        maxNumberOfPlayers = stats?.maxplayers ?: 0,
        minPlayingTime = stats?.minplaytime ?: 0,
        maxPlayingTime = stats?.maxplaytime ?: 0,
        playingTime = stats?.playingtime ?: 0,
        numberOwned = stats?.numowned?.toIntOrNull() ?: 0,
        numberOfUsersRated = stats?.usersrated?.toIntOrNull() ?: 0,
        rating = stats?.rating?.toDoubleOrNull() ?: 0.0,
        average = stats?.average?.toDoubleOrNull() ?: 0.0,
        bayesAverage = stats?.bayesaverage?.toDoubleOrNull() ?: 0.0,
        standardDeviation = stats?.stddev?.toDoubleOrNull() ?: 0.0,
        median = stats?.median?.toDoubleOrNull() ?: 0.0,
        numberOfPlays = numplays
    )
    return item to game
}

fun CollectionItemEntity.mapToFormBodyForDeletion(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "delete")
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItemEntity.mapToFormBodyForInsert(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "additem")
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItemEntity.mapToFormBodyForStatusUpdate(): FormBody {
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
fun CollectionItemEntity.mapToFormBodyForRatingUpdate(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", "rating")
        .add("rating", rating.toString())
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItemEntity.mapToFormBodyForPrivateInfoUpdate(): FormBody {
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

fun CollectionItemEntity.mapToFormBodyForCommentUpdate() = mapToFormBodyForTextUpdate("comment", comment)

@Suppress("SpellCheckingInspection")
fun CollectionItemEntity.mapToFormBodyForWishlistCommentUpdate() = mapToFormBodyForTextUpdate("wishlistcomment", wishListComment)

@Suppress("SpellCheckingInspection")
fun CollectionItemEntity.mapToFormBodyForTradeConditionUpdate() = mapToFormBodyForTextUpdate("conditiontext", conditionText)

@Suppress("SpellCheckingInspection")
fun CollectionItemEntity.mapToFormBodyForWantPartsUpdate() = mapToFormBodyForTextUpdate("wantpartslist", wantPartsList)

@Suppress("SpellCheckingInspection")
fun CollectionItemEntity.mapToFormBodyForHasPartsUpdate() = mapToFormBodyForTextUpdate("haspartslist", hasPartsList)

@Suppress("SpellCheckingInspection")
private fun CollectionItemEntity.mapToFormBodyForTextUpdate(fieldName: String, value: String): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", fieldName)
        .add("value", value)
        .build()
}

@Suppress("SpellCheckingInspection")
private fun CollectionItemEntity.mapToFormBodyBuilder(): FormBody.Builder {
    val builder = FormBody.Builder()
        .add("ajax", "1")
        .add("objecttype", "thing")
        .add("objectid", gameId.toString())
    if (collectionId != BggContract.INVALID_ID) builder.add("collid", collectionId.toString())
    return builder
}

fun CollectionItemLocal.mapToEntity(gameEntity: GameEntity?) = CollectionItemEntity(
    internalId = internalId,
    gameId = gameId,
    gameName = gameEntity?.name.orEmpty(),
    collectionId = collectionId,
    collectionName = collectionName,
    sortName = collectionSortName,
    gameYearPublished = gameEntity?.yearPublished ?: CollectionItemEntity.YEAR_UNKNOWN,
    collectionYearPublished = collectionYearPublished ?: CollectionItemEntity.YEAR_UNKNOWN,
    imageUrl = collectionImageUrl.orEmpty(),
    thumbnailUrl = collectionThumbnailUrl.orEmpty(),
    heroImageUrl = collectionHeroImageUrl.orEmpty(),
    gameImageUrl = gameEntity?.imageUrl.orEmpty(),
    gameThumbnailUrl = gameEntity?.thumbnailUrl.orEmpty(),
    gameHeroImageUrl = gameEntity?.heroImageUrl.orEmpty(),
    averageRating = gameEntity?.rating ?: CollectionItemEntity.UNRATED,
    rating = rating ?: CollectionItemEntity.UNRATED,
    own = statusOwn == 1,
    previouslyOwned = statusPreviouslyOwned == 1,
    forTrade = statusForTrade == 1,
    wantInTrade = statusWant == 1,
    wantToPlay = statusWantToPlay == 1,
    wantToBuy = statusWantToBuy == 1,
    wishList = statusWishlist == 1,
    wishListPriority = statusWishlistPriority ?: CollectionItemEntity.WISHLIST_PRIORITY_UNKNOWN,
    preOrdered = statusPreordered == 1,
    lastModifiedDate = lastModified ?: 0L,
    lastViewedDate = gameEntity?.lastViewedTimestamp ?: 0L,
    numberOfPlays = gameEntity?.numberOfPlays ?: 0,
    pricePaidCurrency = privateInfoPricePaidCurrency.orEmpty(),
    pricePaid = privateInfoPricePaid ?: 0.0,
    currentValueCurrency = privateInfoCurrentValueCurrency.orEmpty(),
    currentValue = privateInfoCurrentValue ?: 0.0,
    quantity = privateInfoQuantity ?: 1,
    acquisitionDate = privateInfoAcquisitionDate.toMillis(SimpleDateFormat("yyyy-MM-dd", Locale.US)),
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
    winsColor = gameEntity?.winsColor ?: Color.TRANSPARENT,
    winnablePlaysColor = gameEntity?.winnablePlaysColor ?: Color.TRANSPARENT,
    allPlaysColor = gameEntity?.allPlaysColor ?: Color.TRANSPARENT,
    playingTime = gameEntity?.playingTime ?: 0,
    minimumAge = gameEntity?.minimumAge ?: 0,
    rank = gameEntity?.overallRank ?: CollectionItemEntity.RANK_UNKNOWN,
    geekRating = gameEntity?.bayesAverage ?: CollectionItemEntity.UNRATED,
    averageWeight = gameEntity?.averageWeight ?: 0.0,
    isFavorite = gameEntity?.isFavorite ?: false,
    lastPlayDate = gameEntity?.lastPlayTimestamp ?: 0L,
    arePlayersCustomSorted = gameEntity?.customPlayerSort ?: false,
    minPlayerCount = gameEntity?.minPlayers ?: 0,
    maxPlayerCount = gameEntity?.maxPlayers ?: 0,
    subtype = gameEntity?.subtype,
    bestPlayerCounts = gameEntity?.playerCountsBest.orEmpty(),
    recommendedPlayerCounts = gameEntity?.playerCountsRecommended.orEmpty(),
    numberOfUsersOwned = gameEntity?.numberOfUsersOwned ?: 0,
    numberOfUsersWanting = gameEntity?.numberOfUsersWanting ?: 0,
    numberOfUsersRating = gameEntity?.numberOfRatings ?: 0,
    standardDeviation = gameEntity?.standardDeviation ?: 0.0,
)
