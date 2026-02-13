package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GeekListsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@AndroidEntryPoint
class GeekListsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GeekLists")
            }
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val viewModel: GeekListsViewModel = viewModel()
            val geekLists = viewModel.geekLists.collectAsLazyPagingItems()
            val sortBy by viewModel.sort.collectAsState()

            BggAppTheme {
                Drawer(
                    selectedItem = DrawerItem.GeekLists,
                    drawerState = drawerState,
                ) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            GeekListsTopAppBar(
                                sortBy = GeekListsSort.entries.find { it.type == sortBy } ?: GeekListsSort.entries.first(),
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                scrollBehavior = scrollBehavior,
                                onSortClick = { viewModel.setSort(it.type) }
                            )
                        }
                    ) { contentPadding ->
                        when (geekLists.loadState.refresh) {
                            is LoadState.Loading -> {
                                BggLoadingIndicatorBox(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                )
                            }
                            is LoadState.Error -> {
                                ErrorContent(
                                    text = stringResource(R.string.empty_geeklists),
                                    painterResource(R.drawable.geeklist_24px),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                )
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                ) {
                                    items(geekLists.itemCount) { position ->
                                        val geekList = geekLists[position]
                                        if (geekList != null) {
                                            GeekListListItem(
                                                geekList = geekList,
                                                onClick = {
                                                    GeekListActivity.start(this@GeekListsActivity, geekList.id, geekList.title)
                                                }
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(72.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun startUp(context: Context) = context.startActivity(context.intentFor<GeekListsActivity>().clearTop())
    }
}

private enum class GeekListsSort(
    val type: GeekList.SortType,
    @param:StringRes val labelResId: Int,
) {
    Hot(GeekList.SortType.Hot, R.string.menu_sort_geeklists_hot),
    Recent(GeekList.SortType.Recent, R.string.menu_sort_geeklists_recent),
    Active(GeekList.SortType.Active, R.string.menu_sort_geeklists_active),
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun GeekListsTopAppBar(
    sortBy: GeekListsSort,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onSortClick: (GeekListsSort) -> Unit = {},
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.title_geeklists)) },
        subtitle = {
            GeekListsSort.entries.find { it == sortBy }?.let {
                Text(stringResource(it.labelResId))
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onMenuClick() }) {
                Icon(
                    painterResource(R.drawable.menu_24px),
                    contentDescription = stringResource(R.string.navigation_drawer)
                )
            }
        },
        actions = {
            IconButton(onClick = { expandedMenu = true }) {
                Icon(
                    painterResource(R.drawable.sort_24px),
                    contentDescription = stringResource(R.string.menu_sort),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                GeekListsSort.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(stringResource(it.labelResId)) },
                        leadingIcon = {
                            RadioButton(
                                selected = (it.type == sortBy.type),
                                onClick = null
                            )
                        },
                        onClick = {
                            expandedMenu = false
                            onSortClick(it)
                        }
                    )
                }
            }
        },
    )
}

@Composable
private fun GeekListListItem(geekList: GeekList, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = com.boardgamegeek.ui.compose.ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(com.boardgamegeek.ui.compose.ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(geekList.title, modifier = modifier.padding(bottom = 4.dp))
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListItemSecondaryText(geekList.username, icon = painterResource(R.drawable.person_24px))
            ListItemVerticalDivider()
            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
            ListItemSecondaryText(
                numberFormat.format(geekList.numberOfItems),
                icon = painterResource(R.drawable.geeklist_24px),
            )
            ListItemVerticalDivider()
            ListItemSecondaryText(
                numberFormat.format(geekList.numberOfThumbs),
                icon = painterResource(R.drawable.thumb_up_24px),
            )
        }
    }
}

@Preview
@Composable
private fun GeekListListItemPreview(
    @PreviewParameter(GeekListListItemPreviewParameterProvider::class) geekList: GeekList
) {
    GeekListListItem(geekList)
}

private class GeekListListItemPreviewParameterProvider : PreviewParameterProvider<GeekList> {
    override val values = sequenceOf(
        GeekList(
            id = 1,
            title = "Top 10 Games",
            username = "ccomeaux",
            numberOfItems = 42,
            numberOfThumbs = 11,
        ),
        GeekList(
            id = 2,
            title = "Short",
            username = "me2",
            numberOfItems = 0,
            numberOfThumbs = 0,
        ),
        GeekList(
            id = 3,
            title = "These are my Top 10 Games for the year of something, I don't know...",
            username = "ccomeaux",
            numberOfItems = 42,
            numberOfThumbs = 11,
        ),
    )
}
