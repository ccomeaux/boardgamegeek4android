package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import com.boardgamegeek.provider.BggContract.Games

/**
 * content://com.boardgamegeek/games/110308/thumbnails
 */
class GamesIdThumbnailProvider : IndirectFileProvider() {
    override val path = "$PATH_GAMES/#/$PATH_THUMBNAILS"

    override val contentPath = PATH_THUMBNAILS

    override val columnName = Games.Columns.THUMBNAIL_URL

    override fun getFileUri(uri: Uri) = Games.buildGameUri(Games.getGameId(uri))
}
