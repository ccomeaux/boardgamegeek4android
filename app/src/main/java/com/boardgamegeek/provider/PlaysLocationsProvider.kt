package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_LOCATIONS
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYS
import com.boardgamegeek.provider.BggContract.PlayLocations
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables

class PlaysLocationsProvider : BaseProvider() {
    override val path = "$PATH_PLAYS/$PATH_LOCATIONS"

    override val defaultSortOrder = PlayLocations.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
            .table(Tables.PLAYS)
            .groupBy(Plays.Columns.LOCATION)
            .mapAsSum(Plays.Columns.SUM_QUANTITY, Plays.Columns.QUANTITY)
    }
}
