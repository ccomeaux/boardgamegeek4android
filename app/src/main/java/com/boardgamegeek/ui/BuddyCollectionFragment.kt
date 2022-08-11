package com.boardgamegeek.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
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

class BuddyCollectionFragment : Fragment() {
    private var _binding: FragmentBuddyCollectionBinding? = null
    private val binding get() = _binding!!
    private var currentStatus = BuddyCollectionViewModel.DEFAULT_STATUS
    private lateinit var statuses: Map<String, String>

    private var subMenu: SubMenu? = null
    private val adapter: BuddyCollectionAdapter by lazy { BuddyCollectionAdapter() }
    private val viewModel by activityViewModels<BuddyCollectionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.buddy_collection, menu)
                subMenu = menu.findItem(R.id.menu_collection_status)?.subMenu
                subMenu?.let {
                    statuses.values.forEachIndexed { index, title ->
                        it.add(Menu.FIRST, Menu.FIRST + index, index, title)
                    }
                    it.setGroupCheckable(Menu.FIRST, true, true)
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.menu_collection_random_game)?.isVisible = adapter.itemCount > 0
                subMenu?.let { submenu ->
                    submenu.children.find { it.title == statuses[currentStatus] }?.setChecked(true)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when {
                    (Menu.FIRST..(Menu.FIRST + statuses.size)).contains(menuItem.itemId) -> {
                        val newStatus = statuses.keys.elementAt(menuItem.itemId - Menu.FIRST)
                        if (newStatus.isNotEmpty() && newStatus != currentStatus) {
                            viewModel.setStatus(newStatus)
                            return true
                        }
                    }
                    menuItem.itemId == R.id.menu_collection_random_game -> {
                        val ci = adapter.items.random()
                        GameActivity.start(requireContext(), ci.gameId, ci.gameName, ci.thumbnailUrl)
                        return true
                    }
                }
                return false
            }
        })

        val statusEntries = resources.getStringArray(R.array.pref_sync_status_entries)
        val statusValues = resources.getStringArray(R.array.pref_sync_status_values)
        statuses = statusValues.zip(statusEntries).toMap()
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
                            showError(getString(R.string.empty_buddy_collection))
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

    private fun showError(message: String) {
        binding.emptyView.text = message
        binding.emptyView.isVisible = true
        binding.recyclerView.isVisible = false
    }
}
