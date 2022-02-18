package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
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
import com.boardgamegeek.entities.NewPlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.fragment_new_play_player_sort.*
import kotlinx.android.synthetic.main.row_new_play_player_sort.view.*
import kotlin.properties.Delegates

class NewPlayPlayerSortFragment : Fragment(R.layout.fragment_new_play_player_sort) {
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

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setSubtitle(R.string.title_sort)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        viewModel.addedPlayers.observe(viewLifecycleOwner) { entity ->
            if (entity.all { it.seat != null }) {
                adapter.players = entity.sortedBy { it.seat }
            } else {
                adapter.players = entity
            }
        }

        randomizeAllButton.setOnClickListener {
            viewModel.randomizePlayers()
        }
        randomizeStartButton.setOnClickListener {
            viewModel.randomizeStartPlayer()
        }
        clearButton.setOnClickListener {
            viewModel.clearSortOrder()
        }

        nextButton.setOnClickListener {
            viewModel.finishPlayerSort()
        }

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private class Diff(private val oldList: List<NewPlayPlayerEntity>, private val newList: List<NewPlayPlayerEntity>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val o = oldList[oldItemPosition]
            val n = newList[newItemPosition]
            return o.id == n.id && o.sortOrder == n.sortOrder
        }
    }

    private class DraggingDiff(private val oldList: List<NewPlayPlayerEntity>, private val newList: List<NewPlayPlayerEntity>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val o = oldList[oldItemPosition]
            val n = newList[newItemPosition]
            return o.id == n.id
        }
    }

    private class PlayersAdapter(private val viewModel: NewPlayViewModel, private val itemTouchHelper: ItemTouchHelper) :
        RecyclerView.Adapter<PlayersAdapter.PlayersViewHolder>() {

        var isDraggable = false
        var isDragging = false

        var players: List<NewPlayPlayerEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            determineDraggability(newValue)
            val diffCallback = if (isDragging) DraggingDiff(oldValue, newValue) else Diff(oldValue, newValue)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(this)
        }

        private fun determineDraggability(newValue: List<NewPlayPlayerEntity>) {
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

        inner class PlayersViewHolder(view: View, private val itemTouchHelper: ItemTouchHelper) : RecyclerView.ViewHolder(view) {
            @SuppressLint("ClickableViewAccessibility")
            fun bind(position: Int) {
                val entity = players.getOrNull(position)
                entity?.let { player ->
                    itemView.nameView.text = player.name
                    itemView.usernameView.setTextOrHide(player.username)

                    if (player.color.isBlank()) {
                        itemView.colorView.isInvisible = true
                        itemView.teamView.isVisible = false
                        itemView.seatView.setTextColor(Color.TRANSPARENT.getTextColor())
                    } else {
                        val color = player.color.asColorRgb()
                        if (color == Color.TRANSPARENT) {
                            itemView.colorView.isInvisible = true
                            itemView.teamView.setTextOrHide(player.color)
                        } else {
                            itemView.colorView.setColorViewValue(color)
                            itemView.colorView.isVisible = true
                            itemView.teamView.isVisible = false
                        }
                        itemView.seatView.setTextColor(color.getTextColor())
                    }

                    if (player.seat == null) {
                        itemView.sortView.setTextOrHide(player.sortOrder)
                        itemView.seatView.isInvisible = true
                    } else {
                        itemView.sortView.isVisible = false
                        itemView.seatView.text = player.seat.toString()
                        itemView.seatView.isVisible = true
                    }

                    itemView.dragHandle.isVisible = isDraggable
                    if (isDraggable) {
                        itemView.dragHandle.setOnTouchListener { v, event ->
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
