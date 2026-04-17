@file:OptIn(ExperimentalMaterial3Api::class)

package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.app.SearchManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
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
                                    selectedIds.toggle(it)
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
                val res = LocalResources.current
                LaunchedEffect(results) {
                    results.data?.query?.let {
                        if (it.text.isNotBlank() && results.status == Status.SUCCESS) {
                            keyboardController?.hide()
                            coroutineScope.launch {
                                val count = results.data?.results?.size ?: 0
                                if (it.exact) {
                                    val message = res.getQuantityString(R.plurals.search_results_exact, count, count, it.text)
                                    val result = snackbarHostState.showSnackbar(
                                        message,
                                        actionLabel = resources.getString(R.string.more),
                                        duration = SnackbarDuration.Indefinite
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.searchInexact(query?.text.orEmpty())
                                    }
                                } else {
                                    val message = res.getQuantityString(R.plurals.search_results, count, count, it.text)
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
            SearchTextField(
                textFieldState = textFieldState,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                onSearchClick = onSearchClick,
                onClearClick = onClearClick
            )
        },
        modifier = modifier,
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
        navigationIcon = { MenuAppBarAction { onMenuClick() } },
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
                Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.cancel))
            }
        },
        actions = {
            if (isAuthenticated) {
                if (selectedCount == 1) {
                    LogPlayAppBarExpandableActions(onLogPlay, onLogPlayWizard, onQuickLogPlay)
                } else {
                    LogPlayQuickAppBarAction(onQuickLogPlay)
                }
            }
            ShareAppBarAction(onShare)
            if (selectedCount == 1) {
                ViewAppBarAction(onView)
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
            BggLoadingIndicatorBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
        Status.ERROR -> {
            ErrorContent(
                stringResource(R.string.search_error, queryText, message),
                painterResource(R.drawable.search_24px),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
        Status.SUCCESS -> {
            if (data.isNullOrEmpty()) {
                EmptyFullSizeScrollableContent(
                    if (queryText.isBlank())
                        R.string.search_initial_help
                    else
                        R.string.empty_search,
                    painterResource(R.drawable.search_24px),
                    padding = contentPadding,
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
