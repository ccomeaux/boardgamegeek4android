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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.databinding.FragmentPlaysSummaryBinding
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.PlaysSummaryViewModel
import org.jetbrains.anko.support.v4.startActivity

class PlaysSummaryFragment : Fragment() {
    private var _binding: FragmentPlaysSummaryBinding? = null
    private val binding get() = _binding!!
    private var syncPlays = false
    private var syncPlaysTimestamp = 0L
    private var oldestSyncDate = Long.MAX_VALUE
    private var newestSyncDate = 0L

    val viewModel by lazy {
        ViewModelProvider(this).get(PlaysSummaryViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
            requireContext().setSyncPlays()
            requireContext().setSyncPlaysTimestamp()
            viewModel.refresh()
        }

        binding.syncCancelButton.setOnClickListener {
            requireContext().setSyncPlaysTimestamp()
        }

        viewModel.plays.observe(this, Observer { binding.swipeRefreshLayout.isRefreshing = (it.status == Status.REFRESHING) })
        viewModel.playsInProgress.observe(this, Observer { playEntities -> bindInProgressPlays(playEntities) })
        viewModel.playsNotInProgress.observe(this, Observer { playEntities -> bindRecentPlays(playEntities) })
        viewModel.playCount.observe(this, Observer { playCount -> bindPlayCount(playCount ?: 0) })
        viewModel.players.observe(this, Observer { playerEntities -> bindPlayers(playerEntities) })
        viewModel.locations.observe(this, Observer { locationEntities -> bindLocations(locationEntities) })
        viewModel.colors.observe(this, Observer { playerColorEntities -> bindColors(playerColorEntities) })
        viewModel.hIndex().observe(this, Observer {
            binding.hIndexView.text = context?.getText(R.string.game_h_index_prefix, it.description)
            binding.morePlayStatsButton.setOnClickListener {
                startActivity<PlayStatsActivity>()
            }
        })
        viewModel.syncPlays.observe(this, Observer {
            syncPlays = it ?: false
            bindSyncCard()
        })
        viewModel.syncPlaysTimestamp.observe(this, Observer {
            syncPlaysTimestamp = it ?: 0L
            bindSyncCard()
        })
        viewModel.oldestSyncDate.observe(this, Observer {
            oldestSyncDate = it ?: Long.MAX_VALUE
            bindStatusMessage()
        })
        viewModel.newestSyncDate.observe(this, Observer {
            newestSyncDate = it ?: 0L
            bindStatusMessage()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindStatusMessage() {
        binding.syncStatusView.text = when {
            oldestSyncDate == Long.MAX_VALUE && newestSyncDate <= 0L -> getString(R.string.plays_sync_status_none)
            oldestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_new), millisAsDate(newestSyncDate))
            newestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_old), millisAsDate(oldestSyncDate))
            else -> String.format(getString(R.string.plays_sync_status_range), millisAsDate(oldestSyncDate), millisAsDate(newestSyncDate))
        }
    }

    private fun millisAsDate(millis: Long) = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE)

    private fun bindSyncCard() {
        binding.syncCard.isGone = syncPlays || syncPlaysTimestamp > 0
    }

    private fun bindInProgressPlays(plays: List<PlayEntity>?) {
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

    private fun bindRecentPlays(plays: List<PlayEntity>?) {
        binding.recentPlaysContainer.removeAllViews()
        if (plays != null && plays.isNotEmpty()) {
            plays.forEach { addPlayToContainer(it, binding.recentPlaysContainer) }
            binding.playsCard.isVisible = true
            binding.recentPlaysContainer.isVisible = true
        }
    }

    private fun addPlayToContainer(play: PlayEntity, container: LinearLayout) {
        val view = createRow(container, play.gameName, play.describe(requireContext(), true))
        view.setOnClickListener {
            PlayActivity.start(context,
                    play.internalId,
                    play.gameId,
                    play.gameName,
                    play.thumbnailUrl,
                    play.imageUrl,
                    play.heroImageUrl)
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

    private fun bindPlayers(players: List<PlayerEntity>?) {
        binding.playersContainer.removeAllViews()
        if (players == null || players.isEmpty()) {
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

    private fun bindLocations(locations: List<LocationEntity>?) {
        binding.locationsContainer.removeAllViews()
        if (locations == null || locations.isEmpty()) {
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

    private fun bindColors(colors: List<PlayerColorEntity>?) {
        binding.colorsContainer.removeAllViews()
        if (colors == null || colors.isEmpty()) {
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
            PlayerColorsActivity.start(requireContext(), AccountUtils.getUsername(context), null)
        }
    }
}
