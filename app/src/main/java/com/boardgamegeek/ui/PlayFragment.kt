package com.boardgamegeek.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentPlayBinding
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.adapter.PlayPlayerAdapter
import com.boardgamegeek.ui.viewmodel.PlayViewModel
import com.boardgamegeek.util.XmlApiMarkupConverter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.Firebase
import dagger.hilt.android.AndroidEntryPoint
import java.util.LinkedList

@AndroidEntryPoint
class PlayFragment : Fragment() {
    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val viewModel by activityViewModels<PlayViewModel>()
    private val adapter: PlayPlayerAdapter by lazy { PlayPlayerAdapter() }
    private val markupConverter by lazy { XmlApiMarkupConverter(requireContext()) }
    private var play: Play? = null
    private var hasBeenNotified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        hasBeenNotified = savedInstanceState?.getBoolean(KEY_HAS_BEEN_NOTIFIED) ?: false

        addMenuProvider()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefreshLayout.setBggColors()
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.refresh() }
        binding.thumbnailView.setOnClickListener {
            play?.let { play ->
                GameActivity.start(requireContext(), play.gameId, play.gameName)
            }
        }
        binding.timerEndButton.setOnClickListener {
            play?.let { play ->
                LogPlayActivity.endPlay(
                    requireContext(),
                    play.internalId,
                    play.gameId,
                    play.gameName,
                    play.robustHeroImageUrl,
                )
            }
        }
        binding.playersView.adapter = adapter
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            it?.let {
                binding.swipeRefreshLayout.post { binding.swipeRefreshLayout.isRefreshing = it }
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { error ->
                showError(error)
            }
        }
        viewModel.play.observe(viewLifecycleOwner) {
            play = it
            requireActivity().invalidateOptionsMenu()
            if (it == null) {
                showError(resources.getString(R.string.empty_play))
            } else {
                showData(it)
                maybeShowNotification()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String) {
        binding.progressBar.hide()
        binding.emptyView.text = message
        binding.emptyView.isVisible = true
        binding.constraintLayout.isVisible = false
    }

    private fun showData(play: Play) {
        binding.thumbnailView.loadImage(
            LinkedList(play.heroImageUrls),
            callback = object : ImageLoadCallback {
                override fun onSuccessfulImageLoad(palette: Palette?) {
                    if (isAdded) binding.gameNameView.setBackgroundResource(R.color.black_overlay_light)
                }

                override fun onFailedImageLoad() {}
            },
        )

        binding.gameNameView.text = play.gameName
        binding.dateView.text = play.dateForDisplay(requireContext())

        binding.quantityView.text = resources.getQuantityString(R.plurals.times_suffix, play.quantity, play.quantity)
        binding.quantityView.isVisible = play.quantity != 1

        binding.locationView.setTextOrHide(play.location)
        binding.locationLabel.isVisible = play.location.isNotBlank()

        when {
            play.length > 0 -> {
                binding.lengthLabel.isVisible = true
                binding.lengthView.text = play.length.asMinutes(requireContext())
                binding.lengthView.isVisible = true
                binding.timerView.isVisible = false
                binding.timerView.stop()
                binding.timerEndButton.isVisible = false
            }
            play.hasStarted() -> {
                binding.lengthLabel.isVisible = true
                binding.lengthView.text = ""
                binding.lengthView.isVisible = true
                binding.timerView.isVisible = true
                binding.timerView.startTimerWithSystemTime(play.startTime)
                binding.timerEndButton.isVisible = true
            }
            else -> {
                binding.lengthLabel.isVisible = false
                binding.lengthView.isVisible = false
                binding.timerView.isVisible = false
                binding.timerEndButton.isVisible = false
            }
        }

        binding.incompleteView.isVisible = play.incomplete
        binding.noWinStatsView.isVisible = play.noWinStats

        binding.commentsView.setTextMaybeHtml(markupConverter.toHtml(play.comments))
        binding.commentsView.isVisible = play.comments.isNotBlank()
        binding.commentsLabel.isVisible = play.comments.isNotBlank()

        when {
            play.deleteTimestamp > 0 -> {
                binding.pendingTimestampView.isVisible = true
                binding.pendingTimestampView.format = getString(R.string.delete_pending_prefix)
                binding.pendingTimestampView.timestamp = play.deleteTimestamp
            }
            play.updateTimestamp > 0 -> {
                binding.pendingTimestampView.isVisible = true
                binding.pendingTimestampView.format = getString(R.string.update_pending_prefix)
                binding.pendingTimestampView.timestamp = play.updateTimestamp
            }
            else -> binding.pendingTimestampView.isVisible = false
        }

        if (play.dirtyTimestamp > 0) {
            binding.dirtyTimestampView.format = getString(if (play.isSynced) R.string.editing_prefix else R.string.draft_prefix)
            binding.dirtyTimestampView.timestamp = play.dirtyTimestamp
            binding.dirtyTimestampView.isVisible = true
        } else {
            binding.dirtyTimestampView.isVisible = false
        }
        if (play.playId > 0) {
            binding.playIdView.text = resources.getString(R.string.play_id_prefix, play.playId.toString())
        }
        binding.syncTimestampView.timestamp = play.syncTimestamp

        // players
        binding.playersLabel.isVisible = play.players.isNotEmpty()
        adapter.players = play.sortedPlayers

        binding.progressBar.hide()
        binding.emptyView.isVisible = false
        binding.constraintLayout.isVisible = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HAS_BEEN_NOTIFIED, hasBeenNotified)
    }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.play, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.menu_discard)?.isVisible = (play?.playId ?: INVALID_ID) != INVALID_ID && (play?.dirtyTimestamp ?: 0L) > 0L
                menu.findItem(R.id.menu_edit)?.isVisible = play != null
                menu.findItem(R.id.menu_send)?.isVisible = (play?.dirtyTimestamp ?: 0L) > 0L
                menu.findItem(R.id.menu_delete)?.isVisible = play != null
                menu.findItem(R.id.menu_rematch)?.isVisible = play != null
                menu.findItem(R.id.menu_change_game)?.isVisible = play != null
                menu.findItem(R.id.menu_share)?.isVisible = play != null
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_discard -> {
                        requireActivity().createDiscardDialog(R.string.play, isNew = true, finishActivity = false) {
                            logDataManipulationAction("Discard")
                            viewModel.discard()
                        }.show()
                        return true
                    }
                    R.id.menu_edit -> {
                        play?.let {
                            logDataManipulationAction("Edit")
                            LogPlayActivity.editPlay(
                                requireContext(),
                                it.internalId,
                                it.gameId,
                                it.gameName,
                                it.robustHeroImageUrl,
                            )
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
                                    if (it.hasStarted()) requireContext().cancelNotification(TAG_PLAY_TIMER, it.internalId)
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
                            LogPlayActivity.rematch(
                                requireContext(),
                                it.internalId,
                                it.gameId,
                                it.gameName,
                                it.robustHeroImageUrl,
                                it.arePlayersCustomSorted()
                            )
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
                            val subject = getString(R.string.play_description_game_segment, it.gameName) + getString(
                                R.string.play_description_date_segment,
                                it.dateInMillis.formatDateTime(requireContext())
                            )
                            val sb = StringBuilder()
                            sb.append(getString(R.string.play_description_game_segment, it.gameName))
                            if (it.dateInMillis != Play.UNKNOWN_DATE) sb.append(
                                getString(
                                    R.string.play_description_date_segment,
                                    it.dateInMillis.formatDateTime(
                                        requireContext(),
                                        DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
                                    )
                                )
                            )
                            if (it.quantity > 1) sb.append(
                                resources.getQuantityString(
                                    R.plurals.play_description_quantity_segment,
                                    it.quantity,
                                    it.quantity
                                )
                            )
                            if (it.location.isNotBlank()) sb.append(getString(R.string.play_description_location_segment, it.location))
                            if (it.length > 0) sb.append(getString(R.string.play_description_length_segment, it.length.asTime()))
                            if (it.players.isNotEmpty()) {
                                sb.append(" ").append(getString(R.string.with))
                                if (it.arePlayersCustomSorted()) {
                                    for (player in it.players) {
                                        sb.append("\n").append(describePlayer(player))
                                    }
                                } else {
                                    for (i in it.sortedPlayers.indices) {
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
                return false
            }
        })
    }

    private fun describePlayer(player: PlayPlayer): String {
        val sb = StringBuilder()
        if (player.seat != PlayPlayer.SEAT_UNKNOWN) sb.append(getString(R.string.player_description_starting_position_segment, player.seat))
        sb.append(player.name)
        if (player.username.isNotEmpty()) sb.append(getString(R.string.player_description_username_segment, player.username))
        if (player.isNew) sb.append(getString(R.string.player_description_new_segment))
        if (player.color.isNotBlank()) sb.append(getString(R.string.player_description_color_segment, player.color))
        if (player.score.isNotBlank()) sb.append(getString(R.string.player_description_score_segment, player.score))
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
