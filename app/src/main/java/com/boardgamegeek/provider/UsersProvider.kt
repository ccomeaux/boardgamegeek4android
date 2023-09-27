package com.boardgamegeek.provider

import android.content.ContentValues
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Users
import com.boardgamegeek.provider.BggContract.Companion.PATH_USERS
import com.boardgamegeek.provider.BggDatabase.Tables

class UsersProvider : BasicProvider() {
    override fun getType(uri: Uri) = Users.CONTENT_TYPE

    override val path: String = PATH_USERS

    override val table = Tables.USERS

    override val defaultSortOrder = Users.DEFAULT_SORT

    override fun insertedUri(values: ContentValues?, rowId: Long): Uri? {
        val username = values?.getAsString(Users.Columns.USERNAME)
        return if (username.isNullOrBlank()) null else Users.buildUserUri(username)
    }
}
