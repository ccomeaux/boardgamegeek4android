package com.boardgamegeek.ui.adapter

import android.database.Cursor
import android.graphics.Color
import android.provider.BaseColumns
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setColorViewValue
import com.boardgamegeek.provider.BggContract.GameColors
import kotlinx.android.synthetic.main.row_color.view.*
import java.util.*

class GameColorRecyclerViewAdapter(@field:LayoutRes @param:LayoutRes private val layoutId: Int, private val callback: Callback?) : RecyclerView.Adapter<GameColorRecyclerViewAdapter.ViewHolder>() {
    private var cursor: Cursor? = null

    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    interface Callback {
        fun onItemClick(position: Int)
        fun onItemLongPress(position: Int): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(layoutId))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (cursor?.moveToPosition(position) == true) {
            holder.bind(position)
        }
    }

    override fun getItemCount(): Int {
        return cursor?.count ?: 0
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
        return if (cursor?.moveToPosition(position) == true) {
            cursor?.getString(1) ?: ""
        } else {
            ""
        }
    }

    fun changeCursor(cursor: Cursor?) {
        val old = swapCursor(cursor)
        old?.close()
    }

    private fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor == cursor) {
            return null
        }
        val oldCursor = cursor
        cursor = newCursor
        if (newCursor != null) {
            notifyDataSetChanged()
        } else {
            notifyItemRangeRemoved(0, oldCursor?.count ?: 0)
        }
        return oldCursor
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

    companion object {
        val PROJECTION = arrayOf(BaseColumns._ID, GameColors.COLOR)
    }
}