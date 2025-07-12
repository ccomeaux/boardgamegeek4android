package com.boardgamegeek.ui

import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.databinding.FragmentHotnessBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.HotGame
import com.boardgamegeek.model.Status
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.compose.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.HotnessViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class HotnessFragment : Fragment(), ActionMode.Callback {
    private var _binding: FragmentHotnessBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<HotnessViewModel>()
    private val adapter: HotGamesAdapter by lazy { createAdapter() }
    private var actionMode: ActionMode? = null

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHotnessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.errorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                binding.coordinatorLayout.snackbar(it)
            }
        }

        viewModel.loggedPlayResult.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                requireContext().notifyLoggedPlay(it)
            }
        }

        viewModel.hotGames.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                when (status) {
                    Status.REFRESHING -> binding.progressView.show()
                    Status.ERROR -> {
                        binding.emptyView.text = message
                        binding.emptyView.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.progressView.hide()
                    }
                    Status.SUCCESS -> {
                        val games = data.orEmpty()
                        adapter.games = games
                        if (games.isEmpty()) {
                            binding.emptyView.setText(R.string.empty_hotness)
                            binding.emptyView.isVisible = true
                            binding.recyclerView.isVisible = false
                        } else {
                            binding.emptyView.isVisible = false
                            binding.recyclerView.isVisible = true
                        }
                        binding.progressView.hide()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    private fun createAdapter(): HotGamesAdapter {
        return HotGamesAdapter(object : Callback {
            override fun onItemClick(position: Int): Boolean {
                if (actionMode == null) return false
                toggleSelection(position)
                return true
            }

            override fun onItemLongClick(position: Int): Boolean {
                if (actionMode != null) return false
                actionMode = requireActivity().startActionMode(this@HotnessFragment)
                if (actionMode == null) return false
                toggleSelection(position)
                return true
            }

            private fun toggleSelection(position: Int) {
                adapter.toggleSelection(position)
                actionMode?.let {
                    if (adapter.selectedItemCount == 0) {
                        it.finish()
                    } else {
                        it.invalidate()
                    }
                }
            }
        })
    }

    interface Callback {
        fun onItemClick(position: Int): Boolean
        fun onItemLongClick(position: Int): Boolean
    }

    inner class HotGamesAdapter(private val callback: Callback?) : RecyclerView.Adapter<HotGamesAdapter.ViewHolder>(), AutoUpdatableAdapter {
        private val selectedItems = SparseBooleanArray()

        init {
            setHasStableIds(true)
        }

        var games: List<HotGame> by Delegates.observable(emptyList()) { _, old, new ->
            autoNotify(old, new) { o, n -> o == n }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ComposeView(parent.context))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = games.size

        override fun getItemId(position: Int) = games.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bind(position: Int) {
                composeView.setContent {
                    HotnessListItem(
                        hotGame = games[position],
                        isSelected = selectedItems[position, false],
                        onClick = { hotGame ->
                            if (callback?.onItemClick(position) != true) {
                                GameActivity.start(requireContext(), hotGame.id, hotGame.name, hotGame.thumbnailUrl)
                            }
                        },
                        onLongClick = {
                            callback?.onItemLongClick(position)
                        }
                    )
                }
            }
        }

        fun toggleSelection(position: Int) {
            selectedItems.toggle(position)
            notifyItemChanged(position)
        }

        fun clearSelections() {
            val oldSelectedItems = selectedItems.clone()
            selectedItems.clear()
            oldSelectedItems.filterTrue().forEach { notifyItemChanged(it) }
        }

        val selectedItemCount: Int
            get() = selectedItems.filterTrue().size

        fun getSelectedGames() = selectedItems.filterTrue().mapNotNull { games.getOrNull(it) }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        adapter.clearSelections()
        mode.menuInflater.inflate(R.menu.game_context, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter.selectedItemCount
        mode.title = resources.getQuantityString(R.plurals.msg_games_selected, count, count)
        if (Authenticator.isSignedIn(context)) {
            menu.findItem(R.id.menu_log_play_form).isVisible = count == 1
            menu.findItem(R.id.menu_log_play_wizard).isVisible = count == 1
            menu.findItem(R.id.menu_log_play).isVisible = true
        } else {
            menu.findItem(R.id.menu_log_play).isVisible = false
        }
        menu.findItem(R.id.menu_link).isVisible = count == 1
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        adapter.clearSelections()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selectedGames = adapter.getSelectedGames()
        if (selectedGames.isEmpty()) return false
        when (item.itemId) {
            R.id.menu_log_play_form -> {
                selectedGames.firstOrNull()?.let { game ->
                    LogPlayActivity.logPlay(requireContext(), game.id, game.name, game.thumbnailUrl)
                }
            }
            R.id.menu_log_play_quick -> {
                binding.coordinatorLayout.snackbar(resources.getQuantityString(R.plurals.msg_logging_plays, adapter.selectedItemCount))
                for (game in selectedGames) {
                    viewModel.logQuickPlay(game.id, game.name)
                }
            }
            R.id.menu_log_play_wizard -> {
                selectedGames.firstOrNull()?.let { game ->
                    NewPlayActivity.start(requireContext(), game.id, game.name)
                }
            }
            R.id.menu_share -> {
                val shareMethod = "Hotness"
                if (selectedGames.size == 1) {
                    selectedGames.firstOrNull()?.let { game ->
                        requireActivity().shareGame(game.id, game.name, shareMethod)
                    }
                } else {
                    requireActivity().shareGames(selectedGames.map { it.id to it.name }, shareMethod)
                }
            }
            R.id.menu_link -> {
                selectedGames.firstOrNull()?.let { game ->
                    context.linkBgg(game.id)
                }
            }
            else -> return false
        }
        mode.finish()
        return true
    }
}

@Composable
fun HotnessListItem(
    hotGame: HotGame,
    modifier: Modifier = Modifier,
    onClick: (hotGame: HotGame) -> Unit = {},
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier // TODO move some of this to other modifier?
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onLongClick = onLongClick,
            ) {
                onClick(hotGame)
            }
            .padding(
                horizontal = dimensionResource(R.dimen.material_margin_horizontal),
                vertical = 12.dp,
            )
            .then(modifier)
    ) {
        ListItemIndex(hotGame.rank)
        ListItemThumbnail(hotGame.thumbnailUrl)
        Column {
            ListItemPrimaryText(hotGame.name)
            ListItemSecondaryText(
                hotGame.yearPublished.asYear(LocalContext.current),
                modifier = modifier.padding(bottom = ListItemTokens.verticalTextPadding),
                icon = Icons.Outlined.CalendarToday,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HotnessListItemPreview(
    @PreviewParameter(HotGamePreviewParameterProvider::class) hotGame: HotGame,
) {
    BggAppTheme {
        HotnessListItem(hotGame, Modifier)
    }
}

private class HotGamePreviewParameterProvider : PreviewParameterProvider<HotGame> {
    override val values = sequenceOf(
        HotGame(
            rank = 1,
            id = 99,
            name = "Spirit Island",
            yearPublished = 2019,
        ),
        HotGame(
            rank = 22,
            id = 99,
            name = "Star Wars: the Deck Building Game",
            yearPublished = 2023,
        ),
        HotGame(
            rank = 50,
            id = 99,
            name = "Sky Team",
            yearPublished = 2022,
        )
    )
}
