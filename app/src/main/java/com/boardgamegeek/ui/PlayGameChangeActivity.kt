package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.boardgamegeek.R
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.compose.SearchTextField
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.PlayGameChangeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayGameChangeActivity : BaseActivity() {
    private var changingGamePlayId: Long = BggContract.INVALID_ID.toLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        changingGamePlayId = intent.getLongExtra(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong())

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val viewModel by viewModels<PlayGameChangeViewModel>()
            val snackbarHostState = remember { SnackbarHostState() }

            val collectionItems by viewModel.items.observeAsState()

            val errorMessage by viewModel.errorMessage.observeAsState()
            LaunchedEffect(errorMessage) {
                errorMessage?.getContentIfNotHandled()?.let {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(it)
                    }
                }
            }

            BggAppTheme {
                val textFieldState: TextFieldState = rememberTextFieldState()
                Scaffold(
                    topBar = {
                        LaunchedEffect(textFieldState.text) {
                            viewModel.filter(textFieldState.text.toString())
                        }
                        PlayGameChangeTopAppBar(
                            textFieldState = textFieldState,
                            onCloseClick = { finish() },
                        )
0                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { contentPadding ->
                    SimpleCollectionItemList(
                        collectionItems,
                        contentPadding = contentPadding,
                        emptyTextResource = if (textFieldState.text.isBlank())
                            R.string.empty_games
                        else
                            R.string.empty_search,
                        onItemClick = {
                            LogPlayActivity.changeGame(
                                context,
                                changingGamePlayId,
                                it.gameId,
                                it.gameName,
                                it.robustHeroImageUrl,
                            )
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun start(context: Context, playId: Long) {
            context.startActivity<PlayGameChangeActivity>(KEY_CHANGING_GAME_PLAY_ID to playId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayGameChangeTopAppBar(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    onCloseClick: () -> Unit = {},
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        title = {
            AnimatedContent(targetState = isSearchExpanded, label = "cross fade") { expanded ->
                if (expanded) {
                    SearchTextField(
                        textFieldState = textFieldState,
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .fillMaxWidth()
                            .padding(end = dimensionResource(R.dimen.material_margin_horizontal)),
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    textFieldState.clearText()
                                    isSearchExpanded = false
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        },
                        placeholderText = stringResource(R.string.search_hint),
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    Text(stringResource(R.string.title_play_game_change))
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            if (!isSearchExpanded) {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        actions = {
            if (!isSearchExpanded) {
                IconButton(
                    onClick = { isSearchExpanded = true },
                ) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.menu_search))
                }
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    BggAppTheme {
        PlayGameChangeTopAppBar()
    }
}
