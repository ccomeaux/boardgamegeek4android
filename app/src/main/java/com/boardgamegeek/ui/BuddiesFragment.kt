package com.boardgamegeek.ui

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.events.BuddiesCountChangedEvent
import com.boardgamegeek.events.BuddySelectedEvent
import com.boardgamegeek.events.SyncCompleteEvent
import com.boardgamegeek.events.SyncEvent
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.ui.model.Buddy
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import com.boardgamegeek.util.AnimationUtils
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.PreferencesUtils
import com.boardgamegeek.util.PresentationUtils
import kotlinx.android.synthetic.main.fragment_buddies.*
import kotlinx.android.synthetic.main.row_buddy.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.support.v4.ctx
import java.util.*

class BuddiesFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private val adapter: BuddiesAdapter by lazy {
        BuddiesAdapter(ctx)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        LoaderManager.getInstance(this).restartLoader(0, arguments, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_buddies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEmptyText()

        swipeRefresh.setOnRefreshListener { triggerRefresh() }
        swipeRefresh.setColorSchemeResources(*PresentationUtils.getColorSchemeResources())

        recyclerView.layoutManager = LinearLayoutManager(ctx)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                true,
                adapter)
        recyclerView.addItemDecoration(sectionItemDecoration)
    }

    private fun triggerRefresh() {
        SyncService.sync(ctx, SyncService.FLAG_SYNC_BUDDIES)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncEvent) {
        if (event.type and SyncService.FLAG_SYNC_BUDDIES == SyncService.FLAG_SYNC_BUDDIES) {
            isSyncing(true)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncCompleteEvent) {
        isSyncing(false)
    }

    private fun isSyncing(value: Boolean) {
        swipeRefresh?.post { swipeRefresh?.isRefreshing = value }
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<Cursor> {
        val loader = CursorLoader(ctx,
                Buddies.CONTENT_URI,
                Buddy.projection,
                String.format("%s!=? AND %s=1", Buddies.BUDDY_ID, Buddies.BUDDY_FLAG),
                arrayOf(Authenticator.getUserId(ctx)), null)
        loader.setUpdateThrottle(2000)
        return loader
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        if (!isAdded) return

        val buddies = ArrayList<Buddy>()
        if (cursor.moveToFirst()) {
            do {
                buddies.add(Buddy.fromCursor(cursor))
            } while (cursor.moveToNext())
        }

        adapter.changeData(buddies)

        EventBus.getDefault().postSticky(BuddiesCountChangedEvent(cursor.count))

        progressBar.hide()
        setListShown(recyclerView.windowToken != null)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.clear()
    }

    private fun setListShown(animate: Boolean) {
        if (adapter.itemCount == 0) {
            AnimationUtils.fadeOut(listContainer)
            AnimationUtils.fadeIn(emptyContainer)
        } else {
            AnimationUtils.fadeOut(emptyContainer)
            AnimationUtils.fadeIn(listContainer, animate)
        }
    }

    private fun setEmptyText() {
        if (PreferencesUtils.getSyncBuddies(activity)) {
            emptyTextView.setText(R.string.empty_buddies)
            emptyButton.visibility = View.GONE
        } else {
            emptyTextView.setText(R.string.empty_buddies_sync_off)
            emptyButton.setOnClickListener {
                PreferencesUtils.setSyncBuddies(ctx)
                setEmptyText()
                triggerRefresh()
            }
            emptyButton.visibility = View.VISIBLE
        }
    }

    class BuddiesAdapter(context: Context) : RecyclerView.Adapter<BuddiesAdapter.BuddyViewHolder>(), SectionCallback {
        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private val buddies = arrayListOf<Buddy>()

        fun clear() {
            this.buddies.clear()
            notifyDataSetChanged()
        }

        fun changeData(buddies: List<Buddy>) {
            this.buddies.clear()
            this.buddies.addAll(buddies)
            notifyDataSetChanged()
        }

        override fun getItemCount() = buddies.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuddyViewHolder {
            return BuddyViewHolder(inflater.inflate(R.layout.row_buddy, parent, false))
        }

        override fun onBindViewHolder(holder: BuddyViewHolder, position: Int) {
            holder.bind(buddies.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (buddies.size == 0) return false
            if (position == 0) return true
            val thisLetter = buddies.getOrNull(position)?.lastName.firstChar()
            val lastLetter = buddies.getOrNull(position - 1)?.lastName.firstChar()
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return buddies.getOrNull(position)?.lastName.firstChar()
        }

        inner class BuddyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(buddy: Buddy?) {
                buddy?.let { b ->
                    itemView.avatarView.loadThumbnail(buddy.avatarUrl, R.drawable.person_image_empty)
                    if (b.fullName.isBlank()) {
                        itemView.fullNameView.text = b.userName
                        itemView.usernameView.visibility = View.GONE
                    } else {
                        itemView.fullNameView.text = b.fullName
                        itemView.usernameView.setTextOrHide(b.userName)
                    }
                    itemView.setOnClickListener { EventBus.getDefault().postSticky(BuddySelectedEvent(buddy.id, b.userName, b.fullName)) }
                }
            }
        }
    }
}
