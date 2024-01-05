package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCategoriesBinding
import com.boardgamegeek.databinding.RowCategoryBinding
import com.boardgamegeek.model.Category
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.viewmodel.CategoriesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<CategoriesViewModel>()
    private val adapter: CategoriesAdapter by lazy { CategoriesAdapter() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.categories.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.recyclerView.isVisible = adapter.itemCount > 0
            binding.emptyTextView.isVisible = adapter.itemCount == 0
            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class CategoriesAdapter : ListAdapter<Category, CategoriesAdapter.CategoryViewHolder>(ItemCallback()) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long = getItem(position).id.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            return CategoryViewHolder(parent.inflate(R.layout.row_category))
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ItemCallback : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
        }

        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowCategoryBinding.bind(itemView)

            fun bind(category: Category) {
                binding.nameView.text = category.name
                binding.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, category.itemCount, category.itemCount)
                itemView.setOnClickListener {
                    CategoryActivity.start(itemView.context, category.id, category.name)
                }
            }
        }
    }
}
