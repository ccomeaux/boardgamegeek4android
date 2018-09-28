package com.boardgamegeek.ui.widget

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import org.jetbrains.anko.support.v4.act
import java.util.*

class DatePickerDialogFragment : DialogFragment() {
    private var listener: OnDateSetListener? = null
    private var dateInMillis: Long = 0
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        calendar.timeInMillis = dateInMillis
        return DatePickerDialog(act,
                listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
    }

    fun setOnDateSetListener(listener: OnDateSetListener) {
        this.listener = listener
    }

    fun setCurrentDateInMillis(date: Long) {
        dateInMillis = date
    }
}
