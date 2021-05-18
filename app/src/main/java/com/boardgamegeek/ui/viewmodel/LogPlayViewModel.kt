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

    private val _internalId = MutableLiveData<Long>()
    val internalId: LiveData<Long>
        get() = _internalId

    private val _play: MutableLiveData<Play> = MutableLiveData<Play>()
    val play: LiveData<Play>
        get() = _play

    private val _gameId = MutableLiveData<Int>()

    val colors: LiveData<List<String>> = Transformations.switchMap(_gameId) {
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

    fun loadPlay(internalId: Long, gameId: Int, gameName: String, isRequestingToEndPlay: Boolean = false) {
        _gameId.postValue(gameId)
        var p: Play? = null
        if (internalId == BggContract.INVALID_ID.toLong()) {
            p = Play(gameId, gameName)
            val lastPlay = prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L
            if (lastPlay.howManyHoursOld() < 12) {
                p.location = prefs[KEY_LAST_PLAY_LOCATION, ""].orEmpty()
                p.addPlayers(prefs.getLastPlayPlayers(), true) // TODO - only choose if game is auto-sortable
            }
        } else {
            val cursor = getApplication<BggApplication>().contentResolver.query(BggContract.Plays.buildPlayUri(internalId), PlayBuilder.PLAY_PROJECTION, null, null, null)
            cursor?.use {
                it.moveToFirst()
                // TODO don't user PlayBuilder
                p = PlayBuilder.fromCursor(it)
                if (isRequestingToEndPlay) {
                    p?.end()
                }
                val cursor2 = getApplication<BggApplication>().contentResolver.query(BggContract.Plays.buildPlayerUri(internalId), PlayBuilder.PLAYER_PROJECTION, null, null, null)
                cursor2?.use { c2 ->
                    PlayBuilder.addPlayers(c2, p!!)
                }
            }
            _internalId.value = internalId
        }
        p?.let {
            if (originalPlay == null) originalPlay = it.copy() // TODO make sure the compares players correctly
            _location.postValue(it.location.orEmpty())
            _play.postValue(it)
        }
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
            it.start()
            _play.value = it.copy()
        }
    }

    fun resumeTimer() {
        logTimer("Resume")
        _play.value?.let {
            it.resume()
            _play.value = it.copy()
        }
    }

    fun endTimer() {
        logTimer("Off")
        _play.value?.let {
            it.end()
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
            play.addPlayers(players.map { Player(name = it.name, username = it.username) }, resort)
            _play.value = play
        }
    }

    fun editPlayer(player: Player = Player(), position: Int? = null, resort: Boolean) {
        play.value?.copy()?.let {
            it.replaceOrAddPlayer(player, position, resort)
            _play.value = it
        }
    }

    fun addPlayer(player: Player = Player(), resort: Boolean) {
        play.value?.deepCopy()?.let {
            it.addPlayer(player, resort)
            _play.value = it
        }
    }

    fun removePlayer(player: Player, resort: Boolean) {
        play.value?.deepCopy()?.let {
            it.removePlayer(player, resort)
            _play.value = it
        }
    }

    fun isDirty(): Boolean {
        return play.value != originalPlay
    }

    fun clearPositions() {
        play.value?.deepCopy()?.let {
            it.clearPlayerPositions()
            _play.value = it
        }
    }

    fun pickStartPlayer(index: Int) {
        play.value?.deepCopy()?.let {
            it.pickStartPlayer(index)
            _play.value = it
        }
    }

    fun pickRandomStartPlayer() {
        play.value?.deepCopy()?.let {
            it.pickStartPlayer((0 until it.getPlayerCount()).random())
            _play.value = it
        }
    }

    fun randomizePlayerOrder() {
        play.value?.deepCopy()?.let {
            it.randomizePlayerOrder()
            _play.value = it
        }
    }

    fun reorderPlayers(fromSeat: Int, toSeat: Int) {
        play.value?.deepCopy()?.let {
            it.reorderPlayers(fromSeat, toSeat)
            _play.value = it
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
            for (p in it.players) {
                p.isWin = (p.score.toDoubleOrNull() ?: Double.NaN) == it.highScore
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

    fun logPlay(internalIdToDelete: Long = BggContract.INVALID_ID.toLong()) {
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
            if (internalIdToDelete != BggContract.INVALID_ID.toLong()) {
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
                _internalId.value ?: BggContract.INVALID_ID.toLong(),
                true)
    }

    private fun triggerUpload() {
        SyncService.sync(getApplication(), SyncService.FLAG_SYNC_PLAYS_UPLOAD)
    }
}
