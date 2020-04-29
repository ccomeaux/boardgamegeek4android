package com.boardgamegeek.ui.adapter

import android.graphics.Color
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setColorViewValue
import kotlinx.android.synthetic.main.row_color.view.*
import java.util.*
import kotlin.properties.Delegates

class GameColorRecyclerViewAdapter(@field:LayoutRes @param:LayoutRes private val layoutId: Int, private val callback: Callback?) :
        RecyclerView.Adapter<GameColorRecyclerViewAdapter.ViewHolder>(), AutoUpdatableAdapter {
    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    var colors: List<String> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n -> o == n }
    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onItemLongPress(position: Int): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(layoutId))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return colors.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(position: Int) {
            val colorName = getColorName(position)
            itemView.colorNameView.text = colorName
            val color = colorName.asColorRgb()
            if (color != Color.TRANSPARENT) {
                itemView.colorView.setColorViewValue(color)
            } else {
                itemView.colorView.setImageDrawable(null)
            }
            itemView.isActivated = selectedItems[position, false]
            itemView.setOnLongClickListener { callback?.onItemLongPress(position) ?: false }
            itemView.setOnClickListener { callback?.onItemClick(position) }
        }
    }

    fun getColorName(position: Int): String {
        return colors.getOrNull(position) ?: ""
    }

    fun toggleSelection(position: Int) {
        if (selectedItems[position, false]) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    val selectedItemCount = selectedItems.size()

    fun getSelectedItems(): List<Int> {
        val items: MutableList<Int> = ArrayList(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }
}