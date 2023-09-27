package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Users
import com.boardgamegeek.provider.BggContract.Companion.PATH_USERS
import com.boardgamegeek.provider.BggDatabase.Tables

class UsersUsernameProvider : BaseProvider() {
    override fun getType(uri: Uri) = Users.CONTENT_ITEM_TYPE

    override val path: String = "$PATH_USERS/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val username = Users.getUsername(uri)
        return SelectionBuilder()
            .table(Tables.USERS)
            .whereEquals(Users.Columns.USERNAME, username)
    }
}
