package com.boardgamegeek.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.util.PreferencesUtils.VIEW_ID_COLLECTION

class CollectionViewAdapter(context: Context) :
        ArrayAdapter<CollectionViewEntity>(context,
                R.layout.actionbar_spinner_item,
                mutableListOf<CollectionViewEntity>()) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getItemId(position: Int): Long {
        return when {
            position < 0 -> VIEW_ID_COLLECTION
            position >= count -> VIEW_ID_COLLECTION
            else -> super.getItem(position)?.id ?: VIEW_ID_COLLECTION
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
