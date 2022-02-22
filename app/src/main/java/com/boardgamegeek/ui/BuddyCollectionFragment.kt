package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentBuddyCollectionBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.ui.adapter.BuddyCollectionAdapter
import com.boardgamegeek.ui.viewmodel.BuddyCollectionViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class BuddyCollectionFragment : Fragment() {
    private var _binding: FragmentBuddyCollectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var currentStatus = BuddyCollectionViewModel.DEFAULT_STATUS
    private lateinit var statuses: Map<String, String>

    private var subMenu: SubMenu? = null
    private val adapter: BuddyCollectionAdapter by lazy { BuddyCollectionAdapter() }
    private val viewModel by activityViewModels<BuddyCollectionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val statusEntries = resources.getStringArray(R.array.pref_sync_status_entries)
        val statusValues = resources.getStringArray(R.array.pref_sync_status_values)
        statuses = statusValues.zip(statusEntries).toMap()

        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentBuddyCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            RecyclerSectionItemDecoration(resources.getDimensionPixelSize(R.dimen.recycler_section_header_height), adapter)
        )

        viewModel.status.observe(viewLifecycleOwner) {
            currentStatus = it ?: BuddyCollectionViewModel.DEFAULT_STATUS
        }
        viewModel.collection.observe(viewLifecycleOwner) { resource ->
            resource?.let { (status, data, message) ->
                when (status) {
                    Status.REFRESHING -> binding.progressView.show()
                    Status.ERROR -> {
                        showError(message)
                        binding.progressView.hide()
                    }
                    Status.SUCCESS -> {
                        activity?.invalidateOptionsMenu()
                        if (data == null || data.isEmpty()) {
                            showError(R.string.empty_buddy_collection)
                        } else {
                            adapter.items = data
                            binding.emptyView.isVisible = false
                            binding.recyclerView.isVisible = true
                        }
                        binding.progressView.hide()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.buddy_collection, menu)
        subMenu = menu.findItem(R.id.menu_collection_status)?.subMenu
        subMenu?.let {
            statuses.values.forEachIndexed { index, title ->
                it.add(Menu.FIRST, Menu.FIRST + index, index, title)
            }
            it.setGroupCheckable(Menu.FIRST, true, true)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_collection_random_game)?.isVisible = adapter.itemCount > 0
        // check the proper submenu item
        subMenu?.let { submenu ->
            submenu.children.find { it.title == statuses[currentStatus] }?.setChecked(true)
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when {
            (Menu.FIRST..(Menu.FIRST + statuses.size)).contains(item.itemId) -> {
                val newStatus = statuses.keys.elementAt(item.itemId - Menu.FIRST)
                if (newStatus.isNotEmpty() && newStatus != currentStatus) {
                    firebaseAnalytics.logEvent("Filter") {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
                        param("filterType", newStatus)
                    }
                    viewModel.setStatus(newStatus)
                    return true
                }
            }
            item.itemId == R.id.menu_collection_random_game -> {
                val ci = adapter.items.random()
                GameActivity.start(requireContext(), ci.gameId, ci.gameName, ci.thumbnailUrl)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showError(@StringRes messageResId: Int) {
        showError(getString(messageResId))
    }

    private fun showError(message: String) {
        binding.emptyView.text = message
        binding.emptyView.isVisible = true
        binding.recyclerView.isVisible = false
    }
}
