package com.boardgamegeek.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.GameCollectionItemActivity
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.android.synthetic.main.widget_collection_row.view.*
import kotlin.properties.Delegates

class GameCollectionItemAdapter(private val context: Context) : RecyclerView.Adapter<GameCollectionItemAdapter.ViewHolder>(), AutoUpdatableAdapter {
    private val xmlConverter by lazy { XmlApiMarkupConverter(context) }

    var gameYearPublished: Int by Delegates.observable(YEAR_UNKNOWN) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    var items: List<CollectionItemEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue.isEmpty()) {
            // this is a hack for the first load
            notifyDataSetChanged()
        } else {
            autoNotify(oldValue, newValue) { old, new ->
                old.collectionId == new.collectionId
            }
        }
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.widget_collection_row), xmlConverter)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items.getOrNull(position), gameYearPublished)
    }

    class ViewHolder(itemView: View, private val markupConverter: XmlApiMarkupConverter) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: CollectionItemEntity?, gameYearPublished: Int) {
            if (item == null) return
            itemView.thumbnail.loadThumbnailInList(item.thumbnailUrl)
            itemView.status.setTextOrHide(describeStatuses(item, itemView.context).formatList())

            itemView.comment.setTextMaybeHtml(markupConverter.toHtml(item.comment), HtmlCompat.FROM_HTML_MODE_COMPACT, false)
            itemView.comment.isVisible = item.comment.isNotBlank()

            val description = if (item.collectionName.isNotBlank() && item.collectionName != item.gameName ||
                    item.yearPublished != YEAR_UNKNOWN && item.yearPublished != gameYearPublished) {
                if (item.yearPublished == YEAR_UNKNOWN) {
                    item.collectionName
                } else {
                    "${item.collectionName} (${item.yearPublished.asYear(itemView.context)})"
                }
            } else ""
            itemView.description.setTextOrHide(description)

            if (item.rating == 0.0) {
                itemView.rating.isVisible = false
            } else {
                itemView.rating.text = item.rating.asPersonalRating(itemView.context)
                itemView.rating.setTextViewBackground(item.rating.toColor(ratingColors))
                itemView.rating.isVisible = true
            }

            if (item.hasPrivateInfo()) {
                itemView.privateInfo.text = item.getPrivateInfo(itemView.context)
                itemView.privateInfo.isVisible = true
            } else {
                itemView.privateInfo.isVisible = false
            }

            itemView.privateComment.setTextMaybeHtml(markupConverter.toHtml(item.privateComment), HtmlCompat.FROM_HTML_MODE_COMPACT, false)
            itemView.privateComment.isVisible = item.privateComment.isNotBlank()

            itemView.setOnClickListener {
                GameCollectionItemActivity.start(
                        itemView.context,
                        item.internalId,
                        item.gameId,
                        item.gameName,
                        item.collectionId,
                        item.collectionName,
                        item.thumbnailUrl,
                        item.heroImageUrl,
                        gameYearPublished,
                        item.yearPublished)
            }
        }

        private fun describeStatuses(item: CollectionItemEntity, ctx: Context): List<String> {
            val statuses = mutableListOf<String>()
            if (item.own) statuses.add(ctx.getString(R.string.collection_status_own))
            if (item.previouslyOwned) statuses.add(ctx.getString(R.string.collection_status_prev_owned))
            if (item.forTrade) statuses.add(ctx.getString(R.string.collection_status_for_trade))
            if (item.wantInTrade) statuses.add(ctx.getString(R.string.collection_status_want_in_trade))
            if (item.wantToBuy) statuses.add(ctx.getString(R.string.collection_status_want_to_buy))
            if (item.wantToPlay) statuses.add(ctx.getString(R.string.collection_status_want_to_play))
            if (item.preOrdered) statuses.add(ctx.getString(R.string.collection_status_preordered))
            if (item.wishList) statuses.add(item.wishListPriority.asWishListPriority(ctx))
            if (statuses.isEmpty()) {
                if (item.numberOfPlays > 0) {
                    statuses.add(ctx.getString(R.string.played))
                } else {
                    if (item.rating > 0.0) statuses.add(ctx.getString(R.string.rated))
                    if (item.comment.isNotBlank()) statuses.add(ctx.getString(R.string.commented))
                }
            }
            return statuses
        }
    }
}
