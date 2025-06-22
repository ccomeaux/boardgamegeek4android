package com.boardgamegeek.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.boardgamegeek.R
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.model.User
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel

enum class DrawerItem(@StringRes val labelResId: Int, val imageVector: ImageVector, val startOfGroup: Boolean = false) {
    Collection(R.string.title_collection, Icons.AutoMirrored.Filled.LibraryBooks), // TODO shelves
    CollectionLegacy(R.string.title_collection_legacy, Icons.AutoMirrored.Filled.LibraryBooks),
    Plays(R.string.title_plays, Icons.Filled.Event),
    Buddies(R.string.title_buddies, Icons.Filled.Person),
    Search(R.string.title_search, Icons.Filled.Search, true),
    Hotness(R.string.title_hotness, Icons.Filled.LocalFireDepartment),
    TopGames(R.string.title_top_games, Icons.AutoMirrored.Filled.TrendingUp),
    GeekLists(R.string.title_geeklists, Icons.AutoMirrored.Filled.List),
    Forums(R.string.title_forums, Icons.Filled.Forum),
    Sync(R.string.title_sync, Icons.Filled.Sync, true),
    Backup(R.string.title_backup, Icons.Filled.FileCopy),
    Settings(R.string.title_settings, Icons.Filled.Settings),
}

@Composable
fun Drawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerContent = {
            val context = LocalContext.current
            val viewModel: SelfUserViewModel = viewModel()
            val user = viewModel.user.observeAsState()
            ModalDrawerSheet {
                DrawerHeader(user.value)
                HorizontalDivider()
                DrawerItem.entries.forEach { item ->
                    if (item.startOfGroup)
                        HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(stringResource(item.labelResId)) },
                        icon = { Icon(item.imageVector, contentDescription = null) },
                        selected = false,
                        onClick = { context.startActivity<CollectionActivity>() }
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
private fun DrawerHeader(user: User?) {
    val context = LocalContext.current
    val horizontalPadding = Modifier.padding(horizontal = 16.dp)
    if (user == null) {
        Button(
            onClick = { context.startActivity<LoginActivity>() },
            modifier = horizontalPadding.padding(vertical = 12.dp)
        ) {
            Icon(Icons.AutoMirrored.Default.Login, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_sign_in))
        }
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { context.linkToBgg("user/${user.username}") }) {
            Spacer(Modifier.height(12.dp))
            if (user.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = horizontalPadding
                        .padding(bottom = 12.dp)
                        .size(dimensionResource(R.dimen.drawer_header_image_size))
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.person_image_empty),
                    error = painterResource(id = R.drawable.person_image_empty),
                )
            }
            if (user.fullName.isBlank()) {
                Text(user.username, modifier = horizontalPadding, style = MaterialTheme.typography.titleLarge)
            } else {
                Text(user.fullName, modifier = horizontalPadding, style = MaterialTheme.typography.titleLarge)
                Text(user.username, modifier = horizontalPadding)
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

@PreviewLightDark
@Composable
private fun DrawerPreview() {
    BggAppTheme {
        Drawer(
            modifier = Modifier,
            drawerState = rememberDrawerState(DrawerValue.Open),
        ) {}
    }
}