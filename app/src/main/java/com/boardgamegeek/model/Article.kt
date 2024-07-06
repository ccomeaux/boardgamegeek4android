package com.boardgamegeek.model

import android.os.Parcelable
import com.boardgamegeek.provider.BggContract
import kotlinx.parcelize.Parcelize

@Parcelize
data class Article(
    val id: Int = BggContract.INVALID_ID,
    val username: String = "",
    val link: String = "",
    val postTicks: Long = 0,
    val editTicks: Long = 0,
    val body: String = "",
    val numberOfEdits: Int = 0
) : Parcelable

