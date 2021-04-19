package com.boardgamegeek.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel
import kotlinx.android.synthetic.main.fragment_mechanics.*
import kotlinx.android.synthetic.main.row_mechanic.view.*
import kotlin.properties.Delegates

class MechanicsFragment : Fragment(R.layout.fragment_mechanics) {
    private val viewModel by activityViewModels<MechanicsViewModel>()
    private val adapter: MechanicsAdapter by lazy { MechanicsAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.mechanics.observe(viewLifecycleOwner, {
            showData(it)
            progressBar.hide()
            swipeRefresh.isRefreshing = false
        })
    }

    private fun showData(mechanics: List<MechanicEntity>) {
        adapter.mechanics = mechanics
        if (adapter.itemCount == 0) {
            recyclerView.fadeOut()
            emptyTextView.fadeIn()
        } else {
            recyclerView.fadeIn()
            emptyTextView.fadeOut()
        }
    }

    class MechanicsAdapter : RecyclerView.Adapter<MechanicsAdapter.MechanicViewHolder>(), AutoUpdatableAdapter {
        var mechanics: List<MechanicEntity> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
            autoNotify(oldValue, newValue) { old, new ->
                old.id == new.id
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = mechanics.size

        override fun getItemId(position: Int) = mechanics.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MechanicViewHolder {
            return MechanicViewHolder(parent.inflate(R.layout.row_mechanic))
        }

        override fun onBindViewHolder(holder: MechanicViewHolder, position: Int) {
            holder.bind(mechanics.getOrNull(position))
        }

        inner class MechanicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(mechanic: MechanicEntity?) {
                mechanic?.let { m ->
                    itemView.nameView.text = m.name
                    itemView.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, m.itemCount, m.itemCount)
                    itemView.setOnClickListener {
                        MechanicActivity.start(itemView.context, m.id, m.name)
                    }
                }
            }
        }
    }
}
