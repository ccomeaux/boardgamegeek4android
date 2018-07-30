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
import com.boardgamegeek.events.PlayerSelectedEvent
import com.boardgamegeek.events.PlayersCountChangedEvent
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.sorter.PlayersSorter
import com.boardgamegeek.sorter.PlayersSorterFactory
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
import java.util.*

class PlayersFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private var sorter: PlayersSorter? = null

    private val adapter: PlayersAdapter by lazy {
        PlayersAdapter(ctx)
    }

    var sort: Int
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
        recyclerView.layoutManager = LinearLayoutManager(ctx)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        val sectionItemDecoration = RecyclerSectionItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                true,
                adapter)
        recyclerView.addItemDecoration(sectionItemDecoration)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        var sortType = PlayersSorterFactory.TYPE_DEFAULT
        if (savedInstanceState != null) {
            sortType = savedInstanceState.getInt(STATE_SORT_TYPE)
        }
        sort = sortType
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

        val players = ArrayList<Player>()
        if (cursor.moveToFirst()) {
            do {
                players.add(Player.fromCursor(cursor))
            } while (cursor.moveToNext())
        }

        adapter.changeData(players, sorter)

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

    class PlayersAdapter(context: Context) : RecyclerView.Adapter<PlayersAdapter.ViewHolder>(), SectionCallback {
        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private val players = arrayListOf<Player>()
        private var sorter: PlayersSorter = PlayersSorterFactory.create(context, PlayersSorterFactory.TYPE_DEFAULT)

        fun clear() {
            this.players.clear()
            notifyDataSetChanged()
        }

        fun changeData(players: List<Player>, sorter: PlayersSorter?) {
            this.players.clear()
            this.players.addAll(players)
            this.sorter = sorter ?: PlayersSorterFactory.create(inflater.context, PlayersSorterFactory.TYPE_DEFAULT)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return players.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_players_player, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(players.getOrNull(position))
        }

        override fun isSection(position: Int): Boolean {
            if (players.size == 0) return false
            if (position == 0) return true
            val thisLetter = sorter.getSectionText(players.getOrNull(position))
            val lastLetter = sorter.getSectionText(players.getOrNull(position - 1))
            return thisLetter != lastLetter
        }

        override fun getSectionHeader(position: Int): CharSequence {
            return if (players.size == 0) "-" else sorter.getSectionText(players[position])
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(player: Player?) {
                player?.let { p ->
                    itemView.nameView.text = p.name
                    itemView.usernameView.setTextOrHide(player.username)
                    itemView.quantityView.setTextOrHide(sorter.getDisplayText(p))
                    itemView.setOnClickListener { EventBus.getDefault().post(PlayerSelectedEvent(p.name, p.username)) }
                }
            }
        }
    }

    companion object {
        private const val STATE_SORT_TYPE = "sortType"
    }
}
