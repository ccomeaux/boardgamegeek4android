@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.boardgamegeek.R
import com.boardgamegeek.extensions.createStatusMap
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.mappers.mapToEnum
import com.boardgamegeek.mappers.mapToPreference
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.BuddyActivity.Companion.startUp
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.BuddyCollectionViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BuddyCollectionActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra(KEY_USERNAME).orEmpty()

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
                param(FirebaseAnalytics.Param.ITEM_ID, username)
            }
        }

        setContent {
            val context = LocalContext.current
            val viewModel by viewModels<BuddyCollectionViewModel>()

            val collection by viewModel.collection.observeAsState(RefreshableResource.refreshing(null))
            val status by viewModel.status.observeAsState()
            viewModel.setUsername(username)

            BggAppTheme {
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                Scaffold(
                    topBar = {
                        BuddyCollectionTopBar(
                            username,
                            status,
                            !collection.data.isNullOrEmpty(),
                            scrollBehavior = scrollBehavior,
                            onUpClick = {
                                startUp(this, username)
                                finish()
                            },
                            onRandomClick = {
                                collection.data?.values?.flatten()?.random()?.let {
                                    GameActivity.start(context, it.gameId, it.gameName, it.thumbnailUrl)
                                }
                            },
                            onStatusClick = {
                                viewModel.setStatus(it)
                            },
                        )
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                ) { contentPadding ->
                    when (collection.status) {
                        Status.REFRESHING -> {
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
                        Status.ERROR -> {
                            ErrorContent(
                                collection.message,
                                Icons.AutoMirrored.Filled.LibraryBooks,
                                modifier = Modifier.padding(contentPadding),
                            )
                        }
                        Status.SUCCESS -> {
                            BuddyCollectionScreen(
                                collection.data.orEmpty(),
                                status = status,
                                contentPadding = contentPadding,)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_USERNAME = "USER_NAME"

        fun start(context: Context, username: String?) {
            context.startActivity<BuddyCollectionActivity>(KEY_USERNAME to username)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BuddyCollectionTopBar(
    username: String,
    status: CollectionStatus?,
    hasCollectionItems: Boolean,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onRandomClick: () -> Unit = {},
    onStatusClick: (CollectionStatus) -> Unit = {},
) {
    val statuses = LocalContext.current.createStatusMap()
    val statusDescription = status?.let { statuses[it.mapToPreference()] } ?: status.toString()
    MediumFlexibleTopAppBar(
        title = { Text(username) },
        subtitle = { Text(stringResource(R.string.title_collection) + " - $statusDescription") },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onUpClick() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.up))
            }
        },
        actions = {
            var expandedMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { expandedMenu = true }) {
                Icon(painterResource(R.drawable.ic_baseline_library_books_24), contentDescription = stringResource(R.string.menu_collection_status_))
            }
            IconButton(
                onClick = { onRandomClick() },
                enabled = hasCollectionItems,
            ) {
                Icon(painterResource(R.drawable.ic_baseline_casino_24), contentDescription = stringResource(R.string.menu_collection_random_game))
            }
            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                statuses.forEach {
                    DropdownMenuItem(
                        text = { Text(it.value) },
                        leadingIcon = {
                            RadioButton(
                                selected = (it.key == status?.mapToPreference()),
                                onClick = null
                            )
                        },
                        onClick = {
                            expandedMenu = false
                            onStatusClick(it.key.mapToEnum())
                        }
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun BuddyCollectionTopBarPreview() {
    BggAppTheme {
        BuddyCollectionTopBar("ccomeaux", CollectionStatus.Rated, false)
    }
}

@Composable
private fun BuddyCollectionScreen(
    collectionItems: Map<String, List<CollectionItem>>,
    status: CollectionStatus?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (collectionItems.isEmpty()) {
        val statuses = LocalContext.current.createStatusMap()
        val statusDescription = status?.let { statuses[it.mapToPreference()] } ?: status.toString()
        EmptyContent(
            stringResource(R.string.empty_buddy_collection, statusDescription),
            Icons.Outlined.Person,
            modifier
                .padding(contentPadding)
                .fillMaxSize()
        )
    } else {
        val context = LocalContext.current
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            collectionItems.forEach { (headerText, items) ->
                stickyHeader {
                    ListHeader(headerText)
                }
                items(
                    items,
                    key = { it.collectionId }
                ) { item ->
                    CollectionListItem(item) {
                        GameActivity.start(context, item.gameId, item.gameName)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionListItem(
    collectionItem: CollectionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(ListItemDefaults.paddingValues)
    ) {
        ListItemPrimaryText(collectionItem.gameName)
        ListItemSecondaryText(collectionItem.gameId.toString())
    }
}

@PreviewLightDark
@Composable
private fun CollectionListItemPreview(
    @PreviewParameter(CollectionItemPreviewParameterProvider::class) collectionItem: CollectionItem,
) {
    BggAppTheme {
        CollectionListItem(collectionItem, Modifier)
    }
}

private class CollectionItemPreviewParameterProvider : PreviewParameterProvider<CollectionItem> {
    override val values = sequenceOf(
        CollectionItem(
            gameId = 13,
            gameName = "CATAN",
            rank = 1,
        ),
    )
}
