package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.CategoriesViewModel
import kotlinx.android.synthetic.main.fragment_categories.*
import kotlinx.android.synthetic.main.row_mechanic.view.*
import kotlin.properties.Delegates

class CategoriesFragment : Fragment() {
    private val viewModel by activityViewModels<CategoriesViewModel>()

    private val adapter: CategoriesAdapter by lazy {
        CategoriesAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        viewModel.categories.observe(viewLifecycleOwner, Observer {
            showData(it)
            progressBar.hide()
        })
    }

    private fun showData(categories: List<CategoryEntity>) {
        adapter.categories = categories
        if (adapter.itemCount == 0) {
            recyclerView.fadeOut()
            emptyTextView.fadeIn()
        } else {
            recyclerView.fadeIn()
            emptyTextView.fadeOut()
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
            return CategoryViewHolder(parent.inflate(R.layout.row_category))
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(categories.getOrNull(position))
        }

        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(category: CategoryEntity?) {
                category?.let { c ->
                    itemView.nameView.text = c.name
                    itemView.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, c.itemCount, c.itemCount)
                    itemView.setOnClickListener {
                        CategoryActivity.start(itemView.context, c.id, c.name)
                    }
                }
            }
        }
    }
}