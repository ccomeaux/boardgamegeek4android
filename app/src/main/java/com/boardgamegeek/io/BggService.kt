@file:Suppress("SpellCheckingInspection", "unused")

package com.boardgamegeek.io

import com.boardgamegeek.io.model.*
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface BggService {
    @GET("/xmlapi2/collection")
    suspend fun collection(@Query("username") username: String?, @QueryMap options: Map<String, String>): CollectionResponse

    @GET("/xmlapi2/thing")
    suspend fun thing(@Query("id") gameId: Int, @Query("stats") stats: Int): ThingResponse

    @GET("/xmlapi2/thing")
    suspend fun things(@Query("id") gameIds: String?, @Query("stats") stats: Int): ThingResponse

    @GET("/xmlapi2/thing?comments=1")
    suspend fun thingWithComments(@Query("id") gameId: Int, @Query("page") page: Int): ThingResponse

    @GET("/xmlapi2/thing?ratingcomments=1")
    suspend fun thingWithRatings(@Query("id") gameId: Int, @Query("page") page: Int): ThingResponse

    @GET("/xmlapi2/plays")
    suspend fun playsByGame(@Query("username") username: String?, @Query("id") gameId: Int, @Query("page") page: Int): PlaysResponse

    @GET("/xmlapi2/plays")
    suspend fun plays(
        @Query("username") username: String?,
        @Query("mindate") minDate: String?,
        @Query("maxdate") maxDate: String?,
        @Query("page") page: Int
    ): PlaysResponse

    @GET("/xmlapi2/user")
    suspend fun user(@Query("name") name: String?): UserRemote

    @GET("/xmlapi2/user")
    suspend fun user(@Query("name") name: String?, @Query("buddies") buddies: Int, @Query("page") page: Int): UserRemote

    @GET("/xmlapi/{type}/{id}")
    suspend fun person(@Path("type") type: PersonType?, @Path("id") id: Int): PersonResponseV1

    @GET("/xmlapi2/person")
    suspend fun person(@Query("id") id: Int): PersonResponse

    @GET("/xmlapi2/company")
    suspend fun company(@Query("id") id: Int): CompanyResponse

    @GET("/xmlapi2/search")
    suspend fun search(@Query("query") query: String?, @Query("type") type: SearchType?, @Query("exact") exact: Int): SearchResponse

    @GET("/xmlapi2/hot")
    suspend fun getHotness(@Query("type") type: HotnessType?): HotnessResponse

    @GET("/xmlapi2/forumlist")
    suspend fun forumList(@Query("type") type: ForumType?, @Query("id") id: Int): ForumListResponse

    @GET("/xmlapi2/forum")
    suspend fun forum(@Query("id") id: Int, @Query("page") page: Int): ForumResponse

    @GET("/xmlapi2/thread")
    suspend fun thread(@Query("id") id: Int): ThreadResponse

    @GET("/xmlapi/geeklist/{id}")
    suspend fun geekList(@Path("id") id: Int, @Query("comments") comments: Int): GeekListResponse

    companion object {
        const val RANK_TYPE_SUBTYPE = "subtype"
        const val RANK_TYPE_FAMILY = "family"

        const val RANK_FAMILY_NAME_ABSTRACT_GAMES = "abstracts"
        const val RANK_FAMILY_NAME_CUSTOMIZABLE_GAMES = "cgs"
        const val RANK_FAMILY_NAME_CHILDRENS_GAMES = "childrensgames"
        const val RANK_FAMILY_NAME_FAMILY_GAMES = "familygames"
        const val RANK_FAMILY_NAME_PARTY_GAMES = "partygames"
        const val RANK_FAMILY_NAME_STRATEGY_GAMES = "strategygames"
        const val RANK_FAMILY_NAME_THEMATIC_GAMES = "thematic"
        const val RANK_FAMILY_NAME_WAR_GAMES = "wargames"

        private const val COLLECTION_QUERY_KEY_ID = "id"
        private const val COLLECTION_QUERY_KEY_SHOW_PRIVATE = "showprivate"
        private const val COLLECTION_QUERY_KEY_STATS = "stats"
        private const val COLLECTION_QUERY_KEY_MODIFIED_SINCE = "modifiedsince"
        private const val COLLECTION_QUERY_KEY_BRIEF = "brief"
        private const val COLLECTION_QUERY_KEY_SUBTYPE = "subtype"
        private const val COLLECTION_QUERY_STATUS_PLAYED = "played"
        private const val COLLECTION_QUERY_KEY_COLLECTION_ID = "collid"
        private val COLLECTION_QUERY_DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        // val COLLECTION_QUERY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        const val TYPE_BOARD_GAME = "boardgame"
        const val TYPE_BOARD_GAME_EXPANSION = "boardgameexpansion"
        const val TYPE_BOARD_GAME_ACCESSORY = "boardgameaccessory"

        fun createCollectionOptionsMap(
            gameId: Int = INVALID_ID,
            gameIds: List<Int>? = null,
            collectionId: Int = INVALID_ID,
            subtype: Game.Subtype? = null,
            includePrivateInfo: Boolean = false,
            includeStats: Boolean = false,
            brief: Boolean = false,
            status: CollectionStatus? = null,
            excludedStatuses: List<CollectionStatus> = emptyList(),
            sinceTimestamp: Long? = null,
        ): Map<String, String> {
            val options = mutableMapOf<String, String>()

            if (collectionId != INVALID_ID)
                options[COLLECTION_QUERY_KEY_COLLECTION_ID] = collectionId.toString()
            else if (gameId != INVALID_ID)
                options[COLLECTION_QUERY_KEY_ID] = gameId.toString()
            else if (gameIds?.isNotEmpty() == true)
                options[COLLECTION_QUERY_KEY_ID] = gameIds.joinToString(",")

            sinceTimestamp?.let {
                options[COLLECTION_QUERY_KEY_MODIFIED_SINCE] = COLLECTION_QUERY_DATE_TIME_FORMAT.format(Date(it))
            }

            if (includePrivateInfo) options += COLLECTION_QUERY_KEY_SHOW_PRIVATE to "1"
            if (includeStats) options += COLLECTION_QUERY_KEY_STATS to "1"
            if (brief) options += COLLECTION_QUERY_KEY_BRIEF to "1"

            status?.let { options += it.toQueryKey() to "1" }
            excludedStatuses.forEach { options[it.toQueryKey()] = "0" }

            subtype?.let {
                options += COLLECTION_QUERY_KEY_SUBTYPE to when (it) {
                    Game.Subtype.BoardGame -> TYPE_BOARD_GAME
                    Game.Subtype.BoardGameExpansion -> TYPE_BOARD_GAME_EXPANSION
                    Game.Subtype.BoardGameAccessory -> TYPE_BOARD_GAME_ACCESSORY
                    Game.Subtype.Unknown -> TYPE_BOARD_GAME
                }
            }
            return options
        }

        private fun CollectionStatus.toQueryKey() = when (this) {
            CollectionStatus.Own -> "own"
            CollectionStatus.PreviouslyOwned -> "prevowned"
            CollectionStatus.Preordered -> "preordered"
            CollectionStatus.ForTrade -> "fortrade"
            CollectionStatus.WantInTrade -> "want"
            CollectionStatus.WantToBuy -> "wanttobuy"
            CollectionStatus.WantToPlay -> "wantoplay"
            CollectionStatus.Wishlist -> "wishlist"
            CollectionStatus.Played -> "played"
            CollectionStatus.Rated -> "rated"
            CollectionStatus.Commented -> "commented"
            CollectionStatus.HasParts -> "hasparts"
            CollectionStatus.WantParts -> "wantparts"
            CollectionStatus.Unknown -> ""
        }
    }

    enum class PersonType {
        @SerializedName("boardgameartist")
        ARTIST,

        @SerializedName("boardgamedesigner")
        DESIGNER,
    }

    enum class SearchType {
        @SerializedName("boardgame")
        BOARDGAME,
    }

    enum class HotnessType {
        @SerializedName("boardgame")
        BOARDGAME,

        @SerializedName("boardgameexpansion")
        BOARD_GAME_EXPANSION,

        @SerializedName("rpg")
        RPG,

        @SerializedName("rpgitem")
        RPG_ITEM,

        @SerializedName("videogame")
        VIDEO_GAME,

        @SerializedName("boardgameartist")
        BOARDGAME_ARTIST,

        @SerializedName("boardgamedesigner")
        BOARDGAME_DESIGNER,

        @SerializedName("boardgamepublisher")
        BOARDGAME_PUBLISHER,
    }

    enum class ForumType {
        @SerializedName("region")
        REGION,

        @SerializedName("thing")
        THING,

        @SerializedName("person")
        PERSON,

        @SerializedName("company")
        COMPANY,
    }

    enum class ForumRegion(val id: Int) {
        BOARDGAME(1),
        RPG(2),
        VIDEOGAME(3),
    }
}
