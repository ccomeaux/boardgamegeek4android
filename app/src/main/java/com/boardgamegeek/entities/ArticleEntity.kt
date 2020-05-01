package com.boardgamegeek.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ArticleEntity(
        val id: Int,
        val username: String,
        val link: String,
        val postTicks: Long,
        val editTicks: Long,
        val body: String,
        val numberOfEdits: Int
) : Parcelable

