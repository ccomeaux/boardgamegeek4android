package com.boardgamegeek.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
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
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionStatusFilterer
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.noPreviousCollectionSync
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.sorter.CollectionSorter
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.CollectionFragment.CollectionAdapter.CollectionItemViewHolder
import com.boardgamegeek.ui.dialog.*
import com.boardgamegeek.ui.dialog.CollectionFilterDialog.OnFilterChangedListener
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
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
import timber.log.Timber
import java.text.NumberFormat
import java.util.*

class CollectionFragment : Fragment(R.layout.fragment_collection), ActionMode.Callback, OnFilterChangedListener {
    private var viewId = CollectionView.DEFAULT_DEFAULT_ID
    private var viewName = ""
    private var sorter: CollectionSorter? = null
    private val filters: MutableList<CollectionFilterer> = ArrayList()
    private var isCreatingShortcut = false
    private var changingGamePlayId: Long = 0
    private var actionMode: ActionMode? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val viewModel by activityViewModels<CollectionViewViewModel>()
    private val adapter by lazy { CollectionAdapter() }
    private val prefs: SharedPreferences by lazy { requireContext().preferences() }
    private val collectionSorterFactory: CollectionSorterFactory by lazy { CollectionSorterFactory(requireContext()) }
    private val numberFormat = NumberFormat.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        readBundle(arguments)
        setHasOptionsMenu(true)
    }

    private fun readBundle(bundle: Bundle?) {
        isCreatingShortcut = bundle?.getBoolean(KEY_IS_CREATING_SHORTCUT) ?: false
        changingGamePlayId = bundle?.getLong(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong()) ?: BggContract.INVALID_ID.toLong()
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
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = viewModel.refresh()
        }

        progressBar.show()
        viewModel.selectedViewId.observe(viewLifecycleOwner) {
            progressBar.show()
            viewId = it
        }
        viewModel.selectedViewName.observe(viewLifecycleOwner) {
            viewName = it
        }
        viewModel.effectiveSortType.observe(viewLifecycleOwner) { sortType: Int ->
            sorter = collectionSorterFactory.create(sortType)
            sortDescriptionView.text = if (sorter == null) "" else requireActivity().getString(R.string.by_prefix, sorter?.description.orEmpty())
        }
        viewModel.effectiveFilters.observe(viewLifecycleOwner) {
            filters.clear()
            it?.let { filters.addAll(it) }
            setEmptyText()
            bindFilterButtons()
        }
        viewModel.items.observe(viewLifecycleOwner) {
            it?.let { showData(it) }
        }
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            swipeRefreshLayout.post { swipeRefreshLayout?.isRefreshing = it }
        }
        viewModel.refresh()
    }

    private fun showData(items: List<CollectionItemEntity>) {
        adapter.items = items

        listView.addHeader(adapter)

        rowCountView.text = numberFormat.format(items.size)
        invalidateMenu()

        if (items.isEmpty()) {
            emptyContainer.fadeIn()
            listView.fadeOut()
        } else {
            listView.fadeIn()
            emptyContainer.fadeOut()
        }
        progressBar.hide()
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
            val hasSortApplied = sorter?.let { it.type != CollectionSorterFactory.TYPE_DEFAULT } ?: false
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
                adapter.items.random().let {
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
                DialogUtils.showAndSurvive(
                    this@CollectionFragment,
                    CollectionSortDialogFragment.newInstance(sorter?.type ?: CollectionSorterFactory.TYPE_DEFAULT)
                )
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
        adapter.items.take(maxGames).map { text.append("\u2022 ${formatGameLink(it.gameId, it.collectionName)}") }
        val leftOverCount = adapter.itemCount - maxGames
        if (leftOverCount > 0) text.append(getString(R.string.and_more, leftOverCount)).append("\n")

        val username = prefs[AccountPreferences.KEY_USERNAME, ""]
        text.append("\n")
            .append(createViewDescription(sorter, filters))
            .append("\n")
            .append("\n")
            .append(getString(R.string.share_collection_complete_footer, "https://www.boardgamegeek.com/collection/user/${HttpUtils.encode(username)}"))
        val fullName = prefs[AccountPreferences.KEY_FULL_NAME, ""]
        requireActivity().share(getString(R.string.share_collection_subject, fullName, username), text, R.string.title_share_collection)
    }

    override fun removeFilter(type: Int) {
        progressBar.show()
        viewModel.removeFilter(type)
    }

    override fun addFilter(filter: CollectionFilterer) {
        progressBar.show()
        viewModel.addFilter(filter)
        firebaseAnalytics.logEvent(
            "Filter",
            bundleOf(
                FirebaseAnalytics.Param.CONTENT_TYPE to "Collection",
                "FilterBy" to filter.type.toString()
            )
        )
    }

    private fun setEmptyText() {
        val syncedStatuses = prefs.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null).orEmpty()
        if (syncedStatuses.isEmpty()) {
            setEmptyStateForSettingsAction(R.string.empty_collection_sync_off)
        } else {
            if (SyncPrefs.getPrefs(requireContext()).noPreviousCollectionSync()) {
                setEmptyStateForNoAction(R.string.empty_collection_sync_never)
            } else if (filters.isNotEmpty()) {
                val appliedStatuses = filters.filterIsInstance<CollectionStatusFilterer>().firstOrNull()?.getSelectedStatusesSet().orEmpty()
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

    inner class CollectionAdapter : RecyclerView.Adapter<CollectionItemViewHolder>(), SectionCallback {
        var items: List<CollectionItemEntity> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        private val selectedItems = SparseBooleanArray()

        fun getItem(position: Int) = items.getOrNull(position)

        val selectedItemCount = selectedItems.size()

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
            fun bindView(item: CollectionItemEntity?, position: Int) {
                if (item == null) return
                itemView.nameView.text = item.collectionName
                val year = if (item.collectionYearPublished == CollectionItemEntity.YEAR_UNKNOWN) item.gameYearPublished else item.collectionYearPublished
                itemView.yearView.text = year.asYear(context)
                itemView.timestampView.timestamp = sorter?.getTimestamp(item) ?: 0L
                itemView.favoriteView.isVisible = item.isFavorite
                val ratingText = sorter?.getRatingText(item).orEmpty()
                if (ratingText.isNotEmpty()) {
                    itemView.ratingView.text = ratingText
                    sorter?.getRating(item)?.let { itemView.ratingView.setTextViewBackground(it.toColor(ratingColors)) }
                    itemView.ratingView.visibility = View.VISIBLE
                    itemView.infoView.visibility = View.GONE
                } else {
                    itemView.infoView.setTextOrHide(sorter?.getDisplayInfo(item))
                    itemView.infoView.visibility = View.VISIBLE
                    itemView.ratingView.visibility = View.GONE
                }
                itemView.thumbnailView.loadThumbnail(item.thumbnailUrl)
                itemView.isActivated = selectedItems[position, false]
                itemView.setOnClickListener {
                    when {
                        isCreatingShortcut -> createShortcut(item.gameId, item.gameName, item.thumbnailUrl)
                        changingGamePlayId != BggContract.INVALID_ID.toLong() -> {
                            LogPlayActivity.changeGame(
                                requireContext(),
                                changingGamePlayId,
                                item.gameId,
                                item.gameName,
                                item.thumbnailUrl,
                                item.imageUrl,
                                item.heroImageUrl
                            )
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
            val item = items.getOrNull(position) ?: return "-"
            return sorter?.getHeaderText(item) ?: return "-"
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
            intent?.let { requireActivity().setResult(Activity.RESULT_OK, it) }
        }
        requireActivity().finish()
    }

    @RequiresApi(api = VERSION_CODES.O)
    private fun createShortcutForOreo(id: Int, name: String, thumbnailUrl: String, shortcutIntent: Intent): Intent? {
        val shortcutManager = requireContext().getSystemService(ShortcutManager::class.java) ?: return null
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

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.game_context, menu)
        adapter.clearSelection()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter.selectedItemCount
        mode.title = resources.getQuantityString(R.plurals.msg_games_selected, count, count)
        menu.findItem(R.id.menu_log_play_form)?.isVisible = count == 1
        menu.findItem(R.id.menu_log_play_wizard)?.isVisible = count == 1
        menu.findItem(R.id.menu_link)?.isVisible = count == 1
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
            R.id.menu_log_play_form -> {
                ci?.let {
                    LogPlayActivity.logPlay(
                        requireContext(),
                        it.gameId,
                        it.gameName,
                        it.thumbnailUrl,
                        it.imageUrl,
                        it.heroImageUrl,
                        it.arePlayersCustomSorted
                    )
                }
                mode.finish()
                return true
            }
            R.id.menu_log_play_quick -> {
                toast(resources.getQuantityString(R.plurals.msg_logging_plays, adapter.selectedItemCount))
                for (position in adapter.selectedItemPositions) {
                    adapter.getItem(position)?.let { viewModel.logQuickPlay(it.gameId, it.gameName) }
                }
                mode.finish()
                return true
            }
            R.id.menu_log_play_wizard -> {
                ci?.let { NewPlayActivity.start(requireContext(), it.gameId, it.gameName) }
                mode.finish()
                return true
            }
            R.id.menu_share -> {
                val shareMethod = "Collection"
                if (adapter.selectedItemCount == 1) {
                    ci?.let { requireActivity().shareGame(it.gameId, it.gameName, shareMethod, firebaseAnalytics) }
                } else {
                    val games: MutableList<Pair<Int, String>> = ArrayList(adapter.selectedItemCount)
                    for (position in adapter.selectedItemPositions) {
                        adapter.getItem(position)?.let { games.add(it.gameId to it.gameName) }
                    }
                    requireActivity().shareGames(games, shareMethod, firebaseAnalytics)
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

    private fun createViewDescription(sort: CollectionSorter?, filters: List<CollectionFilterer>): String {
        val text = StringBuilder()
        if (filters.isNotEmpty()) {
            text.append(getString(R.string.filtered_by))
            filters.forEach {
                text.append("\n\u2022 ${it.toLongDescription()}")
            }
        }
        text.append("\n\n")
        sort?.let { if (it.type != CollectionSorterFactory.TYPE_DEFAULT) text.append(getString(R.string.sort_description, it.description)) }
        return text.trim().toString()
    }

    companion object {
        private const val KEY_IS_CREATING_SHORTCUT = "IS_CREATING_SHORTCUT"
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun newInstance(isCreatingShortcut: Boolean): CollectionFragment {
            return CollectionFragment().apply {
                arguments = bundleOf(KEY_IS_CREATING_SHORTCUT to isCreatingShortcut)
            }
        }

        fun newInstanceForPlayGameChange(playId: Long): CollectionFragment {
            return CollectionFragment().apply {
                arguments = bundleOf(KEY_CHANGING_GAME_PLAY_ID to playId)
            }
        }
    }
}
