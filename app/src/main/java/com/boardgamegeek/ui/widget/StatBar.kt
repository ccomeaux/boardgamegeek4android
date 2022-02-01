package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.view.updateLayoutParams
import com.boardgamegeek.R
import kotlinx.android.synthetic.main.widget_stat_bar.view.*
import java.text.NumberFormat

class StatBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    init {
        layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.stat_bar_height)
        LayoutInflater.from(context).inflate(R.layout.widget_stat_bar, this, true)
    }

    fun setBar(id: Int, progress: Double, max: Double) {
        textView.text = String.format(context.resources.getString(id), FORMAT.format(progress))
        valueView.updateLayoutParams<LinearLayout.LayoutParams> {
            width = 0
            height = ViewGroup.LayoutParams.MATCH_PARENT
            weight = (progress * 1000).toInt().toFloat()
        }
        noValueView.updateLayoutParams<LinearLayout.LayoutParams> {
            width = 0
            height = ViewGroup.LayoutParams.MATCH_PARENT
            weight = ((max - progress) * 1000).toInt().toFloat()
        }
    }

    fun colorize(@ColorInt color: Int) {
        valueView?.setBackgroundColor(color)
    }

    companion object {
        private val FORMAT = NumberFormat.getInstance()
    }
}
