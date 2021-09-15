package com.boardgamegeek.ui.adapter

import android.content.Context
import android.graphics.Color
import android.provider.BaseColumns
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.getStringOrNull
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setColorViewValue
import com.boardgamegeek.provider.BggContract.GameColors
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class GameColorAdapter(context: Context, gameId: Int) : ArrayAdapter<GameColorAdapter.ColorRow>(context, R.layout.autocomplete_color), Filterable {
    private val gameColorsUri = Games.buildColorsUri(gameId)
    private val items = ArrayList<ColorRow>()

    class ColorRow(val title: String, val rgb: Int) {
        override fun toString() = title
    }

    override fun getCount() = items.size

    override fun getItem(index: Int) = items.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: parent.inflate(R.layout.autocomplete_color)
        val result = getItem(position) ?: return view

        view.findViewById<TextView>(R.id.color_name).text = result.title
        val imageView = view.findViewById<ImageView>(R.id.color_view)
        if (result.rgb != Color.TRANSPARENT) {
            imageView.setColorViewValue(result.rgb)
        } else {
            imageView.setImageDrawable(null)
        }
        return view
    }

    override fun getFilter() = ColorFilter()

    companion object {
        private val PROJECTION = arrayOf(BaseColumns._ID, GameColors.COLOR)
        private const val SELECTION = "${GameColors.COLOR} LIKE ?"
    }

    inner class ColorFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString() ?: ""

            val resultList = arrayListOf<ColorRow>()

            val selection = if (filter.isNotEmpty()) SELECTION else null
            val selectionArgs = if (filter.isNotEmpty()) arrayOf("$constraint%") else null
            val cursor = context.contentResolver.query(gameColorsUri, PROJECTION, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val colorName = it.getStringOrNull(1) ?: ""
                        resultList += ColorRow(colorName, colorName.asColorRgb())
                    } while (it.moveToNext())
                }
            }

            return FilterResults().apply {
                values = resultList
                count = resultList.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            items.clear()
            val values = if (results != null && results.count > 0) {
                @Suppress("UNCHECKED_CAST")
                results.values as? ArrayList<ColorRow>
            } else null
            if (values != null && values.size > 0) {
                items.addAll(values)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}
