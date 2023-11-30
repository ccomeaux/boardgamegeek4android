package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.*
import com.boardgamegeek.db.model.*
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.*
import com.boardgamegeek.model.*
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

    suspend fun loadGame(gameId: Int): Game? {
        return if (gameId == INVALID_ID) null else {
            val game = gameDaoNew.loadGame(gameId)
            game?.game?.mapToModel(game.lastPlayedDate)
        }
    }

    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int = 0): List<Pair<Int, String>> {
        return gameDaoNew.loadOldestUpdatedGames(gamesPerFetch).map { it.gameId to it.gameName }
    }

    suspend fun loadUnupdatedGames(gamesPerFetch: Int = 0): List<Pair<Int, String>> {
        return gameDaoNew.loadUnupdatedGames(gamesPerFetch).map { it.gameId to it.gameName }
    }

    suspend fun loadDeletableGames(sinceTimestamp: Long, includeUnplayedGames: Boolean): List<Pair<Int, String>> {
        return if (includeUnplayedGames) {
            gameDaoNew.loadNonCollectionAndUnplayedGames(sinceTimestamp).map { it.gameId to it.gameName }
        } else {
            gameDaoNew.loadNonCollectionGames(sinceTimestamp).map { it.gameId to it.gameName }
        }
    }

    suspend fun refreshGame(vararg gameId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val games = api.thing(gameId.first(), 1).games
        games?.forEach { game ->
            game.mapToDesigners().forEach { designerDao.upsert(it) }
            game.mapToArtists().forEach { artistDao.upsert(it) }
            game.mapToPublishers().forEach { publisherDao.upsert(it) }
            game.mapToCategories().forEach { categoryDao.upsert(it) }
            game.mapToMechanics().forEach { mechanicDao.upsert(it) }
            val gameForUpsert = game.mapForUpsert(timestamp)
            if (gameForUpsert.gameName.isBlank()) {
                Timber.w("Missing name from game ID=${gameForUpsert.gameId}")
            } else {
                Timber.i("Saving game $game")
                dao.save(gameForUpsert)
            }
            Timber.d("Synced game $gameForUpsert")
        }
        games?.size ?: 0
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
            gameDaoNew.updateHeroUrl(game.id, url)
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

    suspend fun getRanks(gameId: Int): List<GameRank> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameDaoNew.loadRanksForGame(gameId).map { it.mapToModel() }
    }

    suspend fun getLanguagePoll(gameId: Int): GamePoll? {
        return if (gameId == INVALID_ID)
            null
        else {
            gameDaoNew.loadLanguagePollForGame(gameId).mapToModel()
        }
    }

    suspend fun getAgePoll(gameId: Int): GamePoll? {
        return if (gameId == INVALID_ID)
            null
        else {
            gameDaoNew.loadAgePollForGame(gameId).mapToModel()
        }
    }

    suspend fun getPlayerPoll(gameId: Int): List<GamePlayerPollResults> {
        return if (gameId == INVALID_ID)
            emptyList()
        else
            gameDaoNew.loadPlayerPollForGame(gameId).map { it.mapToModel() }
    }

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

    suspend fun getExpansions(gameId: Int): List<GameExpansion> = gameDaoNew.loadExpansionsForGame(gameId).map { entity ->
        val items = collectionDao.loadByGame(entity.gameExpansionEntity.expansionId).map { it.second.mapToModel(it.first.mapToModel()) }
        entity.mapToModel(items)
    }

    suspend fun getBaseGames(gameId: Int) = gameDaoNew.loadBaseGamesForGame(gameId).map { entity ->
        val items = collectionDao.loadByGame(entity.gameExpansionEntity.expansionId).map { it.second.mapToModel(it.first.mapToModel()) }
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
        if (gameId != INVALID_ID) gameDaoNew.updateLastViewed(gameId, lastViewed)
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
            gameDaoNew.updateImageColors(gameId, iconColor, darkColor, winsColor, winnablePlaysColor, allPlaysColor)
        }
    }

    suspend fun replaceColors(gameId: Int, colors: List<String>) {
        gameDaoNew.loadGame(gameId)?.let {
            gameColorDao.deleteColorsForGame(gameId)
            gameColorDao.insert(colors.map { GameColorsEntity(internalId = 0L, gameId = gameId, color = it) })
        }
    }

    suspend fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId != INVALID_ID) gameDaoNew.updateStarred(gameId, isFavorite)
    }

    suspend fun delete(gameId: Int): Int {
        return if (gameId != INVALID_ID) gameDaoNew.delete(gameId) else 0
    }

    suspend fun deleteAll() = gameDaoNew.deleteAll()
}
