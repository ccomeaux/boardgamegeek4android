package com.boardgamegeek.ui.adapter

import android.content.Context
import android.graphics.Color
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setColorViewValue
import java.util.*

internal class ColorGridAdapter(private val context: Context, private val disabledColors: ArrayList<String>?, private val choices: List<Pair<String, Int>>) : BaseAdapter() {
    var selectedColor: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount() = choices.size

    override fun getItem(position: Int) = choices.getOrNull(position)

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.widget_color, container, false) as View
        val color = getItem(position)

        view.findViewById<TextView>(R.id.color_description)?.text = color?.first
        view.findViewById<ImageView>(R.id.color_view)?.setColorViewValue(
                color?.second ?: Color.TRANSPARENT,
                disabledColors?.contains(color?.first) == true)
        view.findViewById<ImageView>(R.id.color_picker_selected).visibility = if (color?.first == selectedColor) View.VISIBLE else View.GONE

        return view
    }
}
