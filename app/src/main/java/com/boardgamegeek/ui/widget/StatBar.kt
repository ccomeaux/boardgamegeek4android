package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.updateLayoutParams
import com.boardgamegeek.R
import java.text.NumberFormat

class StatBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_stat_bar, this)
        layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.stat_bar_height)
    }

    fun setBar(id: Int, progress: Double, max: Double) {
        findViewById<TextView>(R.id.textView).text = String.format(context.resources.getString(id), FORMAT.format(progress))
        findViewById<TextView>(R.id.valueView).updateLayoutParams<LinearLayout.LayoutParams> {
            width = 0
            height = ViewGroup.LayoutParams.MATCH_PARENT
            weight = (progress * 1000).toInt().toFloat()
        }
        findViewById<TextView>(R.id.noValueView).updateLayoutParams<LinearLayout.LayoutParams> {
            width = 0
            height = ViewGroup.LayoutParams.MATCH_PARENT
            weight = ((max - progress) * 1000).toInt().toFloat()
        }
    }

    fun colorize(@ColorInt color: Int) {
        findViewById<TextView>(R.id.valueView).setBackgroundColor(color)
    }

    companion object {
        private val FORMAT = NumberFormat.getInstance()
    }
}
