package com.boardgamegeek.db

import android.content.Context
import com.boardgamegeek.provider.BggContract.Avatars
import com.boardgamegeek.provider.BggContract.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageDao(private val context: Context) {
    suspend fun deleteAvatars() = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Avatars.CONTENT_URI, null, null)
    }

    suspend fun deleteThumbnails() = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Thumbnails.CONTENT_URI, null, null)
    }
}
