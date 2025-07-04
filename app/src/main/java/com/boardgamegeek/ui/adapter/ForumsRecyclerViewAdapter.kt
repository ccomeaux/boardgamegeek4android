package com.boardgamegeek.ui.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.model.Forum
import com.boardgamegeek.ui.ForumActivity
import com.boardgamegeek.ui.compose.ForumListItem
import com.boardgamegeek.ui.theme.BggAppTheme

class ForumsRecyclerViewAdapter(
    private val objectId: Int,
    private val objectName: String,
    private val objectType: Forum.Type,
) : RecyclerView.Adapter<ForumsRecyclerViewAdapter.ForumItemViewHolder>() {
    init {
        setHasStableIds(true)
    }

    var forums: List<Forum> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumItemViewHolder {
        return ForumItemViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ForumItemViewHolder, position: Int) {
        return holder.bind(forums.getOrNull(position), objectId, objectName, objectType)
    }

    override fun getItemCount() = forums.size

    override fun getItemId(position: Int) = position.toLong()

    class ForumItemViewHolder(val view: ComposeView) : RecyclerView.ViewHolder(view) {
        fun bind(forum: Forum?, objectId: Int, objectName: String, objectType: Forum.Type) {
            if (forum == null) return
            view.setContent {
                BggAppTheme {
                    ForumListItem(forum, { ForumActivity.start(itemView.context, forum.id, forum.title, objectId, objectName, objectType) })
                }
            }
        }
    }
}
