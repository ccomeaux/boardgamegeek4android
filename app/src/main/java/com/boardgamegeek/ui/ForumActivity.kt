@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.boardgamegeek.R
import com.boardgamegeek.extensions.clearTop
import com.boardgamegeek.extensions.getSerializableCompat
import com.boardgamegeek.extensions.intentFor
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.ForumsActivity.Companion.startUp
import com.boardgamegeek.ui.GameActivity.Companion.startUp
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForArtist
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForDesigner
import com.boardgamegeek.ui.PersonActivity.Companion.startUpForPublisher
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.ForumViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForumActivity : BaseActivity() {
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var forumHeader = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            forumId = it.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getStringExtra(KEY_FORUM_TITLE).orEmpty()
            forumHeader = it.getStringExtra(KEY_FORUM_HEADER).orEmpty()
            objectId = it.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectType = it.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
            objectName = it.getStringExtra(KEY_OBJECT_NAME).orEmpty()
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Forum")
                param(FirebaseAnalytics.Param.ITEM_ID, forumId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, forumTitle)
            }
        }

        setContent {
            val viewModel: ForumViewModel = viewModel()
            viewModel.setForumId(forumId)

            val threads = viewModel.threads.collectAsLazyPagingItems()

            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
            BggAppTheme {
                Drawer {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            ForumTopAppBar(
                                forumTitle,
                                objectName,
                                forumHeader,
                                scrollBehavior = scrollBehavior,
                                onUpClick = {
                                    when (objectType) {
                                        Forum.Type.REGION -> startUp(this)
                                        Forum.Type.GAME -> startUp(this, objectId, objectName)
                                        Forum.Type.ARTIST -> startUpForArtist(this, objectId, objectName)
                                        Forum.Type.DESIGNER -> startUpForDesigner(this, objectId, objectName)
                                        Forum.Type.PUBLISHER -> startUpForPublisher(this, objectId, objectName)
                                    }
                                    finish()
                                },
                                onOpenInBrowser = { linkToBgg("forum", forumId) },
                            )
                        }
                    ) { contentPadding ->
                        when (threads.loadState.refresh) {
                            is LoadState.Loading -> {
                                BggLoadingIndicatorBox(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                )
                            }
                            is LoadState.Error -> {
                                ErrorContent(
                                    text = stringResource(R.string.empty_forum),
                                    painterResource(R.drawable.forum_24px),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                        .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal))
                                )
                            }
                            else -> {
                                if (threads.itemCount == 0) {
                                    EmptyContent(
                                        stringResource(R.string.empty_forum),
                                        painterResource(R.drawable.forum_24px),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(contentPadding),
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = contentPadding,
                                    ) {
                                        items(threads.itemCount) { position ->
                                            val thread = threads[position]
                                            if (thread != null) {
                                                val context = LocalContext.current
                                                ThreadListItem(
                                                    thread = thread,
                                                    onClick = {
                                                        ThreadActivity.start(
                                                            context,
                                                            thread.threadId,
                                                            thread.subject,
                                                            forumId,
                                                            forumTitle,
                                                            forumHeader,
                                                            objectId,
                                                            objectName,
                                                            objectType
                                                        )
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
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_FORUM_HEADER = "FORUM_HEADER"

        fun start(
            context: Context,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type,
            forumHeader: String
        ) {
            context.startActivity(createIntent(context, forumId, forumTitle, objectId, objectName, objectType, forumHeader))
        }

        fun startUp(
            context: Context,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type,
            forumHeader: String
        ) {
            context.startActivity(createIntent(context, forumId, forumTitle, objectId, objectName, objectType, forumHeader).clearTop())
        }

        private fun createIntent(
            context: Context,
            forumId: Int,
            forumTitle: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type,
            forumHeader: String = "",
        ): Intent {
            return context.intentFor<ForumActivity>(
                KEY_FORUM_ID to forumId,
                KEY_FORUM_TITLE to forumTitle,
                KEY_OBJECT_ID to objectId,
                KEY_OBJECT_NAME to objectName,
                KEY_OBJECT_TYPE to objectType,
                KEY_FORUM_HEADER to forumHeader,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun ForumTopAppBar(
    forumTitle: String,
    objectName: String,
    forumHeader: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onOpenInBrowser: () -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(forumTitle.ifEmpty { stringResource(R.string.title_forum) }) },
        subtitle = {
            if (objectName.isNotEmpty()) Text(objectName + "  >  " + stringResource(R.string.title_forums))
            else if (forumHeader.isNotEmpty()) Text(stringResource(R.string.title_forums) + "  >  " + forumHeader)
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { onUpClick() }) {
                Icon(
                    painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.up)
                )
            }
        },
        actions = {
            IconButton(onClick = { onOpenInBrowser() }) {
                Icon(
                    painterResource(R.drawable.open_in_browser_24px),
                    contentDescription = stringResource(R.string.menu_view_in_browser)
                )
            }
        }
    )
}