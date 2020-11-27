package com.boardgamegeek.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask
import com.boardgamegeek.ui.CollectionActivity.Companion.createIntentForGameChange
import com.boardgamegeek.ui.GameActivity.Companion.start
import com.boardgamegeek.ui.adapter.PlayPlayerAdapter
import com.boardgamegeek.ui.viewmodel.PlayViewModel
import com.boardgamegeek.util.*
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_play.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PlayFragment : Fragment(R.layout.fragment_play) {
    private var internalId = BggContract.INVALID_ID.toLong()
    private var gameId = BggContract.INVALID_ID
    private var gameName = ""
    private var thumbnailUrl = ""
    private var imageUrl = ""
    private var heroImageUrl = ""
    private var customPlayerSort = false

    private var playId = BggContract.INVALID_ID
    private var hasStarted = false
    private var isDirty = false
    private var shortDescription = ""
    private var longDescription = ""
    private var isRefreshing = false
    private var hasBeenNotified = false
    private var prefs: SharedPreferences? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val viewModel by activityViewModels<PlayViewModel>()
    private val adapter: PlayPlayerAdapter by lazy { PlayPlayerAdapter() }
    private val markupConverter by lazy { XmlApiMarkupConverter(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readBundle(arguments)
        firebaseAnalytics = Firebase.analytics
        hasBeenNotified = savedInstanceState?.getBoolean(KEY_HAS_BEEN_NOTIFIED) ?: false
        setHasOptionsMenu(true)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private fun readBundle(bundle: Bundle?) {
        if (bundle == null) return
        internalId = bundle.getLong(KEY_ID, BggContract.INVALID_ID.toLong())
        gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID)
        gameName = bundle.getString(KEY_GAME_NAME).orEmpty()
        thumbnailUrl = bundle.getString(KEY_THUMBNAIL_URL).orEmpty()
        imageUrl = bundle.getString(KEY_IMAGE_URL).orEmpty()
        heroImageUrl = bundle.getString(KEY_HERO_IMAGE_URL).orEmpty()
        customPlayerSort = bundle.getBoolean(KEY_CUSTOM_PLAYER_SORT, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout?.setBggColors()
        swipeRefreshLayout?.setOnRefreshListener { triggerRefresh() }
        headerContainer.setOnClickListener { start(requireContext(), gameId, gameName) }
        timerEndButton.setOnClickListener {
            LogPlayActivity.endPlay(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl)
        }
        playersView.adapter = adapter
        viewModel.play.observe(viewLifecycleOwner, { data: RefreshableResource<PlayEntity?> ->
            // TODO handle error and refreshing states
            val play = data.data
            if (play == null) {
                emptyView.text = resources.getString(R.string.empty_play, internalId.toString())
                emptyView.fadeIn()
                listContainer.fadeOut()
                return@observe
            }

            playId = play.playId
            hasStarted = play.hasStarted()
            isDirty = play.dirtyTimestamp > 0
            shortDescription = getString(R.string.play_description_game_segment, gameName) + getString(R.string.play_description_date_segment, play.dateForDisplay(requireContext()))
            longDescription = play.describe(requireContext(), true)

            gameNameView.text = play.gameName
            dateView.text = play.dateForDisplay(requireContext())

            quantityView.text = resources.getQuantityString(R.plurals.times_suffix, play.quantity, play.quantity)
            quantityView.isVisible = play.quantity != 1

            when {
                play.length > 0 -> {
                    lengthContainer.isVisible = true
                    lengthView.text = DateTimeUtils.describeMinutes(requireContext(), play.length)
                    lengthView.isVisible = true
                    timerContainer.isVisible = false
                    timerView.stop()
                }
                play.hasStarted() -> {
                    lengthContainer.isVisible = true
                    lengthView.isVisible = false
                    timerContainer.isVisible = true
                    UIUtils.startTimerWithSystemTime(timerView, play.startTime)
                }
                else -> lengthContainer.isVisible = false
            }

            locationView.text = play.location
            locationContainer.isVisible = play.location.isNotBlank()

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

            maybeShowNotification()
            requireActivity().invalidateOptionsMenu()
            emptyView.fadeOut()
            listContainer.fadeIn()
            progressBar.hide()
        })
        viewModel.setId(internalId)
        thumbnailView.safelyLoadImage(imageUrl, thumbnailUrl, heroImageUrl, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                if (gameNameView != null && isAdded) {
                    gameNameView.setBackgroundResource(R.color.black_overlay_light)
                }
            }

            override fun onFailedImageLoad() {}
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        if (hasStarted) {
            showNotification()
            hasBeenNotified = true
        }
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
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
        menu.findItem(R.id.menu_send).isVisible = isDirty
        menu.findItem(R.id.menu_discard).isVisible = playId > 0 && isDirty
        menu.findItem(R.id.menu_discard).isEnabled = playId > 0
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
                DialogUtils.createThemedBuilder(context)
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
                startActivity(createIntentForGameChange(requireContext(), internalId))
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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncPlaysByGameTask.CompletedEvent) {
        if (event.gameId == gameId) {
            if (event.errorMessage.isNotEmpty() && prefs!!.getSyncShowErrors()) {
                // TODO: 3/30/17 change to a snackbar (will need to change from a ListFragment)
                Toast.makeText(context, event.errorMessage, Toast.LENGTH_LONG).show()
            }
            updateRefreshStatus(false)
        }
    }

    private fun updateRefreshStatus(value: Boolean) {
        isRefreshing = value
        swipeRefreshLayout?.post { swipeRefreshLayout?.isRefreshing = isRefreshing }
    }

    private fun maybeShowNotification() {
        if (hasStarted) {
            showNotification()
        } else if (hasBeenNotified) {
            cancelNotification()
        }
    }

    private fun showNotification() {
//		NotificationUtils.launchPlayingNotification(getActivity(), internalId, play, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort);
    }

    private fun cancelNotification() {
        NotificationUtils.cancel(activity, NotificationUtils.TAG_PLAY_TIMER, internalId)
    }

    private fun triggerRefresh() {
        // TODO move this logic to the view model
        if (!isRefreshing) {
            SyncPlaysByGameTask(requireActivity().application as BggApplication, gameId).executeAsyncTask()
            updateRefreshStatus(true)
        }
    }

    companion object {
        private const val KEY_ID = "ID"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_IMAGE_URL = "IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_HAS_BEEN_NOTIFIED = "HAS_BEEN_NOTIFIED"
        fun newInstance(internalId: Long, gameId: Int, gameName: String?, imageUrl: String?, thumbnailUrl: String?, heroImageUrl: String?, customPlayerSort: Boolean): PlayFragment {
            val args = Bundle()
            args.putLong(KEY_ID, internalId)
            args.putInt(KEY_GAME_ID, gameId)
            args.putString(KEY_GAME_NAME, gameName)
            args.putString(KEY_IMAGE_URL, imageUrl)
            args.putString(KEY_THUMBNAIL_URL, thumbnailUrl)
            args.putString(KEY_HERO_IMAGE_URL, heroImageUrl)
            args.putBoolean(KEY_CUSTOM_PLAYER_SORT, customPlayerSort)
            val fragment = PlayFragment()
            fragment.arguments = args
            return fragment
        }
    }
}