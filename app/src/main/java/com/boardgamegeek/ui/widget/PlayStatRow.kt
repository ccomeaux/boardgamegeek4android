package com.boardgamegeek.ui.widget

import android.content.Context
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.boardgamegeek.R
import com.boardgamegeek.databinding.WidgetPlayStatBinding
import com.boardgamegeek.extensions.setSelectableBackground
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PlayStatRow(context: Context) : TableRow(context) {
    private val binding = WidgetPlayStatBinding.inflate(LayoutInflater.from(context), this)

    fun setLabel(text: CharSequence) {
        binding.labelView.text = text
    }

    fun setLabel(@StringRes textId: Int) {
        binding.labelView.setText(textId)
    }

    fun setValue(value: Int) {
        setValue(value.toString())
    }

    fun setValue(value: Double) {
        setValue(DOUBLE_FORMAT.format(value))
    }

    fun setValueAsDate(date: String, context: Context) {
        if (date.isNotEmpty()) {
            try {
                setValue(DateUtils.formatDateTime(context,
                        FORMAT.parse(date).time,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH))
            } catch (e: ParseException) {
                setValue(date)
            }
        }
    }

    fun setValue(text: CharSequence) {
        binding.valueView.text = text
    }

    fun setInfoText(@StringRes textResId: Int) {
        val text = context.getString(textResId)
        binding.infoImageView.visibility = View.VISIBLE
        binding.labelContainer.setSelectableBackground()
        val spannableString = SpannableString(text)
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(binding.labelView.text).setMessage(spannableString)

        binding.labelContainer.setOnClickListener {
            val dialog = builder.show()
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            textView?.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    companion object {
        private val DOUBLE_FORMAT = DecimalFormat("0.00")
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
