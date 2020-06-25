package com.boardgamegeek.ui

import android.content.ContentProviderOperation
import android.graphics.Color
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.*
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.events.SyncCompleteEvent
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.INVALID_ID
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.PlaysViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.android.synthetic.main.fragment_plays.*
import kotlinx.android.synthetic.main.row_play.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.withArguments
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

open class PlaysFragment : Fragment(), ActionMode.Callback {
    private val viewModel by activityViewModels<PlaysViewModel>()
    private val markupConverter by lazy { XmlApiMarkupConverter(requireContext()) }

    private val adapter: PlayAdapter by lazy {
        PlayAdapter()
    }

    private var gameId: Int = INVALID_ID
    private var gameName: String? = null
    private var thumbnailUrl: String? = null
    private var imageUrl: String? = null
    private var heroImageUrl: String? = null
    private var arePlayersCustomSorted: Boolean = false
    private var emptyStringResId: Int = 0
    private var showGameName = true
    private var isSyncing = false
    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_plays, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        viewModel.plays.observe(viewLifecycleOwner, Observer {
            progressBar.isVisible = it.status == Status.REFRESHING
            adapter.items = it.data ?: emptyList()
            val sectionItemDecoration = RecyclerSectionItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                    adapter
            )
            while (recyclerView.itemDecorationCount > 0) {
                recyclerView.removeItemDecorationAt(0)
            }
            recyclerView.addItemDecoration(sectionItemDecoration)

            if (it.data.isNullOrEmpty()) {
                emptyContainer.fadeIn()
                recyclerView.fadeOut()
            } else {
                recyclerView.fadeIn()
                emptyContainer.fadeOut()
            }
        })

        viewModel.filterType.observe(viewLifecycleOwner, Observer {
            updateEmptyText()
        })
    }

    private fun updateEmptyText() {
        emptyTextView.setText(
                when (viewModel.filterType.value) {
                    PlaysViewModel.FilterType.DIRTY -> R.string.empty_plays_draft
                    PlaysViewModel.FilterType.PENDING -> R.string.empty_plays_pending
                    else -> if (defaultSharedPreferences[PREFERENCES_KEY_SYNC_PLAYS, false] == true) {
                        emptyStringResId
                    } else {
                        R.string.empty_plays_sync_off
                    }
                }
        )
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        emptyStringResId = arguments?.getInt(KEY_EMPTY_STRING_RES_ID, R.string.empty_plays)
                ?: R.string.empty_plays
        showGameName = arguments?.getBoolean(KEY_SHOW_GAME_NAME, true) ?: true
        gameId = arguments?.getInt(KEY_GAME_ID, INVALID_ID) ?: INVALID_ID
        gameName = arguments?.getString(KEY_GAME_NAME)
        thumbnailUrl = arguments?.getString(KEY_THUMBNAIL_URL)
        imageUrl = arguments?.getString(KEY_IMAGE_URL)
        heroImageUrl = arguments?.getString(KEY_HERO_IMAGE_URL)
        arePlayersCustomSorted = arguments?.getBoolean(KEY_CUSTOM_PLAYER_SORT) ?: false
        @ColorInt val iconColor = arguments?.getInt(KEY_ICON_COLOR, Color.TRANSPARENT)
                ?: Color.TRANSPARENT

        if (gameId != INVALID_ID) {
            fabView.colorize(iconColor)
            fabView.setOnClickListener {
                LogPlayActivity.logPlay(context, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted)
            }
            fabView.show()
        } else {
            fabView.hide()
        }

        updateEmptyText()

        swipeRefreshLayout.setBggColors()
        swipeRefreshLayout.setOnRefreshListener { triggerRefresh() }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncCompleteEvent) {
        isSyncing(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncPlaysByDateTask.CompletedEvent) {
        isSyncing(false)
    }

    fun isSyncing(value: Boolean) {
        isSyncing = value
        swipeRefreshLayout?.post {
            swipeRefreshLayout?.isRefreshing = isSyncing
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncPlaysByGameTask.CompletedEvent) {
        if (!showGameName && event.gameId == gameId) {
            isSyncing(false)
            if (!event.errorMessage.isNullOrBlank()) {
                Toast.makeText(context, event.errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun triggerRefresh() {
        viewModel.refresh()
    }

    internal inner class PlayAdapter : RecyclerView.Adapter<PlayAdapter.ViewHolder>(), AutoUpdatableAdapter, RecyclerSectionItemDecoration.SectionCallback {
        private val selectedItems = SparseBooleanArray()

        val selectedItemCount: Int
            get() = selectedItems.size()

        val selectedItemPositions: List<Int>
            get() {
                val items = ArrayList<Int>(selectedItems.size())
                (0 until selectedItems.size()).mapTo(items) { selectedItems.keyAt(it) }
                return items
            }

        init {
            setHasStableIds(true)
        }

        var items: List<PlayEntity> by Delegates.observable(emptyList()) { _, old, new ->
            autoNotify(old, new) { o, n ->
                o.internalId == n.internalId
            }
        }

        fun getItem(position: Int): PlayEntity? {
            return items.getOrNull(position)
        }

        fun areAllSelectedItemsPending(): Boolean {
            return adapter.selectedItemPositions
                    .map { adapter.getItem(it) }
                    .map { (it?.dirtyTimestamp ?: 0) > 0 }
                    .all { it }
        }

        fun toggleSelection(position: Int) {
            if (selectedItems.get(position, false)) {
                selectedItems.delete(position)
            } else {
                selectedItems.put(position, true)
            }
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
            selectedItems.clear()
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun getItemId(position: Int) = getItem(position)?.internalId ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.inflate(R.layout.row_play))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position), position)
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(play: PlayEntity?, position: Int) {
                if (play == null) return

                itemView.titleView.text = if (showGameName) play.gameName else play.dateForDisplay(requireContext())
                itemView.infoView.setTextOrHide(play.describe(requireContext(), showGameName))
                itemView.commentView.setTextOrHide(markupConverter.strip(play.comments))

                @StringRes val statusMessageId = when {
                    play.deleteTimestamp > 0 -> R.string.sync_pending_delete
                    play.updateTimestamp > 0 -> R.string.sync_pending_update
                    play.dirtyTimestamp > 0 -> if (play.playId > 0) R.string.sync_editing else R.string.sync_draft
                    else -> 0
                }
                itemView.statusView.setTextOrHide(statusMessageId)

                itemView.isActivated = selectedItems.get(position, false)

                itemView.setOnClickListener {
                    if (actionMode == null) {
                        PlayActivity.start(context, play.internalId, play.gameId, play.gameName, play.thumbnailUrl, play.imageUrl, play.heroImageUrl)
                    } else {
                        toggleSelection(position)
                    }
                }
                itemView.setOnLongClickListener {
                    if (actionMode != null) {
                        return@setOnLongClickListener false
                    }
                    actionMode = requireActivity().startActionMode(this@PlaysFragment)
                    if (actionMode == null) {
                        return@setOnLongClickListener false
                    }
                    toggleSelection(position)
                    true
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

        @Suppress("SpellCheckingInspection")
        private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        override fun getSectionHeader(position: Int): CharSequence {
            val play = items.getOrNull(position) ?: return "-"
            return when (viewModel.sortType.value ?: PlaysViewModel.SortType.DATE) {
                PlaysViewModel.SortType.DATE -> {
                    if (play.dateInMillis == DateTimeUtils.UNKNOWN_DATE)
                        getString(R.string.text_unknown)
                    else
                        dateFormat.format(play.dateInMillis)
                }
                PlaysViewModel.SortType.LOCATION -> {
                    if (play.location.isBlank()) getString(R.string.no_location) else play.location
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
        if (!adapter.selectedItemPositions.iterator().hasNext()) return false
        when (item.itemId) {
            R.id.menu_send -> {
                AlertDialog.Builder(requireContext())
                        .setMessage(resources.getQuantityString(R.plurals.are_you_sure_send_play, adapter.selectedItemCount))
                        .setCancelable(true)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.send) { _, _ ->
                            updateSelectedPlays(Plays.UPDATE_TIMESTAMP, System.currentTimeMillis())
                            mode.finish()
                        }
                        .show()
                return true
            }
            R.id.menu_edit -> {
                val play = adapter.getItem(adapter.selectedItemPositions.iterator().next())
                if (play != null)
                    LogPlayActivity.editPlay(activity, play.internalId, play.gameId, play.gameName, play.thumbnailUrl, play.imageUrl, play.heroImageUrl)
                mode.finish()
                return true
            }
            R.id.menu_delete -> {
                AlertDialog.Builder(requireContext())
                        .setMessage(resources.getQuantityString(R.plurals.are_you_sure_delete_play, adapter.selectedItemCount))
                        .setCancelable(true)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            updateSelectedPlays(Plays.DELETE_TIMESTAMP, System.currentTimeMillis())
                            mode.finish()
                        }
                        .show()
                return true
            }
        }
        return false
    }

    private fun updateSelectedPlays(key: String, value: Long) {
        val batch = arrayListOf<ContentProviderOperation>()
        for (position in adapter.selectedItemPositions) {
            val play = adapter.getItem(position)
            if (play != null && play.internalId != INVALID_ID.toLong())
                batch.add(ContentProviderOperation
                        .newUpdate(Plays.buildPlayUri(play.internalId))
                        .withValue(key, value)
                        .build())
        }
        requireContext().contentResolver.applyBatch(batch)
        SyncService.sync(activity, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
    }

    companion object {
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_IMAGE_URL = "IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_ICON_COLOR = "ICON_COLOR"
        private const val KEY_EMPTY_STRING_RES_ID = "EMPTY_STRING_RES_ID"
        private const val KEY_SHOW_GAME_NAME = "SHOW_GAME_NAME"

        fun newInstance(): PlaysFragment {
            return PlaysFragment().withArguments(
                    KEY_EMPTY_STRING_RES_ID to R.string.empty_plays
            )
        }

        fun newInstanceForGame(gameId: Int, gameName: String, imageUrl: String, thumbnailUrl: String, heroImageUrl: String, arePlayersCustomSorted: Boolean, @ColorInt iconColor: Int): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(
                        KEY_EMPTY_STRING_RES_ID to R.string.empty_plays_game,
                        KEY_SHOW_GAME_NAME to false,
                        KEY_GAME_ID to gameId,
                        KEY_GAME_NAME to gameName,
                        KEY_IMAGE_URL to imageUrl,
                        KEY_THUMBNAIL_URL to thumbnailUrl,
                        KEY_HERO_IMAGE_URL to heroImageUrl,
                        KEY_CUSTOM_PLAYER_SORT to arePlayersCustomSorted,
                        KEY_ICON_COLOR to iconColor
                )
            }
        }

        fun newInstanceForLocation(): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(KEY_EMPTY_STRING_RES_ID to R.string.empty_plays_location)
            }
        }

        fun newInstanceForBuddy(): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(KEY_EMPTY_STRING_RES_ID to R.string.empty_plays_buddy)
            }
        }

        fun newInstanceForPlayer(): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(KEY_EMPTY_STRING_RES_ID to R.string.empty_plays_player)
            }
        }
    }
}
