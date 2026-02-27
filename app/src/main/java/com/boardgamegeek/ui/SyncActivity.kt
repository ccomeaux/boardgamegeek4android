@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.ui.compose.Drawer
import com.boardgamegeek.ui.compose.DrawerItem
import com.boardgamegeek.ui.compose.EmptyFullSizeScrollableContent
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.SyncViewModel
import com.boardgamegeek.ui.viewmodel.SyncViewModel.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SyncActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Sync")
            }
        }

        setContent {
            val viewModel: SyncViewModel = viewModel()
            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val username = viewModel.username.observeAsState()

            val collectionCompleteCurrentTimestamp = viewModel.collectionCompleteCurrentTimestamp.observeAsState()
            val collectionCompleteTimestamp = viewModel.collectionCompleteTimestamp.observeAsState()
            val collectionPartialTimestamp = viewModel.collectionPartialTimestamp.observeAsState()
            val numberOfUnsyncedGames = viewModel.numberOfUnsyncedGames.observeAsState()
            val collectionSyncProgress = viewModel.collectionSyncProgress.observeAsState()
            val numberOfCollectionItemsToUpload = viewModel.numberOfCollectionItemsToUpload.observeAsState()
            val syncCollectionStatuses = viewModel.syncCollectionStatuses.observeAsState()
            val collectionStatusCompleteTimestamps = viewModel.collectionStatusCompleteTimestamps.observeAsState()

            val playsEnabled = viewModel.syncPlays.observeAsState()
            val playsSyncState = viewModel.playSyncState.observeAsState()
            val playProgress = viewModel.playSyncProgress.observeAsState()
            val numberOfPlaysToBeUpdated = viewModel.numberOfPlaysToBeUpdated.observeAsState(0)
            val numberOfPlaysToBeDeleted = viewModel.numberOfPlaysToBeDeleted.observeAsState(0)

            val usersEnabled = viewModel.syncBuddies.observeAsState()
            val usersSyncDate = viewModel.buddySyncDate.observeAsState()
            val userSyncState = viewModel.userSyncState.observeAsState()
            val userProgress = viewModel.userProgress.observeAsState()

            BggAppTheme {
                Drawer(
                    selectedItem = DrawerItem.Sync,
                    drawerState = drawerState,
                ) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            SyncTopAppBar(
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    ) { contentPadding ->
                        if (username.value.isNullOrBlank()) {
                            EmptyFullSizeScrollableContent(
                                R.string.msg_sync_unauthed,
                                painterResource(R.drawable.sync_24px),
                                padding = contentPadding + PaddingValues(
                                    horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                                    vertical = dimensionResource(R.dimen.material_margin_vertical),
                                ),
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                            ) {
                                val pagerState = rememberPagerState(
                                    initialPage = SyncTab.Collection.ordinal,
                                    pageCount = { SyncTab.entries.size },
                                )
                                val coroutineScope = rememberCoroutineScope()
                                SyncTabRow(
                                    selectedDestination = pagerState.currentPage,
                                    onClick = { newDestination ->
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(newDestination)
                                        }
                                    },
                                )
                                HorizontalPager(
                                    state = pagerState,
                                ) { page ->
                                    when (page) {
                                        SyncTab.Collection.ordinal -> SyncCollectionScreen(
                                            collectionCompleteCurrentTimestamp = collectionCompleteCurrentTimestamp.value,
                                            collectionCompleteTimestamp = collectionCompleteTimestamp.value,
                                            collectionPartialTimestamp = collectionPartialTimestamp.value,
                                            numberOfUnsyncedGames = numberOfUnsyncedGames.value,
                                            collectionSyncProgress = collectionSyncProgress.value,
                                            numberOfCollectionItemsToUpload = numberOfCollectionItemsToUpload.value,
                                            syncCollectionStatuses = syncCollectionStatuses.value,
                                            collectionStatusCompleteTimestamps = collectionStatusCompleteTimestamps.value,
                                            onSyncClick = { viewModel.syncCollection() },
                                            onCancelClick = { viewModel.cancelCollection() },
                                            onUploadClick = { viewModel.uploadCollection() },
                                            onStatusEnabledChange = { status, enabled -> viewModel.modifyCollectionStatus(status, enabled) },
                                            onStatusSyncClick = { viewModel.syncCollection(it) }
                                        )
                                        SyncTab.Plays.ordinal -> SyncPlaysScreen(
                                            enabled = playsEnabled.value ?: false,
                                            playSyncState = playsSyncState.value,
                                            playProgress = playProgress.value,
                                            numberOfPlaysToBeUpdated = numberOfPlaysToBeUpdated.value,
                                            numberOfPlaysToBeDeleted = numberOfPlaysToBeDeleted.value,
                                            onEnabledChange = { viewModel.enablePlaysSync(it) },
                                            onSyncClick = { viewModel.syncPlays() },
                                            onCancelClick = { viewModel.cancelPlays() },
                                            onUploadClick = { viewModel.uploadPlays() },
                                        )
                                        SyncTab.Users.ordinal -> SyncUsersScreen(
                                            enabled = usersEnabled.value ?: false,
                                            syncDate = usersSyncDate.value,
                                            userSyncState = userSyncState.value,
                                            userProgress = userProgress.value,
                                            onEnabledChange = { viewModel.enableUserSync(it) },
                                            onSyncClick = { viewModel.syncBuddies() },
                                            onCancelClick = { viewModel.cancelBuddies() }
                                        )

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncTopAppBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.title_sync)) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onMenuClick() }) {
                Icon(painterResource(R.drawable.menu_24px), stringResource(R.string.navigation_drawer))
            }
        },
    )
}

private enum class SyncTab(@param:StringRes val resId: Int, @param:DrawableRes val iconResId: Int) {
    Collection(R.string.title_collection, R.drawable.collection_24px),
    Plays(R.string.title_plays, R.drawable.plays_24px),
    Users(R.string.title_users, R.drawable.person_24px),
}

@Composable
private fun SyncTabRow(selectedDestination: Int, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = selectedDestination,
        modifier = modifier,
    ) {
        SyncTab.entries.forEachIndexed { index, tab ->
            Tab(
                selected = selectedDestination == index,
                onClick = { onClick(index) },
                text = {
                    Text(
                        text = stringResource(tab.resId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                icon = { Icon(painterResource(tab.iconResId), null) },
            )
        }
    }
}

@Composable
private fun SyncCollectionScreen(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    collectionCompleteCurrentTimestamp: Long? = null,
    collectionCompleteTimestamp: Long? = null,
    collectionPartialTimestamp: Long? = null,
    numberOfUnsyncedGames: Int? = null,
    collectionSyncProgress: CollectionSyncProgress? = null,
    numberOfCollectionItemsToUpload: Int? = null,
    syncCollectionStatuses: Set<CollectionStatus>? = null,
    collectionStatusCompleteTimestamps: Map<Pair<CollectionStatus, Game.Subtype>, Long>? = null,
    onSyncClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onStatusEnabledChange: (CollectionStatus, Boolean) -> Unit = { _, _ -> },
    onStatusSyncClick: (CollectionStatus) -> Unit = { }
) {
    val isSyncing = collectionSyncProgress != null && collectionSyncProgress.step != CollectionSyncProgressStep.NotSyncing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        if ((collectionCompleteTimestamp == null || collectionCompleteTimestamp == 0L) &&
            (collectionPartialTimestamp == null || collectionPartialTimestamp == 0L)
        ) {
            ProgressText(stringResource(id = R.string.collection_unsynced))
        } else {
            TimestampWithLabel(collectionCompleteCurrentTimestamp, stringResource(id = R.string.currently_syncing))
            TimestampWithLabel(collectionCompleteTimestamp, stringResource(id = R.string.complete))
            TimestampWithLabel(collectionPartialTimestamp, stringResource(id = R.string.partial))
            numberOfUnsyncedGames?.let {
                val text = if (it == 0)
                    stringResource(R.string.games_pending_download_zero)
                else
                    pluralStringResource(R.plurals.games_pending_download, it, it)
                ProgressTextVariant(text)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                enabled = isSyncing
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSyncClick,
                enabled = !isSyncing
            ) {
                Icon(painterResource(id = R.drawable.sync_24px), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.sync))
            }
        }

        collectionSyncProgress?.let {
            if (isSyncing) {
                val subtype = stringResource(
                    when (it.subtype) {
                        CollectionSyncProgressSubtype.None -> R.string.items
                        CollectionSyncProgressSubtype.All -> R.string.games_expansions
                        CollectionSyncProgressSubtype.Accessory -> R.string.accessories
                    }
                )
                val statusDescription = stringResource(getStatusDescriptionResId(it.status))
                ProgressText(
                    when (it.step) {
                        CollectionSyncProgressStep.CompleteCollection -> {
                            if (statusDescription.isBlank()) {
                                stringResource(R.string.sync_complete_collection, subtype)
                            } else {
                                stringResource(R.string.sync_complete_collection_status, statusDescription, subtype)
                            }
                        }
                        CollectionSyncProgressStep.PartialCollection -> stringResource(R.string.sync_partial_collection, subtype)
                        CollectionSyncProgressStep.StaleCollection -> stringResource(R.string.sync_stale_collection, subtype)
                        CollectionSyncProgressStep.DeleteCollection -> stringResource(R.string.sync_delete_collection, subtype)
                        CollectionSyncProgressStep.RemoveGames -> stringResource(R.string.sync_remove_games)
                        CollectionSyncProgressStep.StaleGames -> stringResource(R.string.sync_stale_games)
                        CollectionSyncProgressStep.NewGames -> stringResource(R.string.sync_new_games)
                        else -> ""
                    }
                )
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    wavelength = 50.dp,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        numberOfCollectionItemsToUpload?.let {
            ProgressText(text = pluralStringResource(R.plurals.items_pending_upload, it, it))
            Button(
                onClick = onUploadClick,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally),
                enabled = (numberOfCollectionItemsToUpload > 0)
            ) {
                Icon(painterResource(R.drawable.upload_24px), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(id = R.string.upload))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        Text(
            text = stringResource(id = R.string.title_statuses),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall
        )

        val statuses = listOf(
            CollectionStatus.Own,
            CollectionStatus.PreviouslyOwned,
            CollectionStatus.ForTrade,
            CollectionStatus.WantInTrade,
            CollectionStatus.WantToBuy,
            CollectionStatus.WantToPlay,
            CollectionStatus.Preordered,
            CollectionStatus.Wishlist,
            CollectionStatus.Played,
            CollectionStatus.Rated,
            CollectionStatus.Commented,
            CollectionStatus.HasParts,
            CollectionStatus.WantParts,
        )
        statuses.forEachIndexed { index, status ->
            if (index > 0) HorizontalDivider()
            val isChecked //by remember(status) { derivedStateOf { syncCollectionStatuses?.contains(status) == true } }
                    = syncCollectionStatuses?.contains(status) ?: false
            CollectionStatusRow(
                isChecked,
                status,
                collectionStatusCompleteTimestamps,
                collectionSyncProgress,
                isSyncing,
                onStatusEnabledChange,
                onStatusSyncClick,
            )
        }
    }
}

@Composable
private fun CollectionStatusRow(
    isChecked: Boolean,
    status: CollectionStatus,
    collectionStatusCompleteTimestamps: Map<Pair<CollectionStatus, Game.Subtype>, Long>?,
    collectionSyncProgress: CollectionSyncProgress?,
    isSyncing: Boolean,
    onStatusEnabledChange: (CollectionStatus, Boolean) -> Unit,
    onStatusSyncClick: (CollectionStatus) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = getStatusDescriptionResId(status)),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = isChecked,
                onCheckedChange = { isChecked ->
                    onStatusEnabledChange(status, isChecked)
                }
            )
        }
        if (isChecked) {
            collectionStatusCompleteTimestamps?.let { timestamp ->
                timestamp[Pair(status, Game.Subtype.Unknown)]?.let {
                    TimestampWithLabelVariant(it, stringResource(id = R.string.games_expansions))
                }
                timestamp[Pair(status, Game.Subtype.BoardGameAccessory)]?.let {
                    TimestampWithLabelVariant(it, stringResource(id = R.string.accessories))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (collectionSyncProgress?.status == status)
                    LinearWavyProgressIndicator(
                        modifier = Modifier.weight(1f),
                        wavelength = 10.dp,
                    )
                else
                    Box(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { onStatusSyncClick(status) },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .padding(start = 24.dp)
                ) {
                    Text(text = stringResource(id = R.string.sync))
                }
            }
        }
    }
}

@Composable
private fun getStatusDescriptionResId(status: CollectionStatus): Int = when (status) {
    CollectionStatus.Own -> R.string.collection_status_own
    CollectionStatus.PreviouslyOwned -> R.string.collection_status_prev_owned
    CollectionStatus.Preordered -> R.string.collection_status_preordered
    CollectionStatus.ForTrade -> R.string.collection_status_for_trade
    CollectionStatus.WantInTrade -> R.string.collection_status_want_in_trade
    CollectionStatus.WantToBuy -> R.string.collection_status_want_to_buy
    CollectionStatus.WantToPlay -> R.string.collection_status_want_to_play
    CollectionStatus.Wishlist -> R.string.collection_status_wishlist
    CollectionStatus.Played -> R.string.collection_status_played
    CollectionStatus.Rated -> R.string.collection_status_rated
    CollectionStatus.Commented -> R.string.collection_status_commented
    CollectionStatus.HasParts -> R.string.collection_status_has_parts
    CollectionStatus.WantParts -> R.string.collection_status_want_parts
    CollectionStatus.Unknown -> R.string.unknown
}

@Preview(showBackground = true)
@Composable
private fun SyncCollectionScreenPreview() {
    SyncCollectionScreen(
        modifier = Modifier.padding(16.dp),
        collectionCompleteCurrentTimestamp = 999999999999L,
        collectionCompleteTimestamp = 888888888888L,
        collectionPartialTimestamp = 899999999999L,
        numberOfUnsyncedGames = 100,
        collectionSyncProgress = CollectionSyncProgress(
            step = CollectionSyncProgressStep.CompleteCollection,
            subtype = CollectionSyncProgressSubtype.All,
            status = CollectionStatus.ForTrade,
        ),
        numberOfCollectionItemsToUpload = 7,
        syncCollectionStatuses = setOf(CollectionStatus.Own, CollectionStatus.ForTrade),
        collectionStatusCompleteTimestamps = mapOf(
            (CollectionStatus.Own to Game.Subtype.Unknown) to 1111111111111L,
            (CollectionStatus.Own to Game.Subtype.BoardGameAccessory) to 1111112222222L,
            (CollectionStatus.ForTrade to Game.Subtype.Unknown) to 0L,
        )
    )
}

@Composable
private fun SyncPlaysScreen(
    enabled: Boolean = true,
    playSyncState: PlaySyncState? = null,
    playProgress: PlaySyncProgress? = null,
    numberOfPlaysToBeUpdated: Int = 0,
    numberOfPlaysToBeDeleted: Int = 0,
    onEnabledChange: (Boolean) -> Unit = {},
    onSyncClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.sync_plays),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = enabled, onCheckedChange = { isChecked ->
                    onEnabledChange(isChecked)
                }
            )
        }

        playSyncState?.let {
            ProgressTextVariant(
                pluralStringResource(
                    when {
                        it.oldestSyncDate == Long.MAX_VALUE && it.newestSyncDate <= 0L -> R.plurals.plays_sync_status_none
                        it.oldestSyncDate <= 0L -> R.plurals.plays_sync_status_new
                        it.newestSyncDate <= 0L -> R.plurals.plays_sync_status_old
                        else -> R.plurals.plays_sync_status_range
                    },
                    it.size,
                    it.size,
                    it.oldestSyncDate.asDate(LocalContext.current),
                    it.newestSyncDate.asDate(LocalContext.current),
                )
            )
        }

        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    enabled = playProgress?.step != PlaySyncProgressStep.NotSyncing
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSyncClick,
                    enabled = playProgress?.step == PlaySyncProgressStep.NotSyncing
                ) {
                    Icon(painterResource(id = R.drawable.sync_24px), contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.sync))
                }
            }
        }

        playProgress?.let {
            val text = when (it.step) {
                PlaySyncProgressStep.NotSyncing -> ""
                PlaySyncProgressStep.New -> stringResource(R.string.sync_plays_step_new).appendPage(it.page)
                PlaySyncProgressStep.Old -> stringResource(R.string.sync_plays_step_old).appendPage(it.page)
                PlaySyncProgressStep.Stats -> stringResource(R.string.sync_plays_step_stats)
            }
            if (text.isNotBlank()) {
                ProgressText(text)
            }
            if (it.step == PlaySyncProgressStep.New || it.step == PlaySyncProgressStep.Old) {
                ProgressTextVariant(
                    getSyncDateDescription(it)
                )
            }
            val resId = when (it.action) {
                PlaySyncProgressAction.None -> ResourcesCompat.ID_NULL
                PlaySyncProgressAction.Waiting -> R.string.sync_plays_action_waiting
                PlaySyncProgressAction.Downloading -> R.string.sync_plays_action_downloading
                PlaySyncProgressAction.Saving -> R.string.sync_plays_action_saving
                PlaySyncProgressAction.Deleting -> R.string.sync_plays_action_deleting
            }
            if (resId != ResourcesCompat.ID_NULL) {
                ProgressTextVariant(
                    stringResource(
                        resId
                    )
                )
            }
            if (it.step != PlaySyncProgressStep.NotSyncing) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        }

        if (numberOfPlaysToBeUpdated > 0 || numberOfPlaysToBeDeleted > 0) {
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            if (numberOfPlaysToBeUpdated > 0) {
                ProgressTextVariant(pluralStringResource(R.plurals.plays_pending_update, numberOfPlaysToBeUpdated, numberOfPlaysToBeUpdated))
            }
            if (numberOfPlaysToBeDeleted > 0) {
                ProgressTextVariant(pluralStringResource(R.plurals.plays_pending_deletion, numberOfPlaysToBeDeleted, numberOfPlaysToBeDeleted))
            }
            OutlinedButton(
                onClick = onUploadClick,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(painterResource(id = R.drawable.upload_24px), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.upload_plays))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncPlaysScreenPreview() {
    SyncPlaysScreen(
        enabled = true,
        playSyncState = PlaySyncState(
            1234123412345L,
            1256523992360L,
            100,
        ),
        playProgress = PlaySyncProgress(
            step = PlaySyncProgressStep.Old,
            minDate = 1234123412345L,
            page = 2,
            action = PlaySyncProgressAction.Downloading,
        ),
        numberOfPlaysToBeUpdated = 2,
        numberOfPlaysToBeDeleted = 3,
    )
}

@Composable
private fun SyncUsersScreen(
    enabled: Boolean = true,
    syncDate: Long? = null,
    userSyncState: UserSyncState? = null,
    userProgress: UserSyncProgress? = null,
    onEnabledChange: (Boolean) -> Unit = {},
    onSyncClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = userProgress?.progress ?: Float.NaN,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.sync_users),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = enabled, onCheckedChange = { isChecked ->
                    onEnabledChange(isChecked)
                }
            )
        }

        syncDate?.let {
            ProgressText(
                text = if (it <= 0L) stringResource(R.string.sync_buddies_date_zero)
                else stringResource(R.string.sync_buddies_date, it.asDateTime(LocalContext.current)) // TODO - include how many are buddies?
            )
        }
        userSyncState?.let {
            Spacer(modifier = Modifier.height(8.dp))
            ProgressText(
                text = if (it.count == 0)
                    stringResource(R.string.users_synced_none)
                else pluralStringResource(
                    R.plurals.users_synced_total, it.count, it.count, it.oldestUpdatedUserTimestamp.asDateTime(LocalContext.current)
                )
            )
            if (it.numberOfUnupdatedUsers > 0) {
                ProgressTextVariant(
                    text = pluralStringResource(
                        R.plurals.users_unupdated_total,
                        it.numberOfUnupdatedUsers,
                        it.numberOfUnupdatedUsers,
                        it.oldestUpdatedUserTimestamp.asDateTime(LocalContext.current)
                    )
                )
            }
        }

        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    enabled = userProgress?.step != UserSyncProgressStep.NotSyncing
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSyncClick,
                    enabled = userProgress?.step == UserSyncProgressStep.NotSyncing
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sync_24px), contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.sync))
                }
            }
        }

        userProgress?.let {
            @StringRes val resId = when (it.step) {
                UserSyncProgressStep.NotSyncing -> ResourcesCompat.ID_NULL
                UserSyncProgressStep.BuddyList -> R.string.sync_user_step_list
                UserSyncProgressStep.StaleBuddies -> R.string.sync_user_step_stale_buddies
                UserSyncProgressStep.NewBuddies -> R.string.sync_user_step_unupdated_buddies
                UserSyncProgressStep.StalePlayers -> R.string.sync_user_step_stale_players
                UserSyncProgressStep.NewPlayers -> R.string.sync_user_step_unupdated_players
            }
            if (resId != ResourcesCompat.ID_NULL) {
                ProgressText(text = stringResource(resId))
                if (!it.username.isNullOrEmpty()) {
                    ProgressTextVariant(text = stringResource(R.string.sync_notification_user, it.username))
                }
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncUsersScreenPreview() {
    SyncUsersScreen(
        syncDate = 1234123412345L,
        userSyncState = UserSyncState(
            count = 1237,
            numberOfUnupdatedUsers = 456,
            1256523992360L,
        ),
        userProgress = UserSyncProgress(
            step = UserSyncProgressStep.NewPlayers,
            progress = 0.70f,
            username = "ccomeaux",
        ),
    )
}

@Composable
private fun ProgressText(text: String) {
    Text(
        text,
        modifier = Modifier.padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ProgressTextVariant(text: String) {
    Text(
        text,
        modifier = Modifier.padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun TimestampWithLabel(timestamp: Long?, label: String) {
    if (timestamp != null && timestamp > 0L) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(text = timestamp.asDateTime(LocalContext.current).toString())
        }
    }
}

@Composable
private fun TimestampWithLabelVariant(timestamp: Long?, label: String) {
    if (timestamp != null && timestamp > 0L) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = timestamp.asDateTime(LocalContext.current).toString())
        }
    }
}

@Composable
private fun Long?.asDateTime(context: Context): CharSequence {
    return this?.formatDateTime(
        context, flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    ) ?: stringResource(R.string.never)
}

@Composable
private fun Long.asDate(context: Context) = this.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)

@Composable
private fun String.appendPage(page: Int): String {
    val contentText = when {
        page > 1 -> stringResource(R.string.sync_notification_page_suffix, this, page)
        else -> this
    }
    return contentText
}

@Composable
private fun getSyncDateDescription(it: PlaySyncProgress) = when {
    it.minDate == 0L && it.maxDate == 0L -> stringResource(R.string.sync_notification_plays_all)
    it.minDate == 0L -> stringResource(R.string.sync_notification_plays_old, it.maxDate.asDate(LocalContext.current))
    it.maxDate == 0L -> stringResource(R.string.sync_notification_plays_new, it.minDate.asDate(LocalContext.current))
    else -> stringResource(R.string.sync_notification_plays_between, it.minDate.asDate(LocalContext.current), it.maxDate.asDate(LocalContext.current))
}

