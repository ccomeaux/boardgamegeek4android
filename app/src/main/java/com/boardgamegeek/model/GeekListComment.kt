package com.boardgamegeek.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GeekListComment(
    val postDate: Long,
    val editDate: Long,
    val numberOfThumbs: Int,
    val username: String,
    val content: String
) : Parcelable
