@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.model.User
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BuddiesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Buddies")
            }
        }

        setContent {
            BggAppTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                val snackbarHostState = remember { SnackbarHostState() }

                val viewModel: BuddiesViewModel = viewModel()
                val buddies by viewModel.buddiesByHeader.observeAsState()
                val sortBy by viewModel.sort.observeAsState(User.SortType.USERNAME)
                val syncBuddies by viewModel.syncBuddies.observeAsState(false)
                val isRefreshing by viewModel.refreshing.observeAsState(false)
                val errorMessage by viewModel.error.observeAsState()

                LaunchedEffect(errorMessage) {
                    errorMessage?.getContentIfNotHandled()?.let {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(it.ifBlank { getString(R.string.msg_error_buddies) })
                        }
                    }
                }

                Drawer(
                    selectedItem = DrawerItem.Buddies,
                    drawerState = drawerState,
                ) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            BuddiesTopBar(
                                buddyCount = buddies?.values?.sumOf { it.size } ?: 0,
                                sortBy = sortBy,
                                onSortClick = { viewModel.sort(it) },
                                onMenuClick = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                scrollBehavior = scrollBehavior,
                            )
                        },
                        snackbarHost = { ErrorSnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        BuddiesContent(
                            buddies,
                            isSetToSyncBuddies = syncBuddies ?: false,
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            contentPadding = contentPadding,
                            onItemClick = { buddy -> BuddyActivity.start(context, buddy.username, buddy.fullName) },
                            onEnableSyncClick = { viewModel.enableSyncing() },
                        )
                    }
                }
            }
        }
    }
}

private enum class BuddiesSort(
    val type: User.SortType,
    @StringRes val labelResId: Int,
) {
    Username(User.SortType.USERNAME, R.string.menu_sort_username),
    FirstName(User.SortType.FIRST_NAME, R.string.menu_sort_first_name),
    LastName(User.SortType.LAST_NAME, R.string.menu_sort_last_name),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BuddiesTopBar(
    buddyCount: Int,
    sortBy: User.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onMenuClick: () -> Unit = {},
    onSortClick: (User.SortType) -> Unit = {}
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = { Text(stringResource(R.string.title_buddies)) },
        subtitle = {
            if (buddyCount > 0) {
                BuddiesSort.entries.find { it.type == sortBy }?.let {
                    Text(stringResource(R.string.count_by_sort, buddyCount, stringResource(it.labelResId)))
                }
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onMenuClick() }) {
                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.navigation_drawer))
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
                BuddiesSort.entries.forEach {
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
private fun BuddiesContent(
    buddies: Map<String, List<User>>?,
    isSetToSyncBuddies: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onItemClick: (buddy: User) -> Unit = {},
    onEnableSyncClick: () -> Unit = {},
) {
    when {
        buddies == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                LoadingIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .padding(dimensionResource(R.dimen.padding_extra))
                )
            }
        }
        buddies.isEmpty() -> {
            Empty(isSetToSyncBuddies, modifier, contentPadding, onEnableSyncClick)
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier.padding(contentPadding),
            ) {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                ) {
                    buddies.forEach { (headerText, buddies) ->
                        stickyHeader {
                            ListHeader(headerText)
                        }
                        itemsIndexed(
                            items = buddies,
                            key = { _, buddy -> buddy.username }
                        ) { index, buddy ->
                            UserListItem(
                                buddy,
                                modifier = modifier.animateItem(),
                                onClick = { onItemClick(buddy) }
                            )
                            if (index < buddies.lastIndex)
                                HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Empty(
    isSetToSyncBuddies: Boolean,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onEnableSyncClick: () -> Unit
) {
    if (isSetToSyncBuddies) {
        EmptyContent(
            R.string.empty_buddies,
            Icons.Outlined.Person,
            Modifier
                .padding(contentPadding)
                .fillMaxSize()
        )
    } else {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(108.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.empty_buddies_sync_off),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onEnableSyncClick() },
            ) {
                Text(text = stringResource(R.string.sync))
            }
        }
    }
}

@Preview
@Composable
private fun BuddiesContentPreview() {
    BggAppTheme {
        BuddiesContent(
            mapOf(
                "A" to listOf(
                    User(
                        username = "aldie",
                        firstName = "Scott",
                        lastName = "Alden",
                        avatarUrl = "",
                        playNickname = "Aldie",
                        updatedTimestamp = System.currentTimeMillis(),
                    )
                )
            ),
            isSetToSyncBuddies = false,
            isRefreshing = true,
            onRefresh = {}
        )
    }
}

@Composable
private fun UserListItem(
    buddy: User,
    modifier: Modifier = Modifier,
    onClick: (User) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(ListItemDefaults.tallPaddingValues)
            .clickable(onClick = { onClick(buddy) })
    ) {
        ListItemAvatar(buddy.avatarUrl)
        Column {
            if (buddy.fullName.isNotBlank()) {
                ListItemPrimaryText(buddy.fullName)
                ListItemSecondaryText(buddy.username)
            } else {
                ListItemPrimaryText(buddy.username)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun UserListItemPreview(
    @PreviewParameter(BuddyPreviewParameterProvider::class) person: User
) {
    BggAppTheme {
        UserListItem(person)
    }
}

private class BuddyPreviewParameterProvider : PreviewParameterProvider<User> {
    override val values = sequenceOf(
        User(
            username = "ccomeaux",
            firstName = "Chris",
            lastName = "Comeaux",
            avatarUrl = "",
            playNickname = "Chris",
            updatedTimestamp = System.currentTimeMillis(),
            isBuddy = true,
        ),
        User(
            username = "aldie",
            firstName = "Scott",
            lastName = "Alden",
            avatarUrl = "",
            playNickname = "Aldie",
            updatedTimestamp = System.currentTimeMillis(),
            isBuddy = true,
        ),
        User(
            username = "cberg",
            firstName = "",
            lastName = "",
            avatarUrl = "",
            playNickname = "Craig",
            updatedTimestamp = System.currentTimeMillis(),
            isBuddy = true,
        ),
    )
}
