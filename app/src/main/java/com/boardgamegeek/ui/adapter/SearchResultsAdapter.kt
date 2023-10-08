package com.boardgamegeek.ui.adapter

import android.graphics.Typeface
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowSearchBinding
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.filterTrue
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.toggle
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.adapter.SearchResultsAdapter.SearchResultViewHolder
import kotlin.properties.Delegates

class SearchResultsAdapter(private val callback: Callback?) : RecyclerView.Adapter<SearchResultViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var results: List<SearchResult> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.id == n.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder(parent.inflate(R.layout.row_search))
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(results.getOrNull(position), position)
    }

    override fun getItemCount() = results.size

    override fun getItemId(position: Int) = getItem(position)?.id?.toLong() ?: NO_ID

    fun getItem(position: Int) = results.getOrNull(position)

    fun clear() {
        results = emptyList()
    }

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = RowSearchBinding.bind(itemView)

        fun bind(game: SearchResult?, position: Int) {
            game?.let { result ->
                binding.nameView.text = result.name
                val style = when (result.nameType) {
                    SearchResult.NAME_TYPE_ALTERNATE -> Typeface.ITALIC
                    SearchResult.NAME_TYPE_PRIMARY, SearchResult.NAME_TYPE_UNKNOWN -> Typeface.NORMAL
                    else -> Typeface.NORMAL
                }
                binding.nameView.setTypeface(binding.nameView.typeface, style)
                binding.yearView.text = result.yearPublished.asYear(itemView.context)
                binding.gameIdView.text = itemView.context.getString(R.string.id_list_text, result.id.toString())

                itemView.isActivated = selectedItems.get(position, false)

                itemView.setOnClickListener {
                    if (callback?.onItemClick(position) != true) {
                        GameActivity.start(itemView.context, result.id, result.name)
                    }
                }

                itemView.setOnLongClickListener {
                    callback?.onItemLongClick(position) ?: false
                }
            }
        }
    }

    private val selectedItems = SparseBooleanArray()

    val selectedItemCount: Int
        get() = selectedItems.filterTrue().size

    fun toggleSelection(position: Int) {
        selectedItems.toggle(position)
        notifyItemChanged(position)
    }

    fun clearSelections() {
        val oldSelectedItems = selectedItems.clone()
        selectedItems.clear()
        oldSelectedItems.filterTrue().forEach { notifyItemChanged(it) }
    }

    fun getSelectedItems() = selectedItems.filterTrue().mapNotNull { getItem(it) }

    interface Callback {
        fun onItemClick(position: Int): Boolean

        fun onItemLongClick(position: Int): Boolean
    }
}
