package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.lifecycle.*
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.PlayerColorAssigner
import com.boardgamegeek.service.SyncService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LogPlayViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private var originalPlay: PlayEntity? = null
    private val scoreFormat = DecimalFormat("0.#########")
    private var internalIdToDelete: Long = INVALID_ID.toLong()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _internalId = MutableLiveData<Long>()
    val internalId: LiveData<Long>
        get() = _internalId

    private var _playId: Int = INVALID_ID

    private val _game = MutableLiveData<Pair<Int, String>>()

    private val _dateInMillis = MutableLiveData<Long>()
    val dateInMillis: LiveData<Long>
        get() = _dateInMillis

    private val _location = MutableLiveData<String>()
    val location: LiveData<String>
        get() = _location

    private val _length = MutableLiveData<Int>()
    val length: LiveData<Int>
        get() = _length

    private val _quantity = MutableLiveData<Int>()
    val quantity: LiveData<Int>
        get() = _quantity

    private val _incomplete = MutableLiveData<Boolean>()
    val incomplete: LiveData<Boolean>
        get() = _incomplete

    private val _doNotCountWinStats = MutableLiveData<Boolean>()
    val doNotCountWinStats: LiveData<Boolean>
        get() = _doNotCountWinStats

    private val _comments = MutableLiveData<String>()
    val comments: LiveData<String>
        get() = _comments

    private val _players = MutableLiveData<List<PlayPlayerEntity>>()
    val players: LiveData<List<PlayPlayerEntity>>
        get() = _players

    private val _startTime = MutableLiveData<Long>()
    val startTime: LiveData<Long>
        get() = _startTime

    private val _customPlayerSort = MutableLiveData<Boolean>()
    val customPlayerSort: LiveData<Boolean>
        get() = _customPlayerSort

    val colors = _game.switchMap { game ->
        liveData {
            emit(if (game.first == INVALID_ID) null else gameRepository.getPlayColors(game.first))
        }
    }

    val locations = liveData {
        emit(playRepository.loadLocations(PlayDao.LocationSortBy.PLAY_COUNT))
    }

    private val _playersByLocation = MediatorLiveData<List<PlayerEntity>>()
    val playersByLocation: LiveData<List<PlayerEntity>>
        get() = _playersByLocation

    init {
        _playersByLocation.addSource(_location) { result ->
            result?.let { filterPlayersByLocation(location = it) }
        }
        _playersByLocation.addSource(_players) { result ->
            result?.let { filterPlayersByLocation(currentPlayers = it) }
        }
    }

    fun shouldCustomSort(sort: Boolean) {
        _customPlayerSort.value?.let {
            if (it != sort) {
                _customPlayerSort.value = sort
                if (!sort) pickStartPlayer(0)
            }
        }
    }

    private fun filterPlayersByLocation(
        location: String = _location.value.orEmpty(),
        currentPlayers: List<PlayPlayerEntity> = players.value.orEmpty(),
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val allPlayers = playRepository.loadPlayersByLocation(location)
            val filteredPlayers = allPlayers.filter {
                currentPlayers.find { p -> p.id == it.id } == null
            }
            _playersByLocation.postValue(filteredPlayers)
        }
    }

    fun loadPlay(
        internalId: Long,
        gameId: Int,
        gameName: String,
        isRequestingToEndPlay: Boolean,
        isRequestingRematch: Boolean,
        isChangingGame: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.postValue(true)

            _game.postValue(gameId to gameName)
            val game = gameRepository.loadGame(gameId)
            val gameSupportsCustomSort = game?.customPlayerSort ?: true

            if (internalId == INVALID_ID.toLong()) {
                _internalId.value = INVALID_ID.toLong()
                _dateInMillis.postValue(Calendar.getInstance().timeInMillis)
                val recentlyPlayed = (prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L).howManyHoursOld() < 12
                if (recentlyPlayed) {
                    _location.postValue(prefs[KEY_LAST_PLAY_LOCATION, ""].orEmpty())
                    val players = prefs.getLastPlayPlayerEntities()
                    val seatedPlayers: List<PlayPlayerEntity> = if (gameSupportsCustomSort)
                        players.map { PlayPlayerEntity(name = it.name, username = it.username) }
                    else
                        assignSeats(players.map { PlayPlayerEntity(name = it.name, username = it.username) })
                    _players.postValue(seatedPlayers)
                }
            } else {
                playRepository.loadPlay(internalId)?.let { play ->
                    if (originalPlay == null) originalPlay = play.copy() // TODO - confirm this works
                    internalIdToDelete = if (isChangingGame) internalId else INVALID_ID.toLong()
                    _internalId.value = (if (isChangingGame || isRequestingRematch) INVALID_ID.toLong() else internalId)
                    if (!isChangingGame && !isRequestingRematch) _playId = play.playId
                    if (isRequestingRematch) {
                        _dateInMillis.postValue(Calendar.getInstance().timeInMillis)
                        val players = if (gameSupportsCustomSort) play.players.map { player ->
                            player.copy(score = "", rating = 0.0, isWin = false, isNew = false, startingPosition = "")
                        } else play.players.map { player ->
                            player.copy(score = "", rating = 0.0, isWin = false, isNew = false)
                        }
                        _players.postValue(players)
                    } else {
                        _dateInMillis.postValue(play.dateInMillis)
                        _quantity.postValue(play.quantity)
                        _incomplete.postValue(play.incomplete)
                        _doNotCountWinStats.postValue(play.noWinStats)
                        _comments.postValue(play.comments)
                        _players.postValue(play.players)
                    }
                    _location.postValue(play.location)
                    when {
                        isRequestingToEndPlay -> {
                            _length.postValue(play.length + if (play.startTime > 0) play.startTime.howManyMinutesOld() else 0)
                            _startTime.postValue(0L)
                        }
                        isRequestingRematch -> {
                            _length.postValue(0)
                            _startTime.postValue(0L)
                        }
                        else -> {
                            _length.postValue(play.length)
                            _startTime.postValue(play.startTime)
                        }
                    }
                    _customPlayerSort.postValue(gameSupportsCustomSort || play.arePlayersCustomSorted()) // TODO - confirm this works
                }
            }

            _isLoading.postValue(false)
        }
    }

    fun updateDate(dateInMillis: Long) {
        if (_dateInMillis.value != dateInMillis) {
            _dateInMillis.value = dateInMillis
        }
    }

    fun updateLocation(location: String) {
        if (_location.value != location) {
            _location.postValue(location)
        }
    }

    fun updateLength(length: Int) {
        if (_length.value != length) {
            _length.value = length
        }
    }

    fun startTimer() {
        logTimer("Start")
        _startTime.value = System.currentTimeMillis()
        _length.value = 0
    }

    fun resumeTimer() {
        logTimer("Resume")
        val minutes = _length.value ?: 0
        _startTime.value = System.currentTimeMillis() - minutes * DateUtils.MINUTE_IN_MILLIS
        _length.value = 0
    }

    fun endTimer() {
        logTimer("Off")
        val s = _startTime.value ?: 0L
        _length.value = if (s > 0) s.howManyMinutesOld() else 0
        _startTime.value = 0
    }

    private fun logTimer(state: String) {
        firebaseAnalytics.logEvent("LogPlayTimer") {
            param("State", state)
        }
    }

    fun updateQuantity(quantity: Int?) {
        quantity?.let {
            if (_quantity.value != it) {
                _quantity.value = it
            }
        }
    }

    fun updateIncomplete(isIncomplete: Boolean) {
        if (_incomplete.value != isIncomplete) {
            _incomplete.value = isIncomplete
        }
    }

    fun updateNoWinStats(doNotCountWinStats: Boolean) {
        if (_doNotCountWinStats.value != doNotCountWinStats) {
            _doNotCountWinStats.value = doNotCountWinStats
        }
    }

    fun updateComments(comments: String) {
        if (_comments.value != comments) {
            _comments.value = comments
        }
    }

    fun isDirty(): Boolean {
        return buildPlayEntity() != originalPlay
    }

    fun addPlayers(players: List<PlayerEntity>) {
        val newPlayers = _players.value.orEmpty().toMutableList()
        val initialPosition = _players.value?.size ?: 0
        newPlayers += players.mapIndexed { index, entity ->
            if (customPlayerSort.value != true) {
                PlayPlayerEntity(name = entity.name, username = entity.username, startingPosition = (initialPosition + 1 + index).toString())
            } else PlayPlayerEntity(name = entity.name, username = entity.username)
        }
        if (customPlayerSort.value != true) newPlayers.sortBy { it.seat }
        _players.value = newPlayers
    }

    fun editPlayer(player: PlayPlayerEntity = PlayPlayerEntity(), position: Int? = null) {
        players.value?.let {
            val players = it.toMutableList()
            if (position in it.indices) {
                position?.let { pos -> players[pos] = player }
            } else {
                players += player
            }
            if (customPlayerSort.value != true) players.sortBy { player -> player.seat }
            _players.value = players
        }
    }

    fun addPlayer(player: PlayPlayerEntity = PlayPlayerEntity()) {
        val players = _players.value.orEmpty().toMutableList()
        val newPlayers = if (customPlayerSort.value != true) {
            assignSeats(players + player).toMutableList()
        } else {
            players + player
        }
        _players.value = newPlayers
    }

    fun removePlayer(player: PlayPlayerEntity) {
        players.value?.let {
            if (it.isNotEmpty()) {
                val newPlayers = it.toMutableList()
                newPlayers.remove(player)
                val seatedPlayers = if (customPlayerSort.value != true) {
                    assignSeats(newPlayers)
                } else newPlayers
                _players.value = seatedPlayers
            }
        }
    }

    fun clearPositions() {
        modifyPlayers { it.copy(startingPosition = "") }
    }

    fun pickStartPlayer(playerIndex: Int) {
        players.value?.let {
            val players = assignSeats(it, playerIndex)
            _players.value = players
        }
    }

    private val randomDelayMs = 300L

    fun randomizeStartPlayer() {
        players.value?.let {
            viewModelScope.launch {
                when (it.size) {
                    0 -> {} // nothing to do
                    1 -> _players.postValue(assignSeats(it))
                    else -> {
                        val targetIndex = it.indices.random() + it.size + if (it.size < 7) it.size else 0
                        for (playerIndex in 0..targetIndex) {
                            _players.postValue(assignSeats(it, playerIndex))
                            delay(randomDelayMs)
                        }
                    }
                }
            }
        }
    }

    fun randomizePlayerOrder() {
        players.value?.let {
            viewModelScope.launch {
                when (it.size) {
                    0 -> {} // nothing to do
                    1 -> _players.postValue(assignSeats(it))
                    else -> {
                        for (i in 0..6) {
                            _players.postValue(assignSeats(it.shuffled()))
                            delay(randomDelayMs)
                        }
                    }
                }
            }
        }
    }

    fun reorderPlayers(fromSeat: Int, toSeat: Int) {
        players.value?.let {
            if (it.isNotEmpty() && customPlayerSort.value != true) {
                val newPlayers = it.toMutableList()
                newPlayers.find { p -> p.seat == fromSeat }?.let { movingPlayer ->
                    newPlayers.remove(movingPlayer)
                    newPlayers.add(toSeat - 1, movingPlayer)
                    _players.value = assignSeats(newPlayers)
                }
            }
        }
    }

    /**
     * Return the list of players, with seats assigned from 1 to N, starting at the specified index and re-sorted to begin at seat 1.
     */
    private fun assignSeats(players: List<PlayPlayerEntity>, playerIndex: Int = 0): List<PlayPlayerEntity> {
        return players.mapIndexed { index, playPlayerEntity ->
            playPlayerEntity.copy(startingPosition = ((index - playerIndex).mod(players.size) + 1).toString())
        }.sortedBy { player -> player.seat }
    }

    fun assignColors(clearExisting: Boolean = false) {
        players.value?.let {
            viewModelScope.launch(Dispatchers.Default) {
                val existingPlayers = if (clearExisting) it.map { it.copy(color = "") } else it
                val results = PlayerColorAssigner(
                    _game.value?.first ?: INVALID_ID,
                    existingPlayers,
                    gameRepository,
                    playRepository,
                ).execute()
                val newPlayers = mutableListOf<PlayPlayerEntity>()
                existingPlayers.forEach { ppe ->
                    val result = if (ppe.username.isEmpty()) {
                        results.find { it.type == PlayerColorAssigner.PlayerType.NON_USER && it.name == ppe.name }
                    } else {
                        results.find { it.type == PlayerColorAssigner.PlayerType.USER && it.name == ppe.username }
                    }
                    newPlayers += if (result == null) ppe.copy() else ppe.copy(color = result.color)
                }
                _players.postValue(newPlayers)
            }
        }
    }

    fun addColorToPlayer(playerIndex: Int, color: String) {
        modifyPlayerAtIndex(playerIndex) { it.copy(color = color) }
    }

    fun addScoreToPlayer(playerIndex: Int, score: Double) {
        players.value?.let {
            val newPlayers = it.mapIndexed { i, player ->
                if (i == playerIndex) player.copy(score = scoreFormat.format(score)) else player.copy()
            }
            val highScore = newPlayers.maxOfOrNull { player -> player.numericScore ?: -Double.MAX_VALUE }
            _players.value = newPlayers.map { ppe ->
                ppe.copy(isWin = (ppe.numericScore == highScore))
            }
        }
    }

    fun addRatingToPlayer(playerIndex: Int, rating: Double) {
        modifyPlayerAtIndex(playerIndex) { it.copy(rating = rating) }
    }

    fun win(isWin: Boolean, playerIndex: Int) {
        modifyPlayerAtIndex(playerIndex) { it.copy(isWin = isWin) }
    }

    fun new(isNew: Boolean, playerIndex: Int) {
        modifyPlayerAtIndex(playerIndex) { it.copy(isNew = isNew) }
    }

    private fun modifyPlayers(indexFunction: (PlayPlayerEntity) -> PlayPlayerEntity) {
        players.value?.let {
            _players.value = it.map { players -> indexFunction(players) }
        }
    }

    private fun modifyPlayerAtIndex(playerIndex: Int, indexFunction: (PlayPlayerEntity) -> PlayPlayerEntity) {
        players.value?.let {
            _players.value = it.mapIndexed { i, player ->
                if (i == playerIndex) indexFunction(player) else player.copy()
            }
        }
    }

    fun logPlay() {
        viewModelScope.launch {
            val play = buildPlayEntity(updateTimestamp = System.currentTimeMillis(), dirtyTimestamp = System.currentTimeMillis())
            val internalId = playRepository.save(play, _internalId.value ?: INVALID_ID.toLong())
            _internalId.postValue(internalId)
            if (internalIdToDelete != INVALID_ID.toLong()) {
                playRepository.markAsDeleted(internalIdToDelete)
            }
            playRepository.enqueueUploadRequest(play.copy(internalId = internalId))
        }
    }

    fun saveDraft() {
        save(buildPlayEntity(dirtyTimestamp = System.currentTimeMillis()))
    }

    fun deletePlay() {
        save(buildPlayEntity(deleteTimestamp = System.currentTimeMillis()))
        triggerUpload()
    }

    private fun buildPlayEntity(
        dirtyTimestamp: Long = 0L,
        updateTimestamp: Long = 0L,
        deleteTimestamp: Long = 0L,
    ) = PlayEntity(
        internalId = _internalId.value ?: INVALID_ID.toLong(),
        playId = _playId,
        rawDate = _dateInMillis.value?.let { PlayEntity.millisToRawDate(it) } ?: PlayEntity.currentDate(),
        gameId = _game.value?.first ?: INVALID_ID,
        gameName = _game.value?.second.orEmpty(),
        location = _location.value.orEmpty(),
        length = _length.value ?: 0, // TODO allow nulls
        startTime = _startTime.value ?: 0L,
        quantity = _quantity.value ?: 1,
        incomplete = _incomplete.value ?: false,
        noWinStats = _doNotCountWinStats.value ?: false,
        comments = _comments.value.orEmpty(),
        _players = players.value.orEmpty(),
        dirtyTimestamp = dirtyTimestamp,
        updateTimestamp = updateTimestamp,
        deleteTimestamp = deleteTimestamp,
    )

    private fun save(play: PlayEntity) {
        viewModelScope.launch {
            _internalId.postValue(playRepository.save(play, _internalId.value ?: INVALID_ID.toLong()))
        }
    }

    private fun triggerUpload() {
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
    }
}
