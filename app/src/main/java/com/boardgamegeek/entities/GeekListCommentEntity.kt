package com.boardgamegeek.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GeekListCommentEntity(
        val postDate: Long,
        val editDate: Long,
        val numberOfThumbs: Int,
        val username: String,
        val content: String
) : Parcelable
