package com.boardgamegeek.ui.adapter

import android.content.ContentResolver
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.PresentationUtils
import java.util.*

class PlayerNameAdapter(context: Context) : ArrayAdapter<PlayerNameAdapter.Result>(context, R.layout.autocomplete_player, emptyList<Result>()), Filterable {
    private val resolver: ContentResolver = context.contentResolver
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val resultList = ArrayList<Result>()

    class Result(val title: String,
                 val subtitle: String,
                 val username: String,
                 val avatarUrl: String = "") {
        override fun toString() = title
    }

    override fun getCount() = resultList.size

    override fun getItem(index: Int) = resultList.getOrNull(index)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.autocomplete_player, parent, false)
        val result = getItem(position) ?: return view

        val titleView = view.findViewById<TextView>(R.id.player_title)
        val subtitleView = view.findViewById<TextView>(R.id.player_subtitle)
        val avatarView = view.findViewById<ImageView>(R.id.player_avatar)

        PresentationUtils.setTextOrHide(titleView, result.title)
        PresentationUtils.setTextOrHide(subtitleView, result.subtitle)
        view.tag = result.username
        avatarView?.loadThumbnail(result.avatarUrl, R.drawable.person_image_empty)
        return view
    }

    override fun getFilter() = PlayerFilter()

    inner class PlayerFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults? {
            val filter = constraint?.toString() ?: ""
            if (filter.isBlank()) return null

            val (buddies, buddyUserNames) = queryBuddies(resolver, filter)
            val players = queryPlayerHistory(resolver, filter)

            val resultList = ArrayList<Result>()
            resultList.addAll(buddies)
            players.filterTo(resultList) { player ->
                player.username.isBlank() || buddyUserNames.asSequence().any { it.equals(player.username, true) }
            }

            val filterResults = Filter.FilterResults()
            filterResults.values = resultList
            filterResults.count = resultList.size
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) {
            resultList.clear()
            if (results != null && results.count > 0) {
                @Suppress("UNCHECKED_CAST")
                resultList.addAll(results.values as ArrayList<Result>)
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }

    companion object {

        private fun queryPlayerHistory(resolver: ContentResolver, input: String): List<Result> {
            val results = ArrayList<Result>()
            val cursor = resolver.query(Plays.buildPlayersByUniquePlayerUri(),
                    arrayOf(PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME),
                    if (input.isBlank()) null else "${PlayPlayers.NAME} LIKE ?",
                    if (input.isBlank()) null else arrayOf("$input%"),
                    PlayPlayers.NAME)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    do {
                        val name = c.getString(2) ?: ""
                        val username = c.getString(1) ?: ""
                        results.add(Result(name, username, username))
                    } while (c.moveToNext())
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
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    do {
                        val userName = c.getString(1) ?: ""
                        val firstName = c.getString(2) ?: ""
                        val lastName = c.getString(3) ?: ""
                        val nickname = c.getString(4) ?: ""
                        val avatarUrl = c.getString(5) ?: ""
                        val fullName = PresentationUtils.buildFullName(firstName, lastName)

                        results.add(Result(
                                if (nickname.isBlank()) fullName else nickname,
                                if (nickname.isBlank()) userName else "$fullName ($userName)",
                                userName,
                                avatarUrl))
                        userNames.add(userName)
                    } while (cursor.moveToNext())
                }
            }
            return results to userNames
        }
    }
}
