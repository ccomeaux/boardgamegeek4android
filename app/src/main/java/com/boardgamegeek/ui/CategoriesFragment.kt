package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCategoriesBinding
import com.boardgamegeek.model.Category
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
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

        binding.swipeRefresh.setOnRefreshListener { viewModel.reload() }

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
        binding.recyclerView.adapter = null
        _binding = null
    }

    class CategoriesAdapter : ListAdapter<Category, CategoriesAdapter.CategoryViewHolder>(ItemCallback()) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long = getItem(position).id.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            return CategoryViewHolder(ComposeView(parent.context))
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ItemCallback : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
        }

        inner class CategoryViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bind(category: Category) {
                composeView.setContent {
                    CategoryListItem(
                        category,
                        onClick = {
                            CategoryActivity.start(composeView.context, category.id, category.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryListItem(
    category: Category,
    modifier: Modifier = Modifier,
    onClick: (Category) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(category) })
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(category.name)
        ListItemSecondaryText(pluralStringResource(R.plurals.games_suffix, category.itemCount, category.itemCount))
    }
}

@Preview
@Composable
private fun CategoryListItemPreview(
    @PreviewParameter(CategoryPreviewParameterProvider::class) category: Category
) {
    BggAppTheme {
        CategoryListItem(category)
    }
}

private class CategoryPreviewParameterProvider : PreviewParameterProvider<Category> {
    override val values = sequenceOf(
        Category(
            id = 99,
            name = "Deck Building",
            itemCount = 30,
        ),
        Category(
            id = 99,
            name = "Auction",
            itemCount = 0,
        ),
        Category(
            id = 99,
            name = "Dice Rolling",
            itemCount = 1,
        )
    )
}
