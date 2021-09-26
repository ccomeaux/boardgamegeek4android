package com.boardgamegeek.entities

import android.os.Parcelable
import com.boardgamegeek.provider.BggContract
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ArticleEntity(
        val id: Int = BggContract.INVALID_ID,
        val username: String = "",
        val link: String = "",
        val postTicks: Long = 0,
        val editTicks: Long = 0,
        val body: String = "",
        val numberOfEdits: Int = 0
) : Parcelable

