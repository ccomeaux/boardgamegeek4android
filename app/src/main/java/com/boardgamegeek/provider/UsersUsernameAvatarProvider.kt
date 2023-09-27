package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_AVATARS
import com.boardgamegeek.provider.BggContract.Companion.PATH_USERS

class UsersUsernameAvatarProvider : IndirectFileProvider() {
    override val path = "$PATH_USERS/*/$PATH_AVATARS"

    override val contentPath = PATH_AVATARS

    override val columnName = Users.Columns.AVATAR_URL

    override fun getFileUri(uri: Uri) = Users.buildUserUri(Users.getUsername(uri))
}
