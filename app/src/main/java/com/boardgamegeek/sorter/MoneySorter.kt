package com.boardgamegeek.sorter

import android.content.Context
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asMoney
import java.text.DecimalFormat
import kotlin.math.ceil

abstract class MoneySorter(context: Context) : CollectionSorter(context) {

    override val sortColumn: String
        get() = "$currencyColumnName DESC, $amountColumnName"

    override val isSortDescending = true

    protected abstract val amountColumnName: String
    protected abstract val currencyColumnName: String
    protected abstract fun amount(item: CollectionItemEntity): Double
    protected abstract fun currency(item: CollectionItemEntity): String

    private fun round(value: Double): Double {
        return (ceil(value / 10).toInt() * 10).toDouble()
    }

    override fun getHeaderText(item: CollectionItemEntity): String {
        return round(amount(item)).asMoney(currency(item), DecimalFormat("0")).ifEmpty { MISSING_DATA }
    }

    override fun getDisplayInfo(item: CollectionItemEntity): String {
        return amount(item).asMoney(currency(item)).ifEmpty { MISSING_DATA }
    }

    companion object {
        private const val MISSING_DATA = "-"
    }
}
