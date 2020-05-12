package com.boardgamegeek.entities

import android.content.Context
import android.os.Parcelable
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GeekListItemEntity(
        val id: Long = BggContract.INVALID_ID.toLong(),
        val objectId: Int = BggContract.INVALID_ID,
        val objectName: String = "",
        private val objectType: String = "",
        private val subtype: String = "",
        val imageId: Int = 0,
        val username: String = "",
        val body: String = "",
        val numberOfThumbs: Int = 0,
        val postDateTime: Long = 0L,
        val editDateTime: Long = 0L,
        val comments: List<GeekListCommentEntity> = emptyList()
) : Parcelable {
    @IgnoredOnParcel
    val isBoardGame: Boolean = "thing" == objectType

    @IgnoredOnParcel
    val objectUrl: String = when {
        subtype.isNotBlank() -> "https://www.boardgamegeek.com/$subtype/$objectId"
        objectType.isNotBlank() -> "https://www.boardgamegeek.com/$objectType/$objectId"
        else -> ""
    }

    fun objectTypeDescription(context: Context): String {
        val objectTypeResId = getObjectTypeResId()
        return if (objectTypeResId == INVALID_OBJECT_TYPE_RES_ID) "" else context.getString(objectTypeResId)
    }

    private fun getObjectTypeResId(): Int {
        return when (objectType) {
            "thing" -> {
                when (subtype) {
                    "boardgame" -> R.string.title_board_game
                    "boardgameaccessory" -> R.string.title_board_game_accessory
                    else -> R.string.title_thing
                }
            }
            "company" -> {
                when (subtype) {
                    "boardgamepublisher" -> R.string.title_board_game_publisher
                    else -> R.string.title_company
                }
            }
            "person" -> {
                when (subtype) {
                    "boardgamedesigner" -> R.string.title_board_game_designer
                    else -> R.string.title_person
                }
            }
            "family" -> {
                when (subtype) {
                    "boardgamefamily" -> R.string.title_board_game_family
                    else -> R.string.title_family
                }
            }
            "filepage" -> R.string.title_file
            "geeklist" -> R.string.title_geeklist
            else -> INVALID_OBJECT_TYPE_RES_ID
        }
    }

    companion object {
        const val INVALID_OBJECT_TYPE_RES_ID = 0
    }
}