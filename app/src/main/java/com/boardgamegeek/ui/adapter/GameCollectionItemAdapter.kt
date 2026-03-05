package com.boardgamegeek.ui.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.GameCollectionItemActivity
import com.boardgamegeek.ui.compose.GameCollectionItemListItem
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlin.properties.Delegates

class GameCollectionItemAdapter(private val context: Context) : RecyclerView.Adapter<GameCollectionItemAdapter.ViewHolder>(), AutoUpdatableAdapter {
    private val xmlConverter by lazy { XmlApiMarkupConverter(context) }

    var items: List<CollectionItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        autoNotify(oldValue, newValue) { old, new ->
            old.collectionId == new.collectionId
        }
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ComposeView(parent.context), xmlConverter)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it) }
    }

    class ViewHolder(private val composeView: ComposeView, private val markupConverter: XmlApiMarkupConverter) :
        RecyclerView.ViewHolder(composeView) {
        fun bind(item: CollectionItem) {
            composeView.setContent {
                GameCollectionItemListItem(item, markupConverter = markupConverter)
                {
                    GameCollectionItemActivity.start(itemView.context, item)
                }
            }
        }
    }
}
