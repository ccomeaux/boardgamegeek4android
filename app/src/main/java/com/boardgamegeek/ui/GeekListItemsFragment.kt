package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGeeklistItemsBinding
import com.boardgamegeek.databinding.RowGeeklistItemBinding
import com.boardgamegeek.entities.GeekListEntity
import com.boardgamegeek.entities.GeekListItemEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnails
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.GeekListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AndroidEntryPoint
class GeekListItemsFragment : Fragment() {
    private var _binding: FragmentGeeklistItemsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<GeekListViewModel>()
    private val adapter: GeekListRecyclerViewAdapter by lazy { GeekListRecyclerViewAdapter(lifecycleScope) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGeeklistItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.geekList.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                if (status == Status.REFRESHING)
                    binding.progressView.show()
                else binding.progressView.hide()
                if (status == Status.ERROR)
                    setError(message)
                else {
                    val geekListItems = data?.items
                    if (geekListItems.isNullOrEmpty()) {
                        setError(getString(R.string.empty_geeklist))
                        binding.recyclerView.isVisible = false
                    } else {
                        setError(null)
                        adapter.geekList = data
                        adapter.geekListItems = geekListItems
                        binding.recyclerView.isVisible = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setError(message: String?) {
        binding.emptyView.text = message
        binding.emptyView.isVisible = !message.isNullOrBlank()
    }

    class GeekListRecyclerViewAdapter(val lifecycleScope: LifecycleCoroutineScope) :
        RecyclerView.Adapter<GeekListRecyclerViewAdapter.GeekListItemViewHolder>(), AutoUpdatableAdapter {
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

        inner class GeekListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowGeeklistItemBinding.bind(itemView)

            fun bind(entity: GeekListItemEntity?, order: Int) {
                entity?.let { item ->
                    binding.orderView.text = order.toString()
                    item.thumbnailUrls?.let {
                        if (it.isNotEmpty()) {
                            lifecycleScope.launch {
                                binding.thumbnailView.loadThumbnails(*it.toTypedArray())
                            }
                        }
                    }
                    binding.itemNameView.text = item.objectName
                    binding.usernameView.text = item.username
                    binding.usernameView.isVisible = item.username != geekList?.username
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
