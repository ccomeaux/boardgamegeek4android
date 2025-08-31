@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.SimpleCollectionItemList
import com.boardgamegeek.ui.viewmodel.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(KEY_CATEGORY_ID, BggContract.INVALID_ID)
        val title = intent.getStringExtra(KEY_CATEGORY_NAME)

        setContent {
            val viewModel by viewModels<CategoryViewModel>()
            val collectionItems by viewModel.collection.observeAsState()
            val sortBy by viewModel.sort.observeAsState(CollectionItem.SortType.RATING)
            viewModel.setId(id)

            Scaffold(
                topBar = {
                    CategoryTopBar(
                        title = title.orEmpty(),
                        sortBy = sortBy,
                        onUpClick = { finish() },
                        onViewInBrowserClick = { linkToBgg("boardgamecategory", id) },
                        onSortClick = { viewModel.setSort(it) },
                    )
                }
            ) { contentPadding ->
                SimpleCollectionItemList(
                    collectionItems = collectionItems,
                    contentPadding = contentPadding,
                )
            }
        }
    }

    companion object {
        private const val KEY_CATEGORY_ID = "CATEGORY_ID"
        private const val KEY_CATEGORY_NAME = "CATEGORY_NAME"

        fun start(context: Context, categoryId: Int, categoryName: String) {
            context.startActivity<CategoryActivity>(
                KEY_CATEGORY_ID to categoryId,
                KEY_CATEGORY_NAME to categoryName,
            )
        }
    }
}

private enum class CategoryCollectionSort(
    val type: CollectionItem.SortType,
    @StringRes val labelResId: Int,
) {
    Name(CollectionItem.SortType.NAME, R.string.menu_sort_name),
    Rating(CollectionItem.SortType.RATING, R.string.menu_sort_rating),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTopBar(
    title: String,
    sortBy: CollectionItem.SortType,
    modifier: Modifier = Modifier,
    onUpClick: () -> Unit = {},
    onViewInBrowserClick: () -> Unit = {},
    onSortClick: (CollectionItem.SortType) -> Unit = {},
) {
    var expandedMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(title.ifBlank { stringResource(R.string.title_category) })
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onUpClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.up),
                )
            }
        },
        actions = {
            IconButton(onClick = onViewInBrowserClick) {
                Icon(
                    Icons.Default.OpenInBrowser,
                    contentDescription = stringResource(R.string.menu_view_in_browser),
                )
            }
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
                CategoryCollectionSort.entries.forEach {
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

