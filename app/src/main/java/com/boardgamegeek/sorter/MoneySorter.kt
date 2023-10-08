package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.entities.CollectionItem
import com.boardgamegeek.extensions.asMoney
import java.text.DecimalFormat
import kotlin.math.ceil

abstract class MoneySorter(context: Context) : CollectionSorter(context) {
    protected abstract fun amount(item: CollectionItem): Double
    protected abstract fun currency(item: CollectionItem): String

    override fun sortAscending(items: Iterable<CollectionItem>): List<CollectionItem> {
        return items.sortedWith(compareBy<CollectionItem> { currency(it) }.thenBy { amount(it) })
    }

    override fun sortDescending(items: Iterable<CollectionItem>): List<CollectionItem> {
        return items.sortedWith(compareByDescending<CollectionItem> { currency(it) }.thenByDescending { amount(it) })
    }

    private fun round(value: Double) = (ceil(value / 10).toInt() * 10).toDouble()

    override fun getHeaderText(item: CollectionItem) = round(amount(item)).asMoney(currency(item), DecimalFormat("0")).ifEmpty { MISSING_DATA }

    override fun getDisplayInfo(item: CollectionItem) = amount(item).asMoney(currency(item)).ifEmpty { MISSING_DATA }

    companion object {
        private const val MISSING_DATA = "-"
    }
}
