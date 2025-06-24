package com.boardgamegeek.model

import android.os.Parcelable
import com.boardgamegeek.provider.BggContract
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class GeekListItem(
    val id: Long = BggContract.INVALID_ID.toLong(),
    val objectId: Int = BggContract.INVALID_ID,
    val objectName: String = "",
    val objectType: ObjectType = ObjectType.Unknown,
    val imageId: Int = 0,
    val username: String = "",
    val body: String = "",
    val numberOfThumbs: Int = 0,
    val postDateTime: Long = 0L,
    val editDateTime: Long = 0L,
    val comments: List<GeekListComment> = emptyList(),
    val thumbnailUrls: List<String>? = null,
    val heroImageUrls: List<String>? = null,
) : Parcelable {
    enum class ObjectType(val type: String, val subtype: String) {
        BoardGame(TYPE_THING, "boardgame"),
        BoardGameAccessory(TYPE_THING, "boardgameaccessory"),
        Thing(TYPE_THING, ""),
        Publisher(TYPE_COMPANY, "boardgamepublisher"),
        Company(TYPE_COMPANY, ""),
        Designer(TYPE_PERSON, "boardgamedesigner"),
        Person(TYPE_PERSON, ""),
        BoardGameFamily(TYPE_FAMILY, "boardgamefamily"),
        Family(TYPE_FAMILY, ""),
        File(TYPE_FILE, ""),
        GeekList(TYPE_GEEKLIST, ""),
        Unknown("", ""),
    }

    @IgnoredOnParcel
    val isBoardGame: Boolean = objectType in listOf(ObjectType.BoardGame, ObjectType.BoardGameAccessory)

    @IgnoredOnParcel
    val objectUrl: String = when {
        objectType.subtype.isNotBlank() -> "https://www.boardgamegeek.com/${objectType.subtype}/$objectId"
        objectType.type.isNotBlank() -> "https://www.boardgamegeek.com/${objectType.type}/$objectId"
        else -> ""
    }

    companion object {
        private const val TYPE_THING = "thing"
        private const val TYPE_COMPANY = "company"
        private const val TYPE_PERSON = "person"
        private const val TYPE_FAMILY = "family"
        private const val TYPE_FILE = "filepage"
        private const val TYPE_GEEKLIST = "geeklist"
    }
}
