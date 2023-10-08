package com.boardgamegeek.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPlaysSummaryBinding
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.PlaysSummaryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaysSummaryFragment : Fragment() {
    private var _binding: FragmentPlaysSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<PlaysSummaryViewModel>()
    private var syncPlays = false
    private var syncPlaysTimestamp = 0L
    private var oldestSyncDate = Long.MAX_VALUE
    private var newestSyncDate = 0L

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPlaysSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefreshLayout.setBggColors()
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = viewModel.refresh()
        }

        binding.syncButton.setOnClickListener {
            val prefs = requireContext().preferences()
            prefs[PREFERENCES_KEY_SYNC_PLAYS] = true
            prefs[PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP] = System.currentTimeMillis()
            viewModel.refresh()
        }

        binding.syncCancelButton.setOnClickListener {
            requireContext().preferences()[PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP] = System.currentTimeMillis()
        }

        viewModel.plays.observe(viewLifecycleOwner) { binding.swipeRefreshLayout.isRefreshing = (it.status == Status.REFRESHING) }
        viewModel.playsInProgress.observe(viewLifecycleOwner) { plays -> bindInProgressPlays(plays) }
        viewModel.playsNotInProgress.observe(viewLifecycleOwner) { plays -> bindRecentPlays(plays) }
        viewModel.playCount.observe(viewLifecycleOwner) { playCount -> bindPlayCount(playCount ?: 0) }
        viewModel.players.observe(viewLifecycleOwner) { players -> bindPlayers(players) }
        viewModel.locations.observe(viewLifecycleOwner) { locations -> bindLocations(locations) }
        viewModel.colors.observe(viewLifecycleOwner) { playerColors -> bindColors(playerColors) }
        viewModel.hIndex.observe(viewLifecycleOwner) {
            binding.hIndexView.text = context?.getText(R.string.game_h_index_prefix, it.description)
            binding.morePlayStatsButton.setOnClickListener {
                startActivity<PlayStatsActivity>()
            }
        }
        viewModel.syncPlays.observe(viewLifecycleOwner) {
            syncPlays = it ?: false
            bindSyncCard()
        }
        viewModel.syncPlaysTimestamp.observe(viewLifecycleOwner) {
            syncPlaysTimestamp = it ?: 0L
            bindSyncCard()
        }
        viewModel.oldestSyncDate.observe(viewLifecycleOwner) {
            oldestSyncDate = it ?: Long.MAX_VALUE
            bindStatusMessage()
        }
        viewModel.newestSyncDate.observe(viewLifecycleOwner) {
            newestSyncDate = it ?: 0L
            bindStatusMessage()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindStatusMessage() {
        binding.syncStatusView.text = when {
            oldestSyncDate == Long.MAX_VALUE && newestSyncDate <= 0L -> getString(R.string.plays_sync_status_none)
            oldestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_new), newestSyncDate.asDate())
            newestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_old), oldestSyncDate.asDate())
            else -> String.format(
                getString(R.string.plays_sync_status_range),
                oldestSyncDate.asDate(),
                newestSyncDate.asDate()
            )
        }
    }

    private fun Long.asDate() = this.formatDateTime(requireContext(), flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)

    private fun bindSyncCard() {
        binding.syncCard.isGone = syncPlays || syncPlaysTimestamp > 0
    }

    private fun bindInProgressPlays(plays: List<Play>?) {
        val numberOfPlaysInProgress = plays?.size ?: 0
        val visibility = if (numberOfPlaysInProgress == 0) View.GONE else View.VISIBLE
        binding.playsInProgressSubtitle.visibility = visibility
        binding.playsInProgressContainer.visibility = visibility
        binding.recentPlaysSubtitle.visibility = visibility

        binding.playsInProgressContainer.removeAllViews()
        if (numberOfPlaysInProgress > 0) {
            plays?.forEach {
                addPlayToContainer(it, binding.playsInProgressContainer)
            }
            binding.playsCard.isVisible = true
        }
    }

    private fun bindRecentPlays(plays: List<Play>?) {
        binding.recentPlaysContainer.removeAllViews()
        if (!plays.isNullOrEmpty()) {
            plays.forEach { addPlayToContainer(it, binding.recentPlaysContainer) }
            binding.playsCard.isVisible = true
            binding.recentPlaysContainer.isVisible = true
        }
    }

    private fun addPlayToContainer(play: Play, container: LinearLayout) {
        val view = createRow(container, play.gameName, play.describe(requireContext(), true))
        view.setOnClickListener {
            PlayActivity.start(requireContext(), play.internalId)
        }
    }

    private fun bindPlayCount(playCount: Int) {
        binding.morePlaysButton.isVisible = true
        binding.morePlaysButton.setText(R.string.more)
        val morePlaysCount = playCount - PlaysSummaryViewModel.ITEMS_TO_DISPLAY
        if (morePlaysCount > 0) {
            binding.morePlaysButton.text = String.format(getString(R.string.more_suffix), morePlaysCount)
        }
        binding.morePlaysButton.setOnClickListener { startActivity<PlaysActivity>() }
    }

    private fun bindPlayers(players: List<Player>?) {
        binding.playersContainer.removeAllViews()
        if (players.isNullOrEmpty()) {
            binding.playersCard.isGone = true
            binding.morePlayersButton.isGone = true
        } else {
            binding.playersCard.isVisible = true
            for (player in players) {
                createRowWithPlayCount(binding.playersContainer, player.description, player.playCount).apply {
                    setOnClickListener { BuddyActivity.start(requireContext(), player.username, player.name) }
                }
            }
            binding.morePlayersButton.isVisible = true
        }
        binding.morePlayersButton.setOnClickListener { PlayersActivity.start(requireContext()) }
    }

    private fun bindLocations(locations: List<Location>?) {
        binding.locationsContainer.removeAllViews()
        if (locations.isNullOrEmpty()) {
            binding.locationsCard.isGone = true
            binding.moreLocationsButton.isGone = true
        } else {
            binding.locationsCard.isVisible = true
            for ((name, playCount) in locations) {
                createRowWithPlayCount(binding.locationsContainer, name, playCount).apply {
                    setOnClickListener { LocationActivity.start(context, name) }
                }
            }
            binding.moreLocationsButton.isVisible = true
        }
        binding.moreLocationsButton.setOnClickListener { startActivity<LocationsActivity>() }
    }

    private fun createRow(container: ViewGroup, title: String, text: String): View {
        return LayoutInflater.from(context).inflate(R.layout.row_play_summary, container, false).apply {
            findViewById<TextView>(R.id.line1).text = title
            findViewById<TextView>(R.id.line2).text = text
            container.addView(this)
        }
    }

    private fun createRowWithPlayCount(container: LinearLayout, title: String, playCount: Int): View {
        return createRow(container, title, resources.getQuantityString(R.plurals.plays_suffix, playCount, playCount))
    }

    private fun bindColors(colors: List<PlayerColor>?) {
        binding.colorsContainer.removeAllViews()
        if (colors.isNullOrEmpty()) {
            binding.colorsCard.isGone = true
        } else {
            binding.colorsCard.isVisible = true
            colors.forEach {
                binding.colorsContainer.addView(requireContext().createSmallCircle().apply {
                    setColorViewValue(it.rgb)
                })
            }
        }
        binding.editColorsButton.setOnClickListener {
            val username = requireContext().preferences()[AccountPreferences.KEY_USERNAME, ""]
            if (username.isNullOrBlank()) {
                toast("Can't figure out your username.")
            } else {
                PlayerColorsActivity.start(requireContext(), username, null)
            }
        }
    }
}
