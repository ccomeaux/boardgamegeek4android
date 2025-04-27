package com.boardgamegeek.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.extensions.CollectionViewPrefs

class CollectionViewAdapter(context: Context) :
    ArrayAdapter<CollectionView>(context, R.layout.actionbar_spinner_item, mutableListOf<CollectionView>()) {
    init {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }

    override fun getItemId(position: Int): Long {
        return when {
            position < 0 -> CollectionViewPrefs.DEFAULT_DEFAULT_ID
            position >= count -> CollectionViewPrefs.DEFAULT_DEFAULT_ID
            else -> super.getItem(position)?.id ?: CollectionViewPrefs.DEFAULT_DEFAULT_ID
        }.toLong()
    }

    fun findIndexOf(viewId: Int): Int {
        for (i in 0..count) {
            if (getItemId(i) == viewId.toLong()) {
                return i
            }
        }
        return 0 // return the default view if the requested view can't be found
    }
}
