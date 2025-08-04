package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPlayersBinding
import com.boardgamegeek.extensions.requestFocusAndKeyboard
import com.boardgamegeek.extensions.setActionBarCount
import com.boardgamegeek.model.Player
import com.boardgamegeek.ui.compose.ListItemDefaults
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.PlayersViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayersFragment : Fragment() {
    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<PlayersViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(viewModel) }

    private var hasFilter = false
    private var playerCount = -1
        set(value) {
            field = value
            requireActivity().invalidateOptionsMenu()
        }
    private var sortType: Player.SortType? = null
        set(value) {
            field = value
            requireActivity().invalidateOptionsMenu()
        }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addMenuProvider()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter
            )
        )

        binding.filterEditText.doAfterTextChanged { s ->
            viewModel.filter(s.toString())
        }

        viewModel.sortType.observe(viewLifecycleOwner) {
            it?.let { sortType = it }
        }

        viewModel.filter.observe(viewLifecycleOwner) {
            it?.let {
                hasFilter = it.isNotEmpty()
                setVisibility()
            }
        }

        viewModel.players.observe(viewLifecycleOwner) {
            playerCount = it?.size ?: 0
            it?.let {
                adapter.players = it
                binding.progressBar.hide()
                setVisibility()
            }
        }
    }

    private fun setVisibility() {
        binding.emptyContainer.isVisible = playerCount == 0
        binding.contentContainer.isVisible = playerCount > 0 || hasFilter
        binding.recyclerView.isVisible = playerCount > 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.players, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                menu.findItem(
                    when (sortType) {
                        Player.SortType.NAME -> R.id.menu_sort_name
                        Player.SortType.PLAY_COUNT -> R.id.menu_sort_quantity
                        Player.SortType.WIN_COUNT -> R.id.menu_sort_wins
                        else -> View.NO_ID
                    }
                )?.apply {
                    isChecked = true
                    menu.setActionBarCount(R.id.menu_list_count, playerCount, title.toString())
                }
                menu.findItem(R.id.menu_filter)?.let {
                    it.isEnabled = playerCount > 0 || !hasFilter
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_sort_name -> viewModel.sort(Player.SortType.NAME)
                    R.id.menu_sort_quantity -> viewModel.sort(Player.SortType.PLAY_COUNT)
                    R.id.menu_sort_wins -> viewModel.sort(Player.SortType.WIN_COUNT)
                    R.id.menu_filter -> {
                        if (binding.filterView.isVisible) {
                            if (hasFilter) return true
                            binding.filterView.isVisible = false
                        } else {
                            binding.filterView.isVisible = true
                            binding.filterEditText.requestFocusAndKeyboard()
                        }
                    }
                    else -> return false
                }
                return true
            }
        })
    }

    class PlayersAdapter(val viewModel: PlayersViewModel) : RecyclerView.Adapter<PlayersAdapter.ViewHolder>(), SectionCallback {
        var players: List<Player> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ComposeView(parent.context))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            players.getOrNull(position)?.let {
                holder.bind(it)
            }
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (players.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = viewModel.getSectionHeader(players.getOrNull(position))
            val lastLetter = viewModel.getSectionHeader(players.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                players.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(players.getOrNull(position))
            }
        }

        inner class ViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bind(player: Player) {
                composeView.setContent {
                    PlayerListItem(
                        player,
                        onClick = {
                            BuddyActivity.start(itemView.context, it.username, it.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerListItem(
    player: Player,
    modifier: Modifier = Modifier,
    onClick: (Player) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemDefaults.threeLineHeight)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(player) })
            .padding(ListItemDefaults.paddingValues)

    ) {
        val primaryText = buildAnnotatedString {
            withStyle(style = ListItemDefaults.primaryTextStyle().toSpanStyle()) {
                if (player.userFullName.isNullOrBlank()) {
                    append(player.name)
                } else if (player.userFullName.contains(player.name)) {
                    val splits = player.userFullName.split(player.name)
                    splits.forEachIndexed { i, split ->
                        if (i > 0)
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append(player.name)
                            }
                        append(split)
                    }
                } else {
                    append(player.userFullName + "(")
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(player.name)
                    }
                    append(")")
                }
            }
        }
        ListItemPrimaryText(primaryText)
        if (player.username.isNotBlank()) {
            ListItemSecondaryText(player.username)
        }
        ListItemSecondaryText(pluralStringResource(R.plurals.plays_suffix, player.playCount, player.playCount)) // TODO show wins when sorted that way
    }
}

@Preview
@Composable
private fun PlayerListItemPreview() {
    BggAppTheme {
        PlayerListItem(
            Player(
                name = "Chris",
                username = "ccomeaux",
                userFullName = "Mr. Chris Comeaux"
            )
        )
    }
}