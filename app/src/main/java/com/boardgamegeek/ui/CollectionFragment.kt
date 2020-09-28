package com.boardgamegeek.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Pair
import android.util.SparseBooleanArray
import android.view.*
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.events.SyncCompleteEvent
import com.boardgamegeek.events.SyncEvent
import com.boardgamegeek.extensions.*
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionStatusFilterer
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.noPreviousCollectionSync
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.sorter.CollectionSorter
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.sorter.Sorter
import com.boardgamegeek.ui.CollectionFragment.CollectionAdapter.CollectionItemViewHolder
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.dialog.*
import com.boardgamegeek.ui.dialog.CollectionFilterDialog.OnFilterChangedListener
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import com.boardgamegeek.util.ActivityUtils
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.HttpUtils
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.ShortcutUtils
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.row_collection.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.support.v4.withArguments
import timber.log.Timber
import java.util.*
import kotlin.properties.Delegates

class CollectionFragment : Fragment(R.layout.fragment_collection), LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback, OnFilterChangedListener {
    private var viewId = CollectionView.DEFAULT_DEFAULT_ID
    private var viewName = ""
    private var sorter: CollectionSorter? = null
    private val filters: MutableList<CollectionFilterer> = ArrayList()
    private var isCreatingShortcut = false
    private var changingGamePlayId: Long = 0
    private var isSyncing = false
    private var actionMode: ActionMode? = null

    private val collectionSorterFactory: CollectionSorterFactory by lazy {
        CollectionSorterFactory(requireContext())
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val prefs: SharedPreferences by lazy { requireContext().preferences() }

    private val defaultWhereClause: String by lazy {
        prefs.getSyncStatusesAsSql()
    }

    private val viewModel by activityViewModels<CollectionViewViewModel>()

    private val adapter by lazy {
        CollectionAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        readBundle(arguments)
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun readBundle(bundle: Bundle?) {
        isCreatingShortcut = bundle?.getBoolean(KEY_IS_CREATING_SHORTCUT) ?: false
        changingGamePlayId = bundle?.getLong(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong())
                ?: BggContract.INVALID_ID.toLong()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.adapter = adapter

        if (isCreatingShortcut) {
            Snackbar.make(swipeRefreshLayout, R.string.msg_shortcut_create, Snackbar.LENGTH_LONG).show()
        } else if (changingGamePlayId != BggContract.INVALID_ID.toLong()) {
            Snackbar.make(swipeRefreshLayout, R.string.msg_change_play_game, Snackbar.LENGTH_LONG).show()
        }

        footerToolbar?.inflateMenu(R.menu.collection_fragment)
        footerToolbar?.setOnMenuItemClickListener(footerMenuListener)
        invalidateMenu()

        setEmptyText()
        emptyButton.setOnClickListener {
            startActivity(Intent(context, SettingsActivity::class.java))
        }

        swipeRefreshLayout.setBggColors()
        swipeRefreshLayout.setOnRefreshListener { SyncService.sync(activity, SyncService.FLAG_SYNC_COLLECTION) }

        viewModel.selectedViewId.observe(viewLifecycleOwner, { id: Long -> viewId = id })
        viewModel.selectedViewName.observe(viewLifecycleOwner, { name: String -> viewName = name })
        viewModel.effectiveSortType.observe(viewLifecycleOwner, { sortType: Int ->
            progressBar.show()
            sorter = collectionSorterFactory.create(sortType)
            LoaderManager.getInstance(this).restartLoader(Query.TOKEN, null, this)
        })
        viewModel.effectiveFilters.observe(viewLifecycleOwner, {
            progressBar.show()
            filters.clear()
            it?.let { filters.addAll(it) }
            setEmptyText()
            LoaderManager.getInstance(this).restartLoader(Query.TOKEN, null, this)
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncEvent) {
        if (event.type and SyncService.FLAG_SYNC_COLLECTION == SyncService.FLAG_SYNC_COLLECTION) {
            isSyncing(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncCompleteEvent?) {
        isSyncing(false)
    }

    private fun isSyncing(value: Boolean) {
        isSyncing = value
        swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = isSyncing }
    }

    private fun invalidateMenu() {
        val menu = footerToolbar.menu
        if (isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID.toLong()) {
            menu.findItem(R.id.menu_collection_random_game)?.isVisible = false
            menu.findItem(R.id.menu_create_shortcut)?.isVisible = false
            menu.findItem(R.id.menu_collection_view_save)?.isVisible = false
            menu.findItem(R.id.menu_collection_view_delete)?.isVisible = false
            menu.findItem(R.id.menu_share)?.isVisible = false
        } else {
            menu.findItem(R.id.menu_collection_random_game)?.isVisible = true
            menu.findItem(R.id.menu_create_shortcut)?.isVisible = true
            menu.findItem(R.id.menu_collection_view_save)?.isVisible = true
            menu.findItem(R.id.menu_collection_view_delete)?.isVisible = true
            menu.findItem(R.id.menu_share)?.isVisible = true
            val hasFiltersApplied = filters.size > 0
            val hasSortApplied = sorter?.let { it.type != CollectionSorterFactory.TYPE_DEFAULT }
                    ?: false
            val hasViews = activity?.contentResolver?.getCount(CollectionViews.CONTENT_URI) ?: 0 > 0
            val hasItems = adapter.itemCount > 0
            val hasViewSelected = viewId > 0
            menu.findItem(R.id.menu_collection_view_save)?.isEnabled = hasFiltersApplied || hasSortApplied
            menu.findItem(R.id.menu_collection_view_delete)?.isEnabled = hasViews
            menu.findItem(R.id.menu_collection_random_game)?.isEnabled = hasItems
            menu.findItem(R.id.menu_create_shortcut)?.isEnabled = hasViewSelected
            menu.findItem(R.id.menu_share)?.isEnabled = hasItems
        }
    }

    private val footerMenuListener = Toolbar.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.menu_collection_random_game -> {
                firebaseAnalytics.logEvent("RandomGame", null)
                adapter.randomItem?.let {
                    GameActivity.start(requireContext(), it.gameId, it.gameName, it.thumbnailUrl, it.heroImageUrl)
                }
                return@OnMenuItemClickListener true
            }
            R.id.menu_create_shortcut -> if (viewId > 0) {
                ShortcutUtils.createCollectionShortcut(context, viewId, viewName)
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_view_save -> {
                val name = if (viewId <= 0) "" else viewName
                val dialog = SaveViewDialogFragment.newInstance(name, createViewDescription(sorter, filters))
                DialogUtils.showAndSurvive(this@CollectionFragment, dialog)
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_view_delete -> {
                DialogUtils.showAndSurvive(this@CollectionFragment, DeleteViewDialogFragment.newInstance())
                return@OnMenuItemClickListener true
            }
            R.id.menu_share -> {
                shareCollection()
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_sort -> {
                DialogUtils.showAndSurvive(this@CollectionFragment, CollectionSortDialogFragment.newInstance(sorter?.type
                        ?: CollectionSorterFactory.TYPE_DEFAULT))
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_filter -> {
                DialogUtils.showAndSurvive(this@CollectionFragment, CollectionFilterDialogFragment.newInstance(filters.map { it.type }))
                return@OnMenuItemClickListener true
            }
        }
        launchFilterDialog(item.itemId)
    }

    private fun shareCollection() {
        val description: String = when {
            viewId > 0 && viewName.isNotEmpty() -> viewName
            filters.size > 0 -> getString(R.string.title_filtered_collection)
            else -> getString(R.string.title_collection)
        }
        val text = StringBuilder(description)
                .append("\n")
                .append("-".repeat(description.length))
                .append("\n")

        val maxGames = 10
        adapter.items.take(maxGames).map { text.append("\u2022 ${ActivityUtils.formatGameLink(it.gameId, it.collectionName)}") }
        val leftOverCount = adapter.itemCount - maxGames
        if (leftOverCount > 0) text.append(getString(R.string.and_more, leftOverCount)).append("\n")

        val username = AccountUtils.getUsername(context)
        text.append("\n")
                .append(createViewDescription(sorter, filters))
                .append("\n")
                .append("\n")
                .append(getString(R.string.share_collection_complete_footer, "https://www.boardgamegeek.com/collection/user/${HttpUtils.encode(username)}"))
        ActivityUtils.share(activity,
                getString(R.string.share_collection_subject, AccountUtils.getFullName(context), username),
                text,
                R.string.title_share_collection)
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<Cursor> {
        val where = StringBuilder()
        var args: Array<String?> = arrayOf()
        val having = StringBuilder()
        if (viewId == 0L && filters.size == 0) {
            where.append(defaultWhereClause)
        } else {
            for (filter in filters) {
                if (filter.getSelection().isNotEmpty()) {
                    if (where.isNotEmpty()) where.append(" AND ")
                    where.append("(").append(filter.getSelection()).append(")")
                    args += (filter.getSelectionArgs() ?: emptyArray())
                }
                if (filter.getHaving()?.isNotEmpty() == true) {
                    if (having.isNotEmpty()) having.append(" AND ")
                    having.append("(").append(filter.getHaving()).append(")")
                }
            }
        }
        return CursorLoader(requireContext(),
                BggContract.Collection.buildUri(having.toString()),
                getProjection(),
                where.toString(),
                args,
                if (sorter == null) null else sorter!!.orderByClause)
    }

    private fun getProjection(): Array<String> {
        var projection = Query.PROJECTION.union(sorter?.columns?.asIterable() ?: emptyList())
        for (filter in filters) {
            projection = projection.union(filter.getColumns()?.asIterable() ?: emptyList())
        }
        return projection.toTypedArray()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        if (activity == null) return
        if (loader.id == Query.TOKEN) {
            val items: MutableList<CollectionItem> = ArrayList(cursor.count)
            if (cursor.moveToFirst()) {
                do {
                    items.add(CollectionItem(cursor, sorter!!))
                } while (cursor.moveToNext())
            }
            adapter.items = items
            val sectionItemDecoration = RecyclerSectionItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                    getSectionCallback(items)
            )
            while (listView.itemDecorationCount > 0) {
                listView.removeItemDecorationAt(0)
            }
            listView.addItemDecoration(sectionItemDecoration)
            rowCountView.text = String.format(Locale.getDefault(), "%,d", items.size)
            sortDescriptionView.text = if (sorter == null) "" else String.format(requireActivity().getString(R.string.by_prefix), sorter?.description.orEmpty())
            bindFilterButtons()
            invalidateMenu()
            if (items.size > 0) {
                listView.fadeIn()
                emptyContainer.fadeOut()
            } else {
                emptyContainer.fadeIn()
                listView.fadeOut()
            }
            progressBar.hide()
        } else {
            Timber.d("Query complete, Not Actionable: %s", loader.id)
            cursor.close()
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.items = arrayListOf()
    }

    override fun removeFilter(type: Int) {
        viewModel.removeFilter(type)
    }

    override fun addFilter(filter: CollectionFilterer) {
        viewModel.addFilter(filter)
        firebaseAnalytics.logEvent("Filter", bundleOf(
                FirebaseAnalytics.Param.CONTENT_TYPE to "Collection",
                "FilterBy" to filter.type.toString()
        ))
    }

    private fun setEmptyText() {
        val syncedStatuses = prefs.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null) ?: emptySet()
        if (syncedStatuses.isEmpty()) {
            setEmptyStateForSettingsAction(R.string.empty_collection_sync_off)
        } else {
            if (SyncPrefs.getPrefs(requireContext()).noPreviousCollectionSync()) {
                setEmptyStateForNoAction(R.string.empty_collection_sync_never)
            } else if (filters.isNotEmpty()) {
                val appliedStatuses = filters.filterIsInstance<CollectionStatusFilterer>().firstOrNull()?.getSelectedStatusesSet()
                        ?: emptySet()
                if (syncedStatuses.containsAll(appliedStatuses)) {
                    setEmptyStateForNoAction(R.string.empty_collection_filter_on)
                } else {
                    setEmptyStateForSettingsAction(R.string.empty_collection_filter_on_sync_partial)
                }
            } else {
                setEmptyStateForSettingsAction(R.string.empty_collection)
            }
        }
    }

    private fun setEmptyStateForSettingsAction(@StringRes textResId: Int) {
        emptyTextView.setText(textResId)
        emptyButton.visibility = View.VISIBLE
    }

    private fun setEmptyStateForNoAction(@StringRes textResId: Int) {
        emptyTextView.setText(textResId)
        emptyButton.visibility = View.GONE
    }

    private fun bindFilterButtons() {
        chipGroup.removeAllViews()
        for (filter in filters) {
            if (filter.toShortDescription().isNotEmpty()) {
                chipGroup.addView(Chip(requireContext(), null, R.style.Widget_MaterialComponents_Chip_Filter).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    text = filter.toShortDescription()
                    setOnClickListener { launchFilterDialog(filter.type) }
                    setOnLongClickListener {
                        removeFilter(filter.type)
                        true
                    }
                })
            }
        }
        val show = chipGroup.childCount > 0
        if (show) {
            chipGroupScrollView.slideUpIn()
        } else {
            chipGroupScrollView.slideDownOut()
        }
        swipeRefreshLayout.apply {
            setPadding(paddingLeft, paddingTop, paddingRight, if (show) resources.getDimensionPixelSize(R.dimen.chip_group_height) else 0)
        }
    }

    fun launchFilterDialog(filterType: Int): Boolean {
        val dialog = CollectionFilterDialogFactory().create(requireContext(), filterType)
        return if (dialog != null) {
            dialog.createDialog(requireContext(), this, filters.firstOrNull { it.type == filterType })
            true
        } else {
            Timber.w("Couldn't find a filter dialog of type %s", filterType)
            false
        }
    }

    inner class CollectionItem(cursor: Cursor, sorter: CollectionSorter) {
        val internalId = cursor.getLong(Query.ID)
        val collectionName = cursor.getString(Query.COLLECTION_NAME).orEmpty()
        val gameId = cursor.getInt(Query.GAME_ID)
        val gameName = cursor.getString(Query.GAME_NAME).orEmpty()
        val collectionThumbnailUrl = cursor.getString(Query.COLLECTION_THUMBNAIL_URL).orEmpty()
        val thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL).orEmpty()
        val imageUrl: String = cursor.getString(Query.IMAGE_URL).orEmpty()
        val heroImageUrl: String = cursor.getString(Query.HERO_IMAGE_URL).orEmpty()
        val isFavorite = cursor.getInt(Query.STARRED) == 1
        val timestamp: Long = sorter.getTimestamp(cursor)
        val rating = sorter.getRating(cursor)
        val ratingText = sorter.getRatingText(cursor)
        val displayInfo = sorter.getDisplayInfo(cursor)
        val headerText = sorter.getHeaderText(cursor, cursor.position)
        val customPlayerSort = cursor.getInt(Query.CUSTOM_PLAYER_SORT) == 1
        var year = YEAR_UNKNOWN

        init {
            val y = cursor.getInt(Query.COLLECTION_YEAR_PUBLISHED)
            year = if (y == YEAR_UNKNOWN) {
                cursor.getInt(Query.YEAR_PUBLISHED)
            } else {
                y
            }
        }
    }

    inner class CollectionAdapter : RecyclerView.Adapter<CollectionItemViewHolder>(), AutoUpdatableAdapter {
        var items: List<CollectionItem> by Delegates.observable(emptyList()) { _, old, new ->
            autoNotify(old, new) { o, n ->
                o.internalId == n.internalId
            }
        }

        private val selectedItems = SparseBooleanArray()

        fun getItem(position: Int): CollectionItem? {
            return items.getOrNull(position)
        }

        val randomItem: CollectionItem?
            get() = items.random()

        val selectedItemCount: Int
            get() = selectedItems.size()

        val selectedItemPositions: List<Int>
            get() {
                val items = ArrayList<Int>(selectedItems.size())
                (0 until selectedItems.size()).mapTo(items) { selectedItems.keyAt(it) }
                return items
            }

        fun toggleSelection(position: Int) {
            if (selectedItems[position, false]) {
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionItemViewHolder {
            return CollectionItemViewHolder(parent.inflate(R.layout.row_collection))
        }

        override fun onBindViewHolder(holder: CollectionItemViewHolder, position: Int) {
            holder.bindView(getItem(position), position)
        }

        inner class CollectionItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bindView(item: CollectionItem?, position: Int) {
                if (item == null) return
                itemView.nameView.text = item.collectionName
                itemView.yearView.text = item.year.asYear(context)
                itemView.timestampView.timestamp = item.timestamp
                itemView.favoriteView.isVisible = item.isFavorite
                if (item.ratingText.isNotEmpty()) {
                    itemView.ratingView.text = item.ratingText
                    itemView.ratingView.setTextViewBackground(item.rating.toColor(ratingColors))
                    itemView.ratingView.visibility = View.VISIBLE
                    itemView.infoView.visibility = View.GONE
                } else {
                    itemView.infoView.setTextOrHide(item.displayInfo)
                    itemView.infoView.visibility = View.VISIBLE
                    itemView.ratingView.visibility = View.GONE
                }
                itemView.thumbnailView.loadThumbnail(item.collectionThumbnailUrl, item.thumbnailUrl)
                itemView.isActivated = selectedItems[position, false]
                itemView.setOnClickListener {
                    when {
                        isCreatingShortcut -> createShortcut(item.gameId, item.gameName, item.thumbnailUrl)
                        changingGamePlayId != BggContract.INVALID_ID.toLong() -> {
                            LogPlayActivity.changeGame(context, changingGamePlayId, item.gameId, item.gameName, item.thumbnailUrl, item.imageUrl, item.heroImageUrl)
                            requireActivity().finish() // don't want to come back to collection activity in "pick a new game" mode
                        }
                        actionMode == null -> GameActivity.start(requireContext(), item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl)
                        else -> adapter.toggleSelection(position)
                    }
                }
                itemView.setOnLongClickListener {
                    if (isCreatingShortcut) return@setOnLongClickListener false
                    if (changingGamePlayId != BggContract.INVALID_ID.toLong()) return@setOnLongClickListener false
                    if (actionMode != null) return@setOnLongClickListener false
                    actionMode = requireActivity().startActionMode(this@CollectionFragment)
                    if (actionMode == null) return@setOnLongClickListener false
                    toggleSelection(position)
                    true
                }
            }
        }

        init {
            setHasStableIds(true)
        }
    }

    fun createShortcut(id: Int, name: String, thumbnailUrl: String) {
        val shortcutIntent = GameActivity.createIntentAsShortcut(requireContext(), id, name, thumbnailUrl)
        if (shortcutIntent != null) {
            val intent: Intent?
            if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
                intent = createShortcutForOreo(id, name, thumbnailUrl, shortcutIntent)
            } else {
                intent = ShortcutUtils.createShortcutIntent(context, name, shortcutIntent)
                val file = ShortcutUtils.getThumbnailFile(context, thumbnailUrl)
                if (file != null && file.exists()) {
                    @Suppress("DEPRECATION")
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapFactory.decodeFile(file.absolutePath))
                }
            }
            if (intent != null) requireActivity().setResult(Activity.RESULT_OK, intent)
        }
        requireActivity().finish()
    }

    @RequiresApi(api = VERSION_CODES.O)
    private fun createShortcutForOreo(id: Int, name: String, thumbnailUrl: String, shortcutIntent: Intent): Intent? {
        val shortcutManager = requireContext().getSystemService(ShortcutManager::class.java)
                ?: return null
        val builder = ShortcutInfo.Builder(context, ShortcutUtils.createGameShortcutId(id))
                .setShortLabel(name.truncate(ShortcutUtils.SHORT_LABEL_LENGTH))
                .setLongLabel(name.truncate(ShortcutUtils.LONG_LABEL_LENGTH))
                .setIntent(shortcutIntent)
        val file = ShortcutUtils.getThumbnailFile(context, thumbnailUrl)
        if (file != null && file.exists()) {
            builder.setIcon(Icon.createWithAdaptiveBitmap(BitmapFactory.decodeFile(file.absolutePath)))
        } else {
            builder.setIcon(Icon.createWithResource(context, R.drawable.ic_adaptive_game))
        }
        return shortcutManager.createShortcutResultIntent(builder.build())
    }

    private fun getSectionCallback(items: List<CollectionItem>): SectionCallback {
        return object : SectionCallback {
            override fun isSection(position: Int): Boolean {
                if (position == RecyclerView.NO_POSITION) return false
                if (items.isEmpty()) return false
                if (position == 0) return true
                if (position < 0 || position >= items.size) return false
                val thisLetter = getSectionHeader(position)
                val lastLetter = getSectionHeader(position - 1)
                return thisLetter != lastLetter
            }

            override fun getSectionHeader(position: Int): CharSequence {
                return items.getOrNull(position)?.headerText ?: "-"
            }
        }
    }

    private interface Query {
        companion object {
            const val TOKEN = 0x01
            val PROJECTION = arrayOf(
                    BggContract.Collection._ID,
                    BggContract.Collection.COLLECTION_ID,
                    BggContract.Collection.COLLECTION_NAME,
                    BggContract.Collection.YEAR_PUBLISHED,
                    BggContract.Collection.GAME_NAME,
                    Games.GAME_ID,
                    BggContract.Collection.COLLECTION_THUMBNAIL_URL,
                    BggContract.Collection.THUMBNAIL_URL,
                    BggContract.Collection.IMAGE_URL,
                    BggContract.Collection.COLLECTION_YEAR_PUBLISHED,
                    Games.CUSTOM_PLAYER_SORT,
                    Games.STARRED,
                    BggContract.Collection.COLLECTION_HERO_IMAGE_URL
            )
            const val ID = 0

            // int COLLECTION_ID = 1;
            const val COLLECTION_NAME = 2
            const val YEAR_PUBLISHED = 3
            const val GAME_NAME = 4
            const val GAME_ID = 5
            const val COLLECTION_THUMBNAIL_URL = 6
            const val THUMBNAIL_URL = 7
            const val IMAGE_URL = 8
            const val COLLECTION_YEAR_PUBLISHED = 9
            const val CUSTOM_PLAYER_SORT = 10
            const val STARRED = 11
            const val HERO_IMAGE_URL = 12
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.game_context, menu)
        adapter.clearSelection()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter.selectedItemCount
        mode.title = resources.getQuantityString(R.plurals.msg_games_selected, count, count)
        menu.findItem(R.id.menu_log_play).isVisible = count == 1 && prefs.showLogPlay()
        menu.findItem(R.id.menu_log_play_quick).isVisible = prefs.showQuickLogPlay()
        menu.findItem(R.id.menu_link).isVisible = count == 1
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        adapter.clearSelection()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (!adapter.selectedItemPositions.iterator().hasNext()) return false
        val ci = adapter.getItem(adapter.selectedItemPositions.iterator().next())
        when (item.itemId) {
            R.id.menu_log_play -> {
                ci?.let { LogPlayActivity.logPlay(context, it.gameId, it.gameName, it.thumbnailUrl, it.imageUrl, it.heroImageUrl, it.customPlayerSort) }
                mode.finish()
                return true
            }
            R.id.menu_log_play_quick -> {
                toast(resources.getQuantityString(R.plurals.msg_logging_plays, adapter.selectedItemCount))
                for (position in adapter.selectedItemPositions) {
                    adapter.getItem(position)?.let { ActivityUtils.logQuickPlay(activity, it.gameId, it.gameName) }
                }
                mode.finish()
                return true
            }
            R.id.menu_share -> {
                val shareMethod = "Collection"
                if (adapter.selectedItemCount == 1) {
                    ci?.let { ActivityUtils.shareGame(requireActivity(), it.gameId, it.gameName, shareMethod) }
                } else {
                    val games: MutableList<Pair<Int, String>> = ArrayList(adapter.selectedItemCount)
                    for (position in adapter.selectedItemPositions) {
                        adapter.getItem(position)?.let { games.add(Pair.create(it.gameId, it.gameName)) }
                    }
                    ActivityUtils.shareGames(requireActivity(), games, shareMethod)
                }
                mode.finish()
                return true
            }
            R.id.menu_link -> {
                ci?.gameId?.let { activity.linkBgg(it) }
                mode.finish()
                return true
            }
        }
        return false
    }

    private fun createViewDescription(sort: Sorter?, filters: List<CollectionFilterer>): String {
        val text = StringBuilder()
        if (filters.isNotEmpty()) {
            text.append(filters.joinToString(separator = "\n\u2022 ", prefix = getString(R.string.filtered_by)) { it.toLongDescription() })
        }
        text.append("\n\n")
        sort?.let { if (it.type != CollectionSorterFactory.TYPE_DEFAULT) text.append(getString(R.string.sort_description, it.description)) }
        return text.trim().toString()
    }

    companion object {
        private const val KEY_IS_CREATING_SHORTCUT = "IS_CREATING_SHORTCUT"
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun newInstance(isCreatingShortcut: Boolean): CollectionFragment {
            return CollectionFragment().withArguments(
                    KEY_IS_CREATING_SHORTCUT to isCreatingShortcut
            )
        }

        fun newInstanceForPlayGameChange(playId: Long): CollectionFragment {
            return CollectionFragment().withArguments(
                    KEY_CHANGING_GAME_PLAY_ID to playId
            )
        }
    }
}