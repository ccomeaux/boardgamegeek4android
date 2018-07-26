package com.boardgamegeek.ui.adapter

import android.content.ContentResolver
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.extensions.use
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.PresentationUtils
import java.util.*

class PlayerNameAdapter(context: Context) : ArrayAdapter<PlayerNameAdapter.Result>(context, R.layout.autocomplete_player, emptyList<Result>()), Filterable {
    private val resolver = context.contentResolver
    private val inflater = LayoutInflater.from(context)
    private val resultList = ArrayList<Result>()

    class Result(val title: String,
                 val subtitle: String,
                 val username: String,
                 var playCount: Int = 0,
                 avatarUrl: String = "") {
        val avatarUrl: String = avatarUrl
            get() = if (field == "N/A") "" else field

        override fun toString() = title
    }

    override fun getCount() = resultList.size

    override fun getItem(index: Int) = resultList.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.autocomplete_player, parent, false) as View
        val result = getItem(position) ?: return view

        view.findViewById<TextView>(R.id.player_title)?.setTextOrHide(result.title)
        view.findViewById<TextView>(R.id.player_subtitle)?.setTextOrHide(result.subtitle)
        view.findViewById<ImageView>(R.id.player_avatar)?.loadThumbnail(result.avatarUrl, R.drawable.person_image_empty)
        view.tag = result.username
        return view
    }

    override fun getFilter() = PlayerFilter()

    inner class PlayerFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults? {
            val filter = constraint?.toString() ?: ""
            if (filter.isBlank()) return null

            // list all buddies + all players that aren't buddies, sorted by play count
            val resultList = arrayListOf<Result>()
            val players = queryPlayerHistory(resolver, filter)
            val (buddies, buddyUserNames) = queryBuddies(resolver, filter)

            buddies.forEach { buddy ->
                buddy.playCount = (players.find { buddy.username.equals(it.username, true) }?.playCount ?: 0)
            }
            resultList.addAll(buddies)
            players.filterTo(resultList) { player ->
                player.username.isBlank() || buddyUserNames.asSequence().none { it.equals(player.username, true) }
            }
            resultList.sortBy { -it.playCount }

            val filterResults = Filter.FilterResults()
            filterResults.values = resultList
            filterResults.count = resultList.size
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) {
            resultList.clear()
            var values: ArrayList<Result>? = null
            if (results != null && results.count > 0) {
                @Suppress("UNCHECKED_CAST")
                values = results.values as? ArrayList<Result>
            }
            if (values != null && values.size > 0) {
                resultList.addAll(values)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }

    companion object {
        private fun queryPlayerHistory(resolver: ContentResolver, input: String): List<Result> {
            val results = arrayListOf<Result>()
            val cursor = resolver.query(Plays.buildPlayersByUniquePlayerUri(),
                    arrayOf(PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.COUNT),
                    if (input.isBlank()) null else "${PlayPlayers.NAME} LIKE ?",
                    if (input.isBlank()) null else arrayOf("$input%"),
                    "${PlayPlayers.COUNT} DESC, ${PlayPlayers.NAME}")
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val username = it.getString(1) ?: ""
                        val name = it.getString(2) ?: ""
                        val playCount = it.getInt(3)
                        results.add(Result(name, username, username, playCount))
                    } while (it.moveToNext())
                }
            }
            return results
        }

        private fun queryBuddies(resolver: ContentResolver, input: String): Pair<List<Result>, Set<String>> {
            val results = arrayListOf<Result>()
            val userNames = hashSetOf<String>()
            val cursor = resolver.query(Buddies.CONTENT_URI,
                    arrayOf(Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME, Buddies.PLAY_NICKNAME, Buddies.AVATAR_URL),
                    if (input.isBlank()) null else "${Buddies.BUDDY_NAME} LIKE ? OR ${Buddies.BUDDY_FIRSTNAME} LIKE ? OR ${Buddies.BUDDY_LASTNAME} LIKE ? OR ${Buddies.PLAY_NICKNAME} LIKE ?",
                    if (input.isBlank()) null else arrayOf("$input%", "$input%", "$input%", "$input%"),
                    Buddies.NAME_SORT)
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val userName = it.getString(1) ?: ""
                        val firstName = it.getString(2) ?: ""
                        val lastName = it.getString(3) ?: ""
                        val nickname = it.getString(4) ?: ""
                        val avatarUrl = it.getString(5) ?: ""
                        val fullName = PresentationUtils.buildFullName(firstName, lastName)

                        results.add(Result(
                                if (nickname.isBlank()) fullName else nickname,
                                if (nickname.isBlank()) userName else "$fullName ($userName)",
                                userName,
                                avatarUrl = avatarUrl))
                        userNames.add(userName)
                    } while (it.moveToNext())
                }
            }
            return results to userNames
        }
    }
}
