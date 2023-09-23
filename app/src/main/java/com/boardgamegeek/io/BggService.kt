@file:Suppress("SpellCheckingInspection", "unused")

package com.boardgamegeek.io

import com.boardgamegeek.io.model.*
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.text.SimpleDateFormat
import java.util.*

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
    suspend fun person(@Path("type") type: PersonType?, @Path("id") id: Int): Person

    @GET("/xmlapi2/person")
    suspend fun person(@Query("id") id: Int): PersonResponse

    @GET("/xmlapi2/company/{id}")
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

    enum class ThingSubtype(val code: String) {
        BOARDGAME("boardgame"),
        BOARDGAME_EXPANSION("boardgameexpansion"),
        BOARDGAME_ACCESSORY("boardgameaccessory"),
    }

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

        const val COLLECTION_QUERY_KEY_ID = "id"
        const val COLLECTION_QUERY_KEY_SHOW_PRIVATE = "showprivate"
        const val COLLECTION_QUERY_KEY_STATS = "stats"
        const val COLLECTION_QUERY_KEY_MODIFIED_SINCE = "modifiedsince"
        const val COLLECTION_QUERY_KEY_BRIEF = "brief"
        const val COLLECTION_QUERY_KEY_SUBTYPE = "subtype"
        const val COLLECTION_QUERY_STATUS_PLAYED = "played"
        const val COLLECTION_QUERY_KEY_COLLECTION_ID = "collid"

        // val COLLECTION_QUERY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val COLLECTION_QUERY_DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
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
