package com.boardgamegeek.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.User
import com.boardgamegeek.extensions.inflate
import com.boardgamegeek.extensions.loadThumbnail
import com.boardgamegeek.extensions.setTextOrHide

class BuddyNameAdapter(context: Context) : ArrayAdapter<BuddyNameAdapter.Result>(context, R.layout.autocomplete_player), Filterable {
    private var playerList = listOf<Player>()
    private var userList = listOf<User>()
    private var resultList = listOf<Result>()

    /**
     * Represents an adapter item result containing player information.
     *
     * @property title The primary display text.
     * @property subtitle The secondary display text.
     * @property avatarUrl The URL of the user's avatar image, or null if no avatar is available.
     * @property username The unique username of the user.
     * @property nickname The user's name to use for the player name.
     */
    class Result(
        val title: String,
        val subtitle: String,
        val username: String,
        val avatarUrl: String?,
        val nickname: String,
    ) {
        override fun toString() = username
    }

    override fun getCount() = resultList.size

    override fun getItem(index: Int) = resultList.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: parent.inflate(R.layout.autocomplete_player)
        val result = getItem(position) ?: return view

        view.findViewById<TextView>(R.id.player_title)?.setTextOrHide(result.title)
        view.findViewById<TextView>(R.id.player_subtitle)?.setTextOrHide(result.subtitle)
        view.findViewById<ImageView>(R.id.player_avatar)?.loadThumbnail(result.avatarUrl, R.drawable.person_image_empty)
        view.tag = result.nickname
        return view
    }

    fun addPlayers(list: List<Player>) {
        playerList = list
            .filter { it.username.isNotBlank() }
            .sortedByDescending { it.playCount }
            .distinctBy { it.username }
        notifyDataSetChanged()
    }

    fun addUsers(list: List<User>) {
        userList = list.sortedBy { it.username }
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filter = constraint?.toString().orEmpty()

            val playerListFiltered = if (filter.isEmpty()) playerList else {
                playerList.filter { it.username.startsWith(filter, ignoreCase = true) }
            }
            val playerResults = playerListFiltered.map { player ->
                player.mapToResult()
            }

            val userListFiltered = if (filter.isEmpty()) userList else {
                userList.filter { it.username.startsWith(filter, ignoreCase = true) }
            }

            val usernames = playerResults.map { it.username }
            val userResults = userListFiltered
                .asSequence()
                .filterNot { usernames.contains(it.username) }
                .map { it.mapToResult() }

            val combinedResults = playerResults + userResults

            return FilterResults().apply {
                values = combinedResults
                count = combinedResults.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            resultList = results?.values as? List<Result> ?: emptyList()
            notifyDataSetChanged()
        }
    }

    private fun User.mapToResult() = Result(
        title = playNickname.ifBlank { fullName },
        subtitle = if (playNickname.isBlank()) username else "$fullName ($username)",
        avatarUrl = avatarUrl,
        username = username,
        nickname = playNickname.ifBlank { fullName },
    )

    private fun Player.mapToResult() = Result(
        title = userFullName?.ifBlank { name } ?: name,
        subtitle = username,
        avatarUrl = userAvatarUrl,
        username = username,
        nickname = name,
    )
}
