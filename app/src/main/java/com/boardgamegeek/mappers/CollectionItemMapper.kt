package com.boardgamegeek.mappers

import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.CollectionItemForUploadEntity
import com.boardgamegeek.entities.CollectionItemGameEntity
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

fun CollectionItemForUploadEntity.mapToFormBodyForDeletion(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "delete")
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForInsert(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "additem")
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForStatusUpdate(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", "status")
        .add("own", if (owned) "1" else "0")
        .add("prevowned", if (previouslyOwned) "1" else "0")
        .add("fortrade", if (forTrade) "1" else "0")
        .add("want", if (wantInTrade) "1" else "0")
        .add("wanttobuy", if (wantToBuy) "1" else "0")
        .add("wanttoplay", if (wantToPlay) "1" else "0")
        .add("preordered", if (preordered) "1" else "0")
        .add("wishlist", if (wishlist) "1" else "0")
        .add("wishlistpriority", wishlistPriority.toString())
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForRatingUpdate(): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", "rating")
        .add("rating", rating.toString())
        .build()
}

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForPrivateInfoUpdate(): FormBody {
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
        .add("acquisitiondate", acquisitionDate)
        .add("acquiredfrom", acquiredFrom)
        .add("privatecomment", privateComment)
        .add("invlocation", inventoryLocation)
        .build()
}

fun CollectionItemForUploadEntity.mapToFormBodyForCommentUpdate() = mapToFormBodyForTextUpdate("comment")

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForWishlistCommentUpdate() = mapToFormBodyForTextUpdate("wishlistcomment")

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForTradeConditionUpdate() = mapToFormBodyForTextUpdate("conditiontext")

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForWantPartsUpdate() = mapToFormBodyForTextUpdate("wantpartslist")

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForHasPartsUpdate() = mapToFormBodyForTextUpdate("haspartslist")

@Suppress("SpellCheckingInspection")
fun CollectionItemForUploadEntity.mapToFormBodyForTextUpdate(value: String): FormBody {
    return mapToFormBodyBuilder()
        .add("action", "savedata")
        .add("fieldname", value)
        .add("value", hasParts.orEmpty())
        .build()
}

@Suppress("SpellCheckingInspection")
private fun CollectionItemForUploadEntity.mapToFormBodyBuilder(): FormBody.Builder {
    val builder = FormBody.Builder()
        .add("ajax", "1")
        .add("objecttype", "thing")
        .add("objectid", gameId.toString())
    if (collectionId != BggContract.INVALID_ID) builder.add("collid", collectionId.toString())
    return builder
}
