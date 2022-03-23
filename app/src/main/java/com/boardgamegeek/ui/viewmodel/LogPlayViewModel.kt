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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class LogPlayViewModel(application: Application) : AndroidViewModel(application) {
    private val playRepository = PlayRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private var originalPlay: PlayEntity? = null
    private val scoreFormat = DecimalFormat("0.#########")
    private var internalIdToDelete: Long = INVALID_ID.toLong()

    private val _internalId = MutableLiveData<Long>()
    val internalId: LiveData<Long>
        get() = _internalId

    private val _play = MutableLiveData<PlayEntity>()
    val play: LiveData<PlayEntity>
        get() = _play

    private val _customPlayerSort = MutableLiveData<Boolean>()
    val customPlayerSort: LiveData<Boolean>
        get() = _customPlayerSort

    private val _gameId = MutableLiveData<Int>()

    val colors = _gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == INVALID_ID) null else gameRepository.getPlayColors(gameId))
        }
    }

    val locations = liveData {
        emit(playRepository.loadLocations(PlayDao.LocationSortBy.PLAY_COUNT))
    }

    private val _location = MutableLiveData<String>()

    private val _playersByLocation = MediatorLiveData<List<PlayerEntity>>()
    val playersByLocation: LiveData<List<PlayerEntity>>
        get() = _playersByLocation

    init {
        _playersByLocation.addSource(_location) { result ->
            result?.let { filterPlayersByLocation(location = it) }
        }
        _playersByLocation.addSource(_play) { result ->
            result?.let { filterPlayersByLocation(currentPlayers = result.players) }
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
        currentPlayers: List<PlayPlayerEntity> = play.value?.players.orEmpty(),
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
            _gameId.postValue(gameId)
            val game = gameRepository.loadGame(gameId)
            var arePlayersCustomSorted = game?.customPlayerSort ?: true
            val fetchedPlay: PlayEntity? = if (internalId == INVALID_ID.toLong()) {
                var location = ""
                var seatedPlayers: List<PlayPlayerEntity>? = null
                val lastPlayTime = prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L
                if (lastPlayTime.howManyHoursOld() < 12) {
                    location = prefs[KEY_LAST_PLAY_LOCATION, ""].orEmpty()
                    val players = prefs.getLastPlayPlayerEntities()
                    seatedPlayers = if (arePlayersCustomSorted)
                        players.map { PlayPlayerEntity(name = it.name, username = it.username) }
                    else
                        addSeats(players.map { PlayPlayerEntity(name = it.name, username = it.username) })
                }
                PlayEntity(
                    rawDate = PlayEntity.currentDate(),
                    gameId = gameId,
                    gameName = gameName,
                    location = location,
                    _players = seatedPlayers,
                )
            } else {
                val play = playRepository.loadPlay(internalId)
                if (play != null && !arePlayersCustomSorted) {
                    arePlayersCustomSorted = play.arePlayersCustomSorted()
                }
                play
            }
            fetchedPlay?.let {
                if (originalPlay == null) originalPlay = it.copy()
                _location.postValue(it.location)
                _customPlayerSort.postValue(arePlayersCustomSorted)
                val pp = if (isRequestingToEndPlay) {
                    it.copy(
                        length = if (it.startTime > 0) it.startTime.howManyMinutesOld() else 0,
                        startTime = 0,
                    )
                } else it

                when {
                    isRequestingRematch -> {
                        val rematch = PlayEntity(
                            rawDate = PlayEntity.currentDate(),
                            gameId = pp.gameId,
                            gameName = pp.gameName,
                            location = pp.location,
                            noWinStats = pp.noWinStats,
                            _players = if (customPlayerSort.value == true) pp.players.map { player ->
                                player.copy(startingPosition = "", score = "", rating = 0.0, isWin = false, isNew = false)
                            } else pp.players.map { player ->
                                player.copy(score = "", rating = 0.0, isWin = false, isNew = false)
                            },
                        )
                        internalIdToDelete = INVALID_ID.toLong()
                        _internalId.value = INVALID_ID.toLong()
                        _play.postValue(rematch)
                    }
                    isChangingGame -> {
                        internalIdToDelete = internalId
                        _internalId.value = INVALID_ID.toLong()
                        _play.postValue(pp.copy(playId = INVALID_ID, gameId = gameId, gameName = gameName))
                    }
                    else -> {
                        internalIdToDelete = INVALID_ID.toLong()
                        _internalId.value = internalId
                        _play.postValue(pp)
                    }
                }
            }
        }
    }

    fun updateDate(dateInMillis: Long) {
        if (play.value?.dateInMillis != dateInMillis) {
            _play.value = play.value?.copy(rawDate = PlayEntity.millisToRawDate(dateInMillis))
        }
    }

    fun updateLocation(location: String) {
        if (play.value?.location != location) {
            _location.postValue(location)
            _play.value = play.value?.copy(location = location)
        }
    }

    fun updateLength(length: Int) {
        if (play.value?.length != length) {
            _play.value = play.value?.copy(length = length)
        }
    }

    fun startTimer() {
        logTimer("Start")
        _play.value?.let {
            _play.value = it.copy(
                length = 0,
                startTime = System.currentTimeMillis(),
            )
        }
    }

    fun resumeTimer() {
        logTimer("Resume")
        _play.value?.let {
            _play.value = it.copy(
                length = 0,
                startTime = System.currentTimeMillis() - it.length * DateUtils.MINUTE_IN_MILLIS,
            )
        }
    }

    fun endTimer() {
        logTimer("Off")
        _play.value?.let {
            _play.value = it.copy(
                startTime = 0,
                length = if (it.startTime > 0) it.startTime.howManyMinutesOld() else 0,
            )
        }
    }

    private fun logTimer(state: String) {
        firebaseAnalytics.logEvent("LogPlayTimer") {
            param("State", state)
        }
    }

    fun updateQuantity(quantity: Int?) {
        val q = quantity ?: 1
        if (play.value?.quantity != q) {
            _play.value = play.value?.copy(quantity = q)
        }
    }

    fun updateIncomplete(checked: Boolean) {
        if (play.value?.incomplete != checked) {
            _play.value = play.value?.copy(incomplete = checked)
        }
    }

    fun updateNoWinStats(checked: Boolean) {
        if (play.value?.noWinStats != checked) {
            _play.value = play.value?.copy(noWinStats = checked)
        }
    }

    fun updateComments(comments: String) {
        if (play.value?.comments != comments) {
            _play.value = play.value?.copy(comments = comments)
        }
    }

    fun addPlayers(players: List<PlayerEntity>) {
        play.value?.let { it ->
            val playPlayers = it.players.toMutableList()
            playPlayers.addAll(players.mapIndexed { index, pe ->
                if (customPlayerSort.value != true) {
                    PlayPlayerEntity(name = pe.name, username = pe.username, startingPosition = (index + it.players.size + 1).toString())
                } else PlayPlayerEntity(name = pe.name, username = pe.username)
            })
            if (customPlayerSort.value != true) playPlayers.sortBy { it.seat }
            _play.value = it.copy(_players = playPlayers)
        }
    }

    fun editPlayer(player: PlayPlayerEntity = PlayPlayerEntity(), position: Int? = null) {
        play.value?.let {
            val players = it.players.toMutableList()
            if (position in it.players.indices) {
                position?.let { pos -> players[pos] = player }
            } else {
                players += player
            }
            if (customPlayerSort.value != true) players.sortBy { player -> player.seat }
            _play.value = it.copy(_players = players)
        }
    }

    fun addPlayer(player: PlayPlayerEntity = PlayPlayerEntity()) {
        play.value?.let {
            var players = it.players.toMutableList()
            if (customPlayerSort.value != true) {
                players.add(player.seat - 1, player)
                players = addSeats(players).toMutableList()
                players.sortBy { player -> player.seat }
            } else {
                players += player
            }
            _play.value = it.copy(_players = players)
        }
    }

    fun removePlayer(player: PlayPlayerEntity) {
        play.value?.let {
            if (it.players.isNotEmpty()) {
                val players = it.players.toMutableList()
                players.remove(player)
                val seatedPlayers = if (customPlayerSort.value != true) {
                    addSeats(players)
                } else players
                _play.value = it.copy(_players = seatedPlayers)
            }
        }
    }

    fun isDirty(): Boolean {
        return play.value != originalPlay
    }

    fun clearPositions() {
        modifyPlayers { it.copy(startingPosition = "") }
    }

    fun pickStartPlayer(playerIndex: Int) {
        play.value?.let {
            val players = addSeats(it.players, playerIndex)
            _play.value = it.copy(_players = players)
        }
    }

    fun pickRandomStartPlayer() {
        play.value?.let {
            val players = addSeats(it.players, it.players.indices.random()) // TODO - if this is "0", need to animate something
            _play.value = it.copy(_players = players)
        }
    }

    /**
     * Return the list of players, with seats assigned from 1 to N, starting at the specified index and re-sorted to begin at seat 1.
     */
    private fun addSeats(players: List<PlayPlayerEntity>, playerIndex: Int = 0): List<PlayPlayerEntity> {
        val newPlayers = mutableListOf<PlayPlayerEntity>()
        for (index in players.indices) {
            newPlayers += players[index].copy(startingPosition = ((index - playerIndex + players.size) % players.size + 1).toString())
        }
        return newPlayers.sortedBy { player -> player.seat }
    }

    fun randomizePlayerOrder() {
        play.value?.let {
            if (it.players.isNotEmpty()) {
                val sortedPlayers = mutableListOf<PlayPlayerEntity>()
                val players = it.players.shuffled()
                for (index in it.players.indices) {
                    sortedPlayers += players[index].copy(startingPosition = (index + 1).toString())
                }
                _play.value = it.copy(_players = sortedPlayers)
            }
        }
    }

    fun reorderPlayers(fromSeat: Int, toSeat: Int) {
        play.value?.let {
            if (it.players.isNotEmpty() && customPlayerSort.value != true) {
                val players = it.players.toMutableList()
                getPlayerAtSeat(players, fromSeat)?.let { movingPlayer ->
                    players.remove(movingPlayer)
                    players.add(toSeat - 1, movingPlayer)
                    _play.value = it.copy(_players = addSeats(players))
                }
            }
        }
    }

    private fun getPlayerAtSeat(players: List<PlayPlayerEntity>, seat: Int): PlayPlayerEntity? {
        return players.find { it.seat == seat }
    }

    fun assignColors(clearExisting: Boolean = false) {
        _play.value?.let { play ->
            viewModelScope.launch(Dispatchers.Default) {
                val existingPlayers = if (clearExisting) {
                    play.players.map { it.copy(color = "") }.toMutableList()
                } else {
                    play.players.toMutableList()
                }
                val results = PlayerColorAssigner(getApplication(), play.gameId, existingPlayers).execute()
                val players = mutableListOf<PlayPlayerEntity>()
                existingPlayers.forEach { ppe ->
                    val result = if (ppe.username.isEmpty()) {
                        results.find { it.type == PlayerColorAssigner.PlayerType.NON_USER && it.name == ppe.name }
                    } else {
                        results.find { it.type == PlayerColorAssigner.PlayerType.USER && it.name == ppe.username }
                    }
                    players += if (result == null) ppe.copy() else ppe.copy(color = result.color)
                }
                _play.postValue(play.copy(_players = players))
            }
        }
    }

    fun addColorToPlayer(playerIndex: Int, color: String) {
        modifyPlayerAtIndex(playerIndex) { it.copy(color = color) }
    }

    fun addScoreToPlayer(playerIndex: Int, score: Double) {
        play.value?.let {
            val players = it.players.mapIndexed { i, player ->
                if (i == playerIndex) player.copy(score = scoreFormat.format(score)) else player.copy()
            }
            val highScore = players.maxOfOrNull { player -> player.numericScore ?: -Double.MAX_VALUE }
            _play.value = it.copy(_players = players.map { ppe ->
                ppe.copy(isWin = (ppe.numericScore == highScore))
            })
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
        play.value?.let {
            _play.value = it.copy(_players = it.players.map { players -> indexFunction(players) })
        }
    }

    private fun modifyPlayerAtIndex(playerIndex: Int, indexFunction: (PlayPlayerEntity) -> PlayPlayerEntity) {
        play.value?.let {
            val players = it.players.mapIndexed { i, player ->
                if (i == playerIndex) indexFunction(player) else player.copy()
            }
            _play.value = it.copy(_players = players)
        }
    }

    fun logPlay() {
        viewModelScope.launch {
            play.value?.let {
                val now = System.currentTimeMillis()

                if (!it.isSynced &&
                    (it.dateInMillis.isToday() || (now - it.length * DateUtils.MINUTE_IN_MILLIS).isToday())
                ) {
                    prefs[KEY_LAST_PLAY_TIME] = now
                    prefs[KEY_LAST_PLAY_LOCATION] = it.location
                    prefs.putLastPlayPlayerEntities(it.players)
                }

                save(
                    it.copy(
                        updateTimestamp = now,
                        deleteTimestamp = 0,
                        dirtyTimestamp = now,
                    )
                )
                if (internalIdToDelete != INVALID_ID.toLong()) {
                    playRepository.markAsDeleted(internalIdToDelete)
                }
                triggerUpload()
            }
        }
    }

    fun saveDraft() {
        play.value?.let {
            save(
                it.copy(
                    dirtyTimestamp = System.currentTimeMillis(),
                    deleteTimestamp = 0,
                )
            )
        }
    }

    fun deletePlay() {
        play.value?.let {
            save(
                it.copy(
                    updateTimestamp = 0,
                    deleteTimestamp = System.currentTimeMillis(),
                    dirtyTimestamp = 0,
                )
            )
            triggerUpload()
        }
    }

    private fun save(play: PlayEntity) {
        viewModelScope.launch {
            _internalId.postValue(playRepository.save(play, _internalId.value ?: INVALID_ID.toLong()))
        }
    }

    private fun triggerUpload() {
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
    }
}
