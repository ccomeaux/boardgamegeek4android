@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.GameComment
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.GameCommentsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommentsActivity : BaseActivity() {
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var sortType = SORT_TYPE_USER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            gameId = it.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
            gameName = it.getStringExtra(KEY_GAME_NAME).orEmpty()
            sortType = it.getIntExtra(KEY_SORT_TYPE, SORT_TYPE_USER)
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "GameComments")
                param(FirebaseAnalytics.Param.ITEM_ID, gameId.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, gameName)
            }
        }

        setContent {
            val viewModel: GameCommentsViewModel = viewModel()
            viewModel.setGameId(gameId)
            viewModel.setSort(if (sortType == SORT_TYPE_USER) GameComment.SortType.Comment else GameComment.SortType.Rating)

            val sortBy by viewModel.sort.collectAsState(GameComment.SortType.Comment)
            val comments = viewModel.comments.collectAsLazyPagingItems()

            BggAppTheme {
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        CommentsTopBar(
                            gameName = gameName,
                            sortBy = sortBy,
                            scrollBehavior = scrollBehavior,
                            onUpClick = {
                                GameActivity.startUp(this, gameId, gameName)
                                finish()
                            },
                            onSortClick = {
                                viewModel.setSort(it)
                            },
                        )
                    }
                ) { contentPadding ->
                    when (comments.loadState.refresh) {
                        is LoadState.Loading -> {
                            BggLoadingIndicatorBox(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                            )
                        }
                        is LoadState.Error -> {
                            ErrorContent(
                                text = stringResource(R.string.empty_comments),
                                painterResource(R.drawable.comment_24px),
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
                                items(comments.itemCount) { position ->
                                    comments[position]?.let {
                                        CommentListItem(it)
                                        if (position < comments.itemCount - 1)
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
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_SORT_TYPE = "SORT_TYPE"
        const val SORT_TYPE_USER = 0
        const val SORT_TYPE_RATING = 1

        fun startRating(context: Context, gameId: Int, gameName: String) {
            context.startActivity<CommentsActivity>(
                KEY_GAME_ID to gameId,
                KEY_GAME_NAME to gameName,
                KEY_SORT_TYPE to SORT_TYPE_RATING,
            )
        }
    }
}

private enum class CommentsSort(
    val type: GameComment.SortType,
    @param:StringRes val labelResId: Int,
) {
    Rating(GameComment.SortType.Rating, R.string.menu_sort_rating),
    Comment(GameComment.SortType.Comment, R.string.menu_sort_comments),
}

@Composable
private fun CommentsTopBar(
    gameName: String,
    sortBy: GameComment.SortType,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onUpClick: () -> Unit = {},
    onSortClick: (GameComment.SortType) -> Unit = {}
) {
    var expandedMenu by remember { mutableStateOf(false) }
    MediumFlexibleTopAppBar(
        title = { Text(gameName) },
        subtitle = { (CommentsSort.entries.find { it.type == sortBy }?.labelResId)?.let { Text(stringResource(it)) } },
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
                CommentsSort.entries.forEach {
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CommentsTopBarPreview() {
    BggAppTheme {
        CommentsTopBar(
            gameName = "Ticket to Ride",
            sortBy = GameComment.SortType.Comment
        )
    }
}

@Composable
private fun CommentListItem(
    gameComment: GameComment,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.twoLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(ListItemDefaults.paddingValues)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItemPrimaryText(gameComment.username)
            Rating(gameComment.rating, style = RatingDefaults.textStyleSmall())
        }
        if (gameComment.comment.isNotBlank()) {
            ListItemSecondaryText(
                AnnotatedString.fromHtml(gameComment.comment),
                maxLines = 3,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CommentListItemPreview(
    @PreviewParameter(CommentPreviewParameterProvider::class) gameComment: GameComment
) {
    BggAppTheme {
        CommentListItem(gameComment)
    }
}

private class CommentPreviewParameterProvider : PreviewParameterProvider<GameComment> {
    override val values = sequenceOf(
        GameComment(
            username = "username",
            rating = 8.5,
            comment = "I like it.",
        ),
        GameComment(
            username = "joe",
            rating = 10.0,
            comment = "I <b>love</b> it.",
        ),
        GameComment(
            username = "chunky_lover_53",
            rating = 4.0,
            comment = "",
        ),
        GameComment(
            username = "ccomeaux",
            rating = 0.0,
            comment = "Let me go on and on with a lengthy comment. ".repeat(10),
        ),
    )
}
