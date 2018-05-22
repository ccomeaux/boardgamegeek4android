package com.boardgamegeek.mappers

import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Constants
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.sortName
import com.boardgamegeek.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

private val FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

class CollectionItemMapper {
    fun map(from: CollectionItem): CollectionItemEntity {
        val item = CollectionItemEntity()
        item.apply {
            gameId = from.objectid
            gameName = if (from.originalname.isNullOrBlank()) from.name else from.originalname
            collectionId = from.collid.toIntOrNull() ?: BggContract.INVALID_ID
            collectionName = from.name
            sortName = if (from.originalname.isNullOrBlank()) from.name.sortName(from.sortindex) else from.name

            // non-brief
            yearPublished = from.yearpublished?.toIntOrNull() ?: Constants.YEAR_UNKNOWN
            imageUrl = from.image ?: ""
            thumbnailUrl = from.thumbnail ?: ""
            numberOfPlays = from.numplays
            comment = from.comment ?: ""
            wantPartsList = from.wantpartslist ?: ""
            conditionText = from.conditiontext ?: ""
            hasPartsList = from.haspartslist ?: ""
            wishListComment = from.wishlistcomment ?: ""

            // status
            own = from.own?.equals("1") ?: false
            previouslyOwned = from.prevowned?.equals("1") ?: false
            forTrade = from.fortrade?.equals("1") ?: false
            want = from.want?.equals("1") ?: false
            wantToPlay = from.wanttoplay?.equals("1") ?: false
            wantToBuy = from.wanttobuy?.equals("1") ?: false
            wishList = from.wishlist?.equals("1") ?: false
            wishListPriority = from.wishlistpriority
            preOrdered = from.preordered?.equals("1") ?: false
            lastModifiedDate = DateTimeUtils.tryParseDate(lastModifiedDate, from.lastmodified, FORMAT) // TODO improve

            // stats
            if (from.stats != null) {
                hasStatistics = true
                minNumberOfPlayers = from.stats.minplayers
                maxNumberOfPlayers = from.stats.maxplayers
                minPlayingTime = from.stats.minplaytime
                maxPlayingTime = from.stats.maxplaytime
                playingTime = from.stats.playingtime
                numberOwned = from.stats.numowned.toIntOrNull() ?: 0
                numberOfUsersRated = from.stats.usersrated.toIntOrNull() ?: 0
                rating = from.stats.rating.toDoubleOrNull() ?: 0.0
                average = from.stats.average.toDoubleOrNull() ?: 0.0
                bayesAverage = from.stats.bayesaverage.toDoubleOrNull() ?: 0.0
                standardDeviation = from.stats.stddev.toDoubleOrNull() ?: 0.0
                median = from.stats.median.toDoubleOrNull() ?: 0.0
                // TODO: ranks?
            }

            // private info
            pricePaidCurrency = from.pp_currency ?: ""
            pricePaid = from.pricepaid?.toDoubleOrNull() ?: 0.0
            currentValueCurrency = from.cv_currency ?: ""
            currentValue = from.currvalue?.toDoubleOrNull() ?: 0.0
            quantity = from.quantity?.toIntOrNull() ?: 1
            acquisitionDate = from.acquisitiondate ?: ""
            acquiredFrom = from.acquiredfrom ?: ""
            privateComment = from.privatecomment ?: ""
        }
        return item
    }
}