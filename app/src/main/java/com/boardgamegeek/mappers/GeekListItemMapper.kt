package com.boardgamegeek.mappers

import com.boardgamegeek.io.model.GeekListResponse
import com.boardgamegeek.ui.model.GeekList
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.StringUtils

class GeekListItemMapper {
    fun map(from: GeekListResponse): GeekList {
        return GeekList(
                from.id,
                from.title.orEmpty().trim(),
                from.username,
                from.description,
                StringUtils.parseInt(from.numitems),
                StringUtils.parseInt(from.thumbs),
                DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, from.postdate, GeekListResponse.FORMAT),
                DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, from.editdate, GeekListResponse.FORMAT),
                from.items
        )
    }
}
