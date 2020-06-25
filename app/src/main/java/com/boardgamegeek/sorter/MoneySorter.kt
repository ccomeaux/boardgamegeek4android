package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import com.boardgamegeek.extensions.asMoney
import com.boardgamegeek.extensions.getDoubleOrZero
import com.boardgamegeek.extensions.getString
import kotlin.math.ceil

abstract class MoneySorter(context: Context) : CollectionSorter(context) {

    override val sortColumn: String
        get() = "$currencyColumnName DESC, $amountColumnName"

    override val isSortDescending = true

    protected abstract val amountColumnName: String

    protected abstract val currencyColumnName: String

    override val columns: Array<String>
        get() = arrayOf(currencyColumnName, amountColumnName)

    override fun getDisplayInfo(cursor: Cursor): String {
        return getInfoOrMissingInfo(cursor.getDoubleOrZero(amountColumnName).asMoney(cursor.getString(currencyColumnName)))
    }

    public override fun getHeaderText(cursor: Cursor): String {
        return getInfoOrMissingInfo(round(cursor.getDoubleOrZero(amountColumnName)).asMoney(cursor.getString(currencyColumnName)))
    }

    private fun getInfoOrMissingInfo(info: String): String {
        return if (info.isEmpty()) {
            MISSING_DATA
        } else info
    }

    private fun round(value: Double): Double {
        return ((ceil(value + 9) / 10).toInt() * 10).toDouble()
    }

    companion object {
        private const val MISSING_DATA = "-"
    }
}
