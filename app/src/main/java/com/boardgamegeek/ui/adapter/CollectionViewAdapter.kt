package com.boardgamegeek.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.extensions.CollectionView

class CollectionViewAdapter(context: Context) :
        ArrayAdapter<CollectionViewEntity>(context,
                R.layout.actionbar_spinner_item,
                mutableListOf<CollectionViewEntity>()) {

    init {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }

    override fun getItemId(position: Int): Long {
        return when {
            position < 0 -> CollectionView.DEFAULT_DEFAULT_ID
            position >= count -> CollectionView.DEFAULT_DEFAULT_ID
            else -> super.getItem(position)?.id ?: CollectionView.DEFAULT_DEFAULT_ID
        }
    }

    fun findIndexOf(viewId: Long): Int {
        for (i in 0..count) {
            if (getItemId(i) == viewId) {
                return i
            }
        }
        return 0 // return the default view if the requested view can't be found
    }
}
