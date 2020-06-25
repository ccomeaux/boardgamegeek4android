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
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.PlaysSummaryViewModel
import kotlinx.android.synthetic.main.fragment_plays_summary.*
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.startActivity

class PlaysSummaryFragment : Fragment() {
    private var syncPlays = false
    private var syncPlaysTimestamp = 0L
    private var oldestSyncDate = Long.MAX_VALUE
    private var newestSyncDate = 0L

    val viewModel by activityViewModels<PlaysSummaryViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_plays_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout.setBggColors()
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = viewModel.refresh()
        }

        syncButton.setOnClickListener {
            defaultSharedPreferences[PREFERENCES_KEY_SYNC_PLAYS] = true
            defaultSharedPreferences[PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP] = System.currentTimeMillis()
            viewModel.refresh()
        }

        syncCancelButton.setOnClickListener {
            defaultSharedPreferences[PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP] = System.currentTimeMillis()
        }

        viewModel.plays.observe(viewLifecycleOwner, Observer { swipeRefreshLayout.isRefreshing = (it.status == Status.REFRESHING) })
        viewModel.playsInProgress.observe(viewLifecycleOwner, Observer { playEntities -> bindInProgressPlays(playEntities) })
        viewModel.playsNotInProgress.observe(viewLifecycleOwner, Observer { playEntities -> bindRecentPlays(playEntities) })
        viewModel.playCount.observe(viewLifecycleOwner, Observer { playCount -> bindPlayCount(playCount ?: 0) })
        viewModel.players.observe(viewLifecycleOwner, Observer { playerEntities -> bindPlayers(playerEntities) })
        viewModel.locations.observe(viewLifecycleOwner, Observer { locationEntities -> bindLocations(locationEntities) })
        viewModel.colors.observe(viewLifecycleOwner, Observer { playerColorEntities -> bindColors(playerColorEntities) })
        viewModel.hIndex.observe(viewLifecycleOwner, Observer {
            hIndexView.text = context?.getText(R.string.game_h_index_prefix, it.description)
            morePlayStatsButton.setOnClickListener {
                startActivity<PlayStatsActivity>()
            }
        })
        viewModel.syncPlays.observe(viewLifecycleOwner, Observer {
            syncPlays = it ?: false
            bindSyncCard()
        })
        viewModel.syncPlaysTimestamp.observe(viewLifecycleOwner, Observer {
            syncPlaysTimestamp = it ?: 0L
            bindSyncCard()
        })
        viewModel.oldestSyncDate.observe(viewLifecycleOwner, Observer {
            oldestSyncDate = it ?: Long.MAX_VALUE
            bindStatusMessage()
        })
        viewModel.newestSyncDate.observe(viewLifecycleOwner, Observer {
            newestSyncDate = it ?: 0L
            bindStatusMessage()
        })
    }

    private fun bindStatusMessage() {
        syncStatusView.text = when {
            oldestSyncDate == Long.MAX_VALUE && newestSyncDate <= 0L -> getString(R.string.plays_sync_status_none)
            oldestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_new), millisAsDate(newestSyncDate))
            newestSyncDate <= 0L -> String.format(getString(R.string.plays_sync_status_old), millisAsDate(oldestSyncDate))
            else -> String.format(getString(R.string.plays_sync_status_range), millisAsDate(oldestSyncDate), millisAsDate(newestSyncDate))
        }
    }

    private fun millisAsDate(millis: Long) = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE)

    private fun bindSyncCard() {
        syncCard.isGone = syncPlays || syncPlaysTimestamp > 0
    }

    private fun bindInProgressPlays(plays: List<PlayEntity>?) {
        val numberOfPlaysInProgress = plays?.size ?: 0
        val visibility = if (numberOfPlaysInProgress == 0) View.GONE else View.VISIBLE
        playsInProgressSubtitle.visibility = visibility
        playsInProgressContainer.visibility = visibility
        recentPlaysSubtitle.visibility = visibility

        playsInProgressContainer.removeAllViews()
        if (numberOfPlaysInProgress > 0) {
            plays?.forEach {
                addPlayToContainer(it, playsInProgressContainer)
            }
            playsCard.isVisible = true
        }
    }

    private fun bindRecentPlays(plays: List<PlayEntity>?) {
        recentPlaysContainer.removeAllViews()
        if (plays != null && plays.isNotEmpty()) {
            plays.forEach { addPlayToContainer(it, recentPlaysContainer) }
            playsCard.isVisible = true
            recentPlaysContainer.isVisible = true
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
        morePlaysButton.isVisible = true
        morePlaysButton.setText(R.string.more)
        val morePlaysCount = playCount - PlaysSummaryViewModel.ITEMS_TO_DISPLAY
        if (morePlaysCount > 0) {
            morePlaysButton.text = String.format(getString(R.string.more_suffix), morePlaysCount)
        }
        morePlaysButton.setOnClickListener { startActivity<PlaysActivity>() }
    }

    private fun bindPlayers(players: List<PlayerEntity>?) {
        playersContainer.removeAllViews()
        if (players == null || players.isEmpty()) {
            playersCard.isGone = true
            morePlayersButton.isGone = true
        } else {
            playersCard.isVisible = true
            for (player in players) {
                createRowWithPlayCount(playersContainer, player.description, player.playCount).apply {
                    setOnClickListener { BuddyActivity.start(requireContext(), player.username, player.name) }
                }
            }
            morePlayersButton.isVisible = true
        }
        morePlayersButton.setOnClickListener { PlayersActivity.start(requireContext()) }
    }

    private fun bindLocations(locations: List<LocationEntity>?) {
        locationsContainer.removeAllViews()
        if (locations == null || locations.isEmpty()) {
            locationsCard.isGone = true
            moreLocationsButton.isGone = true
        } else {
            locationsCard.isVisible = true
            for ((name, playCount) in locations) {
                createRowWithPlayCount(locationsContainer, name, playCount).apply {
                    setOnClickListener { LocationActivity.start(context, name) }
                }
            }
            moreLocationsButton.isVisible = true
        }
        moreLocationsButton.setOnClickListener { startActivity<LocationsActivity>() }
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
        colorsContainer.removeAllViews()
        if (colors == null || colors.isEmpty()) {
            colorsCard.isGone = true
        } else {
            colorsCard.isVisible = true
            colors.forEach {
                colorsContainer.addView(requireContext().createSmallCircle().apply {
                    setColorViewValue(it.rgb)
                })
            }
        }
        editColorsButton.setOnClickListener {
            PlayerColorsActivity.start(requireContext(), AccountUtils.getUsername(context), null)
        }
    }
}
