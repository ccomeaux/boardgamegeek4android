package com.boardgamegeek.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Pair
import android.util.SparseBooleanArray
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.HotnessViewModel
import com.boardgamegeek.util.ActivityUtils
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import kotlinx.android.synthetic.main.fragment_hotness.*
import kotlinx.android.synthetic.main.row_hotness.view.*
import org.jetbrains.anko.design.snackbar
import kotlin.properties.Delegates

class HotnessFragment : Fragment(R.layout.fragment_hotness), ActionMode.Callback {
    private val prefs: SharedPreferences by lazy { requireActivity().preferences() }
    private val viewModel by activityViewModels<HotnessViewModel>()
    private val adapter: HotGamesAdapter by lazy {
        createAdapter()
    }
    private var actionMode: ActionMode? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        viewModel.hotness.observe(viewLifecycleOwner, Observer { (status, data, message) ->
            when (status) {
                Status.REFRESHING -> progressView.show()
                Status.ERROR -> {
                    emptyView.text = message
                    recyclerView.fadeOut()
                    emptyView.fadeIn()
                    progressView.hide()
                }
                Status.SUCCESS -> {
                    val games = data.orEmpty()
                    adapter.games = games
                    if (games.isEmpty()) {
                        emptyView.setText(R.string.empty_hotness)
                        recyclerView.fadeOut()
                        emptyView.fadeIn()
                    } else {
                        emptyView.fadeOut()
                        recyclerView.fadeIn(isResumed)
                    }
                    progressView.hide()
                }
            }
        })
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

    interface Callback {
        fun onItemClick(position: Int): Boolean
        fun onItemLongClick(position: Int): Boolean
    }

    inner class HotGamesAdapter(private val callback: Callback?) : RecyclerView.Adapter<HotGamesAdapter.ViewHolder>(), AutoUpdatableAdapter {
        private val selectedItems = SparseBooleanArray()

        init {
            setHasStableIds(true)
        }

        var games: List<HotGameEntity> by Delegates.observable(emptyList()) { _, old, new ->
            autoNotify(old, new) { o, n -> o == n }
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

        fun getSelectedGames(): List<HotGameEntity> {
            val selectedGames = mutableListOf<HotGameEntity>()
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
        menu.findItem(R.id.menu_log_play).isVisible = Authenticator.isSignedIn(context) && count == 1 && prefs.showLogPlay()
        menu.findItem(R.id.menu_log_play_quick).isVisible = Authenticator.isSignedIn(context) && prefs.showQuickLogPlay()
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
