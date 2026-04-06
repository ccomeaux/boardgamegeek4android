@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.SimpleCollectionItemList
import com.boardgamegeek.ui.compose.SubtitleWithIcon
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.MechanicViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MechanicActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(KEY_MECHANIC_ID, BggContract.INVALID_ID)
        val title = intent.getStringExtra(KEY_MECHANIC_NAME)

        setContent {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val viewModel by viewModels<MechanicViewModel>()
            val collectionItems by viewModel.collection.observeAsState()
            val sortBy by viewModel.sort.observeAsState(CollectionItem.SortType.RATING)
            viewModel.setId(id)

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MechanicTopBar(
                        title = title.orEmpty(),
                        sortBy = sortBy,
                        scrollBehavior = scrollBehavior,
                        onUpClick = { finish() },
                        onViewInBrowserClick = { linkToBgg("boardgamemechanic", id) },
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
        private const val KEY_MECHANIC_ID = "MECHANIC_ID"
        private const val KEY_MECHANIC_NAME = "MECHANIC_NAME"

        fun start(context: Context, mechanicId: Int, mechanicName: String) {
            context.startActivity<MechanicActivity>(
                KEY_MECHANIC_ID to mechanicId,
                KEY_MECHANIC_NAME to mechanicName,
            )
        }
    }
}

private enum class MechanicCollectionSort(
    val type: CollectionItem.SortType,
    @param:StringRes val labelResId: Int,
) {
    Name(CollectionItem.SortType.NAME, R.string.menu_sort_name),
    Rating(CollectionItem.SortType.RATING, R.string.menu_sort_rating),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MechanicTopBar(
    title: String,
    sortBy: CollectionItem.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onViewInBrowserClick: () -> Unit = {},
    onSortClick: (CollectionItem.SortType) -> Unit = {},
) {
    var showSortMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = {
            Text(title.ifBlank { stringResource(R.string.title_mechanic) })
        },
        subtitle = {
            SubtitleWithIcon(
                stringResource(
                    R.string.title_sorted_by,
                    stringResource(R.string.title_mechanic),
                    when (sortBy) {
                        CollectionItem.SortType.NAME -> stringResource(R.string.menu_sort_name)
                        CollectionItem.SortType.RATING -> stringResource(R.string.menu_sort_rating)
                    }
                ),
                painterResource(R.drawable.mechanic_24px)
            )
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = onUpClick) {
                Icon(
                    painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.up),
                )
            }
        },
        actions = {
            IconButton(onClick = onViewInBrowserClick) {
                Icon(
                    painterResource(R.drawable.open_in_browser_24px),
                    contentDescription = stringResource(R.string.menu_view_in_browser),
                )
            }
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    painterResource(R.drawable.sort_24px),
                    contentDescription = stringResource(R.string.menu_sort),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                MechanicCollectionSort.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(stringResource(it.labelResId)) },
                        leadingIcon = {
                            RadioButton(
                                selected = (it.type == sortBy),
                                onClick = null
                            )
                        },
                        onClick = {
                            showSortMenu = false
                            onSortClick(it.type)
                        }
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun MechanicTopBarPreview() {
    BggAppTheme {
        MechanicTopBar(
            "Blind Bidding",
            CollectionItem.SortType.RATING,
        )
    }
}