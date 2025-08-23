package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.*
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentCollectionBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionStatusFilterer
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.pref.SettingsActivity
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_COMPLETE
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.sorter.CollectionSorter
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.CollectionFragment.CollectionAdapter.CollectionItemViewHolder
import com.boardgamegeek.ui.compose.CollectionItemListItem
import com.boardgamegeek.ui.dialog.*
import com.boardgamegeek.ui.theme.BggAppTheme
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.text.NumberFormat

@AndroidEntryPoint
class CollectionFragment : Fragment(), ActionMode.Callback {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private var viewId = CollectionViewPrefs.DEFAULT_DEFAULT_ID
    private var viewName = ""
    private var sorter: Pair<CollectionSorter, Boolean>? = null
    private val filters = mutableListOf<CollectionFilterer>()
    private var changingGamePlayId: Long = 0
    private var actionMode: ActionMode? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val viewModel by activityViewModels<CollectionViewViewModel>()
    private val adapter by lazy { CollectionAdapter() }
    private val prefs: SharedPreferences by lazy { requireContext().preferences() }
    private val numberFormat = NumberFormat.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        changingGamePlayId = arguments?.getLong(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID.toLong()) ?: BggContract.INVALID_ID.toLong()
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.listView.adapter = adapter

        if (changingGamePlayId != BggContract.INVALID_ID.toLong()) {
            binding.swipeRefreshLayout.longSnackbar(R.string.msg_change_play_game)
        }

        binding.footerToolbar.inflateMenu(R.menu.collection_fragment)
        binding.footerToolbar.setOnMenuItemClickListener(footerMenuListener)
        binding.footerToolbar.menu.apply {
            if (changingGamePlayId != BggContract.INVALID_ID.toLong()) {
                findItem(R.id.menu_collection_random_game)?.isVisible = false
                findItem(R.id.menu_create_shortcut)?.isVisible = false
                findItem(R.id.menu_collection_view_save)?.isVisible = false
                findItem(R.id.menu_collection_view_delete)?.isVisible = false
                findItem(R.id.menu_share)?.isVisible = false
            } else {
                findItem(R.id.menu_collection_random_game)?.isVisible = true
                findItem(R.id.menu_create_shortcut)?.isVisible = true
                findItem(R.id.menu_collection_view_save)?.isVisible = true
                findItem(R.id.menu_collection_view_delete)?.isVisible = true
                findItem(R.id.menu_share)?.isVisible = true
            }
        }

        setEmptyText()
        binding.emptyButton.setOnClickListener {
            startActivity(Intent(context, SettingsActivity::class.java))
        }

        binding.swipeRefreshLayout.setBggColors()
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.progressBar.show()
        viewModel.selectedViewId.observe(viewLifecycleOwner) {
            it?.let {
                binding.progressBar.show()
                binding.listView.isVisible = false
                viewId = it
                binding.footerToolbar.menu.findItem(R.id.menu_create_shortcut)?.isEnabled = it > 0
            }
        }
        viewModel.selectedViewName.observe(viewLifecycleOwner) { it?.let { viewName = it } }
        viewModel.views.observe(viewLifecycleOwner) {
            binding.footerToolbar.menu.findItem(R.id.menu_collection_view_delete)?.isEnabled = it?.isNotEmpty() == true
        }
        viewModel.effectiveSort.observe(viewLifecycleOwner) {
            sorter = it
            bindSortAndFilterButtons()
        }
        viewModel.effectiveFilters.observe(viewLifecycleOwner) { filterList ->
            filterList?.let {
                filters.clear()
                filters.addAll(it)
                setEmptyText()
                bindSortAndFilterButtons()
            }
        }
        viewModel.items.observe(viewLifecycleOwner) {
            it?.let { showData(it) }
        }
        viewModel.isFiltering.observe(viewLifecycleOwner) {
            it?.let { if (it) binding.progressBar.show() else binding.progressBar.hide() }
        }
        viewModel.isRefreshing.observe(viewLifecycleOwner) {
            it?.let { binding.swipeRefreshLayout.isRefreshing = it }
        }
        viewModel.refresh()
    }

    private fun showData(items: List<CollectionItem>) {
        adapter.items = items
        binding.footerToolbar.menu.apply {
            findItem(R.id.menu_collection_random_game)?.isEnabled = items.isNotEmpty()
            findItem(R.id.menu_share)?.isEnabled = items.isNotEmpty()
        }

        binding.listView.addHeader(adapter)
        binding.rowCountView.text = numberFormat.format(items.size)

        binding.emptyContainer.isVisible = items.isEmpty()
        binding.listView.isVisible = items.isNotEmpty()
        binding.progressBar.hide()
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
                viewModel.createShortcut()
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_view_save -> {
                val name = if (viewId <= 0) "" else viewName
                val dialog = SaveViewDialogFragment.newInstance(name, createViewDescription(sorter, filters))
                dialog.show(this@CollectionFragment.parentFragmentManager, "view_save")
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_view_delete -> {
                DeleteViewDialogFragment.newInstance().show(this@CollectionFragment.parentFragmentManager, "view_delete")
                return@OnMenuItemClickListener true
            }
            R.id.menu_share -> {
                shareCollection()
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_sort -> {
                CollectionSortDialogFragment.newInstance(sorter?.first?.getType(sorter?.second ?: false) ?: CollectionSorterFactory.TYPE_DEFAULT)
                    .show(this@CollectionFragment.parentFragmentManager, "collection_sort")
                return@OnMenuItemClickListener true
            }
            R.id.menu_collection_filter -> {
                CollectionFilterDialogFragment().show(this@CollectionFragment.parentFragmentManager, "collection_filter")
                return@OnMenuItemClickListener true
            }
        }
        launchFilterDialog(item.itemId)
    }

    private fun shareCollection() {
        val description: String = when {
            viewId > 0 && viewName.isNotEmpty() -> viewName
            filters.isNotEmpty() -> getString(R.string.title_filtered_collection)
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
            .append(getString(R.string.share_collection_complete_footer, "https://www.boardgamegeek.com/collection/user/${username.encodeForUrl()}"))
        val fullName = prefs[AccountPreferences.KEY_FULL_NAME, ""]
        requireActivity().share(getString(R.string.share_collection_subject, fullName, username), text, R.string.title_share_collection)
    }

    private fun setEmptyText() {
        val syncedStatuses = prefs.getSyncStatusesOrDefault()
        if (syncedStatuses.isEmpty()) {
            setEmptyStateForSettingsAction(R.string.empty_collection_sync_off)
        } else {
            val lastSyncTimestamp = SyncPrefs.getPrefs(requireContext())[TIMESTAMP_COLLECTION_COMPLETE, 0L] ?: 0L
            if (lastSyncTimestamp == 0L) {
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
        binding.emptyTextView.setText(textResId)
        binding.emptyButton.isVisible = true
    }

    private fun setEmptyStateForNoAction(@StringRes textResId: Int) {
        binding.emptyTextView.setText(textResId)
        binding.emptyButton.isVisible = false
    }

    @Suppress("SameParameterValue")
    private fun findOrCreateSortChip(sortTag: String): Chip {
        return binding.chipGroup.findViewWithTag(sortTag) ?: Chip(requireContext(), null, R.style.Widget_MaterialComponents_Chip_Choice).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tag = sortTag
            binding.chipGroup.addView(this)
        }
    }

    private fun findOrCreateFilterChip(filterType: Int): Chip {
        return binding.chipGroup.findViewWithTag(filterType.toString()) ?: Chip(
            requireContext(),
            null,
            R.style.Widget_MaterialComponents_Chip_Filter
        ).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tag = filterType.toString()
            binding.chipGroup.addView(this)
        }
    }

    private fun bindSortAndFilterButtons() {
        val sortTag = "SORT"

        sorter?.let {
            findOrCreateSortChip(sortTag).apply {
                if (it.first.getType(it.second) != CollectionSorterFactory.TYPE_DEFAULT) {
                    text = it.first.description
                    chipIcon = AppCompatResources.getDrawable(
                        requireContext(),
                        if (it.second) R.drawable.ic_baseline_arrow_downward_24 else R.drawable.ic_baseline_arrow_upward_24
                    )
                    chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_dark))
                    setOnClickListener { _ ->
                        viewModel.reverseSort()
                    }
                } else binding.chipGroup.removeView(this)
            }
        }

        val existingTags = binding.chipGroup.children.map { it.tag.toString() }.toList()

        for (filter in filters.filter { it.isValid }) {
            findOrCreateFilterChip(filter.type).apply {
                text = filter.chipText()
                if (filter.iconResourceId != CollectionFilterer.INVALID_ICON) {
                    chipIcon = AppCompatResources.getDrawable(requireContext(), filter.iconResourceId)
                    chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_dark))
                    isChipIconVisible = true
                } else isChipIconVisible = false
                setOnClickListener { launchFilterDialog(filter.type) }
                setOnLongClickListener {
                    viewModel.removeFilter(filter.type)
                    true
                }
            }
        }

        val usedTags = filters.filter { it.isValid }.map { it.type.toString() } + sortTag
        val tagsToRemove = existingTags - usedTags.toSet()
        tagsToRemove.forEach { tag ->
            binding.chipGroup.findViewWithTag<Chip>(tag)?.let { chip ->
                binding.chipGroup.removeView(chip)
            }
        }

        val show = binding.chipGroup.isNotEmpty()
        if (show) {
            binding.chipGroupScrollView.slideUpIn()
        } else {
            binding.chipGroupScrollView.slideDownOut()
        }
        binding.swipeRefreshLayout.updatePadding(
            bottom = if (show) resources.getDimensionPixelSize(R.dimen.chip_group_height) else 0
        )

        val hasFiltersApplied = filters.isNotEmpty()
        val hasSortApplied = sorter?.let { it.first.getType(it.second) != CollectionSorterFactory.TYPE_DEFAULT } ?: false
        binding.footerToolbar.menu.findItem(R.id.menu_collection_view_save)?.isEnabled = hasFiltersApplied || hasSortApplied
    }

    private fun launchFilterDialog(filterType: Int): Boolean {
        val dialog = CollectionFilterDialogFactory().create(requireContext(), filterType)
        return if (dialog != null) {
            dialog.createDialog(requireActivity(), filters.find { it.type == filterType })
            true
        } else {
            Timber.w("Couldn't find a filter dialog of type %s", filterType)
            false
        }
    }

    inner class CollectionAdapter : RecyclerView.Adapter<CollectionItemViewHolder>(), SectionCallback {
        var items: List<CollectionItem> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        private val selectedItems = SparseBooleanArray()

        fun getItem(position: Int) = items.getOrNull(position)

        val selectedItemCount: Int
            get() = selectedItems.filterTrue().size

        private val selectedItemPositions: List<Int>
            get() = selectedItems.filterTrue()

        fun getSelectedItems() = selectedItemPositions.mapNotNull { items.getOrNull(it) }

        @SuppressLint("NotifyDataSetChanged")
        fun toggleSelection(position: Int) {
            selectedItems.toggle(position)
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
            val oldSelectedItems = selectedItems.clone()
            selectedItems.clear()
            oldSelectedItems.filterTrue().forEach { notifyItemChanged(it) }
        }

        override fun getItemCount() = items.size

        override fun getItemId(position: Int) = getItem(position)?.internalId ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionItemViewHolder {
            return CollectionItemViewHolder(ComposeView(parent.context))
        }

        override fun onBindViewHolder(holder: CollectionItemViewHolder, position: Int) {
            getItem(position)?.let { holder.bindView(it, position) }
        }

        inner class CollectionItemViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bindView(item: CollectionItem, position: Int) {
                composeView.setContent {
                    BggAppTheme {
                        CollectionItemListItem(
                            name = item.collectionName,
                            thumbnailUrl = item.thumbnailUrl,
                            yearPublished = item.yearPublished,
                            isFavorite = item.isFavorite,
                            infoText = sorter?.first?.getDisplayInfo(item),
                            rating = sorter?.first?.getRating(item),
                            timestamp = sorter?.first?.getTimestamp(item),
                            modifier = Modifier,
                            isSelected = selectedItems[position, false],
                            onClick = {
                                when {
                                    changingGamePlayId != BggContract.INVALID_ID.toLong() -> {
                                        LogPlayActivity.changeGame(
                                            requireContext(),
                                            changingGamePlayId,
                                            item.gameId,
                                            item.gameName,
                                            item.robustHeroImageUrl,
                                        )
                                        requireActivity().finish() // don't want to come back to collection activity in "pick a new game" mode
                                    }
                                    actionMode == null -> GameActivity.start(
                                        requireContext(),
                                        item.gameId,
                                        item.gameName,
                                        item.thumbnailUrl,
                                        item.heroImageUrl
                                    )
                                    else -> adapter.toggleSelection(position)
                                }
                            },
                            onLongClick = {
                                if (changingGamePlayId == BggContract.INVALID_ID.toLong() &&
                                    actionMode == null
                                ) {
                                    actionMode = requireActivity().startActionMode(this@CollectionFragment)
                                    toggleSelection(position)
                                }
                            }
                        )
                    }
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
            return sorter?.first?.getHeaderText(item) ?: return "-"
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
        menu.findItem(R.id.menu_log_play_form)?.isVisible = count == 1
        menu.findItem(R.id.menu_log_play_wizard)?.isVisible = count == 1
        menu.findItem(R.id.menu_link)?.isVisible = count == 1
        (activity as? DrawerActivity)?.lockDrawerClosed()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        adapter.clearSelection()
        (activity as? DrawerActivity)?.unlockDrawer()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val items = adapter.getSelectedItems()
        if (items.isEmpty()) return false

        when (item.itemId) {
            R.id.menu_log_play_form -> {
                items.firstOrNull()?.let {
                    LogPlayActivity.logPlay(
                        requireContext(),
                        it.gameId,
                        it.gameName,
                        it.robustHeroImageUrl,
                        it.arePlayersCustomSorted,
                    )
                }
            }
            R.id.menu_log_play_quick -> {
                items.forEach {
                    viewModel.logQuickPlay(it.gameId, it.gameName)
                }
                toast(resources.getQuantityString(R.plurals.msg_logging_plays, items.size))
            }
            R.id.menu_log_play_wizard -> {
                items.firstOrNull()?.let { NewPlayActivity.start(requireContext(), it.gameId, it.gameName) }
            }
            R.id.menu_share -> {
                val shareMethod = "Collection"
                if (items.size == 1) {
                    items.firstOrNull()?.let { requireActivity().shareGame(it.gameId, it.gameName, shareMethod, firebaseAnalytics) }
                } else {
                    requireActivity().shareGames(items.map { it.gameId to it.gameName }, shareMethod, firebaseAnalytics)
                }
            }
            R.id.menu_link -> {
                items.firstOrNull()?.gameId?.let { activity.linkBgg(it) }
            }
            else -> return false
        }
        mode.finish()
        return true
    }

    private fun createViewDescription(sort: Pair<CollectionSorter, Boolean>?, filters: List<CollectionFilterer>): String {
        val text = StringBuilder()
        if (filters.isNotEmpty()) {
            text.append(getString(R.string.filtered_by))
            filters.map { "\n\u2022 ${it.description()}" }.forEach { text.append(it) }
        }
        text.append("\n\n")
        sort?.let {
            if (it.first.getType(it.second) != CollectionSorterFactory.TYPE_DEFAULT) text.append(
                getString(
                    R.string.sort_description,
                    it.first.description
                )
            )
        }
        return text.trim().toString()
    }

    companion object {
        private const val KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID"

        fun newInstance(): CollectionFragment {
            return CollectionFragment()
        }

        fun newInstanceForPlayGameChange(playId: Long): CollectionFragment {
            return CollectionFragment().apply {
                arguments = bundleOf(KEY_CHANGING_GAME_PLAY_ID to playId)
            }
        }
    }
}
