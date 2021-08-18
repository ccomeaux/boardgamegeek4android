package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import kotlinx.android.synthetic.main.fragment_geeklist_items.*
import kotlinx.android.synthetic.main.row_geeklist_item.view.*
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class GeekListItemsFragment : Fragment(R.layout.fragment_geeklist_items) {
    private val viewModel by activityViewModels<GeekListViewModel>()
    private val adapter: GeekListRecyclerViewAdapter by lazy {
        GeekListRecyclerViewAdapter(lifecycleScope)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        viewModel.geekList.observe(viewLifecycleOwner, { entity ->
            when (entity.status) {
                Status.REFRESHING -> progressView.show()
                Status.ERROR -> setError(entity.message)
                Status.SUCCESS -> {
                    val geekListItems = entity.data?.items
                    if (geekListItems == null || geekListItems.isEmpty()) {
                        setError(getString(R.string.empty_geeklist))
                    } else {
                        adapter.geekList = entity.data
                        adapter.geekListItems = geekListItems.orEmpty()
                        recyclerView.fadeIn(isResumed)
                        progressView.hide()
                    }
                }
            }
        })
    }

    private fun setError(message: String?) {
        emptyView.text = message
        emptyView.fadeIn(isResumed)
        progressView.hide()
    }

    class GeekListRecyclerViewAdapter(val lifecycleScope: LifecycleCoroutineScope) : RecyclerView.Adapter<GeekListRecyclerViewAdapter.GeekListItemViewHolder>(), AutoUpdatableAdapter {
        var geekListItems: List<GeekListItemEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.objectId == new.objectId
            }
        }

        var geekList: GeekListEntity? = null

        init {
            setHasStableIds(true)
        }

        override fun getItemCount(): Int = geekListItems.size

        override fun getItemId(position: Int) = geekListItems.getOrNull(position)?.id ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeekListItemViewHolder {
            return GeekListItemViewHolder(parent.inflate(R.layout.row_geeklist_item))
        }

        override fun onBindViewHolder(holder: GeekListItemViewHolder, position: Int) {
            holder.bind(geekListItems.getOrNull(position), position + 1)
        }

        inner class GeekListItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(entity: GeekListItemEntity?, order: Int) {
                entity?.let { item ->
                    itemView.orderView.text = order.toString()
                    lifecycleScope.launch {
                        itemView.thumbnailView.loadThumbnail(item.imageId)
                    }
                    itemView.itemNameView.text = item.objectName
                    itemView.usernameView.text = item.username
                    itemView.usernameView.isVisible = item.username != geekList?.username
                    geekList?.let { list ->
                        itemView.setOnClickListener {
                            if (item.objectId != BggContract.INVALID_ID) {
                                GeekListItemActivity.start(itemView.context, list, item, order)
                            }
                        }
                    }
                }
            }
        }
    }
}
