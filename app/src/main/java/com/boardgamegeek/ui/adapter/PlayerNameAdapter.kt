package com.boardgamegeek.ui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import java.util.*

class PlayerNameAdapter(context: Context) : ArrayAdapter<PlayerNameAdapter.Result>(context, R.layout.autocomplete_player), Filterable {
    private val resultList = ArrayList<Result>()

    class Result(val title: String,
                 val subtitle: String,
                 val username: String,
                 var playCount: Int = 0,
                 avatarUrl: String = "") {
        val avatarUrl: String = avatarUrl
            get() = if (field == INVALID_URL) "" else field

        override fun toString() = title
    }

    override fun getCount() = resultList.size

    override fun getItem(index: Int) = resultList.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: parent.inflate(R.layout.autocomplete_player)
        val result = getItem(position) ?: return view

        view.findViewById<TextView>(R.id.player_title)?.setTextOrHide(result.title)
        view.findViewById<TextView>(R.id.player_subtitle)?.setTextOrHide(result.subtitle)
        view.findViewById<ImageView>(R.id.player_avatar)?.loadThumbnailInList(result.avatarUrl, R.drawable.person_image_empty)
        view.tag = result.username
        return view
    }

    override fun getFilter() = PlayerFilter()

    inner class PlayerFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString() ?: ""

            // list all buddies + all players that aren't buddies, sorted by play count
            val resultList = arrayListOf<Result>()
            val players = queryPlayerHistory(filter)
            val (buddies, buddyUserNames) = queryBuddies(filter)

            buddies.forEach { buddy ->
                buddy.playCount = (players.find { buddy.username.equals(it.username, true) }?.playCount ?: 0)
            }
            resultList.addAll(buddies)
            players.filterTo(resultList) { player ->
                player.username.isBlank() || buddyUserNames.asSequence().none { it.equals(player.username, true) }
            }
            resultList.sortByDescending { it.playCount }

            return FilterResults().apply {
                values = resultList
                count = resultList.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            resultList.clear()
            val values = if (results != null && results.count > 0) {
                @Suppress("UNCHECKED_CAST")
                results.values as? ArrayList<Result>
            } else null
            if (values != null && values.size > 0) {
                resultList.addAll(values)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        private fun queryPlayerHistory(input: String): List<Result> {
            val results = arrayListOf<Result>()
            val cursor = context.contentResolver.query(Plays.buildPlayersByUniquePlayerUri(),
                    arrayOf(PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.COUNT),
                    if (input.isBlank()) null else "${PlayPlayers.NAME} LIKE ?",
                    if (input.isBlank()) null else arrayOf("$input%"),
                    "${PlayPlayers.COUNT} DESC, ${PlayPlayers.NAME}")
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val username = it.getStringOrNull(1) ?: ""
                        val name = it.getStringOrNull(2) ?: ""
                        val playCount = it.getIntOrNull(3) ?: 0
                        results += Result(name, username, username, playCount)
                    } while (it.moveToNext())
                }
            }
            return results
        }

        private fun queryBuddies(input: String): Pair<List<Result>, Set<String>> {
            val results = arrayListOf<Result>()
            val userNames = hashSetOf<String>()
            val cursor = context.contentResolver.query(Buddies.CONTENT_URI,
                    arrayOf(Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME, Buddies.PLAY_NICKNAME, Buddies.AVATAR_URL),
                    if (input.isBlank()) null else "${Buddies.BUDDY_NAME} LIKE ? OR ${Buddies.BUDDY_FIRSTNAME} LIKE ? OR ${Buddies.BUDDY_LASTNAME} LIKE ? OR ${Buddies.PLAY_NICKNAME} LIKE ?",
                    if (input.isBlank()) null else arrayOf("$input%", "$input%", "$input%", "$input%"),
                    Buddies.NAME_SORT)
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val userName = it.getStringOrNull(1) ?: ""
                        val firstName = it.getStringOrNull(2) ?: ""
                        val lastName = it.getStringOrNull(3) ?: ""
                        val nickname = it.getStringOrNull(4) ?: ""
                        val avatarUrl = it.getStringOrNull(5) ?: ""
                        val fullName = "${firstName.trim()} ${lastName.trim()}".trim()

                        results += Result(
                                if (nickname.isBlank()) fullName else nickname,
                                if (nickname.isBlank()) userName else "$fullName ($userName)",
                                userName,
                                avatarUrl = avatarUrl)
                        userNames.add(userName)
                    } while (it.moveToNext())
                }
            }
            return results to userNames
        }
    }
}
