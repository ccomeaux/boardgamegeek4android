package com.boardgamegeek.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.databinding.RowGameDetailBinding
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.PersonActivity
import com.boardgamegeek.ui.viewmodel.GameViewModel
import kotlin.properties.Delegates

class GameDetailAdapter : RecyclerView.Adapter<GameDetailAdapter.DetailViewHolder>(), AutoUpdatableAdapter {
    init {
        setHasStableIds(true)
    }

    var type: GameViewModel.ProducerType by Delegates.observable(GameViewModel.ProducerType.UNKNOWN) { _, old, new ->
        if (old != new) notifyDataSetChanged()
    }

    var items: List<GameDetailEntity> by Delegates.observable(emptyList()) { _, old, new ->
        autoNotify(old, new) { o, n ->
            o.id == n.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = RowGameDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id?.toLong() ?: RecyclerView.NO_ID

    inner class DetailViewHolder(private val binding: RowGameDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gameDetail: GameDetailEntity?) {
            gameDetail?.let { entity ->
                binding.nameView.text = entity.name
                binding.descriptionView.setTextOrHide(entity.description)
                when (type) {
                    GameViewModel.ProducerType.EXPANSIONS,
                    GameViewModel.ProducerType.BASE_GAMES -> binding.root.setOnClickListener { GameActivity.start(binding.root.context, entity.id, entity.name) }
                    GameViewModel.ProducerType.PUBLISHER -> binding.root.setOnClickListener { PersonActivity.startForPublisher(binding.root.context, entity.id, entity.name) }
                    GameViewModel.ProducerType.ARTIST -> binding.root.setOnClickListener { PersonActivity.startForArtist(binding.root.context, entity.id, entity.name) }
                    GameViewModel.ProducerType.DESIGNER -> binding.root.setOnClickListener { PersonActivity.startForDesigner(binding.root.context, entity.id, entity.name) }
                    else -> {
                        binding.root.setOnClickListener { }
                        binding.root.isClickable = false
                    }
                }
            }
        }
    }
}
