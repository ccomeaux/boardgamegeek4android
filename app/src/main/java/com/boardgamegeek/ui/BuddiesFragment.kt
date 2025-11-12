package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentBuddiesBinding
import com.boardgamegeek.databinding.RowBuddyBinding
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import org.jetbrains.anko.design.indefiniteSnackbar
import kotlin.properties.Delegates

class BuddiesFragment : Fragment() {
    private var _binding: FragmentBuddiesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BuddiesViewModel by lazy {
        ViewModelProvider(this).get(BuddiesViewModel::class.java)
    }

    private val adapter: BuddiesAdapter by lazy {
        BuddiesAdapter(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
                adapter)
        binding.recyclerView.addItemDecoration(sectionItemDecoration)

        viewModel.buddies.observe(this, Observer {
            binding.swipeRefresh.post { binding.swipeRefresh.isRefreshing = it?.status == Status.REFRESHING }

            when {
                it.status == Status.ERROR -> showError(it.message)
                else -> showData(it?.data ?: emptyList())
            }
            binding.progressBar.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showError(message: String? = null) {
        // TODO default error message
        if (message != null && !message.isNullOrBlank()) {
            binding.coordinatorLayout.indefiniteSnackbar(message)
        }
    }

    private fun showData(buddies: List<UserEntity>) {
        adapter.buddies = buddies
        if (adapter.itemCount == 0) {
            binding.recyclerView.fadeOut()
            showEmpty()
        } else {
            binding.recyclerView.fadeIn()
            binding.emptyContainer.fadeOut()
        }
    }

    private fun showEmpty() {
        if (requireContext().getSyncBuddies()) {
            binding.emptyTextView.setText(R.string.empty_buddies)
            binding.emptyButton.isGone = true
        } else {
            binding.emptyTextView.setText(R.string.empty_buddies_sync_off)
            binding.emptyButton.setOnClickListener {
                requireContext().setSyncBuddies()
                triggerRefresh()
                showEmpty()
            }
            binding.emptyButton.isVisible = true
        }
        binding.emptyContainer.fadeIn()
    }

    private fun triggerRefresh() {
        binding.swipeRefresh.isRefreshing = viewModel.refresh()
    }

    class BuddiesAdapter(private val viewModel: BuddiesViewModel) : RecyclerView.Adapter<BuddiesAdapter.BuddyViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var buddies: List<UserEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
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
            val binding = RowBuddyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return BuddyViewHolder(binding)
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
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                buddies.isEmpty() -> "-"
                else -> viewModel.getSectionHeader(buddies.getOrNull(position))
            }
        }

        inner class BuddyViewHolder(private val binding: RowBuddyBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(buddy: UserEntity?) {
                buddy?.let { b ->
                    binding.avatarView.loadThumbnailInList(b.avatarUrl, R.drawable.person_image_empty)
                    if (b.fullName.isBlank()) {
                        binding.fullNameView.text = b.userName
                        binding.usernameView.visibility = View.GONE
                    } else {
                        binding.fullNameView.text = b.fullName
                        binding.usernameView.setTextOrHide(b.userName)
                    }
                    binding.root.setOnClickListener {
                        BuddyActivity.start(binding.root.context, b.userName, b.fullName)
                    }
                }
            }
        }
    }
}
