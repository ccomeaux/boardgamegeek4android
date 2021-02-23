package com.boardgamegeek.ui

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.INVALID_ID
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.adapter.PlayPlayerAdapter
import com.boardgamegeek.ui.viewmodel.PlayViewModel
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.ImageUtils
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import com.boardgamegeek.util.UIUtils
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_play.*
import org.jetbrains.anko.support.v4.longToast

class PlayFragment : Fragment(R.layout.fragment_play) {
    private var play: PlayEntity? = null
    private var hasBeenNotified = false

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val viewModel by activityViewModels<PlayViewModel>()
    private val adapter: PlayPlayerAdapter by lazy { PlayPlayerAdapter() }
    private val markupConverter by lazy { XmlApiMarkupConverter(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        hasBeenNotified = savedInstanceState?.getBoolean(KEY_HAS_BEEN_NOTIFIED) ?: false
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout?.setBggColors()
        swipeRefreshLayout?.setOnRefreshListener { viewModel.refresh() }
        thumbnailView.setOnClickListener {
            play?.let { play ->
                GameActivity.start(requireContext(), play.gameId, play.gameName)
            }
        }
        timerEndButton.setOnClickListener {
            play?.let { play ->
                LogPlayActivity.endPlay(context, play.internalId, play.gameId, play.gameName, play.thumbnailUrl, play.imageUrl, play.heroImageUrl)
            }
        }
        playersView.adapter = adapter
        viewModel.play.observe(viewLifecycleOwner) {
            swipeRefreshLayout?.post { swipeRefreshLayout?.isRefreshing = it?.status == Status.REFRESHING }
            play = it.data
            val message = resources.getString(R.string.empty_play)
            when {
                it?.data == null -> showError(message)
                it.status == Status.ERROR -> {
                    showData(it.data)
                    longToast(if (it.message.isNotBlank()) it.message else message)
                }
                else -> {
                    showData(it.data)
                    maybeShowNotification()
                    requireActivity().invalidateOptionsMenu()
                }
            }
        }
        viewModel.updatedId.observe(viewLifecycleOwner) {
            Handler().postDelayed({
                SyncService.sync(requireContext(), SyncService.FLAG_SYNC_PLAYS)
                viewModel.refresh()
            }, 200)
        }
    }

    private fun showError(message: String) {
        emptyView.text = message
        emptyView.fadeIn()
        listContainer.fadeOut()
        progressBar.hide()
    }

    private fun showData(play: PlayEntity) {
        thumbnailView.safelyLoadImage(play.imageUrl, play.thumbnailUrl, play.heroImageUrl, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                if (isAdded) gameNameView?.setBackgroundResource(R.color.black_overlay_light)
            }

            override fun onFailedImageLoad() {}
        })

        gameNameView.text = play.gameName
        dateView.text = play.dateForDisplay(requireContext())

        quantityView.text = resources.getQuantityString(R.plurals.times_suffix, play.quantity, play.quantity)
        quantityView.isVisible = play.quantity != 1

        locationView.setTextOrHide(play.location)
        locationLabel.isVisible = play.location.isNotBlank()

        when {
            play.length > 0 -> {
                lengthLabel.isVisible = true
                lengthView.text = play.length.asMinutes(requireContext())
                lengthView.isVisible = true
                timerView.isVisible = false
                timerView.stop()
                timerEndButton.isVisible = false
            }
            play.hasStarted() -> {
                lengthLabel.isVisible = true
                lengthView.text = ""
                lengthView.isVisible = true
                timerView.isVisible = true
                UIUtils.startTimerWithSystemTime(timerView, play.startTime)
                timerEndButton.isVisible = true
            }
            else -> {
                lengthLabel.isVisible = false
                lengthView.isVisible = false
                timerView.isVisible = false
                timerEndButton.isVisible = false
            }
        }

        incompleteView.isVisible = play.incomplete
        noWinStatsView.isVisible = play.noWinStats

        commentsView.setTextMaybeHtml(markupConverter.toHtml(play.comments))
        commentsView.isVisible = play.comments.isNotBlank()
        commentsLabel.isVisible = play.comments.isNotBlank()

        when {
            play.deleteTimestamp > 0 -> {
                pendingTimestampView.isVisible = true
                pendingTimestampView.format = getString(R.string.delete_pending_prefix)
                pendingTimestampView.timestamp = play.deleteTimestamp
            }
            play.updateTimestamp > 0 -> {
                pendingTimestampView.isVisible = true
                pendingTimestampView.format = getString(R.string.update_pending_prefix)
                pendingTimestampView.timestamp = play.updateTimestamp
            }
            else -> pendingTimestampView.isVisible = false
        }

        if (play.dirtyTimestamp > 0) {
            dirtyTimestampView.format = getString(if (play.isSynced) R.string.editing_prefix else R.string.draft_prefix)
            dirtyTimestampView.timestamp = play.dirtyTimestamp
            dirtyTimestampView.isVisible = true
        } else {
            dirtyTimestampView.isVisible = false
        }
        if (play.playId > 0) {
            playIdView.text = resources.getString(R.string.play_id_prefix, play.playId.toString())
        }
        syncTimestampView.timestamp = play.syncTimestamp

        // players
        playersLabel.isVisible = play.players.isNotEmpty()
        adapter.players = play.players

        emptyView.fadeOut()
        listContainer.fadeIn()
        progressBar.hide()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HAS_BEEN_NOTIFIED, hasBeenNotified)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.play, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_discard)?.isVisible =
                (play?.playId ?: INVALID_ID) != INVALID_ID && (play?.dirtyTimestamp ?: 0L) > 0
        menu.findItem(R.id.menu_edit)?.isVisible = play != null
        menu.findItem(R.id.menu_send)?.isVisible = (play?.dirtyTimestamp ?: 0L) > 0
        menu.findItem(R.id.menu_delete)?.isVisible = play != null
        menu.findItem(R.id.menu_rematch)?.isVisible = play != null
        menu.findItem(R.id.menu_change_game)?.isVisible = play != null
        menu.findItem(R.id.menu_share)?.isVisible = play != null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_discard -> {
                DialogUtils.createDiscardDialog(activity, R.string.play, false, false) {
                    logDataManipulationAction("Discard")
                    viewModel.discard()
                }.show()
                return true
            }
            R.id.menu_edit -> {
                play?.let {
                    logDataManipulationAction("Edit")
                    LogPlayActivity.editPlay(activity, it.internalId, it.gameId, it.gameName, it.thumbnailUrl, it.imageUrl, it.heroImageUrl)
                    return true
                }
            }
            R.id.menu_send -> {
                logDataManipulationAction("Send")
                viewModel.send()
                return true
            }
            R.id.menu_delete -> {
                requireContext().createThemedBuilder()
                        .setMessage(R.string.are_you_sure_delete_play)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            play?.let {
                                if (it.hasStarted()) requireContext().cancel(TAG_PLAY_TIMER, it.internalId)
                                logDataManipulationAction("Delete")
                                viewModel.delete()
                                requireActivity().finish() // don't want to show an empty screen upon return
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
                return true
            }
            R.id.menu_rematch -> {
                play?.let {
                    logDataManipulationAction("Rematch")
                    LogPlayActivity.rematch(context, it.internalId, it.gameId, it.gameName, it.thumbnailUrl, it.imageUrl, it.heroImageUrl, it.arePlayersCustomSorted())
                    requireActivity().finish() // don't want to show the "old" play upon return
                    return true
                }
            }
            R.id.menu_change_game -> {
                play?.let {
                    logDataManipulationAction("ChangeGame")
                    CollectionActivity.startForGameChange(requireContext(), it.internalId)
                    requireActivity().finish() // don't want to show the "old" play upon return
                }
                return true
            }
            R.id.menu_share -> {
                play?.let {
                    val subject = getString(R.string.play_description_game_segment, it.gameName) + getString(R.string.play_description_date_segment, it.dateInMillis.asDate(requireContext()))
                    val sb = StringBuilder()
                    sb.append(getString(R.string.play_description_game_segment, it.gameName))
                    if (it.dateInMillis != PlayEntity.UNKNOWN_DATE) sb.append(getString(R.string.play_description_date_segment, it.dateInMillis.asDate(requireContext(), includeWeekDay = true)))
                    if (it.quantity > 1) sb.append(resources.getQuantityString(R.plurals.play_description_quantity_segment, it.quantity, it.quantity))
                    if (it.location.isNotBlank()) sb.append(getString(R.string.play_description_location_segment, it.location))
                    if (it.length > 0) sb.append(getString(R.string.play_description_length_segment, it.length.asTime()))
                    if (it.players.isNotEmpty()) {
                        sb.append(" ").append(getString(R.string.with))
                        if (it.arePlayersCustomSorted()) {
                            for (player in it.players) {
                                sb.append("\n").append(describePlayer(player))
                            }
                        } else {
                            for (i in it.players.indices) {
                                it.getPlayerAtSeat(i + 1)?.let { player ->
                                    sb.append("\n").append(describePlayer(player))
                                }
                            }
                        }
                    }
                    if (it.comments.isNotBlank()) {
                        sb.append("\n\n").append(it.comments)
                    }
                    if (it.playId > 0) {
                        sb.append("\n\n").append(getString(R.string.play_description_play_url_segment, it.playId.toString()).trim())
                    } else {
                        sb.append("\n\n").append(getString(R.string.play_description_game_url_segment, it.gameId.toString()).trim())
                    }
                    val text = sb.toString()
                    requireActivity().share(subject, text, R.string.share_play_title)
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                        param(FirebaseAnalytics.Param.ITEM_ID, it.playId.toString())
                        param(FirebaseAnalytics.Param.ITEM_NAME, subject)
                    }
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun describePlayer(player: PlayPlayerEntity): String {
        val sb = StringBuilder()
        if (player.seat != PlayPlayerEntity.SEAT_UNKNOWN) sb.append(getString(R.string.player_description_starting_position_segment, player.seat))
        sb.append(player.name)
        if (player.username.isNotEmpty()) sb.append(getString(R.string.player_description_username_segment, player.username))
        if (player.isNew) sb.append(getString(R.string.player_description_new_segment))
        if (player.color?.isBlank() == false) sb.append(getString(R.string.player_description_color_segment, player.color))
        if (player.score?.isBlank() == false) sb.append(getString(R.string.player_description_score_segment, player.score))
        if (player.isWin) sb.append(getString(R.string.player_description_win_segment))
        return sb.toString()
    }

    private fun logDataManipulationAction(action: String) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param("Action", action)
            param("GameName", play?.gameName.orEmpty())
        }
    }

    private fun maybeShowNotification() {
        play?.let {
            if (it.hasStarted() && !hasBeenNotified) {
                requireContext().launchPlayingNotification(
                        it.internalId,
                        it.gameName,
                        it.location,
                        it.playerCount,
                        it.startTime,
                        it.thumbnailUrl,
                        it.imageUrl,
                        it.heroImageUrl,
                )
                hasBeenNotified = true
            }
        }
    }

    companion object {
        private const val KEY_HAS_BEEN_NOTIFIED = "HAS_BEEN_NOTIFIED"
    }
}