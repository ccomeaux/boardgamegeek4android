package com.boardgamegeek.ui

import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.databinding.FragmentHotnessBinding
import com.boardgamegeek.databinding.RowHotnessBinding
import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.entities.PlayUploadResult
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.HotnessViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class HotnessFragment : Fragment(), ActionMode.Callback {
    private var _binding: FragmentHotnessBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<HotnessViewModel>()
    private val adapter: HotGamesAdapter by lazy { createAdapter() }
    private var actionMode: ActionMode? = null

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHotnessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                binding.coordinatorLayout.snackbar(it)
            }
        }

        viewModel.loggedPlayResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                val message = when {
                    it.status == PlayUploadResult.Status.UPDATE -> getString(R.string.msg_play_updated)
                    it.play.quantity > 0 -> requireContext().getText(
                        R.string.msg_play_added_quantity,
                        it.numberOfPlays.asRangeDescription(it.play.quantity),
                    )
                    else -> getString(R.string.msg_play_added)
                }
                requireContext().notifyLoggedPlay(it.play.gameName, message, it.play)
            }
        }

        viewModel.hotness.observe(viewLifecycleOwner) {
            it?.let { (status, data, message) ->
                when (status) {
                    Status.REFRESHING -> binding.progressView.show()
                    Status.ERROR -> {
                        binding.emptyView.text = message
                        binding.emptyView.isVisible = true
                        binding.recyclerView.isVisible = false
                        binding.progressView.hide()
                    }
                    Status.SUCCESS -> {
                        val games = data.orEmpty()
                        adapter.games = games
                        if (games.isEmpty()) {
                            binding.emptyView.setText(R.string.empty_hotness)
                            binding.emptyView.isVisible = true
                            binding.recyclerView.isVisible = false
                        } else {
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
                actionMode?.let {
                    if (adapter.selectedItemCount == 0) {
                        it.finish()
                    } else {
                        it.invalidate()
                    }
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.inflate(R.layout.row_hotness))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = games.size

        override fun getItemId(position: Int) = games.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowHotnessBinding.bind(itemView)

            fun bind(position: Int) {
                games.getOrNull(position)?.let { game ->
                    binding.nameView.text = game.name
                    binding.yearView.text = game.yearPublished.asYear(itemView.context)
                    binding.rankView.text = game.rank.toString()
                    binding.thumbnailView.loadThumbnail(game.thumbnailUrl)
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
            selectedItems.toggle(position)
            notifyItemChanged(position)
        }

        fun clearSelections() {
            val oldSelectedItems = selectedItems.clone()
            selectedItems.clear()
            oldSelectedItems.filterTrue().forEach { notifyItemChanged(it) }
        }

        val selectedItemCount: Int
            get() = selectedItems.filterTrue().size

        fun getSelectedGames() = selectedItems.filterTrue().mapNotNull { games.getOrNull(it) }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        adapter.clearSelections()
        mode.menuInflater.inflate(R.menu.game_context, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter.selectedItemCount
        mode.title = resources.getQuantityString(R.plurals.msg_games_selected, count, count)
        if (Authenticator.isSignedIn(context)) {
            menu.findItem(R.id.menu_log_play_form).isVisible = count == 1
            menu.findItem(R.id.menu_log_play_wizard).isVisible = count == 1
            menu.findItem(R.id.menu_log_play).isVisible = true
        } else {
            menu.findItem(R.id.menu_log_play).isVisible = false
        }
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
            R.id.menu_log_play_form -> {
                selectedGames.firstOrNull()?.let { game ->
                    LogPlayActivity.logPlay(requireContext(), game.id, game.name, game.thumbnailUrl)
                }
            }
            R.id.menu_log_play_quick -> {
                binding.coordinatorLayout.snackbar(resources.getQuantityString(R.plurals.msg_logging_plays, adapter.selectedItemCount))
                for (game in selectedGames) {
                    viewModel.logQuickPlay(game.id, game.name)
                }
            }
            R.id.menu_log_play_wizard -> {
                selectedGames.firstOrNull()?.let { game ->
                    NewPlayActivity.start(requireContext(), game.id, game.name)
                }
            }
            R.id.menu_share -> {
                val shareMethod = "Hotness"
                if (selectedGames.size == 1) {
                    selectedGames.firstOrNull()?.let { game ->
                        requireActivity().shareGame(game.id, game.name, shareMethod)
                    }
                } else {
                    requireActivity().shareGames(selectedGames.map { it.id to it.name }, shareMethod)
                }
            }
            R.id.menu_link -> {
                selectedGames.firstOrNull()?.let { game ->
                    context.linkBgg(game.id)
                }
            }
            else -> return false
        }
        mode.finish()
        return true
    }
}
