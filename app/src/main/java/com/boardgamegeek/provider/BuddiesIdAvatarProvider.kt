package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_AVATARS
import com.boardgamegeek.provider.BggContract.Companion.PATH_BUDDIES

class BuddiesIdAvatarProvider : IndirectFileProvider() {
    override val path = "$PATH_BUDDIES/*/$PATH_AVATARS"

    override val contentPath = PATH_AVATARS

    override val columnName = Buddies.Columns.AVATAR_URL

    override fun getFileUri(uri: Uri) = Buddies.buildBuddyUri(Buddies.getBuddyName(uri))
}
