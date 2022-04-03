package com.boardgamegeek.ui.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import com.boardgamegeek.R

class LocationAdapter(context: Context) : ArrayAdapter<String>(context, R.layout.autocomplete_item), Filterable {
    private var locationsList = listOf<String>()
    private var locationsListFiltered = listOf<String>()

    override fun getCount() = locationsListFiltered.size

    override fun getItem(index: Int) = locationsListFiltered.getOrNull(index)

    fun addData(list: List<String>) {
        locationsList = list as ArrayList<String>
        locationsListFiltered = locationsList
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        @Suppress("RedundantNullableReturnType")
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString().orEmpty()

            val locationsListFiltered = if (filter.isEmpty()) locationsList else {
                locationsList.filter { it.contains(filter, ignoreCase = true) }
            }

            return FilterResults().apply {
                values = locationsListFiltered
                count = locationsListFiltered.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            locationsListFiltered = results?.values as? List<String> ?: emptyList()
            notifyDataSetChanged()
        }
    }
}
