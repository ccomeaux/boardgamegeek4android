package com.boardgamegeek.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.db.*
import com.boardgamegeek.db.model.GameColorsEntity
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.GameRemote
import com.boardgamegeek.io.safeApiCall
import com.boardgamegeek.mappers.*
import com.boardgamegeek.model.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

class GameRepository @Inject constructor(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
    private val playDao: PlayDao,
    private val gameColorDao: GameColorDao,
    private val gameDao: GameDao,
    private val artistDao: ArtistDao,
    private val designerDao: DesignerDao,
    private val publisherDao: PublisherDao,
    private val categoryDao: CategoryDao,
    private val mechanicDao: MechanicDao,
    private val collectionDao: CollectionDao,
) {
    fun loadAllAsFlow(): Flow<List<Game?>> {
        return gameDao.loadAllAsFlow().map { list ->
            list.map { it.game?.mapToModel(it.lastPlayedDate) }
        }
    }

    suspend fun loadGame(gameId: Int): Game? {
        return if (gameId == INVALID_ID) null else {
            val game = gameDao.loadGame(gameId)
            game?.game?.mapToModel(game.lastPlayedDate)
        }
    }

    fun loadGameFlow(gameId: Int): Flow<Game?> {
        return gameDao.loadGameFlow(gameId)
            .map { it?.game?.mapToModel(it.lastPlayedDate) }
    }

    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int, beforeTimestamp: Long): List<Pair<Int, String>> {
        return gameDao.loadOldestUpdatedGames(gamesPerFetch, beforeTimestamp).map { it.gameId to it.gameName }
    }

    suspend fun loadUnupdatedGames(gamesPerFetch: Int): List<Pair<Int, String>> {
        return gameDao.loadUnupdatedGames(gamesPerFetch).map { it.gameId to it.gameName }
    }

    suspend fun loadGamesByLastViewed(sinceTimestamp: Long, includeUnplayedGames: Boolean): List<Pair<Int, String>> {
        return if (includeUnplayedGames) {
            gameDao.loadNonCollectionAndUnplayedGamesByLastViewed(sinceTimestamp).map { it.gameId to it.gameName }
        } else {
            gameDao.loadNonCollectionGamesByLastViewed(sinceTimestamp).map { it.gameId to it.gameName }
        }
    }

    suspend fun refreshGame(vararg gameId: Int): Result<Int> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val result = safeApiCall(context) {
            if (gameId.size == 1) {
                api.thing(gameId.first(), 1)
            } else {
                api.things(gameId.joinToString(), 1)
            }
        }
        if (result.isSuccess) {
            result.getOrNull()?.games?.forEach { game ->
                val internalId = gameDao.loadGame(game.id)?.game?.internalId ?: 0L
                val gameForUpsert = game.mapForUpsert(internalId, timestamp)
                Timber.i("Saving game ${gameForUpsert.header.gameName} (${game.id})")
                game.mapToDesigners().forEach { designerDao.insert(it) }
                game.mapToArtists().forEach { artistDao.insert(it) }
                game.mapToPublishers().forEach { publisherDao.insert(it) }
                game.mapToCategories().forEach { categoryDao.insert(it) }
                game.mapToMechanics().forEach { mechanicDao.insert(it) }
                if (gameForUpsert.header.gameName.isBlank()) {
                    Timber.w("Missing name from game ID=${gameForUpsert.header.gameId}")
                } else {
                    gameDao.upsert(gameForUpsert).also {
                        Timber.i("Saved game ${gameForUpsert.header.gameName} (${game.id}) [$it]")
                    }
                }
            }
            Result.success(result.getOrNull()?.games?.size ?: 0)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    suspend fun fetchGameThumbnail(vararg gameId: Int): String? = withContext(Dispatchers.IO) {
        val response = if (gameId.size == 1) {
            api.thing(gameId.first(), 1)
        } else {
            api.things(gameId.joinToString(), 1)
        }
        response.games?.firstOrNull()?.thumbnail
    }

    suspend fun refreshHeroImage(game: Game): Game = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(game.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let { url ->
            gameDao.updateHeroUrl(game.id, url)
            game.copy(heroImageUrl = url)
        } ?: game
    }

    fun loadCommentsPager(gameId: Int, sortByRating: Boolean = false): Pager<Int, GameComment> =
        Pager(PagingConfig(GameRemote.PAGE_SIZE)) {
            CommentsPagingSource(gameId, sortByRating, api, XmlApiMarkupConverter(context))
        }

    fun getSubtypesFlow(gameId: Int): Flow<List<GameSubtype>> {
        return gameDao.loadRanksForGameFlow(gameId)
            .map { list -> list.mapNotNull { it.mapToSubtype() } }
            .flowOn(Dispatchers.Default)
    }

    fun getFamiliesFlow(gameId: Int): Flow<List<GameFamily>> {
        return gameDao.loadRanksForGameFlow(gameId)
            .map { list -> list.mapNotNull { it.mapToFamily() } }
            .flowOn(Dispatchers.Default)
    }

    fun getLanguagePollFlow(gameId: Int): Flow<GameLanguagePoll?> {
        return gameDao.loadLanguagePollForGameFlow(gameId).map { it.mapToModel() }
    }

    fun getAgePollFlow(gameId: Int): Flow<GameAgePoll?> {
        return gameDao.loadAgePollForGameFlow(gameId).map { it.mapToModel() }
    }

    fun getPlayerPollFlow(gameId: Int): Flow<List<GamePlayerPollResults>> {
        return gameDao.loadPlayerPollForGameFlow(gameId).map { list -> list.map { it.mapToModel() } }
    }

    fun getDesignersFlow(gameId: Int): Flow<List<GameDetail>> {
        return gameDao.loadDesignersForGameFlow(gameId)
            .map { it.map { designer -> designer.mapToGameDetail() } }
            .flowOn(Dispatchers.Default)
    }

    fun getArtistsFlow(gameId: Int): Flow<List<GameDetail>> {
        return gameDao.loadArtistsForGameFlow(gameId)
            .map { it.map { artist -> artist.mapToGameDetail() } }
            .flowOn(Dispatchers.Default)
    }

    fun getPublishers(gameId: Int): Flow<List<GameDetail>> {
        return gameDao.loadPublishersForGameFlow(gameId)
            .map { it.map { publisher -> publisher.mapToGameDetail() } }
            .flowOn(Dispatchers.Default)
    }

    fun getCategoriesFlow(gameId: Int): Flow<List<GameDetail>> {
        return gameDao.loadCategoriesForGameFlow(gameId)
            .map { it.map { category -> category.mapToGameDetail() } }
            .flowOn(Dispatchers.Default)
    }

    fun getMechanicsFlow(gameId: Int): Flow<List<GameDetail>> {
        return gameDao.loadMechanicsForGameFlow(gameId)
            .map { it.map { mechanic -> mechanic.mapToGameDetail() } }
            .flowOn(Dispatchers.Default)
    }

    fun getExpansionsFlow(gameId: Int): Flow<List<GameExpansion>> = gameDao.loadExpansionsForGameFlow(gameId).map { list ->
        list.map { entity ->
            val items = collectionDao.loadForGame(entity.gameExpansionEntity.expansionId).map { it.mapToModel() }
            entity.mapToModel(items)
        }
    }

    fun getBaseGamesFlow(gameId: Int): Flow<List<GameExpansion>> = gameDao.loadBaseGamesForGameFlow(gameId).map { list ->
        list.map { entity ->
            val items = collectionDao.loadForGame(entity.gameExpansionEntity.expansionId).map { it.mapToModel() }
            entity.mapToModel(items)
        }
    }

    /**
     * Returns a map of all game IDs with player colors.
     */
    suspend fun getPlayColors(): Map<Int, List<GameColorsEntity>> = withContext(Dispatchers.IO) {
        gameColorDao.loadColors().groupBy { it.gameId }
    }

    suspend fun getPlayColors(gameId: Int): List<String> = withContext(Dispatchers.IO) {
        gameColorDao.loadColorsForGame(gameId).map { it.color }
    }

    fun getPlayColorsFlow(gameId: Int): Flow<List<String>> {
        return gameColorDao.loadColorsForGameFlow(gameId).map { list -> list.map { entity -> entity.color } }
    }

    suspend fun addPlayColor(gameId: Int, color: String?) {
        if (gameId != INVALID_ID && !color.isNullOrBlank()) {
            if (!gameColorDao.loadColorsForGame(gameId).map { it.color }.contains(color))
                gameColorDao.insert(listOf(GameColorsEntity(internalId = 0L, gameId = gameId, color = color)))
        }
    }

    suspend fun deletePlayColor(gameId: Int, color: String) {
        if (gameId != INVALID_ID)
            gameColorDao.deleteColorForGame(gameId, color)
    }

    suspend fun computePlayColors(gameId: Int) {
        val usedColors = playDao.loadPlayersForGame(gameId).mapNotNull { it.player.color }.toSet()
        val currentColors = gameColorDao.loadColorsForGame(gameId).map { it.color }.toSet()
        val colors = (usedColors - currentColors).filterNot { it.isBlank() }
        val entities = colors.map { GameColorsEntity(internalId = 0L, gameId = gameId, color = it) }
        gameColorDao.insert(entities)
    }

    suspend fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) {
        if (gameId != INVALID_ID) gameDao.updateLastViewed(gameId, lastViewed)
    }

    suspend fun updateGameColors(
        gameId: Int,
        iconColor: Int,
        darkColor: Int,
        winsColor: Int,
        winnablePlaysColor: Int,
        allPlaysColor: Int
    ) = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            gameDao.updateImageColors(gameId, iconColor, darkColor, winsColor, winnablePlaysColor, allPlaysColor)
        }
    }

    suspend fun replaceColors(gameId: Int, colors: List<String>) {
        gameDao.loadGame(gameId)?.let {
            gameColorDao.deleteColorsForGame(gameId)
            gameColorDao.insert(colors.map { GameColorsEntity(internalId = 0L, gameId = gameId, color = it) })
        }
    }

    suspend fun updateFavorite(gameId: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) gameDao.updateStarred(gameId, isFavorite)
    }

    suspend fun delete(gameId: Int): Int {
        return if (gameId != INVALID_ID) gameDao.delete(gameId) else 0
    }

    suspend fun deleteAll() = gameDao.deleteAll()
}

private class CommentsPagingSource(
    val gameId: Int,
    private val sortByRating: Boolean = false,
    val api: BggService,
    private val markupConverter: XmlApiMarkupConverter,
) :
    PagingSource<Int, GameComment>() {
    override fun getRefreshKey(state: PagingState<Int, GameComment>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GameComment> {
        return try {
            if (gameId == INVALID_ID) return LoadResult.Error(Exception("Invalid ID"))

            val page = params.key ?: 1
            val response = withContext(Dispatchers.IO) {
                if (sortByRating) api.thingWithRatings(gameId, page)
                else api.thingWithComments(gameId, page)
            }
            val list = response.games?.firstOrNull()?.mapToRatingModel(markupConverter)
            val nextPage = if (page * GameRemote.PAGE_SIZE < (list?.numberOfRatings ?: 0)) page + 1 else null
            LoadResult.Page(list?.ratings.orEmpty(), null, nextPage)
        } catch (e: Exception) {
            if (e is HttpException) {
                Timber.w("Error code: ${e.code()}\n${e.response()?.body()}")
            } else {
                Timber.w(e)
            }
            LoadResult.Error(e)
        }
    }
}
