package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCategoriesBinding
import com.boardgamegeek.databinding.RowCategoryBinding
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.CategoriesViewModel
import kotlin.properties.Delegates

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoriesViewModel by lazy {
        ViewModelProvider(this).get(CategoriesViewModel::class.java)
    }

    private val adapter: CategoriesAdapter by lazy {
        CategoriesAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.categories.observe(this, Observer {
            showData(it)
            binding.progressBar.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(categories: List<CategoryEntity>) {
        adapter.categories = categories
        if (adapter.itemCount == 0) {
            binding.recyclerView.fadeOut()
            binding.emptyTextView.fadeIn()
        } else {
            binding.recyclerView.fadeIn()
            binding.emptyTextView.fadeOut()
        }
    }

    companion object {
        fun newInstance(): CategoriesFragment {
            return CategoriesFragment()
        }
    }

    class CategoriesAdapter : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>(), AutoUpdatableAdapter {
        var categories: List<CategoryEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = categories.size

        override fun getItemId(position: Int) = categories.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val binding = RowCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CategoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(categories.getOrNull(position))
        }

        inner class CategoryViewHolder(private val binding: RowCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(category: CategoryEntity?) {
                category?.let { c ->
                    binding.nameView.text = c.name
                    binding.countView.text = binding.root.context.resources.getQuantityString(R.plurals.games_suffix, c.itemCount, c.itemCount)
                    binding.root.setOnClickListener {
                        CategoryActivity.start(binding.root.context, c.id, c.name)
                    }
                }
            }
        }
    }
}