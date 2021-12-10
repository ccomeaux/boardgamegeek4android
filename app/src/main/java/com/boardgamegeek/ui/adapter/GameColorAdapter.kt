package com.boardgamegeek.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setOrClearColorViewValue

class GameColorAdapter(context: Context) : ArrayAdapter<GameColorAdapter.ColorRow>(context, R.layout.autocomplete_color), Filterable {
    private var colorList = listOf<ColorRow>()
    private var colorListFiltered = listOf<ColorRow>()

    data class ColorRow(val title: String, val rgb: Int = title.asColorRgb()) {
        override fun toString() = title
    }

    override fun getCount() = colorListFiltered.size

    override fun getItem(index: Int) = colorListFiltered.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: parent.inflate(R.layout.autocomplete_color)
        val result = getItem(position) ?: return view
        view.findViewById<TextView>(R.id.color_name).text = result.title
        view.findViewById<ImageView>(R.id.color_view).setOrClearColorViewValue(result.rgb)
        return view
    }

    fun addData(list: List<String>) {
        colorList = list.map { ColorRow(it) }
        colorListFiltered = colorList
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        @Suppress("RedundantNullableReturnType")
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString().orEmpty()

            val colorListFiltered = if (filter.isEmpty()) colorList else {
                colorList.filter { it.title.contains(filter, ignoreCase = true) }
            }

            return FilterResults().apply {
                values = colorListFiltered
                count = colorListFiltered.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            colorListFiltered = results?.values as? List<ColorRow> ?: emptyList()
            notifyDataSetChanged()
        }
    }
}
