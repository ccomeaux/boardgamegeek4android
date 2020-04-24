package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.model.GeekList
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import kotlinx.android.synthetic.main.fragment_geeklist_items.*
import kotlinx.android.synthetic.main.row_geeklist_item.view.*
import kotlin.properties.Delegates

class GeekListItemsFragment : Fragment() {
    private val viewModel by activityViewModels<GeekListViewModel>()
    private val adapter: GeekListRecyclerViewAdapter by lazy {
        GeekListRecyclerViewAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_geeklist_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        viewModel.geekList.observe(viewLifecycleOwner, Observer { (status, body, message) ->
            when (status) {
                Status.REFRESHING -> progressView.show()
                Status.ERROR -> setError(message)
                Status.SUCCESS -> {
                    val geekListItems = body?.items
                    if (geekListItems == null || geekListItems.isEmpty()) {
                        setError(getString(R.string.empty_geeklist))
                    } else {
                        body.let {
                            adapter.geekList = it
                        }
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

    class GeekListRecyclerViewAdapter : RecyclerView.Adapter<GeekListRecyclerViewAdapter.GeekListItemViewHolder>(), AutoUpdatableAdapter {
        var geekListItems: List<GeekListItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.objectId == new.objectId
            }
        }

        var geekList: GeekList? = null

        override fun getItemCount(): Int = geekListItems.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeekListItemViewHolder {
            return GeekListItemViewHolder(parent.inflate(R.layout.row_geeklist_item))
        }

        override fun onBindViewHolder(holder: GeekListItemViewHolder, position: Int) {
            holder.bind(geekListItems.getOrNull(position), position + 1)
        }

        inner class GeekListItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(item: GeekListItem?, order: Int) {
                item?.let {
                    itemView.orderView.text = order.toString()
                    itemView.thumbnailView.loadThumbnail(it.imageId())
                    itemView.itemNameView.text = it.objectName
                    itemView.usernameView.text = it.username
                    itemView.usernameView.isVisible = it.username != geekList?.username
                    itemView.setOnClickListener { _ ->
                        if (geekList != null && it.objectId != BggContract.INVALID_ID) {
                            GeekListItemActivity.start(itemView.context, geekList!!, it, order)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(): GeekListItemsFragment {
            return GeekListItemsFragment()
        }
    }
}
