package com.boardgamegeek.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.GameActivity.Companion.start
import com.boardgamegeek.ui.adapter.BuddyCollectionAdapter.BuddyGameViewHolder
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import kotlinx.android.synthetic.main.row_collection_buddy.view.*
import kotlin.properties.Delegates

class BuddyCollectionAdapter : RecyclerView.Adapter<BuddyGameViewHolder>(), AutoUpdatableAdapter, SectionCallback {
    var items: List<CollectionItemEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        autoNotify(oldValue, newValue) { old, new ->
            old.collectionId == new.collectionId
        }
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuddyGameViewHolder {
        return BuddyGameViewHolder(parent.inflate(R.layout.row_collection_buddy))
    }

    override fun onBindViewHolder(holder: BuddyGameViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    inner class BuddyGameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: CollectionItemEntity?) {
            itemView.nameView.text = item?.gameName ?: ""
            itemView.yearView.text = item?.gameId?.toString() ?: ""
            itemView.setOnClickListener {
                if (item != null) start(itemView.context, item.gameId, item.gameName)
            }
        }
    }

    override fun isSection(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) return false
        if (items.isEmpty()) return false
        if (position == 0) return true
        val thisLetter = items.getOrNull(position)?.sortName.firstChar()
        val lastLetter = items.getOrNull(position - 1)?.sortName.firstChar()
        return thisLetter != lastLetter
    }

    override fun getSectionHeader(position: Int): CharSequence {
        return when {
            position == RecyclerView.NO_POSITION -> return "-"
            items.isEmpty() -> return "-"
            position < 0 || position >= items.size -> "-"
            else -> items[position].sortName.firstChar()
        }
    }
}
