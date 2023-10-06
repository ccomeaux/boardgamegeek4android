package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentMechanicsBinding
import com.boardgamegeek.databinding.RowMechanicBinding
import com.boardgamegeek.entities.Mechanic
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

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
            adapter.mechanics = it
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

    class MechanicsAdapter : RecyclerView.Adapter<MechanicsAdapter.MechanicViewHolder>(), AutoUpdatableAdapter {
        var mechanics: List<Mechanic> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
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

        inner class MechanicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = RowMechanicBinding.bind(itemView)

            fun bind(mechanic: Mechanic?) {
                mechanic?.let { m ->
                    binding.nameView.text = m.name
                    binding.countView.text = itemView.context.resources.getQuantityString(R.plurals.games_suffix, m.itemCount, m.itemCount)
                    itemView.setOnClickListener {
                        MechanicActivity.start(itemView.context, m.id, m.name)
                    }
                }
            }
        }
    }
}
