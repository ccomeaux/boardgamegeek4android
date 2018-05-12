package com.boardgamegeek.ui.adapter

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.setColorViewValue
import java.util.*

internal class ColorGridAdapter(private val context: Context, private val disabledColors: ArrayList<String>?, private val choices: List<Pair<String, Int>>) : BaseAdapter() {
    private var selectedColor: String? = null

    override fun getCount() = choices.size

    override fun getItem(position: Int) = choices.getOrNull(position)

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.widget_color, container, false) as View
        val color = getItem(position)

        view.findViewById<TextView>(R.id.color_description)?.text = color?.first
        view.findViewById<ImageView>(R.id.color_view)?.setColorViewValue(color?.second ?: Color.TRANSPARENT)

        val frameColor = when {
            color == null -> Color.TRANSPARENT
            color.first == selectedColor -> ContextCompat.getColor(context, R.color.light_blue)
            disabledColors != null && disabledColors.contains(color.first) -> ContextCompat.getColor(context, R.color.disabled)
            else -> Color.TRANSPARENT
        }
        view.findViewById<View>(R.id.color_frame)?.setBackgroundColor(frameColor)

        return view
    }

    fun setSelectedColor(selectedColor: String?) {
        if (this.selectedColor != selectedColor) {
            this.selectedColor = selectedColor
            notifyDataSetChanged()
        }
    }
}
