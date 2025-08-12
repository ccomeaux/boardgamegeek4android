package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.*
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPlaysBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.compose.ListItemTertiaryText
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.util.XmlApiMarkupConverter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
open class PlaysFragment : Fragment(), ActionMode.Callback {
    private var _binding: FragmentPlaysBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<PlaysViewModel>()
    private val markupConverter by lazy { XmlApiMarkupConverter(requireContext()) }

    private val adapter: PlayAdapter by lazy { PlayAdapter() }

    private var gameId: Int = INVALID_ID
    private var gameName: String? = null
    private var heroImageUrl: String? = null
    private var arePlayersCustomSorted: Boolean = false
    private var emptyStringResId: Int = 0
    private var showGameName = true
    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPlaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyStringResId = arguments.getIntOrElse(KEY_EMPTY_STRING_RES_ID, R.string.empty_plays)
        showGameName = arguments.getBooleanOrElse(KEY_SHOW_GAME_NAME, true)
        gameId = arguments.getIntOrElse(KEY_GAME_ID, INVALID_ID)
        gameName = arguments?.getString(KEY_GAME_NAME)
        heroImageUrl = arguments?.getString(KEY_HERO_IMAGE_URL)
        arePlayersCustomSorted = arguments.getBooleanOrElse(KEY_CUSTOM_PLAYER_SORT, false)
        @ColorInt val iconColor = arguments.getIntOrElse(KEY_ICON_COLOR, Color.TRANSPARENT)

        binding.fabView.apply {
            if (gameId != INVALID_ID) {
                colorize(iconColor)
                setOnClickListener { // launch the "correct" play logging activity
                    logPlay()
                }
                show()
            } else {
                hide()
            }
        }

        updateEmptyText()

        binding.swipeRefreshLayout.setBggColors()
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.refresh() }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            it?.let { binding.swipeRefreshLayout.isRefreshing = it }
        }

        viewModel.plays.observe(viewLifecycleOwner) {
            it?.let { list ->
                adapter.items = list
                binding.recyclerView.addHeader(adapter)
                binding.progressBar.hide()
                binding.emptyContainer.isVisible = list.isEmpty()
                binding.recyclerView.isVisible = list.isNotEmpty()
            }
        }

        viewModel.filterType.observe(viewLifecycleOwner) {
            updateEmptyText()
        }
    }

    private fun updateEmptyText() {
        binding.emptyTextView.setText(
            when (viewModel.filterType.value) {
                PlaysViewModel.FilterType.DIRTY -> R.string.empty_plays_draft
                PlaysViewModel.FilterType.PENDING -> R.string.empty_plays_pending
                else -> if (requireContext().preferences()[PREFERENCES_KEY_SYNC_PLAYS, false] == true) {
                    emptyStringResId
                } else {
                    R.string.empty_plays_sync_off
                }
            }
        )
    }

    private fun logPlay() {
        when (requireActivity().preferences().logPlayPreference()) {
            LOG_PLAY_TYPE_FORM -> LogPlayActivity.logPlay(
                requireContext(),
                gameId,
                gameName.orEmpty(),
                heroImageUrl.orEmpty(),
                arePlayersCustomSorted
            )
            LOG_PLAY_TYPE_QUICK -> viewModel.logQuickPlay(gameId, gameName.orEmpty())
            LOG_PLAY_TYPE_WIZARD -> NewPlayActivity.start(requireContext(), gameId, gameName.orEmpty())
        }
    }

    internal inner class PlayAdapter : RecyclerView.Adapter<PlayAdapter.ViewHolder>(), RecyclerSectionItemDecoration.SectionCallback {
        private val selectedItems = SparseBooleanArray()

        val selectedItemCount: Int
            get() = selectedItems.filterTrue().size

        val selectedItemPositions: List<Int>
            get() = selectedItems.filterTrue()

        fun getSelectedItems() = selectedItems.filterTrue().mapNotNull { items.getOrNull(it) }

        init {
            setHasStableIds(true)
        }

        var items: List<Play> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun getItem(position: Int): Play? {
            return items.getOrNull(position)
        }

        fun areAllSelectedItemsPending(): Boolean {
            return adapter.selectedItemPositions
                .map { adapter.getItem(it) }
                .map { (it?.dirtyTimestamp ?: 0) > 0 }
                .all { it }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun toggleSelection(position: Int) {
            selectedItems.toggle(position)
            notifyDataSetChanged() // I'd prefer to call notifyItemChanged(position), but that causes the section header to appear briefly
            actionMode?.let {
                if (selectedItemCount == 0) {
                    it.finish()
                } else {
                    it.invalidate()
                }
            }
        }

        fun clearSelection() {
            val oldSelectedItems = selectedItems.clone()
            selectedItems.clear()
            oldSelectedItems.filterTrue().forEach { notifyItemChanged(it) }
        }

        override fun getItemCount() = items.size

        override fun getItemId(position: Int) = getItem(position)?.internalId ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ComposeView(parent.context))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it, position) }
        }

        internal inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bind(play: Play, position: Int) {
                composeView.setContent {
                    PlayListItem(
                        play = play,
                        showGameName = showGameName,
                        markupConverter = markupConverter,
                        isSelected = selectedItems.get(position, false),
                        onClick = {
                            if (actionMode == null) {
                                PlayActivity.start(requireContext(), play.internalId)
                            } else {
                                toggleSelection(position)
                            }
                        },
                        onLongClick = {
                            if (actionMode == null) {
                                actionMode = requireActivity().startActionMode(this@PlaysFragment)
                                if (actionMode != null) {
                                    toggleSelection(position)
                                }
                            }
                        }
                    )
                }
            }
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (items.isEmpty()) return false
            if (position < 0 || position >= items.size) return false
            val thisLetter = getSectionHeader(position)
            val lastLetter = getSectionHeader(position - 1)
            return thisLetter != lastLetter
        }

        private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        override fun getSectionHeader(position: Int): CharSequence {
            val play = items.getOrNull(position) ?: return "-"
            return when (viewModel.sortType.value ?: PlaysViewModel.SortType.DATE) {
                PlaysViewModel.SortType.DATE -> {
                    if (play.dateInMillis == Play.UNKNOWN_DATE)
                        getString(R.string.text_unknown)
                    else
                        dateFormat.format(play.dateInMillis)
                }
                PlaysViewModel.SortType.LOCATION -> {
                    play.location.ifBlank { getString(R.string.no_location) }
                }
                PlaysViewModel.SortType.GAME -> {
                    play.gameName
                }
                PlaysViewModel.SortType.LENGTH -> {
                    val minutes = play.length
                    return when {
                        minutes == 0 -> getString(R.string.no_length)
                        minutes >= 120 -> "${(minutes / 60)}+ ${getString(R.string.hours_abbr)}"
                        minutes >= 60 -> "${(minutes / 10 * 10)}+ ${getString(R.string.minutes_abbr)}"
                        minutes >= 30 -> "${(minutes / 5 * 5)}+ ${getString(R.string.minutes_abbr)}"
                        else -> "$minutes ${getString(R.string.minutes_abbr)}"
                    }
                }
            }
        }
    }

    // TODO Add support for share option

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.plays_context, menu)
        adapter.clearSelection()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter.selectedItemCount
        mode.title = resources.getQuantityString(R.plurals.msg_plays_selected, count, count)
        menu.findItem(R.id.menu_send).isVisible = adapter.areAllSelectedItemsPending()
        menu.findItem(R.id.menu_edit).isVisible = adapter.selectedItemCount == 1
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        adapter.clearSelection()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val plays = adapter.getSelectedItems()
        if (plays.isEmpty()) return false
        when (item.itemId) {
            R.id.menu_send -> {
                requireContext().showDialog(
                    resources.getQuantityString(R.plurals.are_you_sure_send_play, adapter.selectedItemCount),
                    R.string.send,
                ) { _, _ ->
                    viewModel.send(adapter.getSelectedItems())
                    mode.finish()
                }
            }
            R.id.menu_edit -> {
                adapter.getItem(adapter.selectedItemPositions.iterator().next())?.let { play ->
                    LogPlayActivity.editPlay(
                        requireContext(),
                        play.internalId,
                        play.gameId,
                        play.gameName,
                        play.robustHeroImageUrl,
                    )
                }
                mode.finish()
            }
            R.id.menu_delete -> {
                requireContext().showDialog(
                    resources.getQuantityString(R.plurals.are_you_sure_delete_play, adapter.selectedItemCount),
                    R.string.delete
                ) { _, _ ->
                    viewModel.delete(adapter.getSelectedItems())
                    mode.finish()
                }
            }
            else -> return false
        }
        return true
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_ICON_COLOR = "ICON_COLOR"
        private const val KEY_EMPTY_STRING_RES_ID = "EMPTY_STRING_RES_ID"
        private const val KEY_SHOW_GAME_NAME = "SHOW_GAME_NAME"

        fun newInstance(): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(KEY_EMPTY_STRING_RES_ID to R.string.empty_plays)
            }
        }

        fun newInstanceForGame(
            gameId: Int,
            gameName: String,
            heroImageUrl: String,
            arePlayersCustomSorted: Boolean,
            @ColorInt iconColor: Int
        ): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(
                    KEY_EMPTY_STRING_RES_ID to R.string.empty_plays_game,
                    KEY_SHOW_GAME_NAME to false,
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_HERO_IMAGE_URL to heroImageUrl,
                    KEY_CUSTOM_PLAYER_SORT to arePlayersCustomSorted,
                    KEY_ICON_COLOR to iconColor,
                )
            }
        }

        fun newInstanceForLocation(): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(KEY_EMPTY_STRING_RES_ID to R.string.empty_plays_location)
            }
        }

        fun newInstanceForPlayer(): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(KEY_EMPTY_STRING_RES_ID to R.string.empty_plays_player)
            }
        }
    }
}

@Composable
fun PlayListItem(
    play: Play,
    showGameName: Boolean,
    markupConverter: XmlApiMarkupConverter,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
            .then(
                if (isSelected)
                    Modifier.clickable(onClick = onClick)
                else
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
            )
            .padding(ListItemDefaults.tallPaddingValues)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListItemPrimaryText(
                if (showGameName) play.gameName else play.dateForDisplay(LocalContext.current).toString(), isSelected = isSelected
            )
            @StringRes val statusMessageId = when {
                play.deleteTimestamp > 0 -> R.string.sync_pending_delete
                play.updateTimestamp > 0 -> R.string.sync_pending_update
                play.dirtyTimestamp > 0 -> if (play.isSynced) R.string.sync_editing else R.string.sync_draft
                else -> ResourcesCompat.ID_NULL
            }
            if (statusMessageId != ResourcesCompat.ID_NULL) {
                ListItemTertiaryText(stringResource(statusMessageId), isSelected = isSelected)
            }
        }
        ListItemSecondaryText(play.describe(LocalContext.current, showGameName), isSelected = isSelected)
        val comments = markupConverter.strip(play.comments.replace("\n", " "))
        if (comments.isNotBlank()) {
            ListItemSecondaryText(comments, isSelected = isSelected)
        }
    }
}

@Preview
@Composable
private fun PlayListItemPreview(
    @PreviewParameter(PlayPreviewParameterProvider::class) play: Play,
) {
    BggAppTheme {
        PlayListItem(
            play = play,
            showGameName = true,
            markupConverter = XmlApiMarkupConverter(LocalContext.current),
            isSelected = false,
        )
    }
}

private class PlayPreviewParameterProvider : PreviewParameterProvider<Play> {
    override val values = sequenceOf(
        Play(
            internalId = INVALID_ID.toLong(),
            playId = INVALID_ID,
            dateInMillis = System.currentTimeMillis(),
            gameId = 13,
            gameName = "CATAN",
            quantity = 1,
            length = 92,
            location = "House",
//    incomplete = false,
//    val noWinStats = false,
            comments = "Lots of 5s and 9s got rolled!",
//    val syncTimestamp: Long = 0L,
//    private val initialPlayerCount: Int = 0,
            dirtyTimestamp = 1L,
            updateTimestamp = 1L,
            deleteTimestamp = 0L,
//    val startTime: Long = 0L,
//    val imageUrl: String = "",
//    val thumbnailUrl: String = "",
//    val heroImageUrl: String = "",
//    val updatedPlaysTimestamp: Long = 0L,
//    val gameIsCustomSorted: Boolean = false,
//    val subtypes: List<String> = emptyList(),
//    private val _players: List<PlayPlayer>? = null,
        ),
    )
}