package com.boardgamegeek.ui.model

import android.database.Cursor
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.util.CursorUtils
import com.boardgamegeek.util.PresentationUtils

data class Buddy(
        val id: Int = BggContract.INVALID_ID,
        val firstName: String,
        val lastName: String,
        val userName: String,
        private val avatarUrlRaw: String,
        val nickName: String,
        val updated: Long = 0
) {

    val avatarUrl: String = avatarUrlRaw
        get() = if (field == "N/A") "" else field

    val fullName = PresentationUtils.buildFullName(firstName, lastName)

    companion object {
        @JvmStatic
        val projection = arrayOf(
                Buddies._ID,
                Buddies.BUDDY_ID,
                Buddies.BUDDY_NAME,
                Buddies.BUDDY_FIRSTNAME,
                Buddies.BUDDY_LASTNAME,
                Buddies.AVATAR_URL,
                Buddies.PLAY_NICKNAME,
                Buddies.UPDATED
        )

        private const val BUDDY_ID = 1
        private const val BUDDY_NAME = 2
        private const val FIRST_NAME = 3
        private const val LAST_NAME = 4
        private const val AVATAR_URL = 5
        private const val NICKNAME = 6
        private const val UPDATED = 7

        @JvmStatic
        fun fromCursor(cursor: Cursor): Buddy {
            return Buddy(
                    cursor.getInt(BUDDY_ID),
                    cursor.getString(FIRST_NAME) ?: "",
                    cursor.getString(LAST_NAME) ?: "",
                    cursor.getString(BUDDY_NAME) ?: "",
                    CursorUtils.getString(cursor, AVATAR_URL) ?: "",
                    cursor.getString(NICKNAME) ?: "",
                    cursor.getLong(UPDATED)
            )
        }
    }
}
