package com.boardgamegeek.ui.adapter

import android.util.SparseBooleanArray
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.boardgamegeek.extensions.filterTrue
import com.boardgamegeek.extensions.toggle
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.adapter.SearchResultsAdapter.SearchResultViewHolder
import com.boardgamegeek.ui.compose.SearchResultListItem
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
        return SearchResultViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        results.getOrNull(position)?.let { holder.bind(it, position) }
    }

    override fun getItemCount() = results.size

    override fun getItemId(position: Int) = getItem(position)?.id?.toLong() ?: NO_ID

    fun getItem(position: Int) = results.getOrNull(position)

    fun clear() {
        results = emptyList()
    }

    inner class SearchResultViewHolder(val view: ComposeView) : RecyclerView.ViewHolder(view) {
        fun bind(game: SearchResult, position: Int) {
            view.setContent {
                SearchResultListItem(
                    game,
                    isSelected = selectedItems.get(position, false),
                    onClick = {
                        if (callback?.onItemClick(position) != true) {
                            GameActivity.start(itemView.context, game.id, game.name)
                        }
                    },
                    onLongClick = {
                        callback?.onItemLongClick(position) ?: false
                    },
                )
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
