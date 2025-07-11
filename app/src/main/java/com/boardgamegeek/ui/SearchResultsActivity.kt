@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.app.SearchManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.compose.EmptyContent
import com.boardgamegeek.ui.compose.ErrorContent
import com.boardgamegeek.ui.compose.SearchResultListItem
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchResultsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BggAppTheme {
                val coroutineScope = rememberCoroutineScope()
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                val textFieldState = rememberTextFieldState(intent.getStringExtra(SearchManager.QUERY).orEmpty())
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val snackbarHostState = remember { SnackbarHostState() }
                val selectedItems = remember { mutableStateListOf<SearchResult>() } // TODO make saveable by implementing a custom saver
                val context = LocalContext.current

                val viewModel: SearchViewModel = viewModel()
                val query = viewModel.query.observeAsState()
                val results = viewModel.searchResults.observeAsState(RefreshableResource.success(SearchViewModel.SearchResults.EMPTY))
                val errorMessage = viewModel.errorMessage.observeAsState()
                val loggedPlayResult = viewModel.loggedPlayResult.observeAsState()

                Drawer(
                    drawerState = drawerState,
                    selectedItem = DrawerItem.Search,
                ) {
                    Scaffold(
                        topBar = {
                            if (!selectedItems.isEmpty()) {
                                val quickLogPlayMessage = pluralStringResource(R.plurals.msg_logging_plays, selectedItems.size)
                                MultiSelectionTopAppBar(
                                    selectedItems,
                                    Authenticator.isSignedIn(LocalContext.current),
                                    onLogPlay = { LogPlayActivity.logPlay(context, it.id, it.name) },
                                    onLogPlayWizard = { NewPlayActivity.start(context, it.id, it.name) },
                                    onQuickLogPlay = {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(quickLogPlayMessage)
                                        }
                                        selectedItems.forEach {
                                            viewModel.logQuickPlay(it.id, it.name)
                                        }
                                    },
                                    onShare = {
                                        val shareMethod = "Search"
                                        if (selectedItems.size == 1) {
                                            val game = selectedItems.firstOrNull()
                                            game?.let { context.getActivity()?.shareGame(it.id, it.name, shareMethod, firebaseAnalytics) }
                                        } else {
                                            val games = selectedItems.map { it.id to it.name }
                                            context.getActivity()?.shareGames(games, shareMethod, firebaseAnalytics)
                                        }
                                    },
                                    onView = { context.linkBgg(it.id) },
                                )
                            } else {
                                SearchTopBar(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    textFieldState = textFieldState,
                                    onMenuClick = {
                                        coroutineScope.launch {
                                            drawerState.open()
                                        }
                                    },
                                    onSearchClick = {
                                        viewModel.search(textFieldState.text.toString())
                                    },
                                    onClearClick = {
                                        textFieldState.clearText()
                                        focusRequester.requestFocus()
                                        viewModel.search("")
                                    }
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { contentPadding ->
                        SearchResultsContent(
                            results.value.status,
                            results.value.message,
                            results.value.data?.results.orEmpty(),
                            results.value.data?.query?.text.orEmpty(),
                            contentPadding = contentPadding,
                            selectedItems = selectedItems,
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                LaunchedEffect(loggedPlayResult.value) {
                    loggedPlayResult.value?.getContentIfNotHandled()?.let {
                        context.notifyLoggedPlay(it)
                    }
                }
                LaunchedEffect(errorMessage.value) {
                    errorMessage.value?.getContentIfNotHandled()?.let {
                        coroutineScope.launch {
                            // TODO style error message as snackbar
                            snackbarHostState.showSnackbar(it)
                        }
                    }
                }
                LaunchedEffect(results.value) {
                    results.value.data?.query?.let {
                        if (it.text.isNotBlank()) {
                            keyboardController?.hide()
                            coroutineScope.launch {
                                val count = results.value.data?.results?.size ?: 0
                                if (it.exact) {
                                    val message = context.resources.getQuantityString(R.plurals.search_results_exact, count, count, it.text)
                                    val result = snackbarHostState.showSnackbar(
                                        message,
                                        actionLabel = context.getString(R.string.more),
                                        duration = SnackbarDuration.Indefinite
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.searchInexact(query.value?.text.orEmpty())
                                    }
                                } else {
                                    val message = context.resources.getQuantityString(R.plurals.search_results, count, count, it.text)
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = rememberTextFieldState(),
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onClearClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            TextField(
                state = textFieldState,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { performDefaultAction ->
                    onSearchClick()
                    performDefaultAction()
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { onClearClick() }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                    }
                },
            )
        },
        modifier = modifier,
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
        navigationIcon = {
            IconButton(onClick = { onMenuClick() }) {
                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.navigation_drawer))
            }
        }
    )
}

@Composable
private fun MultiSelectionTopAppBar(
    selectedItems: SnapshotStateList<SearchResult>,
    isAuthenticated: Boolean,
    onLogPlay: (SearchResult) -> Unit = {},
    onLogPlayWizard: (SearchResult) -> Unit = {},
    onQuickLogPlay: (List<SearchResult>) -> Unit = {},
    onShare: (List<SearchResult>) -> Unit = {},
    onView: (SearchResult) -> Unit = {},
) {
    TopAppBar(
        title = {
            val count = selectedItems.size
            Text(pluralStringResource(R.plurals.msg_games_selected, count, count))
        },
        modifier = Modifier,
        navigationIcon = {
            IconButton(onClick = { selectedItems.clear() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
            }
        },
        actions = {
            if (isAuthenticated) {
                var expandedMenu by remember { mutableStateOf(false) }
                if (selectedItems.size == 1) {
                    IconButton(onClick = {
                        onQuickLogPlay(selectedItems)
                        selectedItems.clear()
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_event_available_24),
                            contentDescription = stringResource(R.string.menu_log_play),
                        )
                    }
                } else {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_event_available_24),
                            contentDescription = stringResource(R.string.menu_log_play),
                        )
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { stringResource(R.string.menu_log_play_short) },
                            onClick = {
                                selectedItems.firstOrNull()?.let { onLogPlay(it) }
                                expandedMenu = false
                                selectedItems.clear()
                            }
                        )
                        DropdownMenuItem(
                            text = { stringResource(R.string.menu_log_play_wizard_short) },
                            onClick = {
                                selectedItems.firstOrNull()?.let { onLogPlayWizard(it) }
                                expandedMenu = false
                                selectedItems.clear()
                            }
                        )
                        DropdownMenuItem(
                            text = { stringResource(R.string.menu_log_play_quick_short) },
                            onClick = {
                                onQuickLogPlay(selectedItems)
                                expandedMenu = false
                                selectedItems.clear()
                            }
                        )
                    }
                }
            }
            IconButton(onClick = {
                onShare(selectedItems)
                selectedItems.clear()
            }) {
                Icon(
                    painterResource(R.drawable.ic_baseline_share_24),
                    contentDescription = stringResource(R.string.menu_share),
                )
            }
            if (selectedItems.size == 1) {
                IconButton(onClick = {
                    selectedItems.firstOrNull()?.let { onView(it) }
                    selectedItems.clear()
                }) {
                    Icon(
                        painterResource(R.drawable.ic_baseline_open_in_browser_24),
                        contentDescription = stringResource(R.string.menu_view),
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchResultsContent(
    status: Status,
    message: String,
    data: List<SearchResult>?,
    queryText: String,
    contentPadding: PaddingValues = PaddingValues(),
    selectedItems: MutableList<SearchResult> = remember { mutableStateListOf() }
) {
    when (status) {
        Status.REFRESHING -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
            ) {
                com.boardgamegeek.ui.compose.LoadingIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .padding(dimensionResource(R.dimen.padding_extra))
                )
            }
        }
        Status.ERROR -> {
            ErrorContent(
                stringResource(R.string.search_error, queryText, message),
                Icons.Default.Search,
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
            )
        }
        Status.SUCCESS -> {
            if (data.isNullOrEmpty()) {
                EmptyContent(
                    if (queryText.isBlank())
                        R.string.search_initial_help
                    else
                        R.string.empty_search,
                    Icons.Default.Search,
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
                )
            } else {
                LazyColumn(
                    contentPadding = contentPadding
                ) {
                    itemsIndexed(
                        items = data,
                        key = { _, searchResult -> searchResult.id }
                    ) { index, searchResult ->
                        val context = LocalContext.current
                        SearchResultListItem(
                            searchResult = searchResult,
                            modifier = Modifier.animateItem(),
                            isSelected = selectedItems.contains(searchResult),
                            onClick = {
                                if (selectedItems.isNotEmpty()) {
                                    if (selectedItems.contains(searchResult))
                                        selectedItems.remove(searchResult)
                                    else
                                        selectedItems.add(searchResult)
                                } else {
                                    GameActivity.start(context, searchResult.id, searchResult.name)
                                }
                            },
                            onLongClick = {
                                if (selectedItems.isEmpty()) {
                                    selectedItems.add(searchResult)
                                }
                            },
                        )
                        if (index < data.lastIndex)
                            HorizontalDivider()
                    }
                }
            }
        }

    }
}

@PreviewLightDark
@Composable
private fun SearchTopBarPreview() {
    BggAppTheme {
        SearchTopBar()
    }
}

@SuppressLint("UnrememberedMutableState")
@PreviewLightDark
@Composable
private fun MultiSelectionTopAppBarPreview() {
    BggAppTheme {
        MultiSelectionTopAppBar(mutableStateListOf(), true)
    }
}
