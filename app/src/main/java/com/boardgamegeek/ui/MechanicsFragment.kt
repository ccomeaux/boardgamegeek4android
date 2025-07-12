package com.boardgamegeek.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.databinding.FragmentMechanicsBinding
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.ui.compose.ListItemPrimaryText
import com.boardgamegeek.ui.compose.ListItemSecondaryText
import com.boardgamegeek.ui.compose.ListItemTokens
import com.boardgamegeek.ui.theme.BggAppTheme
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

        binding.swipeRefresh.setOnRefreshListener { viewModel.reload() }

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
        binding.recyclerView.adapter = null
        _binding = null
    }

    class MechanicsAdapter : ListAdapter<Mechanic, MechanicsAdapter.MechanicViewHolder>(ItemCallback()) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int) = getItem(position).id.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MechanicViewHolder {
            return MechanicViewHolder(ComposeView(parent.context))
        }

        override fun onBindViewHolder(holder: MechanicViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ItemCallback : DiffUtil.ItemCallback<Mechanic>() {
            override fun areItemsTheSame(oldItem: Mechanic, newItem: Mechanic) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Mechanic, newItem: Mechanic) = oldItem == newItem
        }

        inner class MechanicViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
            fun bind(mechanic: Mechanic) {
                composeView.setContent {
                    MechanicListItem(
                        mechanic,
                        onClick = {
                            MechanicActivity.start(itemView.context, mechanic.id, mechanic.name)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MechanicListItem(
    mechanic: Mechanic,
    modifier: Modifier = Modifier,
    onClick: (Mechanic) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = { onClick(mechanic) })
            .padding(ListItemTokens.paddingValues)
            .then(modifier)
    ) {
        ListItemPrimaryText(mechanic.name)
        ListItemSecondaryText(pluralStringResource(R.plurals.games_suffix, mechanic.itemCount, mechanic.itemCount))
    }
}

@Preview
@Composable
private fun MechanicListItemPreview(
    @PreviewParameter(MechanicPreviewParameterProvider::class) mechanic: Mechanic
) {
    BggAppTheme {
        MechanicListItem(mechanic)
    }
}

private class MechanicPreviewParameterProvider : PreviewParameterProvider<Mechanic> {
    override val values = sequenceOf(
        Mechanic(
            id = 99,
            name = "Deck Building",
            itemCount = 30,
        ),
        Mechanic(
            id = 99,
            name = "Auction",
            itemCount = 0,
        ),
        Mechanic(
            id = 99,
            name = "Dice Rolling",
            itemCount = 1,
        )
    )
}
