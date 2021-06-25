package com.boardgamegeek.mappers

import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.CollectionItemGameEntity
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.sortName
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

fun CollectionItem.mapToEntities(): Pair<CollectionItemEntity, CollectionItemGameEntity> {
    val item = CollectionItemEntity(
        gameId = objectid,
        gameName = if (originalname.isNullOrBlank()) name else originalname,
        collectionId = collid.toIntOrNull() ?: BggContract.INVALID_ID,
        collectionName = name,
        sortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else name,
        gameYearPublished = yearpublished?.toIntOrNull() ?: YEAR_UNKNOWN,
        collectionYearPublished = yearpublished?.toIntOrNull() ?: YEAR_UNKNOWN,
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
        lastModifiedDate = lastmodified.toMillis(dateTimeFormat),
        pricePaidCurrency = pp_currency.orEmpty(),
        pricePaid = pricepaid?.toDoubleOrNull() ?: 0.0,
        currentValueCurrency = cv_currency.orEmpty(),
        currentValue = currvalue?.toDoubleOrNull() ?: 0.0,
        quantity = quantity?.toIntOrNull() ?: 1,
        acquisitionDate = acquisitiondate.toMillis(dateFormat),
        acquiredFrom = acquiredfrom.orEmpty(),
        privateComment = privatecomment.orEmpty(),
        inventoryLocation = inventorylocation.orEmpty()
    )

    val game = CollectionItemGameEntity(
        gameId = objectid,
        gameName = if (originalname.isNullOrBlank()) name else originalname,
        sortName = if (originalname.isNullOrBlank()) name.sortName(sortindex) else name,
        yearPublished = yearpublished?.toIntOrNull() ?: YEAR_UNKNOWN,
        imageUrl = image.orEmpty(),
        thumbnailUrl = thumbnail.orEmpty(), // TODO hero image
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
