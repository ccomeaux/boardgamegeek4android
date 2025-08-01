@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.PlayerColor
import com.boardgamegeek.model.User
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.dialog.EditUsernameDialogFragment
import com.boardgamegeek.ui.dialog.UpdateBuddyNicknameDialogFragment
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.BuddyViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class BuddyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerName = intent.getStringExtra(KEY_PLAYER_NAME).orEmpty()
        val username = intent.getStringExtra(KEY_USERNAME).orEmpty()

        if (playerName.isBlank() && username.isBlank()) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Buddy")
                param(FirebaseAnalytics.Param.ITEM_ID, username)
                param(FirebaseAnalytics.Param.ITEM_NAME, playerName)
            }
        }

        setContent {
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }

            val viewModel: BuddyViewModel = viewModel()
            if (username.isNotBlank()) {
                viewModel.setUsername(username)
            } else {
                viewModel.setPlayerName(playerName)
            }

            val buddy by viewModel.buddy.observeAsState()
            val player by viewModel.player.observeAsState()
            val colors by viewModel.colors.observeAsState()
            val isRefreshing by viewModel.refreshing.observeAsState(false)
            val updateMessage by viewModel.updateMessage.observeAsState()

            LaunchedEffect(updateMessage) {
                updateMessage?.getContentIfNotHandled()?.let { content ->
                    snackbarHostState.showSnackbar(content, duration = SnackbarDuration.Long)
                }
            }
            BggAppTheme {
                Drawer {
                    Scaffold(
                        topBar = {
                            BuddyTopBar(
                                username,
                                playerName,
                                onUpClick = { context.startActivity(context.intentFor<BuddiesActivity>().clearTop()) },
                                onViewUserClick = { linkToBgg("user/${buddy?.username}") },
                                onAddUsernameClick = { showAndSurvive(EditUsernameDialogFragment()) },
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        if (buddy == null && player == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                            ) {
                                BggLoadingIndicator(Modifier.align(Alignment.Center))
                            }
                        } else {
                            BuddyScreen(
                                buddy = buddy,
                                player = player,
                                playerColors = colors.orEmpty(),
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel.refresh() },
                                onGenerateColors = { viewModel.generateColors() },
                                onMoreStatsClick = {
                                    if (username.isBlank()) {
                                        PlayerPlaysActivity.start(context, playerName)
                                    } else {
                                        BuddyPlaysActivity.start(context, username)
                                    }
                                },
                                onEditColorsClick = {
                                    if (username.isBlank()) {
                                        PlayerColorsActivity.start(context, null, playerName)
                                    } else {
                                        PlayerColorsActivity.start(context, username, null)
                                    }
                                },
                                modifier = Modifier.padding(contentPadding),
                            )
                        }
                    }
                }
            }
        }

//        viewModel.username.observe(this) {
//            it?.let {
//                username = it
//                intent.putExtra(KEY_USERNAME, username)
//                setSubtitle()
//            }
//        }
//
//        viewModel.playerName.observe(this) {
//            it?.let {
//                name = it
//                intent.putExtra(KEY_PLAYER_NAME, name)
//                setSubtitle()
//            }
//        }
    }

//    override val optionsMenuId = R.menu.buddy
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.add_username -> {
//                showAndSurvive(EditUsernameDialogFragment())
//                return true
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }

    companion object {
        private const val KEY_USERNAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, username: String, playerName: String) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it)
            }
        }

        fun startUp(context: Context, username: String?, playerName: String? = null) {
            createIntent(context, username, playerName)?.let {
                context.startActivity(it.clearTop())
            }
        }

        fun createIntent(context: Context, username: String?, playerName: String?): Intent? {
            if (username.isNullOrBlank() && playerName.isNullOrBlank()) {
                Timber.w("Unable to create a BuddyActivity intent - missing both a username and a player name")
                return null
            }
            return context.intentFor<BuddyActivity>(
                KEY_USERNAME to username,
                KEY_PLAYER_NAME to playerName,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuddyTopBar(
    username: String,
    playerName: String,
    modifier: Modifier = Modifier,
    onUpClick: () -> Unit = {},
    onViewUserClick: () -> Unit = {},
    onAddUsernameClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(text = username.ifBlank { playerName }.ifBlank { stringResource(id = R.string.title_buddy) })
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { onUpClick() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.up))
            }
        },
        actions = {
            if (username.isBlank()) {
                IconButton(
                    onClick = { onAddUsernameClick() },
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.menu_add_username))
                }
            } else {
                IconButton(
                    onClick = { onViewUserClick() },
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = stringResource(R.string.menu_view_in_browser))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BuddyScreen(
    buddy: User?,
    player: Player?,
    playerColors: List<PlayerColor>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onGenerateColors: () -> Unit,
    onMoreStatsClick: () -> Unit,
    onEditColorsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onRefresh() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            buddy?.let { buddy ->
                Text(
                    text = buddy.fullName,
                    style = MaterialTheme.typography.displaySmall,
                )
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip { Text(stringResource(R.string.nickname_description)) }
                        },
                        state = rememberTooltipState(isPersistent = true)
                    ) {
                        if (buddy.playNickname.isNotBlank()) {
                            Text(
                                text = buddy.playNickname,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        } else {
                            Text(
                                text = buddy.nicknameCandidate,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            )
                        }
                    }
                    FilledTonalIconButton(
                        onClick = {
                            context.getActivity()
                                ?.showAndSurvive(UpdateBuddyNicknameDialogFragment.newInstance(buddy.playNickname.ifBlank { buddy.nicknameCandidate }))
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.extraSmallContainerSize()),
                        shape = IconButtonDefaults.smallRoundShape,
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.title_edit_nickname),
                            modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                        )
                    }
                }
                var showImage by remember { mutableStateOf(true) }
                AnimatedVisibility(showImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(buddy.avatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.person_image_empty),
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
                        onError = { showImage = false },
                    )
                }
                Button(onClick = { BuddyCollectionActivity.start(context, buddy.username) }, modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.title_collection))
                }
            }
            PlayStats(
                player = player,
                modifier = Modifier.padding(bottom = 16.dp),
                onMoreClick = { onMoreStatsClick() },
            )
            PlayerColors(
                playerColors,
                modifier = Modifier.padding(bottom = 16.dp),
                onGenerateClick = { onGenerateColors() },
                onEditClick = { onEditColorsClick() },
            )
            buddy?.let {
                Text(
                    text = if (buddy.updatedTimestamp == 0L)
                        stringResource(R.string.needs_updating)
                    else
                        stringResource(R.string.updated_prefix, buddy.updatedTimestamp.formatTimestamp(context, false).toString()),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun PlayStats(
    player: Player?,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.title_play_stats),
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(
                onClick = { onMoreClick() },
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.more))
            }
        }
        val playCount = player?.playCount ?: 0
        val winCount = player?.winCount ?: 0
        if (playCount > 0 || winCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.winnable_plays_suffix, playCount, playCount),
                modifier = Modifier.padding(start = 40.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = pluralStringResource(R.plurals.wins_suffix, winCount, winCount),
                modifier = Modifier.padding(start = 40.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.percentage, (winCount.toDouble() / playCount * 100).toInt()),
                modifier = Modifier.padding(start = 40.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text(
                stringResource(R.string.empty_plays_buddy),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PlayerColors(
    colors: List<PlayerColor>,
    modifier: Modifier = Modifier,
    onGenerateClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.title_favorite_colors),
                style = MaterialTheme.typography.headlineSmall,
            )
            AnimatedVisibility(
                colors.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Button(
                    onClick = { onEditClick() },
                    colors = ButtonDefaults.buttonColors().copy(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = stringResource(R.string.edit))
                }
            }
        }
        AnimatedVisibility(
            colors.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { onGenerateClick() },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(text = stringResource(R.string.empty_player_colors_button))
                }
            }
        }
        AnimatedVisibility(
            colors.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val firstChoice = colors.firstOrNull()
                firstChoice?.let {
                    ColorBox(firstChoice.description, size = ColorBoxDefaults.sizeLarge) { foregroundColor ->
                        Text(
                            text = firstChoice.sortOrder.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = foregroundColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.Start,
                    verticalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 4,
                    maxLines = 2,
                ) {
                    colors.drop(1).forEach {
                        ColorBox(it.description) { foregroundColor ->
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
    }
}

@Preview(widthDp = 480)
@Composable
private fun BuddyScreenPreview(
    @PreviewParameter(BuddyPreviewParameterProvider::class) buddy: User
) {
    val player = Player(
        name = "Chris",
        username = "ccomeaux",
        playCount = 13,
        winCount = 6,
    )
    BggAppTheme {
        BuddyScreen(
            buddy,
            player,
            BggColors.standardColorList.mapIndexed { index, color -> PlayerColor(color.first, index + 1) },
            true,
            {}, {}, {}, {},
        )
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
