package com.boardgamegeek.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import com.boardgamegeek.R

class AutoCompleteAdapter(context: Context) : ArrayAdapter<String>(context, R.layout.autocomplete_item), Filterable {
    private var listComplete = listOf<String>()
    private var listFiltered = listOf<String>()

    override fun getCount() = listFiltered.size

    override fun getItem(index: Int) = listFiltered.getOrNull(index)

    fun addData(list: List<String>) {
        listComplete = list.filter { it.isNotBlank() }
        listFiltered = listComplete
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        @Suppress("RedundantNullableReturnType")
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString().orEmpty()

            val filteredList = if (filter.isEmpty()) listComplete else {
                listComplete.filter { it.contains(filter, ignoreCase = true) }
            }

            return FilterResults().apply {
                values = filteredList
                count = filteredList.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            listFiltered = results?.values as? List<String> ?: emptyList()
            notifyDataSetChanged()
        }
    }
}
