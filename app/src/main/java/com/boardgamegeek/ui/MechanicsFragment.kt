package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentMechanicsBinding
import com.boardgamegeek.databinding.RowMechanicBinding
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.viewmodel.MechanicsViewModel
import kotlin.properties.Delegates

class MechanicsFragment : Fragment() {
    private var _binding: FragmentMechanicsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MechanicsViewModel by lazy {
        ViewModelProvider(this).get(MechanicsViewModel::class.java)
    }

    private val adapter: MechanicsAdapter by lazy {
        MechanicsAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMechanicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.mechanics.observe(this, Observer {
            showData(it)
            binding.progressBar.hide()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showData(mechanics: List<MechanicEntity>) {
        adapter.mechanics = mechanics
        if (adapter.itemCount == 0) {
            binding.recyclerView.fadeOut()
            binding.emptyTextView.fadeIn()
        } else {
            binding.recyclerView.fadeIn()
            binding.emptyTextView.fadeOut()
        }
    }

    companion object {
        fun newInstance(): MechanicsFragment {
            return MechanicsFragment()
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
            val binding = RowMechanicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MechanicViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MechanicViewHolder, position: Int) {
            holder.bind(mechanics.getOrNull(position))
        }

        inner class MechanicViewHolder(private val binding: RowMechanicBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(mechanic: MechanicEntity?) {
                mechanic?.let {m ->
                    binding.nameView.text = m.name
                    binding.countView.text = binding.root.context.resources.getQuantityString(R.plurals.games_suffix, m.itemCount, m.itemCount)
                    binding.root.setOnClickListener {
                        MechanicActivity.start(binding.root.context, m.id, m.name)
                    }
                }
            }
        }
    }
}