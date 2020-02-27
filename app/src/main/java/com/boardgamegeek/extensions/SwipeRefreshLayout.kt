@file:JvmName("SwipeRefreshLayoutUtils")

package com.boardgamegeek.extensions

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.boardgamegeek.R

fun SwipeRefreshLayout.setBggColors() {
    setColorSchemeResources(R.color.orange, R.color.light_blue, R.color.dark_blue, R.color.light_blue)
}
