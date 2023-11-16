package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.*
import com.boardgamegeek.db.model.*
import com.boardgamegeek.model.GameComments
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.GamePoll
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.*
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class GameRepository @Inject constructor(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
    private val playDao: PlayDao,
    private val gameColorDao: GameColorDao,
    private val gameDaoNew: GameDaoNew,
    private val artistDao: ArtistDao,
    private val designerDao: DesignerDao,
    private val publisherDao: PublisherDao,
    private val categoryDao: CategoryDao,
    private val mechanicDao: MechanicDao,
) {
    private val dao = GameDao(context)
    private val collectionDao = CollectionDao(context)

    suspend fun loadGame(gameId: Int) = dao.load(gameId)?.mapToModel()

    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int = 0): List<Pair<Int, String>> {
        return gameDaoNew.loadOldestUpdatedGames(gamesPerFetch).map { it.gameId to it.gameName }
    }

    suspend fun loadUnupdatedGames(gamesPerFetch: Int = 0): List<Pair<Int, String>> {
        return gameDaoNew.loadUnupdatedGames(gamesPerFetch).map { it.gameId to it.gameName }
    }

    suspend fun loadDeletableGames(sinceTimestamp: Long, includeUnplayedGames: Boolean): List<Pair<Int, String>> {
        return if (includeUnplayedGames){
            gameDaoNew.loadNonCollectionAndUnplayedGames(sinceTimestamp).map { it.gameId to it.gameName }
        } else {
            gameDaoNew.loadNonCollectionGames(sinceTimestamp).map { it.gameId to it.gameName }
        }
    }

    suspend fun refreshGame(vararg gameId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val games = api.thing(gameId.first(), 1).games
        games?.forEach { game ->
            val gameForUpsert = game.mapForUpsert(timestamp)
            saveGame(gameForUpsert)
            Timber.d("Synced game $gameForUpsert")
        }
        games?.size ?: 0
    }

    private suspend fun saveGame(game: GameForUpsert) {
        game.artists?.forEach {
            val artist = artistDao.loadArtist(it.first)
            val entity = ArtistBriefForUpsert(
                internalId = artist?.internalId ?: 0,
                artistId = it.first,
                artistName = it.second,
            )
            artistDao.upsert(entity)
        }
        game.designers?.forEach {
            val designer = designerDao.loadDesigner(it.first)
            val entity = DesignerBriefForUpsert(
                internalId = designer?.internalId ?: 0,
                designerId = it.first,
                designerName = it.second,
            )
            designerDao.upsert(entity)
        }
        game.publishers?.forEach {
            val publisher = publisherDao.loadPublisher(it.first)
            val entity = PublisherBriefForUpsert(
                internalId = publisher?.internalId ?: 0,
                publisherId = it.first,
                publisherName = it.second,
            )
            publisherDao.upsert(entity)
        }
        game.categories?.forEach {
            val category = categoryDao.loadCategory(it.first)
            val entity = CategoryEntity(
                internalId = category?.internalId ?: 0,
                categoryId = it.first,
                categoryName = it.second,
            )
            categoryDao.upsert(entity)
        }
        game.mechanics?.forEach {
            val mechanic = mechanicDao.loadMechanic(it.first)
            val entity = MechanicEntity(
                internalId = mechanic?.internalId ?: 0,
                mechanicId = it.first,
                mechanicName = it.second,
            )
            mechanicDao.upsert(entity)
        }
        dao.save(game)
    }

    suspend fun fetchGame(vararg gameId: Int): List<Game> = withContext(Dispatchers.IO) {
        val response = if (gameId.size == 1) {
            api.thing(gameId.first(), 1)
        } else {
            api.things(gameId.joinToString(), 1)
        }
        response.games?.map { it.mapToModel() }.orEmpty()
    }

    suspend fun refreshHeroImage(game: Game): Game = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(game.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let { url ->
            dao.updateHeroUrl(game.id, url)
            game.copy(heroImageUrl = url)
        } ?: game
    }

    suspend fun loadComments(gameId: Int, page: Int): GameComments? = withContext(Dispatchers.IO) {
        val response = api.thingWithComments(gameId, page)
        response.games?.firstOrNull()?.mapToRatingModel()
    }

    suspend fun loadRatings(gameId: Int, page: Int): GameComments? = withContext(Dispatchers.IO) {
        val response = api.thingWithRatings(gameId, page)
        response.games?.firstOrNull()?.mapToRatingModel()
    }

    suspend fun getRanks(gameId: Int) = dao.loadRanks(gameId).map { it.mapToModel() }

    suspend fun getLanguagePoll(gameId: Int) = GamePoll(dao.loadPoll(gameId, GameDao.PollType.LANGUAGE_DEPENDENCE).map { it.mapToModel() })

    suspend fun getAgePoll(gameId: Int) = GamePoll(dao.loadPoll(gameId, GameDao.PollType.SUGGESTED_PLAYER_AGE).map { it.mapToModel() })

    suspend fun getPlayerPoll(gameId: Int) = dao.loadPlayerPoll(gameId).map { it.mapToModel() }

    suspend fun getDesigners(gameId: Int): List<GameDetail> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameDaoNew.loadDesignersForGame(gameId)?.designers?.map { it.mapToGameDetail() }.orEmpty()
    }

    suspend fun getArtists(gameId: Int): List<GameDetail> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameDaoNew.loadArtistsForGame(gameId)?.artists?.map { it.mapToGameDetail() }.orEmpty()
    }

    suspend fun getPublishers(gameId: Int): List<GameDetail> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameDaoNew.loadPublishersForGame(gameId)?.publishers?.map { it.mapToGameDetail() }.orEmpty()
    }

    suspend fun getCategories(gameId: Int): List<GameDetail> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameDaoNew.loadCategoriesForGame(gameId)?.categories?.map { it.mapToGameDetail() }.orEmpty()
    }

    suspend fun getMechanics(gameId: Int): List<GameDetail> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameDaoNew.loadMechanicsForGame(gameId)?.mechanics?.map { it.mapToGameDetail() }.orEmpty()
    }

    suspend fun getExpansions(gameId: Int) = dao.loadExpansions(gameId).map { entity ->
        val items = collectionDao.loadByGame(entity.expansionId).map { it.second.mapToModel(it.first.mapToModel()) }
        entity.mapToModel(items)
    }

    suspend fun getBaseGames(gameId: Int) = dao.loadExpansions(gameId, true).map { entity ->
        val items = collectionDao.loadByGame(entity.expansionId).map { it.second.mapToModel(it.first.mapToModel()) }
        entity.mapToModel(items)
    }

    /**
     * Returns a map of all game IDs with player colors.
     */
    suspend fun getPlayColors(): Map<Int, List<GameColorsEntity>> {
        return gameColorDao.loadColors().groupBy { it.gameId }
    }

    suspend fun getPlayColors(gameId: Int): List<String> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameColorDao.loadColorsForGame(gameId).map { it.color }
    }

    suspend fun addPlayColor(gameId: Int, color: String?) {
        if (gameId != INVALID_ID && !color.isNullOrBlank()) {
            if (!gameColorDao.loadColorsForGame(gameId).map { it.color }.contains(color))
                gameColorDao.insert(listOf(GameColorsEntity(internalId = 0L, gameId = gameId, color = color)))
        }
    }

    suspend fun deletePlayColor(gameId: Int, color: String) {
        if (gameId != INVALID_ID && color.isNotBlank())
            gameColorDao.deleteColorForGame(gameId, color)
    }

    suspend fun computePlayColors(gameId: Int) {
        val usedColors = playDao.loadPlayersForGame(gameId).mapNotNull { it.player.color }.toSet()
        val currentColors = gameColorDao.loadColorsForGame(gameId).map { it.color }.toSet()
        val colors = usedColors - currentColors
        val entities = colors.map { GameColorsEntity(internalId = 0L, gameId = gameId, color = it) }
        gameColorDao.insert(entities)
    }

    suspend fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) {
        if (gameId != INVALID_ID) {
            dao.updateLastViewed(gameId, lastViewed)
        }
    }

    suspend fun updateGameColors(
        gameId: Int,
        iconColor: Int,
        darkColor: Int,
        winsColor: Int,
        winnablePlaysColor: Int,
        allPlaysColor: Int
    ) {
        if (gameId != INVALID_ID) {
            dao.updateGameColors(gameId, iconColor, darkColor, winsColor, winnablePlaysColor, allPlaysColor)
        }
    }

    suspend fun replaceColors(gameId: Int, colors: List<String>) {
        dao.load(gameId)?.let {
            gameColorDao.deleteColorsForGame(gameId)
            gameColorDao.insert(colors.map { GameColorsEntity(internalId = 0L, gameId = gameId, color = it) })
        }
    }

    suspend fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId != INVALID_ID) dao.updateStarred(gameId, isFavorite)
    }

    suspend fun delete(gameId: Int): Int {
        return if (gameId != INVALID_ID) gameDaoNew.delete(gameId) else 0
    }

    suspend fun deleteAll() = gameDaoNew.deleteAll()
}
