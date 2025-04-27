package com.boardgamegeek.ui.adapter

import android.annotation.SuppressLint
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import androidx.core.util.size
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowColorBinding
import com.boardgamegeek.extensions.*
import kotlin.properties.Delegates

class GameColorRecyclerViewAdapter(private val callback: Callback?) :
    RecyclerView.Adapter<GameColorRecyclerViewAdapter.ViewHolder>(), AutoUpdatableAdapter {
    private val selectedItems = SparseBooleanArray()

    var colors: List<String> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n -> o == n }
    }

    interface Callback {
        fun onItemClick(position: Int)
        fun onItemLongPress(position: Int): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.row_color))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return colors.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = RowColorBinding.bind(itemView)

        fun bind(position: Int) {
            getColorName(position)?.let { colorName ->
                binding.colorNameView.text = colorName
                binding.colorView.setOrClearColorViewValue(colorName.asColorRgb())
                itemView.isActivated = selectedItems[position, false]
                itemView.setOnLongClickListener { callback?.onItemLongPress(position) ?: false }
                itemView.setOnClickListener { callback?.onItemClick(position) }
            }
        }
    }

    fun getColorName(position: Int): String? {
        return colors.getOrNull(position)
    }

    fun toggleSelection(position: Int) {
        selectedItems.toggle(position)
        notifyItemChanged(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    val selectedItemCount
        get() = selectedItems.size

    fun getSelectedColors(): List<String> {
        return selectedItems.filterTrue().mapNotNull { getColorName(it) }
    }
}
