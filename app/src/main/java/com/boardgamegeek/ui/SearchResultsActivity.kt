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
import com.boardgamegeek.extensions.getActivity
import com.boardgamegeek.extensions.linkBgg
import com.boardgamegeek.extensions.notifyLoggedPlay
import com.boardgamegeek.extensions.shareGames
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.model.SearchResult
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.compose.*
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
                val selectedIds = remember { mutableStateSetOf<Int>() }
                val inSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }
                val context = LocalContext.current

                val viewModel: SearchViewModel = viewModel()
                val query by viewModel.query.observeAsState()
                val results by viewModel.searchResults.observeAsState(RefreshableResource.success(SearchViewModel.SearchResults.EMPTY))
                val errorMessage by viewModel.errorMessage.observeAsState()
                val loggedPlayResult by viewModel.loggedPlayResult.observeAsState()

                fun nameFromId(id: Int): String? =
                    results.data?.results?.find { result ->
                        result.id == id
                    }?.name

                Drawer(
                    drawerState = drawerState,
                    selectedItem = DrawerItem.Search,
                ) {
                    Scaffold(
                        topBar = {
                            if (inSelectionMode) {
                                val quickLogPlayMessage = pluralStringResource(R.plurals.msg_logging_plays, selectedIds.size)
                                MultiSelectionTopAppBar(
                                    selectedCount = selectedIds.size,
                                    Authenticator.isSignedIn(LocalContext.current),
                                    onClear = { selectedIds.clear() },
                                    onLogPlay = {
                                        selectedIds.firstOrNull()?.let { LogPlayActivity.logPlay(context, it, nameFromId(it).orEmpty()) }
                                        selectedIds.clear()
                                    },
                                    onLogPlayWizard = {
                                        selectedIds.firstOrNull()?.let {
                                            NewPlayActivity.start(context, it, nameFromId(it).orEmpty())
                                        }
                                        selectedIds.clear()
                                    },
                                    onQuickLogPlay = {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(quickLogPlayMessage)
                                        }
                                        selectedIds.forEach {
                                            viewModel.logQuickPlay(it, nameFromId(it).orEmpty())
                                        }
                                        selectedIds.clear()
                                    },
                                    onShare = {
                                        context.getActivity()
                                            ?.shareGames(selectedIds.map { it to nameFromId(it).orEmpty() }, "Search", firebaseAnalytics)
                                        selectedIds.clear()
                                    },
                                    onView = {
                                        selectedIds.firstOrNull()?.let { context.linkBgg(it) }
                                        selectedIds.clear()
                                    },
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
                            results.status,
                            results.message,
                            results.data?.results.orEmpty(),
                            results.data?.query?.text.orEmpty(),
                            contentPadding = contentPadding,
                            selectedIds = selectedIds,
                            onClick = {
                                if (inSelectionMode) {
                                    if (selectedIds.contains(it))
                                        selectedIds.remove(it)
                                    else
                                        selectedIds.add(it)
                                } else {
                                    GameActivity.start(context, it, nameFromId(it).orEmpty())
                                }
                            },
                            onLongClick = {
                                selectedIds.add(it)
                            }
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                LaunchedEffect(loggedPlayResult) {
                    loggedPlayResult?.getContentIfNotHandled()?.let {
                        context.notifyLoggedPlay(it)
                    }
                }
                LaunchedEffect(errorMessage) {
                    errorMessage?.getContentIfNotHandled()?.let {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(it)
                        }
                    }
                }
                LaunchedEffect(results) {
                    results.data?.query?.let {
                        if (it.text.isNotBlank()) {
                            keyboardController?.hide()
                            coroutineScope.launch {
                                val count = results.data?.results?.size ?: 0
                                if (it.exact) {
                                    val message = context.resources.getQuantityString(R.plurals.search_results_exact, count, count, it.text)
                                    val result = snackbarHostState.showSnackbar(
                                        message,
                                        actionLabel = context.getString(R.string.more),
                                        duration = SnackbarDuration.Indefinite
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.searchInexact(query?.text.orEmpty())
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
    selectedCount: Int,
    isAuthenticated: Boolean,
    onClear: () -> Unit = {},
    onLogPlay: () -> Unit = {},
    onLogPlayWizard: () -> Unit = {},
    onQuickLogPlay: () -> Unit = {},
    onShare: () -> Unit = {},
    onView: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(pluralStringResource(R.plurals.msg_games_selected, selectedCount, selectedCount))
        },
        modifier = Modifier,
        navigationIcon = {
            IconButton(onClick = { onClear() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
            }
        },
        actions = {
            if (isAuthenticated) {
                var expandedMenu by remember { mutableStateOf(false) }
                if (selectedCount == 1) {
                    IconButton(onClick = { onQuickLogPlay() }) {
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
                                onLogPlay()
                                expandedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { stringResource(R.string.menu_log_play_wizard_short) },
                            onClick = {
                                onLogPlayWizard()
                                expandedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { stringResource(R.string.menu_log_play_quick_short) },
                            onClick = {
                                onQuickLogPlay()
                                expandedMenu = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = { onShare() }) {
                Icon(
                    painterResource(R.drawable.ic_baseline_share_24),
                    contentDescription = stringResource(R.string.menu_share),
                )
            }
            if (selectedCount == 1) {
                IconButton(onClick = { onView() }) {
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
    selectedIds: MutableSet<Int> = remember { mutableStateSetOf() },
    onClick: (Int) -> Unit = {},
    onLongClick: (Int) -> Unit = {},
) {
    when (status) {
        Status.REFRESHING -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(R.dimen.material_margin_horizontal)),
            ) {
                LoadingIndicator(
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
                        val isSelected by remember { derivedStateOf { selectedIds.contains(searchResult.id) } }
                        SearchResultListItem(
                            searchResult = searchResult,
                            modifier = Modifier.animateItem(),
                            isSelected = isSelected,
                            onClick = { onClick(searchResult.id) },
                            onLongClick = { onLongClick(searchResult.id) },
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
        MultiSelectionTopAppBar(
            2,
            isAuthenticated = true,
        )
    }
}
