package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentBuddiesBinding
import com.boardgamegeek.databinding.RowBuddyBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.User
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class BuddiesFragment : Fragment() {
    private var _binding: FragmentBuddiesBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<BuddiesViewModel>()
    private val adapter: BuddiesAdapter by lazy { BuddiesAdapter(viewModel) }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentBuddiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { triggerRefresh() }
        binding.swipeRefresh.setBggColors()

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
            resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
            adapter
        )
        binding.recyclerView.addItemDecoration(sectionItemDecoration)

        viewModel.buddies.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING

            when (it.status) {
                Status.ERROR -> showError(it.message)
                else -> it.data?.let { data -> showData(data) }
            }

            binding.progressBar.hide()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String?) {
        binding.coordinatorLayout.indefiniteSnackbar(message ?: getString(R.string.msg_error_buddies))
    }

    private fun showData(buddies: List<User>) {
        adapter.buddies = buddies
        if (buddies.isEmpty()) {
            binding.recyclerView.isVisible = false
            showEmpty()
        } else {
            binding.recyclerView.isVisible = true
            binding.emptyContainer.isVisible = false
        }
    }

    private fun showEmpty() {
        val prefs = requireContext().preferences()
        if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] == true) {
            binding.emptyTextView.setText(R.string.empty_buddies)
            binding.emptyButton.isGone = true
        } else {
            binding.emptyTextView.setText(R.string.empty_buddies_sync_off)
            binding.emptyButton.setOnClickListener {
                prefs[PREFERENCES_KEY_SYNC_BUDDIES] = true
                triggerRefresh()
                showEmpty()
            }
            binding.emptyButton.isVisible = true
        }
        binding.emptyContainer.isVisible = true
    }

    private fun triggerRefresh() {
        binding.swipeRefresh.isRefreshing = viewModel.refresh()
    }

    class BuddiesAdapter(private val viewModel: BuddiesViewModel) :
        RecyclerView.Adapter<BuddiesAdapter.BuddyViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var buddies: List<User> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = buddies.size

        override fun getItemId(position: Int) = buddies.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuddyViewHolder {
            return BuddyViewHolder(parent.inflate(R.layout.row_buddy))
        }

        override fun onBindViewHolder(holder: BuddyViewHolder, position: Int) {
            holder.bind(buddies.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (buddies.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = viewModel.getSectionHeader(buddies.getOrNull(position))
            val lastLetter = viewModel.getSectionHeader(buddies.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return viewModel.getSectionHeader(buddies.getOrNull(position)) ?: "-"
        }

        inner class BuddyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowBuddyBinding.bind(itemView)

            fun bind(buddy: User?) {
                buddy?.let { b ->
                    binding.avatarView.loadThumbnail(b.avatarUrl, R.drawable.person_image_empty)
                    if (b.fullName.isBlank()) {
                        binding.fullNameView.text = b.userName
                        binding.usernameView.isVisible = false
                    } else {
                        binding.fullNameView.text = b.fullName
                        binding.usernameView.setTextOrHide(b.userName)
                    }
                    itemView.setOnClickListener {
                        BuddyActivity.start(itemView.context, b.userName, b.fullName)
                    }
                }
            }
        }
    }
}
