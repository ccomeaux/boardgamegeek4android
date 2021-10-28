package com.boardgamegeek.ui.adapter

import android.content.Context
import android.widget.*
import com.boardgamegeek.R
import kotlin.collections.ArrayList

class LocationAdapter(context: Context) : ArrayAdapter<String>(context, R.layout.autocomplete_item), Filterable {
    private var locationsList: ArrayList<String> = ArrayList()
    private var locationsListFiltered: ArrayList<String> = ArrayList()

    override fun getCount() = locationsListFiltered.size

    override fun getItem(index: Int) = locationsListFiltered.getOrNull(index)

    fun addData(list: List<String>) {
        locationsList = list as ArrayList<String>
        locationsListFiltered = locationsList
        notifyDataSetChanged()
    }

    override fun getFilter() = LocationFilter()

    inner class LocationFilter : Filter() {
        @Suppress("RedundantNullableReturnType")
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString().orEmpty()

            val locationsListFiltered = if (filter.isEmpty()) locationsList else {
                locationsList.filter { it.contains(filter, ignoreCase = true) } as ArrayList<String>
            }

            return FilterResults().apply {
                values = locationsListFiltered
                count = locationsListFiltered.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            locationsListFiltered = results?.values as? ArrayList<String> ?: ArrayList()
            notifyDataSetChanged()
        }
    }
}
