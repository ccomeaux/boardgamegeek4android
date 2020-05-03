package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*

class BuddiesIdAvatarProvider : IndirectFileProvider() {
    override val path = "$PATH_BUDDIES/*/$PATH_AVATARS"

    override val contentPath = PATH_AVATARS

    override val columnName = Buddies.AVATAR_URL

    override fun getFileUri(uri: Uri): Uri? {
        return Buddies.buildBuddyUri(Buddies.getBuddyName(uri))
    }
}