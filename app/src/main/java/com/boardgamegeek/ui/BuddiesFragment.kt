package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentBuddiesBinding
import com.boardgamegeek.databinding.RowBuddyBinding
import com.boardgamegeek.model.User
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import dagger.hilt.android.AndroidEntryPoint

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

        viewModel.refreshing.observe(viewLifecycleOwner) {
            it?.let { binding.swipeRefresh.isRefreshing = it }
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let { it.getContentIfNotHandled()?.let { message ->
                showError(message)
            } }
        }

        viewModel.buddies.observe(viewLifecycleOwner) {
            it?.let {
                showData(it)
                binding.progressBar.hide()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    private fun showError(message: String?) {
        binding.coordinatorLayout.indefiniteSnackbar(message ?: getString(R.string.msg_error_buddies))
    }

    private fun showData(buddies: List<User>) {
        adapter.submitList(buddies)
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
        viewModel.refresh()
    }

    class BuddiesAdapter(private val viewModel: BuddiesViewModel) :
        ListAdapter<User, BuddiesAdapter.BuddyViewHolder>(
            object : DiffUtil.ItemCallback<User>() {
                override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.username == newItem.username
                override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
            }
        ), SectionCallback {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int) = getItem(position).username.hashCode().toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuddyViewHolder {
            return BuddyViewHolder(parent.inflate(R.layout.row_buddy))
        }

        override fun onBindViewHolder(holder: BuddyViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (currentList.isEmpty()) return false
            if (position == 0) return true
            val thisHeader = viewModel.getSectionHeader(currentList.getOrNull(position))
            val lastHeader = viewModel.getSectionHeader(currentList.getOrNull(position - 1))
            return thisHeader != lastHeader
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return viewModel.getSectionHeader(getItem(position))
        }

        inner class BuddyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowBuddyBinding.bind(itemView)

            fun bind(buddy: User) {
                binding.avatarView.loadThumbnail(buddy.avatarUrl, R.drawable.person_image_empty)
                if (buddy.fullName.isBlank()) {
                    binding.fullNameView.text = buddy.username
                    binding.usernameView.isVisible = false
                } else {
                    binding.fullNameView.text = buddy.fullName
                    binding.usernameView.setTextOrHide(buddy.username)
                }
                itemView.setOnClickListener {
                    BuddyActivity.start(itemView.context, buddy.username, buddy.fullName)
                }
            }
        }
    }
}
