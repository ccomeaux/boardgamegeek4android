package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import com.boardgamegeek.extensions.getDouble
import com.boardgamegeek.extensions.getString
import com.boardgamegeek.util.PresentationUtils

abstract class MoneySorter(context: Context) : CollectionSorter(context) {

    override val sortColumn: String
        get() = "$currencyColumnName DESC, $amountColumnName"

    override val isSortDescending: Boolean
        get() = true

    protected abstract val amountColumnName: String

    protected abstract val currencyColumnName: String

    override val columns: Array<String>
        get() = arrayOf(currencyColumnName, amountColumnName)

    override fun getDisplayInfo(cursor: Cursor): String {
        val amount = cursor.getDouble(amountColumnName)
        val currency = cursor.getString(currencyColumnName)
        val info = PresentationUtils.describeMoney(currency, amount)
        return getInfoOrMissingInfo(info)
    }

    public override fun getHeaderText(cursor: Cursor): String {
        val amount = round(cursor.getDouble(amountColumnName))
        val currency = cursor.getString(currencyColumnName)
        val info = PresentationUtils.describeMoneyWithoutDecimals(currency, amount)
        return getInfoOrMissingInfo(info)
    }

    private fun getInfoOrMissingInfo(info: String): String {
        return if (info.isEmpty()) {
            MISSING_DATA
        } else info
    }

    private fun round(value: Double): Double {
        return ((Math.ceil(value + 9) / 10).toInt() * 10).toDouble()
    }

    companion object {
        val MISSING_DATA = "-"
    }
}
