package com.boardgamegeek.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowCollectionItemBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.google.android.material.divider.MaterialDividerItemDecoration
import kotlin.properties.Delegates

class CollectionShelf @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var adapter: CollectionItemAdapter? = null
    private var headerText: String? = ""

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_collection_shelf, this)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        orientation = VERTICAL
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            addItemDecoration(MaterialDividerItemDecoration(context, RecyclerView.HORIZONTAL).apply {
                dividerColor = Color.TRANSPARENT
                dividerThickness = context.resources.getDimensionPixelSize(R.dimen.padding_small)
            })
        }

        context.withStyledAttributes(attrs, R.styleable.CollectionShelf, defStyleAttr) {
            headerText = getString(R.styleable.CollectionShelf_headerLabel)
            findViewById<TextView>(R.id.headerView).text = headerText

            val infoText = getString(R.styleable.CollectionShelf_helpText)
            if (infoText.isNullOrBlank()) {
                findViewById<ImageView>(R.id.infoView).isVisible = false
            } else {
                findViewById<ImageView>(R.id.infoView).isVisible = true
                findViewById<ImageView>(R.id.infoView).setOnClickListener {
                    context.showClickableAlertDialog(
                        ResourcesCompat.ID_NULL,
                        infoText,
                    )
                }
            }
        }
    }

    fun setAdapter(adapter: CollectionItemAdapter) {
        this.adapter = adapter
        findViewById<RecyclerView>(R.id.recyclerView).adapter = adapter
    }

    fun bindList(list: List<CollectionItem>?) {
        list?.let {
            findViewById<TextView>(R.id.headerContainer).isVisible = it.isNotEmpty()
            findViewById<RecyclerView>(R.id.recyclerView).isVisible = it.isNotEmpty()
            adapter?.items = it
            findViewById<ContentLoadingProgressBar>(R.id.progressBar).hide()
        }
    }

    fun setCount(count: Int?) {
        findViewById<TextView>(R.id.headerView).text = count?.let {
            "$headerText - $count"
        } ?: headerText
    }

    class CollectionItemAdapter(
        @MenuRes private val menuResourceId: Int = ResourcesCompat.ID_NULL,
        private val onMenuClick: ((item: CollectionItem, menuItem: MenuItem) -> Boolean)? = null,
        private val bindBadge: ((item: CollectionItem) -> Pair<CharSequence, Int>)? = null,
    ) : Adapter<CollectionItemAdapter.CollectionItemViewHolder>(), AutoUpdatableAdapter {
        var items: List<CollectionItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.collectionId == new.collectionId
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionItemViewHolder {
            return CollectionItemViewHolder(parent.inflate(R.layout.row_collection_item))
        }

        override fun onBindViewHolder(holder: CollectionItemViewHolder, position: Int) {
            holder.bindView(getItem(position))
        }

        override fun getItemCount() = items.size

        fun getItem(position: Int) = items.getOrNull(position)

        override fun getItemId(position: Int) = getItem(position)?.internalId ?: RecyclerView.NO_ID

        inner class CollectionItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding = RowCollectionItemBinding.bind(itemView)

            fun bindView(item: CollectionItem?) {
                if (item == null) return
                binding.nameView.text = item.gameName
                binding.yearView.text = item.yearPublished.asYear(itemView.context)
                binding.thumbnailView.loadThumbnail(item.thumbnailUrl)
                bindBadge?.let {
                    it.invoke(item).also { badge ->
                        binding.badgeView.setTextOrHide(badge.first)
                        binding.badgeView.setTextViewBackground(badge.second)
                    }
                }
                if (menuResourceId == ResourcesCompat.ID_NULL) {
                    binding.moreButton.isVisible = false
                } else {
                    binding.moreButton.isVisible = true
                    binding.moreButton.setOnClickListener {
                        PopupMenu(itemView.context, binding.moreButton).apply {
                            inflate(menuResourceId)
                            setOnMenuItemClickListener { menuItem: MenuItem ->
                                onMenuClick?.invoke(item, menuItem) ?: false
                                true
                            }
                            show()
                        }
                    }
                }
                itemView.setOnClickListener {
                    GameActivity.start(itemView.context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
                }
                itemView.setOnLongClickListener {
                    GameActivity.start(itemView.context, item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
                    true
                }
            }
        }
    }
}