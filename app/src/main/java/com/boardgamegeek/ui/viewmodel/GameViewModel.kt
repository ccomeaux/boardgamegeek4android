package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.annotation.ColorInt
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.*
import androidx.palette.graphics.Palette
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.EventLiveData
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.ImageRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.util.RemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class GameViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val gameCollectionRepository: GameCollectionRepository,
    private val imageRepository: ImageRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val isGameRefreshing = AtomicBoolean(false)
    private val areItemsRefreshing = AtomicBoolean(false)
    private val forceItemsRefresh = AtomicBoolean(false)
    private val arePlaysRefreshing = AtomicBoolean(false)
    private val forcePlaysRefresh = AtomicBoolean(false)
    private val gameRefreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_MINUTES)
    private val itemsRefreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_COLLECTION_MINUTES)
    private val playsFullMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_FULL_HOURS)
    private val playsPartialMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_PARTIAL_MINUTES)

    val username: LiveSharedPreference<String> = LiveSharedPreference(getApplication(), AccountPreferences.KEY_USERNAME)
    val syncPlaysPreference: LiveSharedPreference<Boolean> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS)
    val syncCollectionPreference: LiveSharedPreference<Set<String>> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_STATUSES)

    private val _gameId = MutableLiveData<Int>()
    val gameId: LiveData<Int>
        get() = _gameId

    private val _gameIsRefreshing = MutableLiveData<Boolean>()
    val gameIsRefreshing: LiveData<Boolean>
        get() = _gameIsRefreshing

    private val _itemsAreRefreshing = MutableLiveData<Boolean>()
    val itemsAreRefreshing: LiveData<Boolean>
        get() = _itemsAreRefreshing

    private val _playsAreRefreshing = MutableLiveData<Boolean>()
    val playsAreRefreshing: LiveData<Boolean>
        get() = _playsAreRefreshing

    private val _producerType = MutableLiveData<ProducerType>()
    val producerType: LiveData<ProducerType>
        get() = _producerType

    private val _errorMessage = EventLiveData()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _loggedPlayResult = MutableLiveData<Event<PlayUploadResult>>()
    val loggedPlayResult: LiveData<Event<PlayUploadResult>>
        get() = _loggedPlayResult

    enum class ProducerType {
        UNKNOWN,
        DESIGNER,
        ARTIST,
        PUBLISHER,
        CATEGORY,
        MECHANIC,
        EXPANSION,
        BASE_GAME,
    }

    fun setId(gameId: Int) {
        if (_gameId.value != gameId) {
            viewModelScope.launch {
                gameRepository.updateLastViewed(gameId)
            }
            _gameId.value = gameId
        }
    }

    fun setProducerType(type: ProducerType) {
        if (_producerType.value != type) _producerType.value = type
    }

    val game: LiveData<Game?> = _gameId.switchMap { gameId ->
        liveData {
            try {
                emitSource(gameRepository.loadGameFlow(gameId)
                    .onEach {
                        if (it == null || it.updated.isOlderThan(gameRefreshMinutes.minutes)) {
                            refreshGame()
                        }
                    }
                    .asLiveData()
                )
            } catch (e: Exception) {
                Timber.w(e)
                _errorMessage.setMessage(e)
            }
        }.distinctUntilChanged()
    }

    val subtypes = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getSubtypesFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val families = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getFamiliesFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val languagePoll = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getLanguagePollFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val agePoll = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getAgePollFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val playerPoll = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getPlayerPollFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val designers = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getDesignersFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val artists = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getArtistsFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val publishers = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getPublishers(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val categories = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getCategoriesFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val mechanics = game.switchMap {
        it?.let {
            liveData {
                emitSource(gameRepository.getMechanicsFlow(it.id).distinctUntilChanged().asLiveData())
            }
        }
    }

    val expansions = game.switchMap {
        it?.let {
            liveData {
                emitSource(
                    gameRepository.getExpansionsFlow(it.id)
                        .distinctUntilChanged()
                        .map { list ->
                            list.map { expansion ->
                                GameDetail(expansion.id, expansion.name, describeStatuses(expansion), expansion.thumbnailUrl)
                            }
                        }
                        .flowOn(Dispatchers.Default)
                        .asLiveData()
                )
            }
        }
    }

    val baseGames = game.switchMap {
        it?.let {
            liveData {
                emitSource(
                    gameRepository.getBaseGamesFlow(it.id)
                        .distinctUntilChanged()
                        .map { list ->
                            list.map { baseGame ->
                                GameDetail(baseGame.id, baseGame.name, describeStatuses(baseGame), baseGame.thumbnailUrl)
                            }
                        }
                        .flowOn(Dispatchers.Default)
                        .asLiveData()
                )
            }
        }
    }

    private fun describeStatuses(expansion: GameExpansion): String {
        val ctx = getApplication<BggApplication>()
        val statuses = mutableListOf<String>()
        if (expansion.own) statuses.add(ctx.getString(R.string.collection_status_own))
        if (expansion.previouslyOwned) statuses.add(ctx.getString(R.string.collection_status_prev_owned))
        if (expansion.forTrade) statuses.add(ctx.getString(R.string.collection_status_for_trade))
        if (expansion.wantInTrade) statuses.add(ctx.getString(R.string.collection_status_want_in_trade))
        if (expansion.wantToBuy) statuses.add(ctx.getString(R.string.collection_status_want_to_buy))
        if (expansion.wantToPlay) statuses.add(ctx.getString(R.string.collection_status_want_to_play))
        if (expansion.preOrdered) statuses.add(ctx.getString(R.string.collection_status_preordered))
        if (expansion.wishList) statuses.add(expansion.wishListPriority.asWishListPriority(ctx))
        if (expansion.numberOfPlays > 0) statuses.add(ctx.getString(R.string.played))
        if (expansion.rating > 0.0) statuses.add(ctx.getString(R.string.rated))
        if (expansion.comment.isNotBlank()) statuses.add(ctx.getString(R.string.commented))
        return statuses.formatList()
    }

    val producers = _producerType.switchMap { type ->
        when (type) {
            ProducerType.DESIGNER -> designers
            ProducerType.ARTIST -> artists
            ProducerType.PUBLISHER -> publishers
            ProducerType.CATEGORY -> categories
            ProducerType.MECHANIC -> mechanics
            ProducerType.EXPANSION -> expansions
            ProducerType.BASE_GAME -> baseGames
            else -> liveData { emit(emptyList()) }
        }
    }

    val collectionItems: LiveData<List<CollectionItem>> = gameId.switchMap {
        liveData {
            try {
                emitSource(gameCollectionRepository.loadCollectionItemsForGameFlow(it)
                    .onEach { attemptRefreshItems(it) }
                    .asLiveData()
                )
            } catch (e: Exception) {
                Timber.w(e)
                _errorMessage.setMessage(e)
            }
        }
    }

    val plays: LiveData<List<Play>> = game.switchMap { game ->
        liveData {
            try {
                game?.let {
                    emitSource(playRepository.loadPlaysByGameFlow(it.id).asLiveData())
                }
                attemptRefreshPlays()
            } catch (e: Exception) {
                Timber.w(e)
                _errorMessage.setMessage(e)
            }
        }
    }

    val playColors = _gameId.switchMap { gameId ->
        liveData {
            emitSource(gameRepository.getPlayColorsFlow(gameId).distinctUntilChanged().asLiveData())
        }
    }

    fun refreshGame() {
        gameId.value?.let {
            if (isGameRefreshing.compareAndSet(false, true)) {
                _gameIsRefreshing.value = true
                viewModelScope.launch {
                    val result = gameRepository.refreshGame(it)
                    if (result.isFailure) {
                        result.exceptionOrNull()?.let { _errorMessage.setMessage(it) }
                    } else {
                        gameRepository.loadGame(it)?.let { newGame ->
                            if (newGame.doesHeroImageNeedUpdating()) {
                                gameRepository.refreshHeroImage(newGame)
                            }
                        }
                    }
                    _gameIsRefreshing.value = false
                    isGameRefreshing.set(false)
                }
            }
        }
    }

    fun refreshItems() {
        forceItemsRefresh.set(true)
        attemptRefreshItems()
    }

    private fun attemptRefreshItems(list: List<CollectionItem>? = collectionItems.value) {
        game.value?.let {
            if (areItemsRefreshing.compareAndSet(false, true)) {
                _itemsAreRefreshing.value = true
                viewModelScope.launch {
                    if (list?.isEmpty() == true ||
                        (list != null && list.minOf { item -> item.syncTimestamp }.isOlderThan(itemsRefreshMinutes.minutes)) ||
                        forceItemsRefresh.compareAndSet(true, false)
                    ) {
                        Timber.d("Refreshing items for game $it")
                        gameCollectionRepository.refreshCollectionItems(it.id, it.subtype)?.let { _errorMessage.setMessage(it) }
                    } else {
                        Timber.d("NOT refreshing items for game $it")
                    }
                    _itemsAreRefreshing.value = false
                    areItemsRefreshing.set(false)
                }
            }
        }
    }

    fun refreshPlays() {
        forcePlaysRefresh.set(true)
        attemptRefreshPlays()
    }

    private fun attemptRefreshPlays() {
        game.value?.let {
            if (arePlaysRefreshing.compareAndSet(false, true)) {
                _playsAreRefreshing.value = true
                viewModelScope.launch {
                    val lastUpdated = it.updatedPlays
                    when {
                        lastUpdated.isOlderThan(playsFullMinutes.minutes) -> {
                            playRepository.refreshPlaysForGame(it.id)?.let { _errorMessage.setMessage(it) }
                        }
                        lastUpdated.isOlderThan(playsPartialMinutes.minutes) -> {
                            playRepository.refreshPlaysForGame(it.id, 1)?.let { _errorMessage.setMessage(it) }
                        }
                        forcePlaysRefresh.compareAndSet(true, false) -> {
                            playRepository.refreshPlaysForGame(it.id, 1)?.let { _errorMessage.setMessage(it) }
                        }
                    }
                    _playsAreRefreshing.value = false
                    arePlaysRefreshing.set(false)
                }
            }
        }
    }

    fun reload() {
        _gameId.value?.let { _gameId.value = it }
    }

    fun updateGameColors(palette: Palette?) {
        palette?.let { p ->
            game.value?.let { game ->
                viewModelScope.launch {
                    @ColorInt
                    val iconColor = p.getIconColor()
                    @ColorInt
                    val darkColor = p.getDarkColor()
                    val (winsColor, winnablePlaysColor, allPlaysColor) = p.getPlayCountColors(getApplication())
                    val modified = game.iconColor != iconColor ||
                            game.darkColor != darkColor ||
                            game.winsColor != winsColor ||
                            game.winnablePlaysColor != winnablePlaysColor ||
                            game.allPlaysColor != allPlaysColor
                    if (modified) {
                        gameRepository.updateGameColors(
                            gameId.value ?: BggContract.INVALID_ID,
                            iconColor,
                            darkColor,
                            winsColor,
                            winnablePlaysColor,
                            allPlaysColor,
                        )
                    }
                }
            }
        }
    }

    fun updateFavorite(isFavorite: Boolean) {
        viewModelScope.launch {
            gameRepository.updateFavorite(gameId.value ?: BggContract.INVALID_ID, isFavorite)
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure)
                result.exceptionOrNull()?.let { _errorMessage.setMessage(it) }
            else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID)
                        _loggedPlayResult.value = Event(it)
                }
            }
        }
    }

    fun addCollectionItem(statuses: List<String>, wishListPriority: Int) {
        viewModelScope.launch {
            gameCollectionRepository.addCollectionItem(
                gameId.value ?: BggContract.INVALID_ID,
                statuses,
                wishListPriority
            )
        }
    }

    fun createShortcut() {
        viewModelScope.launch(Dispatchers.Default) {
            val context = getApplication<BggApplication>().applicationContext
            val gameId = _gameId.value ?: BggContract.INVALID_ID
            val gameName = game.value?.name.orEmpty()
            val thumbnailUrl = game.value?.thumbnailUrl.orEmpty()
            val bitmap = imageRepository.fetchThumbnail(thumbnailUrl.ensureHttpsScheme())
            GameActivity.createShortcutInfo(context, gameId, gameName, bitmap)?.let { info ->
                ShortcutManagerCompat.requestPinShortcut(context, info, null)
            }
        }
    }
}
