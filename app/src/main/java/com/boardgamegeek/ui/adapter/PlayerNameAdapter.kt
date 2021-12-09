package com.boardgamegeek.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnailInList
import com.boardgamegeek.extensions.setTextOrHide

class PlayerNameAdapter(context: Context) : ArrayAdapter<PlayerNameAdapter.Result>(context, R.layout.autocomplete_player), Filterable {
    private var playerList = listOf<PlayerEntity>()
    private var userList = listOf<UserEntity>()
    private var resultsFiltered = listOf<Result>()

    class Result(
        val title: String,
        val subtitle: String,
        val username: String,
        val avatarUrl: String = "",
    ) {
        override fun toString() = title
    }

    override fun getCount() = resultsFiltered.size

    override fun getItem(index: Int) = resultsFiltered.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: parent.inflate(R.layout.autocomplete_player)
        val result = getItem(position) ?: return view

        view.findViewById<TextView>(R.id.player_title)?.setTextOrHide(result.title)
        view.findViewById<TextView>(R.id.player_subtitle)?.setTextOrHide(result.subtitle)
        view.findViewById<ImageView>(R.id.player_avatar)?.loadThumbnailInList(result.avatarUrl, R.drawable.person_image_empty)
        view.tag = result.username
        return view
    }

    fun addPlayers(list: List<PlayerEntity>) {
        playerList = list.sortedByDescending { it.playCount }
        notifyDataSetChanged()
    }

    fun addUsers(list: List<UserEntity>) {
        userList = list.sortedBy { it.userName }
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        @Suppress("RedundantNullableReturnType")
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString().orEmpty()

            val playerListFiltered = if (filter.isEmpty()) playerList else {
                playerList.filter { it.name.contains(filter, ignoreCase = true) }
            }

            val userListFiltered = if (filter.isEmpty()) userList else {
                userList.filter {
                    it.userName.contains(filter, ignoreCase = true) ||
                            it.firstName.contains(filter, ignoreCase = true) ||
                            it.lastName.contains(filter, ignoreCase = true) ||
                            it.playNickname.contains(filter, ignoreCase = true)
                }
            }

            val playerResults = playerListFiltered.map { player ->
                Result(
                    player.name,
                    player.username,
                    player.username,
                    player.avatarUrl,
                )
            }
            val usernames = playerResults.map { it.username }
            val userResults = userListFiltered.filterNot {
                usernames.contains(it.userName)
            }.map {
                Result(
                    if (it.playNickname.isBlank()) it.fullName else it.playNickname,
                    if (it.playNickname.isBlank()) it.userName else "${it.fullName} (${it.userName})",
                    it.userName,
                    it.avatarUrl,
                )
            }

            return FilterResults().apply {
                values = playerResults + userResults
                count = (playerResults + userResults).size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            resultsFiltered = results?.values as? List<Result> ?: emptyList()
            notifyDataSetChanged()
        }
    }
}
