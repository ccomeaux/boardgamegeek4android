package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val isGameRefreshing = AtomicBoolean()
    private val arePlaysRefreshing = AtomicBoolean()
    private val areItemsRefreshing = AtomicBoolean()
    private val refreshGameMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_MINUTES)
    private val refreshPlaysPartialMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_PARTIAL_MINUTES)
    private val refreshPlaysFullHours = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_FULL_HOURS)
    private val refreshItemsMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)

    private val _gameId = MutableLiveData<Int>()
    val gameId: LiveData<Int>
        get() = _gameId

    private val _producerType = MutableLiveData<ProducerType>()
    val producerType: LiveData<ProducerType>
        get() = _producerType

    enum class ProducerType(val value: Int) {
        UNKNOWN(0),
        DESIGNER(1),
        ARTIST(2),
        PUBLISHER(3),
        CATEGORIES(4),
        MECHANICS(5),
        EXPANSIONS(6),
        BASE_GAMES(7);

        companion object {
            private val map = values().associateBy(ProducerType::value)
            fun fromInt(value: Int?) = map[value] ?: UNKNOWN
        }
    }

    private val gameRepository = GameRepository(getApplication())
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    fun setId(gameId: Int) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    fun setProducerType(type: ProducerType) {
        if (_producerType.value != type) _producerType.value = type
    }

    val game: LiveData<RefreshableResource<GameEntity>> = _gameId.switchMap { gameId ->
        liveData {
            try {
                val game = gameRepository.loadGame(gameId)
                val refreshedGame =
                    if ((game == null || game.updated.isOlderThan(refreshGameMinutes, TimeUnit.MINUTES)) &&
                        isGameRefreshing.compareAndSet(false, true)
                    ) {
                        emit(RefreshableResource.refreshing(game))
                        gameRepository.refreshGame(gameId).also {
                            isGameRefreshing.set(false)
                        }
                    } else game
                val gameWithHeroImage = refreshedGame?.let {
                    if (it.heroImageUrl.isBlank()) {
                        emit(RefreshableResource.refreshing(it))
                        gameRepository.refreshHeroImage(it)
                    } else refreshedGame
                }
                emit(RefreshableResource.success(gameWithHeroImage))
            } catch (e: Exception) {
                emit(RefreshableResource.error<GameEntity>(e, application))
            }
        }
    }

    val ranks = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getRanks(gameId))
        }
    }

    val languagePoll = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getLanguagePoll(gameId))
        }
    }

    val agePoll = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getAgePoll(gameId))
        }
    }

    val playerPoll = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getPlayerPoll(gameId))
        }
    }

    val designers = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getDesigners(gameId))
        }
    }

    val artists = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getArtists(gameId))
        }
    }

    val publishers = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getPublishers(gameId))
        }
    }

    val categories = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getCategories(gameId))
        }
    }

    val mechanics = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getMechanics(gameId))
        }
    }

    val expansions = _gameId.switchMap { gameId ->
        liveData {
            emit(
                if (gameId == BggContract.INVALID_ID) null else gameRepository.getExpansions(gameId).map {
                    GameDetailEntity(it.id, it.name, describeStatuses(it, application))
                }
            )
        }
    }

    val baseGames = _gameId.switchMap { gameId ->
        liveData {
            emit(
                if (gameId == BggContract.INVALID_ID) null else gameRepository.getBaseGames(gameId).map {
                    GameDetailEntity(it.id, it.name, describeStatuses(it, application))
                }
            )
        }
    }

    private fun describeStatuses(entity: GameExpansionsEntity, application: Application): String {
        val ctx = application.applicationContext
        val statuses = mutableListOf<String>()
        if (entity.own) statuses.add(ctx.getString(R.string.collection_status_own))
        if (entity.previouslyOwned) statuses.add(ctx.getString(R.string.collection_status_prev_owned))
        if (entity.forTrade) statuses.add(ctx.getString(R.string.collection_status_for_trade))
        if (entity.wantInTrade) statuses.add(ctx.getString(R.string.collection_status_want_in_trade))
        if (entity.wantToBuy) statuses.add(ctx.getString(R.string.collection_status_want_to_buy))
        if (entity.wantToPlay) statuses.add(ctx.getString(R.string.collection_status_want_to_play))
        if (entity.preOrdered) statuses.add(ctx.getString(R.string.collection_status_preordered))
        if (entity.wishList) statuses.add(entity.wishListPriority.asWishListPriority(ctx))
        if (entity.numberOfPlays > 0) statuses.add(ctx.getString(R.string.played))
        if (entity.rating > 0.0) statuses.add(ctx.getString(R.string.rated))
        if (entity.comment.isNotBlank()) statuses.add(ctx.getString(R.string.commented))
        return statuses.formatList()
    }

    val producers = _producerType.switchMap { type ->
        when (type) {
            ProducerType.DESIGNER -> designers
            ProducerType.ARTIST -> artists
            ProducerType.PUBLISHER -> publishers
            ProducerType.CATEGORIES -> categories
            ProducerType.MECHANICS -> mechanics
            ProducerType.EXPANSIONS -> expansions
            ProducerType.BASE_GAMES -> baseGames
            else -> AbsentLiveData.create() // TODO
        }
    }

    val collectionItems: LiveData<RefreshableResource<List<CollectionItemEntity>>> = game.switchMap { game ->
        liveData {
            val gameId = game.data?.id ?: BggContract.INVALID_ID
            val items =
                if (gameId == BggContract.INVALID_ID) emptyList()
                else gameCollectionRepository.loadCollectionItems(gameId)
            val refreshedItems =
                if (areItemsRefreshing.compareAndSet(false, true)) {
                    val lastUpdated = items.minByOrNull { it.syncTimestamp }?.syncTimestamp ?: 0L
                    when {
                        lastUpdated.isOlderThan(refreshItemsMinutes, TimeUnit.MINUTES) -> {
                            emit(RefreshableResource.refreshing(items))
                            gameCollectionRepository.refreshCollectionItems(gameId, game.data?.subtype.orEmpty())
                        }
                        else -> items
                    }.also { areItemsRefreshing.set(false) }
                } else items
            emit(RefreshableResource.success(refreshedItems))
        }
    }

    val plays = game.switchMap { game ->
        liveData {
            val gameId = game.data?.id ?: BggContract.INVALID_ID
            val plays = if (gameId == BggContract.INVALID_ID) emptyList() else gameRepository.getPlaysC(gameId)
            val refreshedPlays =
                if (arePlaysRefreshing.compareAndSet(false, true)) {
                    val lastUpdated = game.data?.updatedPlays ?: System.currentTimeMillis()
                    when {
                        lastUpdated.isOlderThan(refreshPlaysFullHours, TimeUnit.HOURS) -> {
                            emit(RefreshableResource.refreshing(plays))
                            gameRepository.refreshPlays(gameId)
                        }
                        lastUpdated.isOlderThan(refreshPlaysPartialMinutes, TimeUnit.MINUTES) -> {
                            emit(RefreshableResource.refreshing(plays))
                            gameRepository.refreshPartialPlays(gameId)
                        }
                        else -> plays
                    }.also { arePlaysRefreshing.set(false) }
                } else plays
            emit(RefreshableResource.success(refreshedPlays))
        }
    }

    val playColors = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getPlayColors(gameId))
        }
    }

    fun refresh() {
        _gameId.value?.let { _gameId.value = it }
    }

    fun updateLastViewed(lastViewed: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            gameRepository.updateLastViewed(gameId.value ?: BggContract.INVALID_ID, lastViewed)
            refresh()
        }
    }

    fun updateGameColors(palette: Palette?) {
        palette?.let { p ->
            viewModelScope.launch {
                val iconColor = p.getIconSwatch().rgb
                val darkColor = p.getDarkSwatch().rgb
                val (wins, winnablePlays, allPlays) = p.getPlayCountColors(getApplication())
                gameRepository.updateGameColors(
                    gameId.value ?: BggContract.INVALID_ID,
                    iconColor,
                    darkColor,
                    wins,
                    winnablePlays,
                    allPlays,
                )
                refresh()
            }
        }
    }

    fun updateFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            gameRepository.updateFavorite(gameId.value ?: BggContract.INVALID_ID, isFavorite)
            refresh()
        }
    }

    fun addCollectionItem(statuses: List<String>, wishListPriority: Int?) {
        gameCollectionRepository.addCollectionItem(gameId.value ?: BggContract.INVALID_ID, statuses, wishListPriority)
    }
}
