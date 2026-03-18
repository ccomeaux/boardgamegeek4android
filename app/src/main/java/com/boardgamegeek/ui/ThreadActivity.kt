@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, FlowPreview::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Article
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.ThreadViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class ThreadActivity : BaseActivity() {
    private var threadId = BggContract.INVALID_ID
    private var threadSubject = ""
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var forumHeader = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION

    private val prefs by lazy {
        applicationContext.preferences()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            threadId = it.getIntExtra(KEY_THREAD_ID, BggContract.INVALID_ID)
            threadSubject = it.getStringExtra(KEY_THREAD_SUBJECT).orEmpty()
            forumId = it.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getStringExtra(KEY_FORUM_TITLE).orEmpty()
            forumHeader = it.getStringExtra(KEY_FORUM_HEADER).orEmpty()
            objectId = it.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getStringExtra(KEY_OBJECT_NAME).orEmpty()
            objectType = it.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Thread")
                param(FirebaseAnalytics.Param.ITEM_ID, threadId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, threadSubject)
            }
        }

        setContent {
            val viewModel: ThreadViewModel = viewModel()
            val thread = viewModel.articles.observeAsState(RefreshableResource.refreshing(null))
            viewModel.setThreadId(threadId)

            BggAppTheme {
                Drawer {
                    val coroutineScope = rememberCoroutineScope()

                    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                    val listState = rememberLazyListState()

                    val latestArticleId = prefs[getThreadKey(threadId), INVALID_ARTICLE_ID] ?: INVALID_ARTICLE_ID
                    val scrollToLastEnabled by remember { derivedStateOf { thread.value.data?.articles?.isNotEmpty() == true && latestArticleId != INVALID_ARTICLE_ID } }
                    val scrollToBottomEnabled by remember { derivedStateOf { thread.value.data?.articles?.isNotEmpty() == true } }

                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            ThreadTopAppBar(
                                threadSubject = threadSubject,
                                forumTitle = forumTitle,
                                forumHeader = forumHeader,
                                objectName = objectName,
                                scrollBehavior = scrollBehavior,
                                scrollToLastEnabled = scrollToLastEnabled,
                                scrollToBottomEnabled = scrollToBottomEnabled,
                                onUpClick = {
                                    ForumActivity.startUp(
                                        this,
                                        forumId,
                                        forumTitle,
                                        objectId,
                                        objectName,
                                        objectType,
                                        forumHeader,
                                    )
                                },
                                onScrollToLastClick = {
                                    coroutineScope.launch {
                                        if (latestArticleId != INVALID_ARTICLE_ID) {
                                            thread.value.data?.articles.orEmpty().indexOfFirst { it.id == latestArticleId }.takeIf { it != -1 }?.let {
                                                listState.animateScrollToItem(it)
                                            }
                                        }
                                    }
                                },
                                onScrollToBottomClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(thread.value.data?.articles.orEmpty().lastIndex)
                                    }
                                },
                                onOpenInBrowserClick = { linkToBgg("thread", threadId) },
                                onShareClick = { shareThread() },
                            )
                        }
                    ) { contentPadding ->
                        when (thread.value.status) {
                            Status.REFRESHING -> {
                                BggLoadingIndicatorBox(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                )
                            }
                            Status.ERROR -> {
                                ErrorContent(
                                    text = thread.value.message.ifEmpty { stringResource(R.string.error_loading_forums) },
                                    painterResource(R.drawable.forum_24px),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(contentPadding)
                                )
                            }
                            Status.SUCCESS -> {
                                ThreadScreen(listState, thread.value.data?.articles.orEmpty(), contentPadding)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ThreadScreen(
        listState: LazyListState,
        thread: List<Article>,
        contentPadding: PaddingValues
    ) {
        var bottomSheetArticleId: Int? by remember { mutableStateOf(null) }

        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .debounce(500L)
                .collectLatest { index ->
                    thread.getOrNull(index)?.let {
                        prefs.edit { putInt(getThreadKey(threadId), it.id) }
                    }
                }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            state = listState,
        ) {
            items(
                items = thread,
                key = { it.id }
            ) {
                ArticleListItem(
                    it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    bottomSheetArticleId = it.id
                }
            }
        }
        if (bottomSheetArticleId != null) {
            val article = thread.find { it.id == bottomSheetArticleId }
            if (article == null) {
                bottomSheetArticleId = null
            } else {
                ModalBottomSheet(
                    onDismissRequest = { bottomSheetArticleId = null },
                ) {
                    ArticleScreen(
                        article,
                        onOpenInBrowserClick = { link(article.link) },
                        onShareClick = { shareArticle(article) }
                    )
                }
            }
        }
    }

    private fun getThreadKey(threadId: Int): String {
        return "THREAD-$threadId"
    }

    private fun shareThread() {
        val description = if (objectName.isBlank())
            String.format(getString(R.string.share_thread_text), threadSubject, forumTitle)
        else
            String.format(getString(R.string.share_thread_game_text), threadSubject, forumTitle, objectName)
        val link = createBggUri("thread", threadId).toString()
        share(
            getString(R.string.share_thread_subject), """
                        $description
                        
                        $link
                        """.trimIndent(), R.string.title_share
        )
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Thread")
            param(FirebaseAnalytics.Param.ITEM_ID, threadId.toString())
            param(
                FirebaseAnalytics.Param.ITEM_NAME,
                if (objectName.isBlank()) "$forumTitle | $threadSubject" else "$objectName | $forumTitle | $threadSubject"
            )
        }
    }

    private fun shareArticle(article: Article) {
        val description = if (objectName.isEmpty())
            String.format(getString(R.string.share_thread_article_text), threadSubject, forumTitle)
        else
            String.format(getString(R.string.share_thread_article_object_text), threadSubject, forumTitle, objectName)
        val message = """
            $description
    
            ${article.link}""".trimIndent()
        share(getString(R.string.share_thread_subject), message, R.string.title_share)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.ITEM_ID, article.id.toString())
            param(
                FirebaseAnalytics.Param.ITEM_NAME,
                if (objectName.isEmpty()) "$forumTitle | $threadSubject" else "$objectName | $forumTitle | $threadSubject"
            )
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
        }
    }

    companion object {
        private const val KEY_FORUM_ID = "FORUM_ID"
        private const val KEY_FORUM_TITLE = "FORUM_TITLE"
        private const val KEY_FORUM_HEADER = "KEY_FORUM_HEADER"
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val KEY_THREAD_SUBJECT = "THREAD_SUBJECT"
        private const val INVALID_ARTICLE_ID = -1

        fun start(
            context: Context,
            threadId: Int,
            threadSubject: String,
            forumId: Int,
            forumTitle: String,
            forumHeader: String,
            objectId: Int,
            objectName: String,
            objectType: Forum.Type
        ) {
            context.startActivity<ThreadActivity>(
                KEY_THREAD_ID to threadId,
                KEY_THREAD_SUBJECT to threadSubject,
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
private fun ThreadTopAppBar(
    threadSubject: String,
    forumTitle: String,
    forumHeader: String,
    objectName: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    scrollToBottomEnabled: Boolean = false,
    scrollToLastEnabled: Boolean = false,
    onUpClick: () -> Unit = {},
    onScrollToBottomClick: () -> Unit = {},
    onScrollToLastClick: () -> Unit = {},
    onOpenInBrowserClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(threadSubject.truncate(100)) },
        subtitle = {
            Text(
                if (objectName.isBlank()) {
                    if (forumHeader.isNotBlank())
                        "${stringResource(R.string.title_forums)}  >  $forumHeader  >  $forumTitle"
                    else
                        "${stringResource(R.string.title_forums)}  >  $forumTitle"
                } else {
                    "$objectName  >  ${stringResource(R.string.title_forums)}  >  $forumTitle"
                }
            )
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
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    painterResource(R.drawable.overflow_menu_24px),
                    contentDescription = stringResource(R.string.more),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_scroll_to_last_read)) },
                    enabled = scrollToLastEnabled,
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.scroll_to_last_24px),
                            contentDescription = stringResource(R.string.menu_scroll_to_last_read)
                        )
                    },
                    onClick = {
                        onScrollToLastClick()
                        showMenu = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_scroll_to_bottom)) },
                    onClick = { onScrollToBottomClick() },
                    enabled = scrollToBottomEnabled,
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.scroll_to_bottom_24px),
                            contentDescription = stringResource(R.string.menu_scroll_to_bottom)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_view_in_browser)) },
                    onClick = { onOpenInBrowserClick() }, leadingIcon = {
                        Icon(
                            painterResource(R.drawable.open_in_browser_24px),
                            contentDescription = stringResource(R.string.menu_view_in_browser)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_share)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.share_24px),
                            contentDescription = stringResource(R.string.menu_share)
                        )
                    },
                    onClick = { onShareClick() },
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun ThreadTopAppBarPreview() {
    BggAppTheme {
        ThreadTopAppBar(
            threadSubject = "This is a thread subject. ".repeat(10),
            forumTitle = "Strategy",
            objectName = "Gloomhaven",
            forumHeader = "General Gaming"
        )
    }
}

@Composable
private fun ArticleListItem(
    article: Article,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(ListItemDefaults.tallPaddingValues)
        ) {
            val context = LocalContext.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (article.username.isNotBlank() || article.postTicks > 0L) {
                    ListItemSecondaryText(
                        text = article.username,
                        icon = painterResource(id = R.drawable.account_circle_24px),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ListItemVerticalDivider()
                    ListItemSecondaryText(
                        text = article.postTicks.formatTimestamp(context, includeTime = true, isForumTimestamp = true).toString(),
                        icon = painterResource(id = R.drawable.time_24px),
                    )
                    if (article.numberOfEdits > 0) {
                        ListItemVerticalDivider()
                        ListItemSecondaryText(stringResource(R.string.edited))
                    }
                }
            }
        }
        Text(
            text = AnnotatedString.fromHtml(article.body.trim()),
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(ListItemDefaults.tallPaddingValues),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleListItemPreview() {
    BggAppTheme {
        ArticleListItem(
            article = Article(
                username = "ccomeaux",
                postTicks = System.currentTimeMillis() - 1_000_000_000L,
                editTicks = System.currentTimeMillis() - 100_000_000L,
                numberOfEdits = 3,
                body = "This is a preview of an article body. ".repeat(10)
            ),
            Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ArticleScreen(
    article: Article,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onOpenInBrowserClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(contentPadding)
            .padding(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = dimensionResource(R.dimen.material_margin_vertical),
            )
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    val context = LocalContext.current
                    ListItemSecondaryText(
                        text = article.username,
                        icon = painterResource(id = R.drawable.account_circle_24px),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    var postTimestamp by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        while (true) {
                            postTimestamp = article.postTicks.formatTimestamp(context, isForumTimestamp = true).toString()
                            delay(30.seconds)
                        }
                    }
                    ListItemSecondaryText(
                        text = postTimestamp,
                        icon = painterResource(id = R.drawable.time_24px),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (article.numberOfEdits > 0) {
                        var relativeEditTimestamp by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            while (true) {
                                relativeEditTimestamp = article.editTicks.formatTimestamp(context, isForumTimestamp = true).toString()
                                delay(30.seconds)
                            }
                        }
                        ListItemSecondaryText(
                            text = pluralStringResource(
                                id = R.plurals.edit_timestamp,
                                count = article.numberOfEdits,
                                relativeEditTimestamp,
                                article.numberOfEdits.toFormattedString(),
                            ),
                            icon = painterResource(id = R.drawable.ic_outline_edit_18),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenInBrowserClick) {
                    Icon(
                        painterResource(R.drawable.open_in_browser_24px),
                        contentDescription = stringResource(R.string.menu_view_in_browser)
                    )
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        painterResource(R.drawable.share_24px),
                        contentDescription = stringResource(R.string.menu_share)
                    )
                }
            }
        }
        ComposeWebView(
            article.body,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleScreenPreview() {
    BggAppTheme {
        ArticleScreen(
            article = Article(
                username = "ccomeaux",
                postTicks = System.currentTimeMillis() - 10_000_000L,
                editTicks = System.currentTimeMillis() - 1_000_000L,
                numberOfEdits = 3,
                body = "This is a preview of an article body."
            )
        )
    }
}
