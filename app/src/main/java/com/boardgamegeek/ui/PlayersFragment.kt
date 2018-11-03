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
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.events.PlayersCountChangedEvent
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.sorter.PlayersSorter
import com.boardgamegeek.sorter.PlayersSorterFactory
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.model.Player
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback
import com.boardgamegeek.util.AnimationUtils
import com.boardgamegeek.util.StringUtils
import com.boardgamegeek.util.fabric.SortEvent
import kotlinx.android.synthetic.main.fragment_players.*
import kotlinx.android.synthetic.main.row_players_player.view.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.support.v4.ctx
import kotlin.properties.Delegates

class PlayersFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private var sorter: PlayersSorter? = null

    private val adapter: PlayersAdapter by lazy {
        PlayersAdapter(ctx)
    }

    var sortType: Int
        get() = sorter?.type ?: PlayersSorterFactory.TYPE_DEFAULT
        set(sortType) {
            if ((sorter?.type ?: PlayersSorterFactory.TYPE_UNKNOWN) != sortType) {
                SortEvent.log("Players", sortType.toString())
                sorter = PlayersSorterFactory.create(ctx, sortType)
                LoaderManager.getInstance(this).restartLoader(0, arguments, this)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                adapter)
        recyclerView.addItemDecoration(sectionItemDecoration)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sortType = savedInstanceState?.getInt(STATE_SORT_TYPE) ?: PlayersSorterFactory.TYPE_DEFAULT
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SORT_TYPE, sorter?.type ?: PlayersSorterFactory.TYPE_DEFAULT)
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<Cursor> {
        return CursorLoader(ctx,
                Plays.buildPlayersByUniquePlayerUri(),
                StringUtils.unionArrays(Player.PROJECTION, sorter?.columns),
                null,
                null,
                sorter?.orderByClause)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        if (!isAdded) return

        val players = arrayListOf<Player>()
        if (cursor.moveToFirst()) {
            do {
                players.add(Player.fromCursor(cursor))
            } while (cursor.moveToNext())
        }

        adapter.players = players
        adapter.sorter = sorter ?: PlayersSorterFactory.create(ctx, PlayersSorterFactory.TYPE_DEFAULT)

        EventBus.getDefault().postSticky(PlayersCountChangedEvent(cursor.count))

        progressBar.hide()
        setListShown(recyclerView.windowToken != null)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.clear()
    }

    private fun setListShown(animate: Boolean) {
        if (adapter.itemCount == 0) {
            AnimationUtils.fadeOut(recyclerView)
            AnimationUtils.fadeIn(emptyContainer)
        } else {
            AnimationUtils.fadeOut(emptyContainer)
            AnimationUtils.fadeIn(recyclerView, animate)
        }
    }

    class PlayersAdapter(context: Context) : RecyclerView.Adapter<PlayersAdapter.ViewHolder>(), AutoUpdatableAdapter, SectionCallback {
        var players: List<Player> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.name == new.name
            }
        }

        var sorter: PlayersSorter by Delegates.observable(PlayersSorterFactory.create(context, PlayersSorterFactory.TYPE_DEFAULT)) { _, oldValue, newValue ->
            if (oldValue.type != newValue.type) notifyDataSetChanged()
        }

        fun clear() {
            players = emptyList()
        }

        override fun getItemCount() = players.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent.inflate(R.layout.row_players_player))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(players.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (position == RecyclerView.NO_POSITION) return false
            if (players.isEmpty()) return false
            if (position == 0) return true
            val thisLetter = sorter.getSectionText(players.getOrNull(position))
            val lastLetter = sorter.getSectionText(players.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return when {
                position == RecyclerView.NO_POSITION -> "-"
                players.isEmpty() -> "-"
                else -> sorter.getSectionText(players.getOrNull(position))
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(player: Player?) {
                player?.let { p ->
                    itemView.nameView.text = p.name
                    itemView.usernameView.setTextOrHide(player.username)
                    itemView.quantityView.setTextOrHide(sorter.getDisplayText(p))
                    itemView.setOnClickListener {
                        BuddyActivity.start(itemView.context, p.username, p.name)
                    }
                }
            }
        }
    }

    companion object {
        private const val STATE_SORT_TYPE = "sortType"
    }
}
