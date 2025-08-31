@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.*
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.PlaysSummaryViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaysSummaryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlaysSummary")
            }
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val snackbarHostState = remember { SnackbarHostState() }

            val viewModel by viewModels<PlaysSummaryViewModel>()
            val isSyncing by viewModel.isSyncing.observeAsState(false)
            val syncPlays by viewModel.syncPlays.observeAsState(false)
            val syncPlaysTimestamp by viewModel.syncPlaysTimestamp.observeAsState(0L)
            val playCount by viewModel.playCount.observeAsState(0)
            val plays by viewModel.playsNotInProgress.observeAsState()
            val playsInProgress by viewModel.playsInProgress.observeAsState()
            val players by viewModel.players.observeAsState()
            val locations by viewModel.locations.observeAsState()
            val colors by viewModel.colors.observeAsState()
            val hIndex by viewModel.hIndex.observeAsState()
            val oldestSyncDate by viewModel.oldestSyncDate.observeAsState(Long.MAX_VALUE)
            val newestSyncDate by viewModel.newestSyncDate.observeAsState(0L)
            val errorMessage by viewModel.errorMessage.observeAsState()

            LaunchedEffect(errorMessage) {
                errorMessage?.getContentIfNotHandled()?.let {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it.ifBlank { getString(R.string.msg_error_buddies) }) // TODO
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
                            PlaysSummaryTopBar(
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                onRefreshClick = { viewModel.reset() },
                            )
                        },
                        snackbarHost = { ErrorSnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        PlaysSummaryScreen(
                            isSyncing = isSyncing,
                            syncPlays = syncPlays,
                            syncPlaysTimestamp = syncPlaysTimestamp,
                            playCount = playCount,
                            plays = plays,
                            playsInProgress = playsInProgress.orEmpty(),
                            players = players,
                            locations = locations,
                            colors = colors,
                            hIndex = hIndex,
                            oldestSyncDate = oldestSyncDate,
                            newestSyncDate = newestSyncDate,
                            modifier = Modifier.padding(contentPadding),
                            onRefresh = { viewModel.refresh() },
                            onSyncClick = { viewModel.enableSyncing(true) },
                            onCancelClick = { viewModel.enableSyncing(false) },
                            onMorePlaysClick = { startActivity<PlaysActivity>() },
                            onPlayItemClick = { PlayActivity.start(this, it.internalId) },
                            onMorePlayersClick = { startActivity<PlayersActivity>() },
                            onPlayerItemClick = { BuddyActivity.start(this, it.username, it.name) },
                            onMoreLocationsClick = { startActivity<LocationsActivity>() },
                            onLocationItemClick = { LocationActivity.start(this, it.name) },
                        )
                    }
                }
            }
        }
    }
}

private fun Long.asDate(context: Context) = this.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)

@Composable
private fun PlaysSummaryTopBar(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
) {
    var openAlertDialog by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.title_plays)) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { onMenuClick() }) {
                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.navigation_drawer))
            }
        },
        actions = {
            IconButton(onClick = {
                openAlertDialog = true
            }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.re_sync))
            }
        }
    )
    if (openAlertDialog) {
        AreYouSureDialog(
            onDismissRequest = { openAlertDialog = false },
            onConfirmation = {
                openAlertDialog = false
                onRefreshClick()
            }
        )
    }
}

@Composable
private fun PlaysSummaryScreen(
    isSyncing: Boolean,
    syncPlays: Boolean?,
    syncPlaysTimestamp: Long,
    playCount: Int,
    plays: List<Play>?,
    playsInProgress: List<Play>,
    players: List<Player>?,
    locations: List<Location>?,
    colors: List<PlayerColor>?,
    hIndex: HIndex?,
    oldestSyncDate: Long,
    newestSyncDate: Long,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onMorePlaysClick: () -> Unit = {},
    onPlayItemClick: (Play) -> Unit = {},
    onMorePlayersClick: () -> Unit = {},
    onPlayerItemClick: (Player) -> Unit = {},
    onMoreLocationsClick: () -> Unit = {},
    onLocationItemClick: (Location) -> Unit = {},
) {
    BggAppTheme {
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                        vertical = dimensionResource(R.dimen.material_margin_vertical)
                    ),
            ) {
                AnimatedVisibility(syncPlays == false && syncPlaysTimestamp == 0L) {
                    SyncCard(
                        onSyncClick = onSyncClick,
                        onCancelClick = onCancelClick,
                    )
                    SegmentSpacer()
                }
                PlaysSegment(
                    playCount,
                    plays = plays,
                    playsInProgress = playsInProgress,
                    onMoreClick = onMorePlaysClick,
                    onItemClick = { onPlayItemClick(it) }
                )
                PlayersSegment(
                    players = players,
                    onMoreClick = onMorePlayersClick,
                    onItemClick = onPlayerItemClick,
                )
                LocationsSegment(
                    locations = locations,
                    onMoreClick = onMoreLocationsClick,
                    onItemClick = onLocationItemClick,
                )
                ColorsSegment(
                    colors = colors,
                    username = LocalContext.current.preferences()[AccountPreferences.KEY_USERNAME, ""].orEmpty()
                )
                hIndex?.let {
                    StatsSegment(hIndex = it)
                }
                val context = LocalContext.current
                val statusMessage = when {
                    oldestSyncDate == Long.MAX_VALUE && newestSyncDate <= 0L -> stringResource(R.string.plays_sync_status_none)
                    oldestSyncDate <= 0L -> String.format(stringResource(R.string.plays_sync_status_new), newestSyncDate.asDate(context))
                    newestSyncDate <= 0L -> String.format(stringResource(R.string.plays_sync_status_old), oldestSyncDate.asDate(context))
                    else -> String.format(
                        stringResource(R.string.plays_sync_status_range),
                        oldestSyncDate.asDate(context),
                        newestSyncDate.asDate(context)
                    )
                }
                Text(statusMessage, style = MaterialTheme.typography.labelSmall)
                SegmentSpacer()
            }
        }
    }
}

@Composable
private fun SyncCard(
    modifier: Modifier = Modifier,
    onSyncClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.msg_play_sync))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(onClick = onCancelClick) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onSyncClick) {
                    Text(stringResource(R.string.sync))
                }
            }
        }
    }
}

@Composable
private fun PlaysSegment(
    playCount: Int,
    plays: List<Play>?,
    playsInProgress: List<Play>,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onItemClick: (Play) -> Unit = {},
) {
    AnimatedVisibility(plays != null && plays.isNotEmpty()) {
        Column {
            SegmentHeader(
                titleResId = R.string.title_plays,
                count = playCount,
                modifier = modifier,
                onMoreClick = onMoreClick
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (playsInProgress.isNotEmpty()) {
                    ListHeader(R.string.title_in_progress)
                    playsInProgress.forEachIndexed { index, play ->
                        if (index > 0)
                            HorizontalDivider()
                        SimpleListItem(
                            title = play.gameName,
                            text = play.describe(LocalContext.current, false),
                        ) {
                            onItemClick(play)
                        }
                    }
                    ListHeader(R.string.title_recent)
                }
                plays?.forEachIndexed { index, play ->
                    if (index > 0)
                        HorizontalDivider()
                    SimpleListItem(
                        title = play.gameName,
                        text = play.describe(LocalContext.current, false),
                    ) {
                        onItemClick(play)
                    }
                }
            }
            SegmentSpacer()
        }
    }
}

@Composable
private fun PlayersSegment(
    players: List<Player>?,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onItemClick: (Player) -> Unit = {},
) {
    AnimatedVisibility(players != null && players.isNotEmpty()) {
        Column {
            SegmentHeader(
                titleResId = R.string.title_players,
                modifier = modifier,
                onMoreClick = onMoreClick
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                players?.forEachIndexed { index, player ->
                    if (index > 0)
                        HorizontalDivider()
                    SimpleListItem(
                        title = player.description,
                        text = pluralStringResource(R.plurals.plays_suffix, player.playCount, player.playCount)
                    ) {
                        onItemClick(player)
                    }
                }
            }
            SegmentSpacer()
        }
    }
}

@Composable
private fun LocationsSegment(
    locations: List<Location>?,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onItemClick: (Location) -> Unit = {},
) {
    AnimatedVisibility(locations != null && locations.isNotEmpty()) {
        Column {
            SegmentHeader(
                titleResId = R.string.title_locations,
                modifier = modifier,
                onMoreClick = onMoreClick,
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                locations?.forEachIndexed { index, location ->
                    if (index > 0)
                        HorizontalDivider()
                    SimpleListItem(
                        title = location.name,
                        text = pluralStringResource(R.plurals.plays_suffix, location.playCount, location.playCount)
                    ) {
                        onItemClick(location)
                    }
                }
            }
            SegmentSpacer()
        }
    }
}

@Composable
private fun ColorsSegment(colors: List<PlayerColor>?, username: String, modifier: Modifier = Modifier) {
    Column {
        val context = LocalContext.current
        SegmentHeader(
            titleResId = R.string.title_favorite_colors,
            moreResId = R.string.edit,
            onMoreClick = {
                if (username.isNotBlank()) {
                    PlayerColorsActivity.start(context, username, null)
                }
            }
        )
        AnimatedVisibility(colors != null && !colors.isEmpty()) {
            ElevatedCard {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    colors?.forEach {
                        ColorBox(it.description, size = ColorBoxDefaults.sizeSmall) { foregroundColor ->
                            Text(
                                text = it.sortOrder.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = foregroundColor,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
        SegmentSpacer()
    }
}

@Composable
private fun StatsSegment(hIndex: HIndex, modifier: Modifier = Modifier) {
    Column {
        val context = LocalContext.current
        SegmentHeader(
            titleResId = R.string.title_play_stats,
            onMoreClick = {
                context.startActivity<PlayStatsActivity>()
            }
        )
        ElevatedCard {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.play_stat_game_h_index))
                Text(text = hIndex.description, fontWeight = FontWeight.Bold)
            }
        }
        SegmentSpacer()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SegmentHeader(
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    @StringRes moreResId: Int = R.string.more,
    count: Int = 0,
    onMoreClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(titleResId),
            style = MaterialTheme.typography.titleLarge,
        )
        val size = ButtonDefaults.ExtraSmallContainerHeight
        Button(
            onClick = onMoreClick,
            modifier = Modifier
                .heightIn(size)
                .widthIn(72.dp),
            contentPadding = ButtonDefaults.contentPaddingFor(size),
        ) {
            val text = if (count > 0) "$count ${stringResource(moreResId)}" else stringResource(moreResId)
            Text(text, style = ButtonDefaults.textStyleFor(size))
        }
    }
}

@Composable
private fun ListHeader(@StringRes titleResId: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(titleResId),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(ListItemDefaults.paddingValues),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun AreYouSureDialog(
    modifier: Modifier = Modifier,
    onConfirmation: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = onConfirmation) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(text = stringResource(R.string.pref_sync_re_sync_plays)) },
        text = {
            Text(text = stringResource(R.string.pref_sync_re_sync_plays_info_message))
        },
        modifier = modifier,
    )
}

@Composable
private fun SegmentSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun PlaysSegmentPreview() {
    BggAppTheme {
        PlaysSegment(
            100,
            plays = listOf(
                Play(gameName = "Terra Mystica", gameId = 13, dateInMillis = System.currentTimeMillis(), location = "House"),
                Play(gameName = "Res Arcana", gameId = 42, dateInMillis = System.currentTimeMillis(), location = "Game Store"),
            ),
            playsInProgress = listOf(
                Play(gameName = "Terra Mystica", gameId = 13, dateInMillis = System.currentTimeMillis(), location = "House"),
                Play(gameName = "Res Arcana", gameId = 42, dateInMillis = System.currentTimeMillis(), location = "Game Store"),
            ),
        )
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun LocationsSegmentPreview() {
    BggAppTheme {
        LocationsSegment(
            locations = listOf(
                Location("House", 10),
                Location("Game Store", 4),
            ),
        )
    }
}

@Preview
@Composable
private fun ColorsSegmentPreview() {
    BggAppTheme {
        ColorsSegment(
            colors = BggColors.standardColorList.mapIndexed { index, color -> PlayerColor(color.first, index + 1) },
            "ccomeaux",
        )
    }
}

@Preview
@Composable
private fun SyncCardPreview() {
    BggAppTheme {
        SyncCard()
    }
}

@Composable
private fun SimpleListItem(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(title)
        ListItemSecondaryText(text)
    }
}

@Preview
@Composable
private fun SimpleListItemPreview() {
    BggAppTheme {
        SimpleListItem("Title", "Supporting text")
    }
}
