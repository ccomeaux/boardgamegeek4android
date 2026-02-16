@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Article
import com.boardgamegeek.model.Forum
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.Drawer
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.theme.BggAppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class ArticleActivity : BaseActivity() {
    private var threadId = BggContract.INVALID_ID
    private var threadSubject = ""
    private var forumId = BggContract.INVALID_ID
    private var forumTitle = ""
    private var objectId = BggContract.INVALID_ID
    private var objectName = ""
    private var objectType = Forum.Type.REGION
    private var article = Article()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            threadId = it.getIntExtra(KEY_THREAD_ID, BggContract.INVALID_ID)
            threadSubject = it.getStringExtra(KEY_THREAD_SUBJECT).orEmpty()
            forumId = it.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID)
            forumTitle = it.getStringExtra(KEY_FORUM_TITLE).orEmpty()
            objectId = it.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID)
            objectName = it.getStringExtra(KEY_OBJECT_NAME).orEmpty()
            objectType = it.getSerializableCompat(KEY_OBJECT_TYPE) ?: Forum.Type.REGION
            article = it.getParcelableCompat(KEY_ARTICLE) ?: Article()
        }

        if (article.id == BggContract.INVALID_ID) {
            Timber.w("Invalid article ID")
            finish()
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Article")
                param(FirebaseAnalytics.Param.ITEM_ID, article.id.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, threadSubject)
            }
        }

        setContent {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            BggAppTheme {
                Drawer {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            ArticleTopAppBar(
                                title = if (objectName.isBlank()) forumTitle else "$threadSubject - $forumTitle",
                                subtitle = objectName.ifBlank { threadSubject },
                                scrollBehavior = scrollBehavior,
                                onUpClick = {
                                    ThreadActivity.startUp(
                                        this,
                                        threadId,
                                        threadSubject,
                                        forumId,
                                        forumTitle,
                                        objectId,
                                        objectName,
                                        objectType
                                    )
                                },
                                onOpenInBrowserClick = { link(article.link) },
                                onShareClick = { share() },
                            )
                        }
                    ) { contentPadding ->
                        ArticleScreen(
                            article = article,
                            contentPadding = contentPadding,
                        )
                    }
                }
            }
        }
    }

    private fun share() {
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
        private const val KEY_OBJECT_ID = "OBJECT_ID"
        private const val KEY_OBJECT_NAME = "OBJECT_NAME"
        private const val KEY_OBJECT_TYPE = "OBJECT_TYPE"
        private const val KEY_THREAD_ID = "THREAD_ID"
        private const val KEY_THREAD_SUBJECT = "THREAD_SUBJECT"
        private const val KEY_ARTICLE = "ARTICLE"

        fun start(
            context: Context,
            threadId: Int,
            threadSubject: String?,
            forumId: Int,
            forumTitle: String?,
            objectId: Int,
            objectName: String?,
            objectType: Forum.Type?,
            article: Article?
        ) {
            context.startActivity<ArticleActivity>(
                KEY_THREAD_ID to threadId,
                KEY_THREAD_SUBJECT to threadSubject,
                KEY_FORUM_ID to forumId,
                KEY_FORUM_TITLE to forumTitle,
                KEY_OBJECT_ID to objectId,
                KEY_OBJECT_NAME to objectName,
                KEY_OBJECT_TYPE to objectType,
                KEY_ARTICLE to article,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
private fun ArticleTopAppBar(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onOpenInBrowserClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(title) },
        subtitle = { Text(subtitle) },
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
            IconButton(onClick = { onOpenInBrowserClick() }) {
                Icon(
                    painterResource(R.drawable.open_in_browser_24px),
                    contentDescription = stringResource(R.string.menu_view_in_browser)
                )
            }
            IconButton(onClick = { onShareClick() }) {
                Icon(
                    painterResource(R.drawable.share_24px),
                    contentDescription = stringResource(R.string.menu_share)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun ArticleTopAppBarPreview() {
    BggAppTheme {
        ArticleTopAppBar(
            title = "This is a long title for the article",
            subtitle = "This is a longer subtitle for the article to see how it wraps"
        )
    }
}

@Composable
private fun ArticleScreen(
    article: Article,
    contentPadding: PaddingValues = PaddingValues(0.dp),
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
            .verticalScroll(state = rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                val context = LocalContext.current
                ListItemSecondaryText(
                    text = article.username,
                    icon = painterResource(id = R.drawable.account_circle_24px),
                )
                ListItemSecondaryText(
                    text = article.postTicks.formatTimestamp(context, isForumTimestamp = true).toString(),
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
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    setWebViewText(article.body)
                }
            }
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
