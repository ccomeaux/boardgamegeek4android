package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*

/**
 * content://com.boardgamegeek/games/110308/thumbnails
 */
class GamesIdThumbnailProvider : IndirectFileProvider() {
    override val path = "$PATH_GAMES/#/$PATH_THUMBNAILS"

    override val contentPath = PATH_THUMBNAILS

    override val columnName = Games.THUMBNAIL_URL

    override fun getFileUri(uri: Uri): Uri? {
        return Games.buildGameUri(Games.getGameId(uri))
    }
}