package com.boardgamegeek.ui

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.ContentProviderOperation
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.SparseBooleanArray
import android.view.*
import android.widget.DatePicker
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.events.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.sorter.PlaysSorter
import com.boardgamegeek.sorter.PlaysSorterFactory
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.util.HelpUtils
import com.boardgamegeek.util.fabric.FilterEvent
import com.boardgamegeek.util.fabric.SortEvent
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.Target
import kotlinx.android.synthetic.main.fragment_plays.*
import kotlinx.android.synthetic.main.row_play.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

open class PlaysFragment : Fragment(), LoaderCallbacks<Cursor>, ActionMode.Callback, OnDateSetListener {

    private val adapter: PlayAdapter by lazy {
        PlayAdapter()
    }

    private var mode = MODE_ALL
    private var modeValue: String? = null
    private var gameId: Int = INVALID_ID
    private var gameName: String? = null
    private var thumbnailUrl: String? = null
    private var imageUrl: String? = null
    private var heroImageUrl: String? = null
    private var arePlayersCustomSorted: Boolean = false

    private var isSyncing = false
    private var hasAutoSyncTriggered: Boolean = false
    private var filterType = FILTER_TYPE_STATUS_ALL
    private var sorter: PlaysSorter? = null
    private var showcaseView: ShowcaseView? = null
    private var actionMode: ActionMode? = null

    private val emptyStringResource: Int
        @StringRes get() {
            return when (mode) {
                MODE_BUDDY -> R.string.empty_plays_buddy
                MODE_PLAYER -> R.string.empty_plays_player
                MODE_LOCATION -> R.string.empty_plays_location
                MODE_GAME -> R.string.empty_plays_game
                else -> when (filterType) {
                    FILTER_TYPE_STATUS_DIRTY -> R.string.empty_plays_draft
                    FILTER_TYPE_STATUS_UPDATE -> R.string.empty_plays_update
                    FILTER_TYPE_STATUS_DELETE -> R.string.empty_plays_delete
                    FILTER_TYPE_STATUS_PENDING -> R.string.empty_plays_pending
                    else -> if (requireActivity().getSyncPlays()) {
                        R.string.empty_plays
                    } else {
                        R.string.empty_plays_sync_off
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_plays, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
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

        sorter = PlaysSorterFactory.create(requireContext(), PlaysSorterFactory.TYPE_DEFAULT)

        mode = arguments?.getInt(KEY_MODE, mode) ?: MODE_ALL
        when (mode) {
            MODE_GAME -> {
                gameId = arguments?.getInt(KEY_GAME_ID, INVALID_ID) ?: INVALID_ID
                gameName = arguments?.getString(KEY_GAME_NAME)
                thumbnailUrl = arguments?.getString(KEY_THUMBNAIL_URL)
                imageUrl = arguments?.getString(KEY_IMAGE_URL)
                heroImageUrl = arguments?.getString(KEY_HERO_IMAGE_URL)
                arePlayersCustomSorted = arguments?.getBoolean(KEY_CUSTOM_PLAYER_SORT) ?: false
                LoaderManager.getInstance(this).restartLoader(GameQuery.TOKEN, arguments, this)
            }
            else -> modeValue = arguments?.getString(KEY_MODE_VALUE)
        }
        @ColorInt val iconColor = arguments?.getInt(KEY_ICON_COLOR, Color.TRANSPARENT) ?: Color.TRANSPARENT

        if (mode == MODE_GAME) {
            fabView.colorize(iconColor)
            fabView.setOnClickListener {
                LogPlayActivity.logPlay(context, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted)
            }
            fabView.show()
        } else {
            fabView.hide()
        }

        emptyTextView.setText(emptyStringResource)
        requery()

        swipeRefreshLayout.setBggColors()
        swipeRefreshLayout.setOnRefreshListener { triggerRefresh() }

        maybeShowHelp()
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu?.findItem(
                when (filterType) {
                    FILTER_TYPE_STATUS_DIRTY -> R.id.menu_filter_in_progress
                    FILTER_TYPE_STATUS_PENDING -> R.id.menu_filter_pending
                    FILTER_TYPE_STATUS_ALL -> R.id.menu_filter_all
                    else -> R.id.menu_filter_all
                })?.isChecked = true
        sorter?.let {
            menu?.findItem(when (it.type) {
                PlaysSorterFactory.TYPE_PLAY_DATE -> R.id.menu_sort_date
                PlaysSorterFactory.TYPE_PLAY_GAME -> R.id.menu_sort_game
                PlaysSorterFactory.TYPE_PLAY_LENGTH -> R.id.menu_sort_length
                PlaysSorterFactory.TYPE_PLAY_LOCATION -> R.id.menu_sort_location
                else -> R.id.menu_sort_date
            })?.isChecked = true
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val title = item?.title?.toString() ?: ""
        when (item?.itemId) {
            R.id.menu_sort_date -> {
                setSort(PlaysSorterFactory.TYPE_PLAY_DATE)
                return true
            }
            R.id.menu_sort_location -> {
                setSort(PlaysSorterFactory.TYPE_PLAY_LOCATION)
                return true
            }
            R.id.menu_sort_game -> {
                setSort(PlaysSorterFactory.TYPE_PLAY_GAME)
                return true
            }
            R.id.menu_sort_length -> {
                setSort(PlaysSorterFactory.TYPE_PLAY_LENGTH)
                return true
            }
            R.id.menu_filter_all -> {
                filter(FILTER_TYPE_STATUS_ALL, title)
                return true
            }
            R.id.menu_filter_in_progress -> {
                filter(FILTER_TYPE_STATUS_DIRTY, title)
                return true
            }
            R.id.menu_filter_pending -> {
                filter(FILTER_TYPE_STATUS_PENDING, title)
                return true
            }
            R.id.menu_refresh_on -> {
                val datePickerFragment = DatePickerFragment()
                datePickerFragment.setListener(this)
                datePickerFragment.show(requireFragmentManager(), "datePicker")
                return true
            }
            R.id.menu_help -> {
                showHelp()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncCompleteEvent) {
        isSyncing(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncPlaysByDateTask.CompletedEvent) {
        isSyncing(false)
    }

    private fun isSyncing(value: Boolean) {
        isSyncing = value
        swipeRefreshLayout?.post {
            swipeRefreshLayout?.isRefreshing = isSyncing
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: SyncPlaysByGameTask.CompletedEvent) {
        if (mode == MODE_GAME && event.gameId == gameId) {
            isSyncing(false)
            if (!event.errorMessage.isNullOrBlank()) {
                Toast.makeText(context, event.errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showHelp() {
        val builder = HelpUtils.getShowcaseBuilder(activity)
        if (builder != null) {
            showcaseView = builder
                    .setContentText(R.string.help_plays)
                    .setTarget(Target.NONE)
                    .setOnClickListener {
                        showcaseView?.hide()
                        HelpUtils.updateHelp(context, HelpUtils.HELP_PLAYS_KEY, HELP_VERSION)
                    }
                    .build()
            showcaseView?.show()
        }
    }

    private fun maybeShowHelp() {
        if (HelpUtils.shouldShowHelp(context, HelpUtils.HELP_PLAYS_KEY, HELP_VERSION)) {
            Handler().postDelayed({ this.showHelp() }, 100)
        }
    }

    private fun requery() {
        if (mode == MODE_ALL || mode == MODE_LOCATION || mode == MODE_GAME) {
            LoaderManager.getInstance(this).restartLoader(SumQuery.TOKEN, arguments, this)
        } else if (mode == MODE_PLAYER || mode == MODE_BUDDY) {
            LoaderManager.getInstance(this).restartLoader(PlayerSumQuery.TOKEN, arguments, this)
        }
        LoaderManager.getInstance(this).restartLoader(PLAY_QUERY_TOKEN, arguments, this)
    }

    @Subscribe(sticky = true)
    fun onEvent(event: PlaysSortChangedEvent) {
        setSort(event.type)
    }

    @Subscribe(sticky = true)
    fun onEvent(event: PlaysFilterChangedEvent) {
        filter(event.type, event.description)
    }

    private fun setSort(sortType: Int) {
        if (sorter != null && sortType == sorter?.type) {
            return
        }
        val type = if (sortType == PlaysSorterFactory.TYPE_UNKNOWN) {
            PlaysSorterFactory.TYPE_DEFAULT
        } else {
            sortType
        }
        sorter = PlaysSorterFactory.create(requireContext(), type)
        SortEvent.log("Plays", sorter?.description ?: "")
        requery()
    }

    class DatePickerFragment : DialogFragment() {
        private var listener: OnDateSetListener? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val calendar = Calendar.getInstance()
            return DatePickerDialog(requireContext(),
                    listener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
        }

        fun setListener(listener: OnDateSetListener) {
            this.listener = listener
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        isSyncing(true)
        SyncPlaysByDateTask(requireActivity().application as BggApplication, year, month, day).executeAsyncTask()
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<Cursor> {
        var loader: CursorLoader? = null
        when (id) {
            PLAY_QUERY_TOKEN -> {
                val uri = when (mode) {
                    MODE_BUDDY -> Plays.buildPlayersByPlayUri()
                    MODE_PLAYER -> Plays.buildPlayersByPlayUri()
                    else -> Plays.CONTENT_URI
                }
                loader = CursorLoader(requireContext(),
                        uri,
                        PlayEntity.projection.union(sorter?.columns?.asIterable() ?: emptyList()).toTypedArray(),
                        selection(),
                        selectionArgs(),
                        sorter?.orderByClause)
                loader.setUpdateThrottle(2000)
            }
            GameQuery.TOKEN -> {
                loader = CursorLoader(requireContext(), Games.buildGameUri(gameId), GameQuery.PROJECTION, null, null, null)
                loader.setUpdateThrottle(0)
            }
            SumQuery.TOKEN -> {
                loader = CursorLoader(requireContext(), Plays.CONTENT_SIMPLE_URI, SumQuery.PROJECTION, selection(), selectionArgs(), null)
                loader.setUpdateThrottle(0)
            }
            PlayerSumQuery.TOKEN -> {
                val uri = if (mode == MODE_BUDDY) {
                    Plays.buildPlayersByUniqueUserUri()
                } else {
                    Plays.buildPlayersByUniquePlayerUri()
                }
                loader = CursorLoader(requireContext(), uri, PlayerSumQuery.PROJECTION, selection(), selectionArgs(), null)
                loader.setUpdateThrottle(0)
            }
        }
        return loader!!
    }

    private fun selection(): String? {
        return when (mode) {
            MODE_ALL -> when (filterType) {
                FILTER_TYPE_STATUS_ALL -> null
                FILTER_TYPE_STATUS_PENDING -> "${Plays.DELETE_TIMESTAMP}>0 OR ${Plays.UPDATE_TIMESTAMP}>0"
                else -> "${Plays.DIRTY_TIMESTAMP}>0"
            }
            MODE_GAME -> Plays.OBJECT_ID + "=?"
            MODE_BUDDY -> PlayPlayers.USER_NAME + "=?"
            MODE_PLAYER -> PlayPlayers.USER_NAME + "='' AND play_players." + PlayPlayers.NAME + "=?"
            MODE_LOCATION -> Plays.LOCATION + "=?"
            else -> null
        }
    }

    private fun selectionArgs(): Array<String>? {
        return when (mode) {
            MODE_ALL -> null
            MODE_GAME -> arrayOf(gameId.toString())
            MODE_BUDDY, MODE_PLAYER, MODE_LOCATION -> {
                if (modeValue == null) {
                    null
                } else {
                    arrayOf(modeValue!!)
                }
            }
            else -> null
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        if (activity == null) return

        val token = loader.id
        if (token == PLAY_QUERY_TOKEN) {
            val plays = ArrayList<PlayEntity>()
            if (cursor?.moveToFirst() == true) {
                do {
                    plays.add(PlayEntity.fromCursor(cursor))
                } while (cursor.moveToNext())
            }
            adapter.items = plays

            val sectionItemDecoration = RecyclerSectionItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                    getSectionCallback(plays, sorter),
                    true
            )
            while (recyclerView.itemDecorationCount > 0) {
                recyclerView.removeItemDecorationAt(0)
            }
            recyclerView.addItemDecoration(sectionItemDecoration)

            if (cursor?.count == 0) {
                emptyContainer.fadeIn()
                recyclerView.fadeOut()
            } else {
                recyclerView.fadeIn()
                emptyContainer.fadeOut()
            }

            EventBus.getDefault().postSticky(PlaysSortChangedEvent(sorter?.type
                    ?: PlaysSorterFactory.TYPE_UNKNOWN, sorter?.description ?: ""))
            progressBar.hide()
        } else if (token == GameQuery.TOKEN) {
            if (!hasAutoSyncTriggered && cursor?.moveToFirst() == true) {
                hasAutoSyncTriggered = true
                val updated = cursor.getLong(GameQuery.UPDATED_PLAYS)
                if (updated == 0L || updated.isOlderThan(2, TimeUnit.DAYS)) {
                    triggerRefresh()
                }
            }
        } else if (token == SumQuery.TOKEN) {
            val count = if (cursor?.moveToFirst() == true) {
                cursor.getInt(SumQuery.TOTAL_COUNT)
            } else {
                0
            }
            EventBus.getDefault().postSticky(PlaysCountChangedEvent(count))
        } else if (token == PlayerSumQuery.TOKEN) {
            val count = if (cursor != null && cursor.moveToFirst()) {
                cursor.getInt(PlayerSumQuery.SUM_QUANTITY)
            } else {
                0
            }
            EventBus.getDefault().postSticky(PlaysCountChangedEvent(count))
        } else {
            Timber.d("Query complete, Not Actionable: %s", token)
            cursor?.close()
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (loader.id == PLAY_QUERY_TOKEN) {
            adapter.items = emptyList()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncEvent) {
        if (event.type and SyncService.FLAG_SYNC_PLAYS == SyncService.FLAG_SYNC_PLAYS) {
            isSyncing(true)
        }
    }

    private fun triggerRefresh() {
        if (isSyncing) return
        when (mode) {
            MODE_ALL, MODE_BUDDY, MODE_PLAYER, MODE_LOCATION -> SyncService.sync(activity, SyncService.FLAG_SYNC_PLAYS)
            MODE_GAME -> {
                isSyncing(true)
                SyncService.sync(activity, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
                SyncPlaysByGameTask(requireActivity().application as BggApplication, gameId).executeAsyncTask()
            }
        }
    }

    fun filter(type: Int, description: String) {
        if (type != filterType && mode == MODE_ALL) {
            filterType = type
            FilterEvent.log("Plays", type.toString())
            EventBus.getDefault().postSticky(PlaysFilterChangedEvent(filterType, description))
            emptyTextView.setText(emptyStringResource)
            requery()
        }
    }

    internal inner class PlayAdapter : RecyclerView.Adapter<PlayAdapter.ViewHolder>(), AutoUpdatableAdapter {
        private val selectedItems = SparseBooleanArray()

        val selectedItemCount: Int
            get() = selectedItems.size()

        val selectedItemPositions: List<Int>
            get() {
                val items = ArrayList<Int>(selectedItems.size())
                for (i in 0 until selectedItems.size()) {
                    items.add(selectedItems.keyAt(i))
                }
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
            for (pos in adapter.selectedItemPositions) {
                val play = adapter.getItem(pos)
                val pending = (play?.dirtyTimestamp ?: 0) > 0
                if (!pending) return false
            }
            return true
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

        override fun getItemId(position: Int): Long {
            return getItem(position)?.internalId ?: RecyclerView.NO_ID
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent.inflate(R.layout.row_play))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position), position)
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(play: PlayEntity?, position: Int) {
                if (play == null) return

                itemView.titleView.text = if (mode != MODE_GAME) play.gameName else play.dateForDisplay(requireContext())
                itemView.infoView.setTextOrHide(play.describe(requireContext(), mode != MODE_GAME))
                itemView.commentView.setTextOrHide(play.comments)

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
    }

    private fun getSectionCallback(plays: List<PlayEntity>?, sorter: PlaysSorter?): RecyclerSectionItemDecoration.SectionCallback {
        return object : RecyclerSectionItemDecoration.SectionCallback {
            override fun isSection(position: Int): Boolean {
                if (position == RecyclerView.NO_POSITION) return false
                if (plays == null || plays.isEmpty()) return false
                if (position == 0) return true
                if (position < 0 || position >= plays.size) return false
                val thisLetter = sorter?.getSectionText(plays[position]) ?: "-"
                val lastLetter = sorter?.getSectionText(plays[position - 1]) ?: "-"
                return thisLetter != lastLetter
            }

            override fun getSectionHeader(position: Int): CharSequence {
                if (position == RecyclerView.NO_POSITION) return "-"
                if (plays == null || plays.isEmpty()) return "-"
                return if (position < 0 || position >= plays.size) "-"
                else sorter?.getSectionText(plays[position]) ?: "-"
            }
        }
    }

    private interface GameQuery {
        companion object {
            const val TOKEN = 0x22
            val PROJECTION = arrayOf(Games.UPDATED_PLAYS)
            const val UPDATED_PLAYS = 0
        }
    }

    private interface SumQuery {
        companion object {
            const val TOKEN = 0x23
            val PROJECTION = arrayOf(Plays.SUM_QUANTITY)
            const val TOTAL_COUNT = 0
        }
    }

    private interface PlayerSumQuery {
        companion object {
            const val TOKEN = 0x24
            val PROJECTION = arrayOf(PlayPlayers.SUM_QUANTITY)
            const val SUM_QUANTITY = 0
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
            if (play != null && play.internalId != BggContract.INVALID_ID.toLong())
                batch.add(ContentProviderOperation
                        .newUpdate(Plays.buildPlayUri(play.internalId))
                        .withValue(key, value)
                        .build())
        }
        requireContext().contentResolver.applyBatch(requireContext(), batch)
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
        private const val KEY_MODE = "MODE"
        private const val KEY_MODE_VALUE = "MODE_VALUE"
        const val FILTER_TYPE_STATUS_ALL = -2
        const val FILTER_TYPE_STATUS_UPDATE = 1
        const val FILTER_TYPE_STATUS_DIRTY = 2
        const val FILTER_TYPE_STATUS_DELETE = 3
        const val FILTER_TYPE_STATUS_PENDING = 4
        private const val MODE_ALL = 0
        private const val MODE_GAME = 1
        private const val MODE_BUDDY = 2
        private const val MODE_PLAYER = 3
        private const val MODE_LOCATION = 4
        private const val PLAY_QUERY_TOKEN = 0x21
        private const val HELP_VERSION = 2

        fun newInstance(): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(KEY_MODE to MODE_ALL)
            }
        }

        fun newInstanceForGame(gameId: Int, gameName: String, imageUrl: String, thumbnailUrl: String, heroImageUrl: String, arePlayersCustomSorted: Boolean, @ColorInt iconColor: Int): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(
                        KEY_MODE to MODE_GAME,
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

        fun newInstanceForLocation(locationName: String): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(
                        KEY_MODE to MODE_LOCATION,
                        KEY_MODE_VALUE to locationName
                )
            }
        }

        fun newInstanceForBuddy(username: String): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(
                        KEY_MODE to MODE_BUDDY,
                        KEY_MODE_VALUE to username
                )
            }
        }

        fun newInstanceForPlayer(playerName: String): PlaysFragment {
            return PlaysFragment().apply {
                arguments = bundleOf(
                        KEY_MODE to MODE_PLAYER,
                        KEY_MODE_VALUE to playerName
                )
            }
        }
    }
}
