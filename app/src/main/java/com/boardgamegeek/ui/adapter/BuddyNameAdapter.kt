package com.boardgamegeek.ui.adapter

import android.content.ContentResolver
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.loadThumbnail
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.extensions.use
import com.boardgamegeek.provider.BggContract.*
import java.util.*

class BuddyNameAdapter(context: Context) : ArrayAdapter<BuddyNameAdapter.Result>(context, R.layout.autocomplete_player, ArrayList<Result>()), Filterable {
    private val resolver = context.contentResolver
    private val inflater = LayoutInflater.from(context)
    private val resultList = ArrayList<Result>()

    class Result(val title: String,
                 val subtitle: String,
                 val username: String,
                 avatarUrl: String = "") {
        val avatarUrl: String = avatarUrl
            get() = if (field == "N/A") "" else field

        override fun toString() = username
    }

    override fun getCount() = resultList.size

    override fun getItem(index: Int) = resultList.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.autocomplete_player, parent, false) as View
        val result = getItem(position) ?: return view

        view.findViewById<TextView>(R.id.player_title)?.setTextOrHide(result.title)
        view.findViewById<TextView>(R.id.player_subtitle)?.setTextOrHide(result.subtitle)
        view.findViewById<ImageView>(R.id.player_avatar)?.loadThumbnail(result.avatarUrl, R.drawable.person_image_empty)
        view.tag = result.title
        return view
    }

    override fun getFilter() = PlayerFilter()

    inner class PlayerFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults? {
            val filter = constraint?.toString() ?: ""
            if (filter.isBlank()) return null

            // list all buddies + players with a username that aren't buddies, sorted by username
            val resultList = arrayListOf<Result>()
            val players = queryPlayerHistory(resolver, filter)
            val (buddies, buddyUserNames) = queryBuddies(resolver, filter)
            resultList.addAll(buddies)
            players.filterTo(resultList) { player ->
                buddyUserNames.asSequence().none { it.equals(player.username, true) }
            }
            resultList.sortBy { it.username }

            return FilterResults().apply {
                values = resultList
                count = resultList.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
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
            val cursor = resolver.query(Plays.buildPlayersByUniqueUserUri(),
                    arrayOf(PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME),
                    if (input.isBlank()) null else "${PlayPlayers.USER_NAME} LIKE ?",
                    if (input.isBlank()) null else arrayOf("$input%"),
                    PlayPlayers.NAME)
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val username = it.getString(1) ?: ""
                        val nickname = it.getString(2) ?: ""
                        results.add(Result(nickname, username, username))
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
                    if (input.isBlank()) null else "${Buddies.BUDDY_NAME} LIKE ?",
                    if (input.isBlank()) null else arrayOf("$input%"),
                    Buddies.NAME_SORT)
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val userName = it.getString(1) ?: ""
                        val firstName = it.getString(2) ?: ""
                        val lastName = it.getString(3) ?: ""
                        val nickname = it.getString(4) ?: ""
                        val avatarUrl = it.getString(5) ?: ""
                        val fullName = "${firstName.trim()} ${lastName.trim()}".trim()

                        results.add(Result(
                                if (nickname.isBlank()) fullName else nickname,
                                userName,
                                userName,
                                avatarUrl))
                        userNames.add(userName)
                    } while (it.moveToNext())
                }
            }
            return results to userNames
        }
    }
}
