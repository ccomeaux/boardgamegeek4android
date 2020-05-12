package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PlaysLocationsProvider : BaseProvider() {
    override val path = "$PATH_PLAYS/$PATH_LOCATIONS"

    override val defaultSortOrder = PlayLocations.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
                .table(Tables.PLAYS)
                .groupBy(Plays.LOCATION)
                .mapAsSum(Plays.SUM_QUANTITY, Plays.QUANTITY)
    }
}
