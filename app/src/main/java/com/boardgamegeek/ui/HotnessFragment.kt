package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import android.util.Pair
import android.util.SparseBooleanArray
import android.view.*
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.HotGame
import com.boardgamegeek.model.HotnessResponse
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.loader.BggLoader
import com.boardgamegeek.ui.loader.SafeResponse
import com.boardgamegeek.util.ActivityUtils
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.fragment_hotness.*
import kotlinx.android.synthetic.main.row_hotness.view.*
import org.jetbrains.anko.design.snackbar
import kotlin.properties.Delegates

class HotnessFragment : Fragment(R.layout.fragment_hotness), LoaderManager.LoaderCallbacks<SafeResponse<HotnessResponse>>, ActionMode.Callback {
    private val adapter: HotGamesAdapter by lazy {
        createAdapter()
    }
    private var actionMode: ActionMode? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        LoaderManager.getInstance(this).initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<SafeResponse<HotnessResponse>> {
        return HotnessLoader(activity)
    }

    override fun onLoadFinished(loader: Loader<SafeResponse<HotnessResponse>?>, data: SafeResponse<HotnessResponse>?) {
        if (activity == null) return
        recyclerView.adapter = adapter
        adapter.games = data?.body?.games.orEmpty()
        if (data == null) {
            recyclerView.fadeOut()
            emptyView.fadeIn()
        } else if (data.hasError()) {
            emptyView.text = getString(R.string.empty_http_error, data.errorMessage)
            recyclerView.fadeOut()
            emptyView.fadeIn()
        } else {
            emptyView.fadeOut()
            recyclerView.fadeIn(isResumed)
        }
        progressView.hide()
    }

    private fun createAdapter(): HotGamesAdapter {
        return HotGamesAdapter(object : Callback {
            override fun onItemClick(position: Int): Boolean {
                if (actionMode == null) return false
                toggleSelection(position)
                return true
            }

            override fun onItemLongClick(position: Int): Boolean {
                if (actionMode != null) return false
                actionMode = requireActivity().startActionMode(this@HotnessFragment)
                if (actionMode == null) return false
                toggleSelection(position)
                return true
            }

            private fun toggleSelection(position: Int) {
                adapter.toggleSelection(position)
                val count = adapter.selectedItemCount
                if (count == 0) {
                    actionMode?.finish()
                } else {
                    actionMode?.title = resources.getQuantityString(R.plurals.msg_games_selected, count, count)
                    actionMode?.invalidate()
                }
            }
        })
    }

    override fun onLoaderReset(loader: Loader<SafeResponse<HotnessResponse>>) {}

    private class HotnessLoader(context: Context?) : BggLoader<SafeResponse<HotnessResponse>>(context) {
        private val bggService: BggService = Adapter.createForXml()
        override fun loadInBackground(): SafeResponse<HotnessResponse> {
            val call = bggService.getHotness(BggService.HOTNESS_TYPE_BOARDGAME)
            return SafeResponse(call)
        }
    }

    interface Callback {
        fun onItemClick(position: Int): Boolean
        fun onItemLongClick(position: Int): Boolean
    }

    inner class HotGamesAdapter(private val callback: Callback?) : RecyclerView.Adapter<HotGamesAdapter.ViewHolder>(), AutoUpdatableAdapter {
        private val selectedItems = SparseBooleanArray()

        init {
            setHasStableIds(true)
        }

        var games: List<HotGame> by Delegates.observable(emptyList()) { _, old, new ->
            // TODO
            autoNotify(old, new) { o, n -> o.id == n.id }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent.inflate(R.layout.row_hotness))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount(): Int {
            return games.size
        }

        override fun getItemId(position: Int): Long {
            return games.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID
        }

        inner class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
            fun bind(position: Int) {
                games.getOrNull(position)?.let { game ->
                    itemView.nameView.text = game.name
                    itemView.yearView.text = game.yearPublished.asYear(itemView.context)
                    itemView.rankView.text = game.rank.toString()
                    itemView.thumbnailView.loadThumbnail(game.thumbnailUrl)
                    itemView.isActivated = selectedItems[position, false]
                    itemView.setOnClickListener {
                        if (callback?.onItemClick(position) != true) {
                            GameActivity.start(requireContext(), game.id, game.name, game.thumbnailUrl)
                        }
                    }
                    itemView.setOnLongClickListener { callback?.onItemLongClick(position) ?: false }

                }
            }
        }

        fun toggleSelection(position: Int) {
            if (selectedItems[position, false]) {
                selectedItems.delete(position)
            } else {
                selectedItems.put(position, true)
            }
            notifyItemChanged(position)
        }

        fun clearSelections() {
            selectedItems.clear()
            notifyDataSetChanged()
        }

        val selectedItemCount: Int
            get() = selectedItems.size()

        fun getSelectedGames(): List<HotGame> {
            val selectedGames = mutableListOf<HotGame>()
            for (i in 0 until selectedItems.size()) {
                games.getOrNull(selectedItems.keyAt(i))?.let { selectedGames.add(it) }
            }
            return selectedGames
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        adapter.clearSelections()
        mode.menuInflater.inflate(R.menu.game_context, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter.selectedItemCount
        menu.findItem(R.id.menu_log_play).isVisible = Authenticator.isSignedIn(context) && count == 1 && PreferencesUtils.showLogPlay(activity)
        menu.findItem(R.id.menu_log_play_quick).isVisible = Authenticator.isSignedIn(context) && PreferencesUtils.showQuickLogPlay(activity)
        menu.findItem(R.id.menu_link).isVisible = count == 1
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        adapter.clearSelections()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selectedGames = adapter.getSelectedGames()
        if (selectedGames.isEmpty()) return false
        when (item.itemId) {
            R.id.menu_log_play -> {
                mode.finish()
                selectedGames.firstOrNull()?.let { game ->
                    LogPlayActivity.logPlay(context, game.id, game.name, game.thumbnailUrl, game.thumbnailUrl, "", false)
                }
                return true
            }
            R.id.menu_log_play_quick -> {
                mode.finish()
                val text = resources.getQuantityString(R.plurals.msg_logging_plays, adapter.selectedItemCount)
                containerView.snackbar(text).show()
                for (game in selectedGames) {
                    requireContext().logQuickPlay(game.id, game.name)
                }
                return true
            }
            R.id.menu_share -> {
                mode.finish()
                val shareMethod = "Hotness"
                if (selectedGames.size == 1) {
                    selectedGames.firstOrNull()?.let { game ->
                        ActivityUtils.shareGame(activity, game.id, game.name, shareMethod)
                    }
                } else {
                    val games = mutableListOf<Pair<Int, String>>()
                    for (game in selectedGames) {
                        games.add(Pair.create(game.id, game.name))
                    }
                    ActivityUtils.shareGames(activity, games, shareMethod)
                }
                return true
            }
            R.id.menu_link -> {
                mode.finish()
                selectedGames.firstOrNull()?.let { game ->
                    context.linkBgg(game.id)
                }
                return true
            }
        }
        return false
    }
}
