package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentNewPlayPlayerSortBinding
import com.boardgamegeek.databinding.RowNewPlayPlayerSortBinding
import com.boardgamegeek.entities.NewPlayPlayer
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class NewPlayPlayerSortFragment : Fragment() {
    private var _binding: FragmentNewPlayPlayerSortBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<NewPlayViewModel>()
    private val adapter: PlayersAdapter by lazy { PlayersAdapter(viewModel, itemTouchHelper) }
    private val itemTouchHelper by lazy {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // swiping not supported
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return viewModel.movePlayer(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    (viewHolder as? PlayersAdapter.PlayersViewHolder)?.onItemDragging()
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                (viewHolder as? PlayersAdapter.PlayersViewHolder)?.onItemClear()
                super.clearView(recyclerView, viewHolder)
            }
        })
    }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNewPlayPlayerSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(viewLifecycleOwner) { players ->
            if (players.all { it.seat != null }) {
                adapter.players = players.sortedBy { it.seat }
            } else {
                adapter.players = players
            }
        }

        binding.randomizeAllButton.setOnClickListener {
            viewModel.randomizePlayers()
        }
        binding.randomizeStartButton.setOnClickListener {
            viewModel.randomizeStartPlayer()
        }
        binding.clearButton.setOnClickListener {
            viewModel.clearSortOrder()
        }

        binding.nextButton.setOnClickListener {
            viewModel.finishPlayerSort()
        }

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_sort)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class Diff(private val oldList: List<NewPlayPlayer>, private val newList: List<NewPlayPlayer>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    private class DraggingDiff(private val oldList: List<NewPlayPlayer>, private val newList: List<NewPlayPlayer>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
    }

    private class PlayersAdapter(private val viewModel: NewPlayViewModel, private val itemTouchHelper: ItemTouchHelper) :
        RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>() {

        var isDraggable = false
        var isDragging = false

        var players: List<NewPlayPlayer> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            determineDraggability(newValue)
            val diffCallback = if (isDragging) DraggingDiff(oldValue, newValue) else Diff(oldValue, newValue)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(this)
        }

        private fun determineDraggability(newValue: List<NewPlayPlayer>) {
            if (newValue.all { it.seat != null }) {
                for (seat in 1..newValue.size + 1) {
                    if (newValue.find { it.seat == seat } == null) {
                        isDraggable = false
                        break
                    }
                }
                isDraggable = true
            } else isDraggable = false
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = players.size

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
            return PlayersViewHolder(parent.inflate(R.layout.row_new_play_player_sort), itemTouchHelper)
        }

        override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
            holder.bind(position)
        }

        inner class PlayersViewHolder(itemView: View, private val itemTouchHelper: ItemTouchHelper) : RecyclerView.ViewHolder(itemView) {
            val binding = RowNewPlayPlayerSortBinding.bind(itemView)

            @SuppressLint("ClickableViewAccessibility")
            fun bind(position: Int) {
                players.getOrNull(position)?.let { player ->
                    binding.nameView.text = player.name
                    binding.usernameView.setTextOrHide(player.username)

                    if (player.color.isBlank()) {
                        binding.colorView.isInvisible = true
                        binding.teamView.isVisible = false
                        binding.seatView.setTextColor(Color.TRANSPARENT.getTextColor())
                    } else {
                        val color = player.color.asColorRgb()
                        if (color == Color.TRANSPARENT) {
                            binding.colorView.isInvisible = true
                            binding.teamView.setTextOrHide(player.color)
                        } else {
                            binding.colorView.setColorViewValue(color)
                            binding.colorView.isVisible = true
                            binding.teamView.isVisible = false
                        }
                        binding.seatView.setTextColor(color.getTextColor())
                    }

                    if (player.seat == null) {
                        binding.sortView.setTextOrHide(player.sortOrder)
                        binding.seatView.isInvisible = true
                    } else {
                        binding.sortView.isVisible = false
                        binding.seatView.text = player.seat.toString()
                        binding.seatView.isVisible = true
                    }

                    binding.dragHandle.isVisible = isDraggable
                    if (isDraggable) {
                        binding.dragHandle.setOnTouchListener { v, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                itemTouchHelper.startDrag(this@PlayersViewHolder)
                            } else if (event.action == MotionEvent.ACTION_UP) {
                                v.performClick()
                            }
                            false
                        }
                    }
                    itemView.setOnClickListener { viewModel.selectStartPlayer(position) }
                }
            }

            fun onItemDragging() {
                isDragging = true
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.light_blue_transparent))
            }

            @SuppressLint("NotifyDataSetChanged")
            fun onItemClear() {
                isDragging = false
                itemView.setBackgroundColor(Color.TRANSPARENT)
                notifyDataSetChanged() // force UI to update the seat numbers
            }
        }
    }
}
