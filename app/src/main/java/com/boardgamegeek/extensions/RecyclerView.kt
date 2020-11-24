package com.boardgamegeek.extensions

import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration

fun RecyclerView.addHeader(sectionCallback: RecyclerSectionItemDecoration.SectionCallback) {
    val sectionItemDecoration = RecyclerSectionItemDecoration(
            resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
            sectionCallback
    )
    while (itemDecorationCount > 0) {
        removeItemDecorationAt(0)
    }
    addItemDecoration(sectionItemDecoration)
}
