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
import androidx.compose.material.icons.filled.Build
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
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MechanicsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val viewModel by viewModels<MechanicsViewModel>()
            val sortBy by viewModel.sortType.observeAsState(Mechanic.SortType.ITEM_COUNT)
            val mechanics by viewModel.mechanics.observeAsState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            BggAppTheme {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        MechanicsTopBar(
                            categoryCount = mechanics?.size ?: 0,
                            sortBy = sortBy,
                            scrollBehavior = scrollBehavior,
                            onUpClick = { finish() },
                            onSortClick = { viewModel.sort(it) },
                        )
                    },
                ) { contentPadding ->
                    MechanicsScreen(
                        mechanics,
                        contentPadding = contentPadding,
                        onItemClick = {
                            MechanicActivity.start(context, it.id, it.name)
                        }
                    )
                }
            }
        }
    }
}

private enum class MechanicsSort(
    val type: Mechanic.SortType,
    @StringRes val labelResId: Int,
) {
    Name(Mechanic.SortType.NAME, R.string.menu_sort_name),
    ItemCount(Mechanic.SortType.ITEM_COUNT, R.string.menu_sort_item_count),
}

@Composable
private fun MechanicsTopBar(
    categoryCount: Int,
    sortBy: Mechanic.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onSortClick: (Mechanic.SortType) -> Unit = {},
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.title_mechanics)) },
        subtitle = {
            if (categoryCount > 0) {
                MechanicsSort.entries.find { it.type == sortBy }?.let {
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
                MechanicsSort.entries.forEach {
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
private fun MechanicsScreen(
    mechanics: List<Mechanic>?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItemClick: (Mechanic) -> Unit = {},
) {
    when {
        mechanics == null -> {
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
        mechanics.isEmpty() -> {
            EmptyContent(
                R.string.empty_mechanics,
                Icons.Default.Build,
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
                items(mechanics) { mechanic ->
                    MechanicListItem(
                        mechanic,
                        onClick = { onItemClick(mechanic) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MechanicListItem(
    mechanic: Mechanic,
    modifier: Modifier = Modifier,
    onClick: (Mechanic) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(mechanic) })
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(mechanic.name)
        ListItemSecondaryText(pluralStringResource(R.plurals.games_suffix, mechanic.itemCount, mechanic.itemCount))
    }
}

@Preview
@Composable
private fun MechanicListItemPreview(
    @PreviewParameter(MechanicPreviewParameterProvider::class) mechanic: Mechanic
) {
    BggAppTheme {
        MechanicListItem(mechanic)
    }
}

private class MechanicPreviewParameterProvider : PreviewParameterProvider<Mechanic> {
    override val values = sequenceOf(
        Mechanic(
            id = 99,
            name = "Deck Building",
            itemCount = 30,
        ),
        Mechanic(
            id = 99,
            name = "Auction",
            itemCount = 0,
        ),
        Mechanic(
            id = 99,
            name = "Dice Rolling",
            itemCount = 1,
        )
    )
}
