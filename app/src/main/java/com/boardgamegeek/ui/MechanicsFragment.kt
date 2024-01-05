package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentMechanicsBinding
import com.boardgamegeek.databinding.RowMechanicBinding
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MechanicsFragment : Fragment() {
    private var _binding: FragmentMechanicsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<MechanicsViewModel>()
    private val adapter: MechanicsAdapter by lazy { MechanicsAdapter() }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMechanicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.mechanics.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.recyclerView.isVisible = adapter.itemCount > 0
            binding.emptyTextView.isVisible = adapter.itemCount == 0
            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class MechanicsAdapter : ListAdapter<Mechanic, MechanicsAdapter.MechanicViewHolder>(ItemCallback()) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int) = getItem(position).id.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MechanicViewHolder {
            return MechanicViewHolder(parent.inflate(R.layout.row_mechanic))
        }

        override fun onBindViewHolder(holder: MechanicViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ItemCallback : DiffUtil.ItemCallback<Mechanic>() {
            override fun areItemsTheSame(oldItem: Mechanic, newItem: Mechanic) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Mechanic, newItem: Mechanic) = oldItem == newItem
        }

        inner class MechanicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowMechanicBinding.bind(itemView)

            fun bind(mechanic: Mechanic) {
                binding.nameView.text = mechanic.name
                binding.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, mechanic.itemCount, mechanic.itemCount)
                itemView.setOnClickListener {
                    MechanicActivity.start(itemView.context, mechanic.id, mechanic.name)
                }
            }
        }
    }
}
