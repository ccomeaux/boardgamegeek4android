@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.PlayersViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayersActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Players")
            }
        }

        setContent {
            val context = LocalContext.current
            val viewModel by viewModels<PlayersViewModel>()
            val sortBy by viewModel.sortType.observeAsState(Player.SortType.PlayCount)
            val players by viewModel.playersMap.observeAsState()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            BggAppTheme {
                var filterOpen by remember { mutableStateOf(false) }
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        PlayersTopBar(
                            playerCount = players?.values?.sumOf { it.size } ?: 0,
                            sortBy = sortBy,
                            scrollBehavior = scrollBehavior,
                            onUpClick = { finish() },
                            onSortClick = { viewModel.sort(it) },
                            onFilterClick = { filterOpen = !filterOpen }
                        )
                    },
                ) { contentPadding ->
                    PlayersScreen(
                        players,
                        showFilter = filterOpen,
                        contentPadding = contentPadding,
                        onItemClick = {
                            BuddyActivity.start(context, it.username, it.name)
                        },
                        onFilter = {
                            viewModel.filter(it)
                        },
                    )
                }
            }
        }
    }

    companion object {
        private const val KEY_SORT_TYPE = "SORT_TYPE"

        fun start(context: Context) {
            context.startActivity<PlayersActivity>()
        }

        fun startByPlayCount(context: Context) {
            context.startActivity<PlayersActivity>(
                KEY_SORT_TYPE to Player.SortType.PlayCount
            )
        }
    }
}

private enum class PlayersSort(
    val type: Player.SortType,
    @StringRes val labelResId: Int,
) {
    Name(Player.SortType.Name, R.string.menu_sort_name),
    PlayCount(Player.SortType.PlayCount, R.string.menu_sort_quantity),
    WinCount(Player.SortType.WinCount, R.string.menu_sort_wins),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayersTopBar(
    playerCount: Int,
    sortBy: Player.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onSortClick: (Player.SortType) -> Unit = {},
    onFilterClick: () -> Unit = {},
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = {
            Text(stringResource(R.string.title_players))
        },
        subtitle = {
            if (playerCount > 0) {
                PlayersSort.entries.find { it.type == sortBy }?.let {
                    Text(stringResource(R.string.count_by_sort, playerCount, stringResource(it.labelResId)))
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
            IconButton(onClick = { onFilterClick() }) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.menu_filter),
                    tint = MaterialTheme.colorScheme.primary,
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
                PlayersSort.entries.forEach {
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
private fun PlayersScreen(
    players: Map<String, List<Player>>?,
    modifier: Modifier = Modifier,
    showFilter: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItemClick: (Player) -> Unit = {},
    onFilter: (String) -> Unit = {},
    focusRequester: FocusRequester = FocusRequester(),
) {
    when {
        players == null -> {
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
        players.isEmpty() -> {
            EmptyFullSizeScrollableContent(
                R.string.empty_players,
                Icons.Default.Person,
                padding = contentPadding,
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                var textFieldValue by remember { mutableStateOf("") }
                AnimatedVisibility(showFilter) {
                    TextField(
                        value = textFieldValue,
                        maxLines = 1,
                        onValueChange = {
                            textFieldValue = it
                            onFilter(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                                vertical = dimensionResource(R.dimen.material_margin_vertical)
                            )
                            .focusRequester(focusRequester = focusRequester)
                            .focusable(),
                        shape = MaterialTheme.shapes.small,
                        placeholder = { Text("Filter") },
                        leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                textFieldValue = ""
                                onFilter("")
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = null)
                            }
                        }
                    )
                    LaunchedEffect(showFilter) {
                        if (showFilter) {
                            focusRequester.requestFocus()
                        }
                    }
                }
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    // contentPadding = contentPadding,
                ) {
                    players.forEach { (headerText, players) ->
                        stickyHeader {
                            ListHeader(headerText)
                        }
                        items(
                            items = players,
                            key = { it.id }
                        ) { player ->
                            PlayerListItem(
                                player,
                                onClick = { onItemClick(player) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayersScreenPreview() {
    BggAppTheme {
        PlayersScreen(
            mapOf(
                "C" to listOf(
                    Player(
                        name = "Chris",
                        username = "ccomeaux",
                        userFullName = "Chris Comeaux"
                    ),
                    Player(
                        name = "Craig",
                        username = "cberg",
                        userFullName = "Craig Berg"
                    )
                )
            )
        )
    }
}

@Composable
private fun PlayerListItem(
    player: Player,
    modifier: Modifier = Modifier,
    onClick: (Player) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(player) })
            .padding(ListItemDefaults.paddingValues)

    ) {
        val primaryText = buildAnnotatedString {
            withStyle(style = ListItemDefaults.primaryTextStyle().toSpanStyle()) {
                if (player.userFullName.isNullOrBlank()) {
                    append(player.name)
                } else if (player.userFullName.contains(player.name)) {
                    val splits = player.userFullName.split(player.name)
                    splits.forEachIndexed { i, split ->
                        if (i > 0)
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append(player.name)
                            }
                        append(split)
                    }
                } else {
                    append(player.userFullName + " (")
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(player.name)
                    }
                    append(")")
                }
            }
        }
        ListItemPrimaryText(primaryText)
        if (player.username.isNotBlank()) {
            ListItemSecondaryText(player.username)
        }
        ListItemSecondaryText(pluralStringResource(R.plurals.plays_suffix, player.playCount, player.playCount)) // TODO show wins when sorted that way
    }
}

@Preview
@Composable
private fun PlayerListItemPreview() {
    BggAppTheme {
        PlayerListItem(
            Player(
                name = "Chris",
                username = "ccomeaux",
                userFullName = "Mr. Chris Comeaux"
            )
        )
    }
}
