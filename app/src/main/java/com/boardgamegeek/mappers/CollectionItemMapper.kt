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

class CollectionItemMapper {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun map(from: CollectionItem): Pair<CollectionItemEntity, CollectionItemGameEntity> {
        val item = CollectionItemEntity(
                gameId = from.objectid,
                gameName = if (from.originalname.isNullOrBlank()) from.name else from.originalname,
                collectionId = from.collid.toIntOrNull() ?: BggContract.INVALID_ID,
                collectionName = from.name,
                sortName = if (from.originalname.isNullOrBlank()) from.name.sortName(from.sortindex) else from.name,
                gameYearPublished = from.yearpublished?.toIntOrNull() ?: YEAR_UNKNOWN,
                collectionYearPublished = from.yearpublished?.toIntOrNull() ?: YEAR_UNKNOWN,
                imageUrl = from.image ?: "",
                thumbnailUrl = from.thumbnail ?: "",
                rating = from.stats?.rating?.toDoubleOrNull() ?: 0.0,
                numberOfPlays = from.numplays,
                comment = from.comment ?: "",
                wantPartsList = from.wantpartslist ?: "",
                conditionText = from.conditiontext ?: "",
                hasPartsList = from.haspartslist ?: "",
                wishListComment = from.wishlistcomment ?: "",
                own = from.own?.equals("1") ?: false,
                previouslyOwned = from.prevowned?.equals("1") ?: false,
                forTrade = from.fortrade?.equals("1") ?: false,
                wantInTrade = from.want?.equals("1") ?: false,
                wantToPlay = from.wanttoplay?.equals("1") ?: false,
                wantToBuy = from.wanttobuy?.equals("1") ?: false,
                wishList = from.wishlist?.equals("1") ?: false,
                wishListPriority = from.wishlistpriority,
                preOrdered = from.preordered?.equals("1") ?: false,
                lastModifiedDate = from.lastmodified.toMillis(dateTimeFormat),
                pricePaidCurrency = from.pp_currency ?: "",
                pricePaid = from.pricepaid?.toDoubleOrNull() ?: 0.0,
                currentValueCurrency = from.cv_currency ?: "",
                currentValue = from.currvalue?.toDoubleOrNull() ?: 0.0,
                quantity = from.quantity?.toIntOrNull() ?: 1,
                acquisitionDate = from.acquisitiondate.toMillis(dateFormat),
                acquiredFrom = from.acquiredfrom ?: "",
                privateComment = from.privatecomment ?: "",
                inventoryLocation = from.inventorylocation ?: ""
        )

        val game = CollectionItemGameEntity(
                gameId = from.objectid,
                gameName = if (from.originalname.isNullOrBlank()) from.name else from.originalname,
                sortName = if (from.originalname.isNullOrBlank()) from.name.sortName(from.sortindex) else from.name,
                yearPublished = from.yearpublished?.toIntOrNull() ?: YEAR_UNKNOWN,
                imageUrl = from.image ?: "",
                thumbnailUrl = from.thumbnail ?: "",
                minNumberOfPlayers = from.stats?.minplayers ?: 0,
                maxNumberOfPlayers = from.stats?.maxplayers ?: 0,
                minPlayingTime = from.stats?.minplaytime ?: 0,
                maxPlayingTime = from.stats?.maxplaytime ?: 0,
                playingTime = from.stats?.playingtime ?: 0,
                numberOwned = from.stats?.numowned?.toIntOrNull() ?: 0,
                numberOfUsersRated = from.stats?.usersrated?.toIntOrNull() ?: 0,
                rating = from.stats?.rating?.toDoubleOrNull() ?: 0.0,
                average = from.stats?.average?.toDoubleOrNull() ?: 0.0,
                bayesAverage = from.stats?.bayesaverage?.toDoubleOrNull() ?: 0.0,
                standardDeviation = from.stats?.stddev?.toDoubleOrNull() ?: 0.0,
                median = from.stats?.median?.toDoubleOrNull() ?: 0.0,
                numberOfPlays = from.numplays
        )
        return item to game
    }
}
