package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository

class GameViewModel(application: Application) : AndroidViewModel(application) {
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

    fun setId(gameId: Int?) {
        if (_gameId.value != gameId) _gameId.value = gameId
    }

    fun setProducerType(type: ProducerType?) {
        if (_producerType.value != type) _producerType.value = type
    }

    val game: LiveData<RefreshableResource<GameEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getGame(gameId)
        }
    }

    val languagePoll: LiveData<GamePollEntity> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getLanguagePoll(gameId)
        }
    }

    val agePoll: LiveData<GamePollEntity> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getAgePoll(gameId)
        }
    }

    val ranks: LiveData<List<GameRankEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getRanks(gameId)
        }
    }

    val playerPoll: LiveData<GamePlayerPollEntity> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPlayerPoll(gameId)
        }
    }

    val designers: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getDesigners(gameId)
        }
    }

    val artists: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getArtists(gameId)
        }
    }

    val publishers: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPublishers(gameId)
        }
    }

    val categories: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getCategories(gameId)
        }
    }

    val mechanics: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getMechanics(gameId)
        }
    }

    val expansions: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> Transformations.map(gameRepository.getExpansions(gameId)) { items ->
                val list = arrayListOf<GameDetailEntity>()
                items.forEach {
                    list += GameDetailEntity(it.id, it.name, describeStatuses(it, application))
                }
                return@map list.toList()
            }
        }
    }

    val baseGames: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> Transformations.map(gameRepository.getBaseGames(gameId)) { items ->
                val list = arrayListOf<GameDetailEntity>()
                items.forEach {
                    list += GameDetailEntity(it.id, it.name, describeStatuses(it, application))
                }
                return@map list.toList()
            }
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

    val producers: LiveData<List<GameDetailEntity>> = Transformations.switchMap(_producerType) { type ->
        when (type) {
            ProducerType.DESIGNER -> designers
            ProducerType.ARTIST -> artists
            ProducerType.PUBLISHER -> publishers
            ProducerType.CATEGORIES -> categories
            ProducerType.MECHANICS -> mechanics
            ProducerType.EXPANSIONS -> expansions
            ProducerType.BASE_GAMES -> baseGames
            else -> AbsentLiveData.create()
        }
    }

    val collectionItems: LiveData<RefreshableResource<List<CollectionItemEntity>>> = Transformations.switchMap(game) { game ->
        when (val gameId = game.data?.id ?: BggContract.INVALID_ID) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameCollectionRepository.getCollectionItems(gameId, game.data?.subtype ?: "")
        }
    }

    val plays: LiveData<RefreshableResource<List<PlayEntity>>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPlays(gameId)
        }
    }

    val playColors: LiveData<List<String>> = Transformations.switchMap(_gameId) { gameId ->
        when (gameId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.getPlayColors(gameId)
        }
    }

    fun refresh() {
        _gameId.value?.let { _gameId.value = it }
    }

    fun updateLastViewed(lastViewed: Long = System.currentTimeMillis()) {
        gameRepository.updateLastViewed(gameId.value ?: BggContract.INVALID_ID, lastViewed)
    }

    fun updateGameColors(palette: Palette?) {
        if (palette != null) {
            val iconColor = palette.getIconSwatch().rgb
            val darkColor = palette.getDarkSwatch().rgb
            val (wins, winnablePlays, allPlays) = palette.getPlayCountColors(getApplication())
            gameRepository.updateGameColors(gameId.value ?: BggContract.INVALID_ID,
                    iconColor,
                    darkColor,
                    wins,
                    winnablePlays,
                    allPlays)
        }
    }

    fun updateFavorite(isFavorite: Boolean) {
        gameRepository.updateFavorite(gameId.value ?: BggContract.INVALID_ID, isFavorite)
    }

    fun addCollectionItem(statuses: List<String>, wishListPriority: Int?) {
        gameCollectionRepository.addCollectionItem(gameId.value ?: BggContract.INVALID_ID, statuses, wishListPriority)
    }
}
