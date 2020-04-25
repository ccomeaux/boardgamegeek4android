package com.boardgamegeek.entities

import android.content.Context
import com.boardgamegeek.R

data class GeekListItemEntity(
        val id: Long,
        val objectId: Int,
        val objectName: String,
        private val objectType: String,
        private val subtype: String,
        val imageId: Int,
        val username: String,
        val body: String,
        val numberOfThumbs: Int,
        val postDateTime: Long,
        val editDateTime: Long,
        val comments: List<GeekListCommentEntity>
) {
    val isBoardGame: Boolean
        get() = "thing" == objectType // TODO this will fail - objectType is a description

    val objectUrl: String
        get() = "https://www.boardgamegeek.com/${if (subtype.isBlank()) objectType else subtype}/$objectId"

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