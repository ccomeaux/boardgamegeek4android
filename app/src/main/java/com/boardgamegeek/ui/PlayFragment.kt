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
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
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
    private var internalId = BggContract.INVALID_ID.toLong()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var playId = BggContract.INVALID_ID
    private var hasStarted = false
    private var location = ""
    private var playerCount = 0
    private var startTime = 0L
    private var thumbnailUrl = ""
    private var imageUrl = ""
    private var heroImageUrl = ""
    private var customPlayerSort = false
    private var shortDescription = ""
    private var longDescription = ""
    private var isDirty = false
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
        thumbnailView.setOnClickListener { GameActivity.start(requireContext(), gameId, gameName) }
        timerEndButton.setOnClickListener {
            LogPlayActivity.endPlay(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl)
        }
        playersView.adapter = adapter
        viewModel.play.observe(viewLifecycleOwner) {
            swipeRefreshLayout?.post { swipeRefreshLayout?.isRefreshing = it?.status == Status.REFRESHING }
            val message = resources.getString(R.string.empty_play, internalId.toString())
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
        internalId = play.internalId
        gameId = play.gameId
        gameName = play.gameName
        playId = play.playId
        hasStarted = play.hasStarted()
        isDirty = play.dirtyTimestamp > 0
        location = play.location
        playerCount = play.playerCount
        startTime = play.startTime
        thumbnailUrl = play.thumbnailUrl
        imageUrl = play.imageUrl
        heroImageUrl = play.heroImageUrl
        customPlayerSort = play.arePlayersCustomSorted()
        shortDescription = getString(R.string.play_description_game_segment, gameName) + getString(R.string.play_description_date_segment, play.dateForDisplay(requireContext()))
        longDescription = play.describe(requireContext(), true)

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
            dirtyTimestampView.format = getString(if (play.playId > 0) R.string.editing_prefix else R.string.draft_prefix)
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

    override fun onResume() {
        super.onResume()
        if (hasStarted) {
            showNotification()
            hasBeenNotified = true
        }
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
        menu.findItem(R.id.menu_send)?.isVisible = isDirty
        menu.findItem(R.id.menu_discard)?.isVisible = playId > 0 && isDirty
        menu.findItem(R.id.menu_share)?.isEnabled = playId > 0
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
                logDataManipulationAction("Edit")
                LogPlayActivity.editPlay(activity, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl)
                return true
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
                            if (hasStarted) cancelNotification()
                            logDataManipulationAction("Delete")
                            viewModel.delete()
                            requireActivity().finish() // don't want to show an empty screen upon return
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .setCancelable(true)
                        .show()
                return true
            }
            R.id.menu_rematch -> {
                logDataManipulationAction("Rematch")
                LogPlayActivity.rematch(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort)
                requireActivity().finish() // don't want to show the "old" play upon return
                return true
            }
            R.id.menu_change_game -> {
                logDataManipulationAction("ChangeGame")
                CollectionActivity.startForGameChange(requireContext(), internalId)
                requireActivity().finish() // don't want to show the "old" play upon return
                return true
            }
            R.id.menu_share -> {
                requireActivity().share(shortDescription, longDescription, R.string.share_play_title)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE) {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                    param(FirebaseAnalytics.Param.ITEM_ID, playId.toString())
                    param(FirebaseAnalytics.Param.ITEM_NAME, shortDescription)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logDataManipulationAction(action: String) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param("Action", action)
            param("GameName", gameName)
        }
    }

    private fun maybeShowNotification() {
        if (hasStarted) {
            showNotification()
        } else if (hasBeenNotified) {
            cancelNotification()
        }
    }

    private fun showNotification() {
        requireContext().launchPlayingNotification(
                internalId,
                gameName,
                location,
                playerCount,
                startTime,
                thumbnailUrl,
                imageUrl,
                heroImageUrl,
        )
    }

    private fun cancelNotification() {
        requireContext().cancel(TAG_PLAY_TIMER, internalId)
    }

    companion object {
        private const val KEY_HAS_BEEN_NOTIFIED = "HAS_BEEN_NOTIFIED"
    }
}