package com.boardgamegeek.ui.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setColorViewValue
import java.util.*

internal class ColorGridAdapter(private val choices: List<Pair<String, Int>>, private val disabledColors: ArrayList<String>?) : BaseAdapter() {
    var selectedColor: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount() = choices.size

    override fun getItem(position: Int) = choices.getOrNull(position)

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
        val view = convertView ?: container.inflate(R.layout.widget_color)
        val color = getItem(position)

        val isSelected = color?.first == selectedColor
        val isDisabled = !isSelected && disabledColors?.contains(color?.first) == true
        view.findViewById<TextView>(R.id.colorDescriptionView)?.text = color?.first
        view.findViewById<ImageView>(R.id.colorView)?.setColorViewValue(color?.second ?: Color.TRANSPARENT, isDisabled)
        view.findViewById<ImageView>(R.id.colorSelectedView).isVisible = isSelected

        return view
    }
}
