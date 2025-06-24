package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentGeeklistItemBinding
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.getParcelableCompat
import com.boardgamegeek.extensions.setWebViewText
import com.boardgamegeek.model.GeekListItem
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.util.XmlApiMarkupConverter
import java.text.NumberFormat
import java.util.Locale

class GeekListItemFragment : Fragment() {
    private var _binding: FragmentGeeklistItemBinding? = null
    private val binding get() = _binding!!
    private var order = 0
    private var geekListTitle = ""
    private var geekListItem = GeekListItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            order = it.getInt(KEY_ORDER, 0)
            geekListTitle = it.getString(KEY_TITLE).orEmpty()
            geekListItem = it.getParcelableCompat(KEY_ITEM) ?: GeekListItem()
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentGeeklistItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markupConverter = XmlApiMarkupConverter(requireContext())
        binding.composeView.setContent {
            BggAppTheme {
                GeekListItem(
                    geekListItem,
                    order,
                    geekListTitle,
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                        vertical = dimensionResource(R.dimen.material_margin_vertical)
                    )
                )
            }
        }
        binding.bodyView.setWebViewText(markupConverter.toHtml(geekListItem.body))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_ORDER = "GEEK_LIST_ORDER"
        private const val KEY_TITLE = "GEEK_LIST_TITLE"
        private const val KEY_ITEM = "ITEM"

        fun newInstance(order: Int, title: String, item: GeekListItem): GeekListItemFragment {
            return GeekListItemFragment().apply {
                arguments = bundleOf(
                    KEY_ORDER to order,
                    KEY_TITLE to title,
                    KEY_ITEM to item,
                )
            }
        }
    }
}

@StringRes
private fun GeekListItem.titleResId(): Int {
    return when (this.objectType) {
        GeekListItem.ObjectType.BoardGame -> R.string.title_board_game
        GeekListItem.ObjectType.BoardGameAccessory -> R.string.title_board_game_accessory
        GeekListItem.ObjectType.Thing -> R.string.title_thing
        GeekListItem.ObjectType.Publisher -> R.string.title_board_game_publisher
        GeekListItem.ObjectType.Company -> R.string.title_company
        GeekListItem.ObjectType.Designer -> R.string.title_board_game_designer
        GeekListItem.ObjectType.Person -> R.string.title_person
        GeekListItem.ObjectType.BoardGameFamily -> R.string.title_board_game_family
        GeekListItem.ObjectType.Family -> R.string.title_family
        GeekListItem.ObjectType.File -> R.string.title_file
        GeekListItem.ObjectType.GeekList -> R.string.title_geeklist
        GeekListItem.ObjectType.Unknown -> ResourcesCompat.ID_NULL
    }
}

@Composable
fun GeekListItem(geekListItem: GeekListItem, rank: Int, geekListTitle: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    val dividerModifier = Modifier
        .size(18.dp)
        .padding(horizontal = 8.dp)
    val iconModifier = Modifier
        .size(18.dp)
        .padding(end = 8.dp)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.card_padding_openSource))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = numberFormat.format(rank),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = geekListTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = stringResource(R.string.author),
                    modifier = iconModifier
                )
                Text(
                    text = geekListItem.username,
                    style = MaterialTheme.typography.bodyLarge,
                )
                val typeResId = geekListItem.titleResId()
                if (typeResId != ResourcesCompat.ID_NULL) {
                    VerticalDivider(dividerModifier)
                    Icon(
                        Icons.AutoMirrored.Outlined.Label,
                        contentDescription = stringResource(R.string.type),
                        modifier = iconModifier
                    )
                    Text(
                        text = stringResource(typeResId),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = stringResource(R.string.number_of_thumbs),
                    modifier = iconModifier,
                )
                Text(
                    text = numberFormat.format(geekListItem.numberOfThumbs),
                    style = MaterialTheme.typography.bodyMedium,
                )
                VerticalDivider(dividerModifier)
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = stringResource(R.string.posted),
                    modifier = iconModifier,
                )
                Text(
                    text = geekListItem.postDateTime.formatTimestamp(context).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                if (geekListItem.postDateTime != geekListItem.editDateTime) {
                    VerticalDivider(dividerModifier)
                    Icon(
                        Icons.Outlined.Update,
                        contentDescription = stringResource(R.string.edited),
                        modifier = iconModifier,
                    )
                    Text(
                        text = geekListItem.editDateTime.formatTimestamp(context).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GeekListItemPreview(
    @PreviewParameter(provider = GeekListItemPreviewParameterProvider::class) geekListItem: Pair<GeekListItem, Int>
) {
    BggAppTheme {
        GeekListItem(
            geekListItem.first,
            geekListItem.second,
            "This is the GeekList Title. It is a very long title that should be truncated! Alas, I'm not sure it will be.",
            modifier = Modifier.padding(8.dp)
        )
    }
}

class GeekListItemPreviewParameterProvider : PreviewParameterProvider<Pair<GeekListItem, Int>> {
    override val values = sequenceOf(
        GeekListItem(
            id = 1,
            objectId = 31,
            objectName = "Gaia Project",
            objectType = GeekListItem.ObjectType.BoardGame,
            username = "ccomeaux",
            numberOfThumbs = 42,
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png"),
            postDateTime = 1234123412345L,
            editDateTime = 1234123412345L,
        ) to 1,
        GeekListItem(
            id = 12,
            objectId = 31,
            objectName = "Gaia Project",
            objectType = GeekListItem.ObjectType.BoardGame,
            username = "author",
            numberOfThumbs = 4321,
            thumbnailUrls = listOf("https://cf.geekdo-images.com/PyUol9QxBnZQCJqZI6bmSA__square/img/610c2mQNSggoh45dO3leJaLBruk=/75x75/filters:strip_icc()/pic8632666.png"),
            postDateTime = 1234123412345L,
            editDateTime = 1234123454321L,
        ) to 99,
        GeekListItem(
            id = 13,
            objectId = 31,
            objectName = "No Image",
            objectType = GeekListItem.ObjectType.BoardGame,
            numberOfThumbs = 0,
            username = "ccomeaux",
        ) to 100,
    )
}
