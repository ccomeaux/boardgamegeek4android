package com.boardgamegeek.ui.adapter

import android.content.Context
import android.database.Cursor
import androidx.cursoradapter.widget.SimpleCursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.util.PreferencesUtils

class CollectionViewAdapter(context: Context, cursor: Cursor) : SimpleCursorAdapter(context, R.layout.actionbar_spinner_item, cursor, arrayOf(CollectionViews._ID, CollectionViews.NAME), intArrayOf(0, android.R.id.text1), 0) {
    private val inflater = LayoutInflater.from(context)

    init {
        setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
    }

    override fun getCount() = super.getCount() + 1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (position) {
            0 -> createDefaultItem(convertView, parent, R.layout.actionbar_spinner_item)
            else -> try {
                super.getView(position - 1, convertView, parent)
            } catch (e: IllegalStateException) {
                createDefaultItem(convertView, parent, R.layout.actionbar_spinner_item)
            }
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        return when (position) {
            0 -> createDefaultItem(convertView, parent, R.layout.support_simple_spinner_dropdown_item)
            else -> super.getDropDownView(position - 1, convertView, parent)
        }
    }

    private fun createDefaultItem(convertView: View?, parent: ViewGroup, layout: Int): View {
        val view = convertView ?: inflater.inflate(layout, parent, false) as View
        (view as? TextView)?.setText(R.string.title_collection)
        return view
    }

    override fun getItem(position: Int): Any? = if (position == 0) null else super.getItem(position - 1)

    override fun getItemId(position: Int): Long {
        return when (position) {
            0 -> PreferencesUtils.VIEW_ID_COLLECTION
            else -> super.getItemId(position - 1)
        }
    }
}