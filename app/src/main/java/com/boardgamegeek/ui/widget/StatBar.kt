package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.boardgamegeek.R
import com.boardgamegeek.databinding.WidgetStatBarBinding
import java.text.NumberFormat

class StatBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {
    private val binding = WidgetStatBarBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.stat_bar_height)
    }

    fun setBar(id: Int, progress: Double, max: Double) {
        binding.textView.text = String.format(context.resources.getString(id), FORMAT.format(progress))
        binding.valueView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (progress * 1000).toInt().toFloat())
        binding.noValueView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, ((max - progress) * 1000).toInt().toFloat())
    }

    fun colorize(@ColorInt color: Int) {
        binding.valueView.setBackgroundColor(color)
    }

    companion object {
        private val FORMAT = NumberFormat.getInstance()
    }
}
