@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.toggle
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.TimeZone

@AndroidEntryPoint
class PlaysActivity : BaseActivity() {
    enum class DialogType {
        RefreshDate,
        SendPlays,
        DeletePlays,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Plays")
            }
        }

        val viewModel by viewModels<PlaysViewModel>()
        val markupConverter = XmlApiMarkupConverter(this)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val snackbarHostState = remember { SnackbarHostState() }
            val selectedIds = remember { mutableStateSetOf<Long>() }

            val syncPlays by viewModel.syncPlays.observeAsState()
            val isRefreshing by viewModel.isRefreshing.observeAsState(false)
            val plays by viewModel.plays.observeAsState()
            val playCount by remember { derivedStateOf { plays?.values?.sumOf { list -> list.sumOf { play -> play.quantity } } ?: 0 } }
            val filterType by viewModel.filterType.observeAsState(PlaysViewModel.FilterType.ALL)
            val sortType by viewModel.sortType.observeAsState(PlaysViewModel.SortType.DATE)
            val errorMessage by viewModel.errorMessage.observeAsState()

            var showDialog by remember { mutableStateOf<DialogType?>(null) }

            LaunchedEffect(errorMessage) {
                errorMessage?.getContentIfNotHandled()?.let {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it.ifBlank { getString(R.string.msg_error_buddies) })
                    }
                }
            }

            BggAppTheme {
                Drawer(
                    selectedItem = DrawerItem.Plays,
                    drawerState = drawerState,
                ) {
                    Scaffold(
                        topBar = {
                            PlaysTopAppBar(
                                playCount = playCount,
                                filterBy = filterType,
                                sortBy = sortType,
                                selectedCount = selectedIds.size,
                                onUpClick = { startActivity(intentFor<PlaysSummaryActivity>().clearTop()) },
                                onCloseClick = { selectedIds.clear() },
                                onFilterClick = { viewModel.setFilter(it) },
                                onSortClick = { viewModel.setSort(it) },
                                onRefreshClick = { showDialog = DialogType.RefreshDate },
                                onSendClick = {
                                    showDialog = DialogType.SendPlays
                                    selectedIds.clear()
                                },
                                onDeleteClick = {
                                    showDialog = DialogType.DeletePlays
                                    selectedIds.clear()
                                },
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        when {
                            plays == null -> {
                                BggLoadingIndicatorBox(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding),
                                )
                            }
                            plays.orEmpty().isEmpty() -> {
                                val emptyMessageResId = when (filterType) {
                                    PlaysViewModel.FilterType.DIRTY -> R.string.empty_plays_draft
                                    PlaysViewModel.FilterType.PENDING -> R.string.empty_plays_pending
                                    else -> if (syncPlays == true) {
                                        R.string.empty_plays
                                    } else {
                                        R.string.empty_plays_sync_off
                                    }
                                }
                                EmptyContent(
                                    stringResource(emptyMessageResId),
                                    painterResource(R.drawable.plays_24px),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
                                        .padding(contentPadding),
                                )
                            }
                            else -> {
                                PullToRefreshBox(
                                    isRefreshing = isRefreshing,
                                    onRefresh = { viewModel.refresh() },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = contentPadding.calculateTopPadding())
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                                    ) {
                                        plays.orEmpty().forEach { (headerText, list) ->
                                            stickyHeader {
                                                ListHeader(headerText)
                                            }
                                            items(
                                                items = list,
                                                key = { play -> play.internalId },
                                            ) {
                                                PlayListItem(
                                                    play = it,
                                                    showGameName = true,
                                                    markupConverter = markupConverter,
                                                    isSelected = selectedIds.contains(it.internalId),
                                                    onClick = {
                                                        if (selectedIds.isNotEmpty())
                                                            selectedIds.toggle(it.internalId)
                                                        else
                                                            PlayActivity.start(this@PlaysActivity, it.internalId)
                                                    },
                                                    onLongClick = {
                                                        selectedIds.toggle(it.internalId)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    showDialog?.let {
                        when (it) {
                            DialogType.RefreshDate -> {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDate.now() else null
                                )
                                DatePickerDialog(
                                    onDismissRequest = { showDialog = null },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            datePickerState.selectedDateMillis?.let { timestamp ->
                                                val timeZone = TimeZone.getDefault()
                                                val localMillis = timestamp - timeZone.getOffset(timestamp)
                                                viewModel.refreshPlaysByDate(localMillis)
                                            }
                                            showDialog = null
                                        }) {
                                            Text("Refresh")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDialog = null }) {
                                            Text("Cancel")
                                        }
                                    }
                                ) {
                                    DatePicker(state = datePickerState)
                                }
                            }
                            DialogType.SendPlays -> {
                                AlertDialog(
                                    onDismissRequest = { showDialog = null },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.send(selectedIds.toList())
                                                selectedIds.clear()
                                            },
                                        ) {
                                            Text(stringResource(R.string.ok))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDialog = null }) { Text(stringResource(R.string.cancel)) }
                                    },
                                    title = { Text(text = stringResource(R.string.send)) },
                                    text = {
                                        Text(
                                            pluralStringResource(R.plurals.are_you_sure_send_play, selectedIds.size, selectedIds.size),
                                        )
                                    },
                                )
                            }
                            DialogType.DeletePlays -> {
                                AlertDialog(
                                    onDismissRequest = { showDialog = null },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.delete(selectedIds.toList())
                                                selectedIds.clear()
                                            },
                                        ) {
                                            Text(stringResource(R.string.ok))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDialog = null }) { Text(stringResource(R.string.cancel)) }
                                    },
                                    title = { Text(text = stringResource(R.string.delete)) },
                                    text = {
                                        Text(
                                            pluralStringResource(R.plurals.are_you_sure_delete_play, selectedIds.size, selectedIds.size),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class PlaysFilter(
    val type: PlaysViewModel.FilterType,
    @param:StringRes val labelResId: Int,
) {
    All(PlaysViewModel.FilterType.ALL, R.string.menu_plays_filter_all),
    Pending(PlaysViewModel.FilterType.PENDING, R.string.menu_plays_filter_pending),
    Dirty(PlaysViewModel.FilterType.DIRTY, R.string.menu_plays_filter_in_progress),
}

private enum class PlaysSort(
    val type: PlaysViewModel.SortType,
    @param:StringRes val labelResId: Int,
) {
    Date(PlaysViewModel.SortType.DATE, R.string.menu_plays_sort_date),
    Location(PlaysViewModel.SortType.LOCATION, R.string.menu_plays_sort_location),
    Game(PlaysViewModel.SortType.GAME, R.string.menu_plays_sort_game),
    Length(PlaysViewModel.SortType.LENGTH, R.string.menu_plays_sort_length),
}

@Composable
private fun PlaysTopAppBar(
    playCount: Int,
    filterBy: PlaysViewModel.FilterType,
    sortBy: PlaysViewModel.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    selectedCount: Int = 0,
    onUpClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    onFilterClick: (PlaysViewModel.FilterType) -> Unit = {},
    onSortClick: (PlaysViewModel.SortType) -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onSendClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = {
            Text(stringResource(R.string.title_plays))
        },
        subtitle = {
            if (selectedCount > 0) {
                Text(stringResource(R.string.selected_suffix, selectedCount))
            } else if (playCount > 0) {
                val filterDescription = PlaysFilter.entries.find { it.type == filterBy }?.let { stringResource(it.labelResId) }.orEmpty()
                val sortDescription = PlaysSort.entries.find { it.type == sortBy }?.let { stringResource(it.labelResId) }.orEmpty()
                Text(
                    stringResource(
                        R.string.play_count_by_sort, playCount,
                        sortDescription,
                        filterDescription
                    )
                )
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (selectedCount > 0)
                CloseAppBarAction { onCloseClick() }
            else
                UpAppBarAction { onUpClick() }
        },
        actions = {
            if (selectedCount > 0) {
                IconButton(onSendClick) {
                    // ideally, only show if all selected items are pending
                    Icon(
                        painterResource(R.drawable.send_24px),
                        contentDescription = stringResource(R.string.send),
                    )
                }
                IconButton(onDeleteClick) {
                    Icon(
                        painterResource(R.drawable.delete_24px),
                        contentDescription = stringResource(R.string.menu_delete),
                    )
                }
            } else {
                FilterAppBarAction { showFilterMenu = true }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    PlaysFilter.entries.forEach {
                        DropdownMenuItem(
                            text = { Text(stringResource(it.labelResId)) },
                            leadingIcon = {
                                RadioButton(
                                    selected = (it.type == filterBy),
                                    onClick = null
                                )
                            },
                            onClick = {
                                showSortMenu = false
                                onFilterClick(it.type)
                            }
                        )
                    }
                }
                SortAppBarAction { showSortMenu = true }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    PlaysSort.entries.forEach {
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
                OverflowAppBarAction { showOverflowMenu = !showOverflowMenu }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_refresh_on)) },
                        onClick = {
                            showOverflowMenu = false
                            onRefreshClick()
                        }
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun PlaysTopAppBarPreview() {
    PlaysTopAppBar(
        123,
        PlaysViewModel.FilterType.PENDING,
        PlaysViewModel.SortType.LENGTH,
    )
}


@Preview
@Composable
private fun PlaysTopAppBar2Preview() {
    PlaysTopAppBar(
        123,
        PlaysViewModel.FilterType.PENDING,
        PlaysViewModel.SortType.LENGTH,
        selectedCount = 1,
    )
}
