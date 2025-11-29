package com.boardgamegeek.ui.compose

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.User
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.ui.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel
import kotlinx.coroutines.launch

enum class DrawerItem(
    @param:StringRes val labelResId: Int,
    @param:DrawableRes val painterResId: Int,
    val startOfGroup: Boolean = false,
    val onClick: (Context) -> Unit = {}
) {
    CollectionShelves(R.string.title_collection_shelves,R.drawable.shelves_24px, true, onClick = { context -> context.startActivity<CollectionDetailsActivity>() }),
    Collection(R.string.title_collection_legacy, R.drawable.collection_24px, onClick = { context -> context.startActivity<CollectionActivity>() }),
    Plays(R.string.title_plays, R.drawable.plays_24px, onClick = { context -> context.startActivity<PlaysSummaryActivity>() }),
    Buddies(R.string.title_buddies, R.drawable.geekbuddy_24px, onClick = { context -> context.startActivity<BuddiesActivity>() }),
    Search(R.string.title_search, R.drawable.search_24px, true, onClick = { context -> context.startActivity<SearchResultsActivity>() }),
    Hotness(R.string.title_hotness, R.drawable.hotness_24px, onClick = { context -> context.startActivity<HotnessActivity>() }),
    TopGames(R.string.title_top_games, R.drawable.top_24px, onClick = { context -> context.startActivity<TopGamesActivity>() }),
    GeekLists(R.string.title_geeklists, R.drawable.geeklist_24px, onClick = { context -> context.startActivity<GeekListsActivity>() }),
    Forums(R.string.title_forums, R.drawable.forum_24px, onClick = { context -> context.startActivity<ForumsActivity>() }),
    Sync(R.string.title_sync, R.drawable.sync_24px, true, onClick = { context -> context.startActivity<SyncActivity>() }),
    Backup(R.string.title_backup, R.drawable.backup_24px, onClick = { context -> context.startActivity<DataActivity>() }),
    Settings(R.string.title_settings, R.drawable.settings_24px, onClick = { context -> context.startActivity<SettingsActivity>() }),
}

@Composable
fun Drawer(
    modifier: Modifier = Modifier,
    selectedItem: DrawerItem? = null,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerContent = {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val viewModel: SelfUserViewModel = viewModel()
            val user = viewModel.user.observeAsState()
            ModalDrawerSheet(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                DrawerHeader(
                    user.value,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                DrawerItem.entries.forEachIndexed { index, item ->
                    if (item.startOfGroup)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    NavigationDrawerItem(
                        label = { Text(stringResource(item.labelResId)) },
                        icon = { Icon(painterResource(item.painterResId), contentDescription = null) },
                        selected = (index == selectedItem?.ordinal),
                        onClick = {
                            item.onClick(context)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = true,
    ) {
        content()
    }
}

@Composable
private fun DrawerHeader(user: User?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (user == null) {
        Button(
            onClick = { context.startActivity<LoginActivity>() },
            modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(painterResource(R.drawable.login_24px), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_sign_in))
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { context.linkToBgg("user/${user.username}") }
        ) {
            Spacer(Modifier.height(12.dp))
            if (user.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(dimensionResource(R.dimen.drawer_header_image_size))
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.person_image_empty),
                    error = painterResource(id = R.drawable.person_image_empty),
                )
            }
            if (user.fullName.isBlank()) {
                Text(user.username, style = MaterialTheme.typography.titleLarge)
            } else {
                Text(user.fullName, style = MaterialTheme.typography.titleLarge)
                Text(user.username, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun DrawerHeaderPreview(
    @PreviewParameter(UserPreviewParameterProvider::class) user: User?
) {
    BggAppTheme {
        Column {
            DrawerHeader(user)
        }
    }
}

class UserPreviewParameterProvider : PreviewParameterProvider<User?> {
    override val values = sequenceOf(
        User("ccomeaux", "Chris", lastName = "Comeaux", avatarUrl = "x"),
        User("ccomeaux", "Chris", lastName = "Comeaux", avatarUrl = ""),
        User("ccomeaux", "", lastName = "", avatarUrl = "x"),
        User("ccomeaux", "", lastName = "", avatarUrl = ""),
        User("ccomeaux", "Chris", lastName = "", avatarUrl = "x"),
        User("ccomeaux", "", lastName = "Comeaux", avatarUrl = "x"),
        null,
    )
}
