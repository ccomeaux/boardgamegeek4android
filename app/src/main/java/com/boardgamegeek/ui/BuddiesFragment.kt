package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.BuddiesViewModel
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.fragment_buddies.*
import kotlinx.android.synthetic.main.row_buddy.view.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.support.v4.ctx
import kotlin.properties.Delegates

class BuddiesFragment : Fragment() {
    private val viewModel: BuddiesViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(BuddiesViewModel::class.java)
    }

    private val adapter: BuddiesAdapter by lazy {
        BuddiesAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_buddies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh.setOnRefreshListener { triggerRefresh() }
        swipeRefresh.setBggColors()

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter)
        recyclerView.addItemDecoration(sectionItemDecoration)

        viewModel.buddies.observe(this, Observer {
            swipeRefresh?.post { swipeRefresh?.isRefreshing = it?.status == Status.REFRESHING }

            when {
                it.status == Status.ERROR -> showError(it.message)
                else -> showData(it?.data ?: emptyList())
            }
            progressBar.hide()
        })
    }

    private fun showError(message: String? = null) {
        // TODO default error message
        if (message != null && !message.isNullOrBlank()) {
            coordinatorLayout.indefiniteSnackbar(message)
        }
    }

    private fun showData(buddies: List<UserEntity>) {
        adapter.buddies = buddies
        if (adapter.itemCount == 0) {
            recyclerView.fadeOut()
            showEmpty()
        } else {
            recyclerView.fadeIn()
            emptyContainer.fadeOut()
        }
    }

    private fun showEmpty() {
        if (PreferencesUtils.getSyncBuddies(ctx)) {
            emptyTextView.setText(R.string.empty_buddies)
            emptyButton.isGone = true
        } else {
            emptyTextView.setText(R.string.empty_buddies_sync_off)
            emptyButton.setOnClickListener {
                PreferencesUtils.setSyncBuddies(ctx)
                triggerRefresh()
                showEmpty()
            }
            emptyButton.isVisible = true
        }
        emptyContainer.fadeIn()
    }

    private fun triggerRefresh() {
        swipeRefresh.isRefreshing = viewModel.refresh()
    }

    class BuddiesAdapter : RecyclerView.Adapter<BuddiesAdapter.BuddyViewHolder>(), AutoUpdatableAdapter, SectionCallback {
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
            return BuddyViewHolder(parent.inflate(R.layout.row_buddy))
        }

        override fun onBindViewHolder(holder: BuddyViewHolder, position: Int) {
            holder.bind(buddies.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (buddies.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = buddies.getOrNull(position)?.lastName.firstChar()
            val lastLetter = buddies.getOrNull(position - 1)?.lastName.firstChar()
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                buddies.isEmpty() -> "-"
                else -> buddies.getOrNull(position)?.lastName.firstChar()
            }
        }

        inner class BuddyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(buddy: UserEntity?) {
                buddy?.let { b ->
                    itemView.avatarView.loadThumbnail(buddy.avatarUrl, R.drawable.person_image_empty)
                    if (b.fullName.isBlank()) {
                        itemView.fullNameView.text = b.userName
                        itemView.usernameView.visibility = View.GONE
                    } else {
                        itemView.fullNameView.text = b.fullName
                        itemView.usernameView.setTextOrHide(b.userName)
                    }
                    itemView.setOnClickListener {
                        BuddyActivity.start(itemView.context, b.userName, b.fullName)
                    }
                }
            }
        }
    }
}
