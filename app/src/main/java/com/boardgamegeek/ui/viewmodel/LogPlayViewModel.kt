package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.builder.PlayBuilder
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.INVALID_ID
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.PlayerColorAssigner
import com.boardgamegeek.service.SyncService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*
import kotlin.io.use

class LogPlayViewModel(application: Application) : AndroidViewModel(application) {
    private val playPersister = PlayPersister(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private var originalPlay: Play? = null
    private val scoreFormat = DecimalFormat("0.#########")
    private var internalIdToDelete: Long = INVALID_ID.toLong()

    private val _internalId = MutableLiveData<Long>()
    val internalId: LiveData<Long>
        get() = _internalId

    private val _play: MutableLiveData<Play> = MutableLiveData<Play>()
    val play: LiveData<Play>
        get() = _play

    private val _gameId = MutableLiveData<Int>()

    val colors: LiveData<List<String>> = _gameId.switchMap {
        gameRepository.getPlayColors(it)
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

    private fun filterPlayersByLocation(
            location: String = _location.value.orEmpty(),
            currentPlayers: List<Player> = play.value?.players.orEmpty(),
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val allPlayers = playRepository.loadPlayersByLocation(location)
            val filteredPlayers = allPlayers.filter {
                currentPlayers.find { p -> p.id == it.id } == null
            }
            _playersByLocation.postValue(filteredPlayers)
        }
    }

    fun loadPlay(internalId: Long, gameId: Int, gameName: String, isRequestingToEndPlay: Boolean, isRequestingRematch: Boolean, isChangingGame: Boolean) {
        _gameId.postValue(gameId)
        val p: Play? = if (internalId == INVALID_ID.toLong()) {
            Play(gameId, gameName).apply {
                val lastPlay = prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L
                if (lastPlay.howManyHoursOld() < 12) {
                    this.location = prefs[KEY_LAST_PLAY_LOCATION, ""].orEmpty()
                    this.players.addAll(prefs.getLastPlayPlayers())
                    pickStartPlayer(0)// TODO - only choose if game is auto-sortable
                }
            }
        } else {
            loadPlay(internalId)
        }
        p?.let {
            if (originalPlay == null) originalPlay = it.copy() // TODO make sure the compares players correctly
            _location.postValue(it.location.orEmpty())
            if (isRequestingToEndPlay) {
                it.length = if (it.startTime > 0) {
                    it.startTime.howManyMinutesOld()
                } else {
                    0
                }
                it.startTime = 0
            }

            when {
                isRequestingRematch -> {
                    val rematch = Play(
                            gameId = it.gameId,
                            gameName = it.gameName,
                            location = it.location,
                            noWinStats = it.noWinStats,
                    )
                    for (player in it.players) {
                        val rematchPlayer = player.copy(score = "", rating = 0.0, isWin = false, isNew = false)
                        if (it.arePlayersCustomSorted()) {
                            rematchPlayer.startingPosition = ""
                        }
                        rematch.addPlayer(rematchPlayer)
                    }
                    internalIdToDelete = INVALID_ID.toLong()
                    _internalId.value = INVALID_ID.toLong()
                    _play.postValue(rematch)
                }
                isChangingGame -> {
                    internalIdToDelete = internalId
                    _internalId.value = INVALID_ID.toLong()
                    _play.postValue(it.copy(playId = INVALID_ID, gameId = gameId, gameName = gameName))
                }
                else -> {
                    internalIdToDelete = INVALID_ID.toLong()
                    _internalId.value = internalId
                    _play.postValue(it)
                }
            }
        }
    }

    private fun loadPlay(internalId: Long): Play? {
        var p: Play? = null
        val cursor = getApplication<BggApplication>().contentResolver.query(BggContract.Plays.buildPlayUri(internalId), PlayBuilder.PLAY_PROJECTION, null, null, null)
        cursor?.use { cursor ->
            cursor.moveToFirst()
            // TODO don't user PlayBuilder
            p = PlayBuilder.fromCursor(cursor)
            val cursor2 = getApplication<BggApplication>().contentResolver.query(BggContract.Plays.buildPlayerUri(internalId), PlayBuilder.PLAYER_PROJECTION, null, null, null)
            cursor2?.use { c2 ->
                PlayBuilder.addPlayers(c2, p!!)
            }
        }
        return p
    }

    fun updateDate(year: Int, month: Int, day: Int) {
        val c = Calendar.getInstance()
        c[Calendar.DAY_OF_MONTH] = day
        c[Calendar.MONTH] = month
        c[Calendar.YEAR] = year
        if (play.value?.dateInMillis != c.timeInMillis) {
            _play.value = play.value?.copy(dateInMillis = c.timeInMillis)
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
            it.length = 0
            it.startTime = System.currentTimeMillis()
            _play.value = it.copy()
        }
    }

    fun resumeTimer() {
        logTimer("Resume")
        _play.value?.let {
            it.startTime = System.currentTimeMillis() - it.length * DateUtils.MINUTE_IN_MILLIS
            it.length = 0
            _play.value = it.copy()
        }
    }

    fun endTimer() {
        logTimer("Off")
        _play.value?.let {
            it.length = if (it.startTime > 0) {
                it.startTime.howManyMinutesOld()
            } else {
                0
            }
            it.startTime = 0
            _play.value = it.copy()
        }
    }

    private fun logTimer(state: String) {
        firebaseAnalytics.logEvent("LogPlayTimer") {
            param("State", state)
        }
    }

    fun updateQuantity(quantity: Int?) {
        val q = quantity ?: Play.QUANTITY_DEFAULT
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

    fun addPlayers(players: List<PlayerEntity>, resort: Boolean) {
        play.value?.deepCopy()?.let { play ->
            play.players.addAll(players.map { Player(name = it.name, username = it.username) })
            if (resort) {
                val playerCount = play.players.size
                for (i in 0 until playerCount) {
                    play.players[i].seat = (i + playerCount) % playerCount + 1
                }
                play.players.sortBy { player -> player.seat }
            }
            _play.value = play
        }
    }

    fun editPlayer(player: Player = Player(), position: Int? = null, resort: Boolean) {
        play.value?.copy()?.let {
            if (position in it.players.indices) {
                position?.let { p -> it.players[p] = player }
            } else {
                it.players.add(player)
            }
            if (resort) it.players.sortBy { player -> player.seat }
            _play.value = it
        }
    }

    fun addPlayer(player: Player = Player(), resort: Boolean) {
        play.value?.deepCopy()?.let {
            if (resort) {
                if (player.seat == Player.SEAT_UNKNOWN) {
                    player.seat = it.players.size + 1
                } else {
                    it.players.filter { p -> p.seat >= player.seat }.forEach { p -> p.seat = p.seat + 1 }
                }
            }
            it.players.add(player)
            if (resort) it.players.sortBy { player -> player.seat }
            _play.value = it
        }
    }

    fun removePlayer(player: Player, resort: Boolean) {
        play.value?.deepCopy()?.let {
            if (it.players.size > 0) {
                if (resort && !it.arePlayersCustomSorted()) {
                    for (i in player.seat until it.players.size) {
                        it.getPlayerAtSeat(i + 1)?.seat = i
                    }
                }
                it.players.remove(player)
                _play.value = it
            }
        }
    }

    fun isDirty(): Boolean {
        return play.value != originalPlay
    }

    fun clearPositions() {
        play.value?.deepCopy()?.let {
            it.players.forEach { player -> player.startingPosition = "" }
            _play.value = it
        }
    }

    fun pickStartPlayer(index: Int) {
        play.value?.deepCopy()?.let {
            val playerCount = it.players.size
            for (i in 0 until playerCount) {
                it.players[i].seat = (i - index + playerCount) % playerCount + 1
            }
            it.players.sortBy { player -> player.seat }
            _play.value = it
        }
    }

    fun pickRandomStartPlayer() {
        play.value?.deepCopy()?.let {
            val startPlayerIndex = (0 until it.getPlayerCount()).random()
            val playerCount = it.players.size
            for (i in 0 until playerCount) {
                it.players[i].seat = (i - startPlayerIndex + playerCount) % playerCount + 1
            }
            it.players.sortBy { player -> player.seat }
            _play.value = it
        }
    }

    fun randomizePlayerOrder() {
        play.value?.deepCopy()?.let {
            if (it.players.size > 0) {
                it.players.shuffle()
                for (i in 0 until it.players.size) {
                    it.players[i].seat = i + 1
                }
                _play.value = it
            }
        }
    }

    fun reorderPlayers(fromSeat: Int, toSeat: Int) {
        play.value?.deepCopy()?.let {
            if (it.players.size > 0 && !it.arePlayersCustomSorted()) {
                it.getPlayerAtSeat(fromSeat)?.let { player ->
                    player.seat = Player.SEAT_UNKNOWN
                    if (fromSeat > toSeat) {
                        for (i in fromSeat - 1 downTo toSeat) {
                            it.getPlayerAtSeat(i)?.seat = i + 1
                        }
                    } else {
                        for (i in fromSeat + 1..toSeat) {
                            it.getPlayerAtSeat(i)?.seat = i - 1
                        }
                    }
                    player.seat = toSeat
                    it.players.sortBy { player -> player.seat }
                    _play.value = it
                }
            }
        }
    }

    fun assignColors(clearExisting: Boolean = false) {
        _play.value?.copy()?.let {
            getApplication<BggApplication>().appExecutors.diskIO.execute {
                if (clearExisting) it.players.forEach { p -> p.color = "" }
                val results = PlayerColorAssigner(getApplication(), it).execute()
                for (pr in results) {
                    when (pr.type) {
                        PlayerColorAssigner.PlayerType.USER -> it.players.find { player -> player.username == pr.name }?.color = pr.color
                        PlayerColorAssigner.PlayerType.NON_USER -> it.players.find { player -> player.username.isEmpty() && player.name == pr.name }?.color = pr.color
                    }
                }
            }
            _play.value = it
        }
    }

    fun addColorToPlayer(playerIndex: Int, color: String) {
        play.value?.copy()?.let {
            it.players[playerIndex].color = color
            _play.value = it
        }
    }

    fun addScoreToPlayer(playerIndex: Int, score: Double) {
        play.value?.deepCopy()?.let {
            it.players[playerIndex].score = scoreFormat.format(score)
            val highScore = it.players.maxOf { player ->
                player.score.toDoubleOrNull() ?: -Double.MAX_VALUE
            }
            for (p in it.players) {
                p.isWin = (p.score.toDoubleOrNull() ?: Double.NaN) == highScore
            }
            _play.value = it
        }
    }

    fun addRatingToPlayer(playerIndex: Int, rating: Double) {
        play.value?.deepCopy()?.let {
            it.players[playerIndex].rating = rating
            _play.value = it
        }
    }

    fun win(isWin: Boolean, playerIndex: Int) {
        play.value?.deepCopy()?.let {
            it.players[playerIndex].isWin = isWin
            _play.value = it
        }
    }

    fun new(isNew: Boolean, playerIndex: Int) {
        play.value?.deepCopy()?.let {
            it.players[playerIndex].isNew = isNew
            _play.value = it
        }
    }

    fun logPlay() {
        play.value?.let {
            val now = System.currentTimeMillis()

            if (!it.isSynced &&
                    (it.dateInMillis.isToday() || (now - it.length * DateUtils.MINUTE_IN_MILLIS).isToday())) {
                prefs[KEY_LAST_PLAY_TIME] = now
                prefs[KEY_LAST_PLAY_LOCATION] = it.location
                prefs.putLastPlayPlayers(it.players)
            }

            it.updateTimestamp = now
            it.deleteTimestamp = 0
            it.dirtyTimestamp = now
            save(it)
            if (internalIdToDelete != INVALID_ID.toLong()) {
                playRepository.markAsDeleted(internalIdToDelete, null)
            }
            triggerUpload()
        }
    }

    fun saveDraft() {
        play.value?.let {
            it.dirtyTimestamp = System.currentTimeMillis()
            it.deleteTimestamp = 0
            save(it)
//            maybeShowNotification()
        }
    }

    fun deletePlay() {
        play.value?.let {
            it.updateTimestamp = 0
            it.deleteTimestamp = System.currentTimeMillis()
            it.dirtyTimestamp = 0
            save(it)
            triggerUpload()
//            cancelNotification()
        }
    }

    private fun save(play: Play) {
        _internalId.value = playPersister.save(
                play,
                _internalId.value ?: INVALID_ID.toLong(),
                true)
    }

    private fun triggerUpload() {
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
    }
}
