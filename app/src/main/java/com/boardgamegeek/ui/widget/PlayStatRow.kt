package com.boardgamegeek.ui.widget

import android.content.Context
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.setSelectableBackground
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PlayStatRow(context: Context) : TableRow(context) {
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_play_stat, this)
    }

    fun setLabel(text: CharSequence) {
        findViewById<TextView>(R.id.labelView).text = text
    }

    fun setLabel(@StringRes textId: Int) {
        findViewById<TextView>(R.id.labelView).setText(textId)
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
                val millis = FORMAT.parse(date)?.time ?: 0L
                setValue(millis.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH))
            } catch (e: ParseException) {
                setValue(date)
            }
        }
    }

    fun setValue(text: CharSequence) {
        findViewById<TextView>(R.id.valueView).text = text
    }

    fun setInfoText(@StringRes textResId: Int) {
        val text = context.getString(textResId)
        findViewById<ImageView>(R.id.infoImageView).isVisible = true
        val spannableString = SpannableString(text)
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(findViewById<TextView>(R.id.labelView).text).setMessage(spannableString)

        findViewById<LinearLayout>(R.id.labelContainer).apply {
            setSelectableBackground()
            setOnClickListener {
                val dialog = builder.show()
                val textView = dialog.findViewById<TextView>(android.R.id.message)
                textView?.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    companion object {
        private val DOUBLE_FORMAT = DecimalFormat("0.00")
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
