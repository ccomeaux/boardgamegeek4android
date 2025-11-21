package com.boardgamegeek.ui.adapter

import android.graphics.Typeface
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowSearchBinding
import com.boardgamegeek.entities.SearchResultEntity
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.adapter.SearchResultsAdapter.SearchResultViewHolder
import java.util.*
import kotlin.properties.Delegates

interface Callback {
    fun onItemClick(position: Int): Boolean

    fun onItemLongClick(position: Int): Boolean
}

class SearchResultsAdapter(private val callback: Callback?) : RecyclerView.Adapter<SearchResultViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var results: List<SearchResultEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.id == n.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = RowSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchResultViewHolder(binding)
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

    inner class SearchResultViewHolder(private val binding: RowSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(game: SearchResultEntity?, position: Int) {
            game?.let { result ->
                binding.nameView.text = result.name
                val style = when (result.nameType) {
                    SearchResultEntity.NAME_TYPE_ALTERNATE -> Typeface.ITALIC
                    SearchResultEntity.NAME_TYPE_PRIMARY, SearchResultEntity.NAME_TYPE_UNKNOWN -> Typeface.NORMAL
                    else -> Typeface.NORMAL
                }
                binding.nameView.setTypeface(binding.nameView.typeface, style)
                binding.yearView.text = result.yearPublished.asYear(binding.root.context)
                binding.gameIdView.text = binding.root.context.getString(R.string.id_list_text, result.id.toString())

                binding.root.isActivated = selectedItems.get(position, false)

                binding.root.setOnClickListener {
                    if (callback?.onItemClick(position) != true) {
                        GameActivity.start(binding.root.context, result.id, result.name)
                    }
                }

                binding.root.setOnLongClickListener {
                    callback?.onItemLongClick(position) ?: false
                }
            }
        }
    }

    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    val selectedItemCount: Int
        get() = selectedItems.size()

    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
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

    fun getSelectedItems(): List<Int> {
        val items = ArrayList<Int>(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }
}

