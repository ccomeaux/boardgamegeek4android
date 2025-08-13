@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.model.Category
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.CategoriesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoriesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val viewModel by viewModels<CategoriesViewModel>()
            val sortBy by viewModel.sortType.observeAsState(Category.SortType.ITEM_COUNT)
            val categories by viewModel.categories.observeAsState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            BggAppTheme {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        CategoriesTopBar(
                            categoryCount = categories?.size ?: 0,
                            sortBy = sortBy,
                            scrollBehavior = scrollBehavior,
                            onUpClick = { finish() },
                            onSortClick = { viewModel.sort(it) },
                        )
                    },
                ) { contentPadding ->
                    CategoriesScreen(
                        categories,
                        contentPadding = contentPadding,
                        onItemClick = {
                            CategoryActivity.start(context, it.id, it.name)
                        }
                    )
                }
            }
        }
    }
}


private enum class CategoriesSort(
    val type: Category.SortType,
    @StringRes val labelResId: Int,
) {
    Name(Category.SortType.NAME, R.string.menu_sort_name),
    ItemCount(Category.SortType.ITEM_COUNT, R.string.menu_sort_item_count),
}

@Composable
private fun CategoriesTopBar(
    categoryCount: Int,
    sortBy: Category.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onSortClick: (Category.SortType) -> Unit = {},
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.title_categories)) },
        subtitle = {
            if (categoryCount > 0) {
                CategoriesSort.entries.find { it.type == sortBy }?.let {
                    Text(stringResource(R.string.count_by_sort, categoryCount, stringResource(it.labelResId)))
                }
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onUpClick() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.up))
            }
        },
        actions = {
            IconButton(onClick = { expandedMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.menu_sort),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                CategoriesSort.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(stringResource(it.labelResId)) },
                        leadingIcon = {
                            RadioButton(
                                selected = (it.type == sortBy),
                                onClick = null
                            )
                        },
                        onClick = {
                            expandedMenu = false
                            onSortClick(it.type)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun CategoriesScreen(
    categories: List<Category>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItemClick: (Category) -> Unit = {},
) {
    when {
        categories == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                BggLoadingIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .padding(dimensionResource(R.dimen.padding_extra))
                )
            }
        }
        categories.isEmpty() -> {
            EmptyContent(
                R.string.empty_categories,
                Icons.Default.Category,
                Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                items(categories) { category ->
                    CategoryListItem(
                        category,
                        onClick = { onItemClick(category) }
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
