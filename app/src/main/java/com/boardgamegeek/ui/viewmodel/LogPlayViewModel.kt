package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.builder.PlayBuilder
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.tasks.ColorAssignerTask
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import java.util.*
import kotlin.io.use

class LogPlayViewModel(application: Application) : AndroidViewModel(application) {
    private val playPersister = PlayPersister(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private var originalPlay: Play? = null

    private val _internalId = MutableLiveData<Long>()
    val internalId: LiveData<Long>
        get() = _internalId
//
//    fun setInternalId(internalId: Long) {
//        if (_internalId.value != internalId) _internalId.value = internalId
//    }

//    val play: LiveData<Play> = Transformations.map(_internalId) { id ->
//        PlayBuilder.
//    }
//    val play: LiveData<RefreshableResource<PlayEntity>> = Transformations.switchMap(_internalId) { id ->
//        when (id) {
//            null -> AbsentLiveData.create()
//            else -> repository.getPlay(id)
//        }
//    }

    private val _gameId = MutableLiveData<Int>()

    fun setGame(id: Int) {
        if (_gameId.value != id) _gameId.value = id
    }

    val colors: LiveData<List<String>> = Transformations.switchMap(_gameId) {
        gameRepository.getPlayColors(it)
    }

    private val _play: MutableLiveData<Play> = MutableLiveData<Play>()
    val play: LiveData<Play>
        get() = _play

    fun loadPlay(internalId: Long, gameId: Int, gameName: String, isRequestingToEndPlay: Boolean = false) {
        var p: Play? = null
        if (internalId == BggContract.INVALID_ID.toLong()) {
            p = Play(gameId, gameName)
            val lastPlay = prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L
            if (lastPlay.howManyHoursOld() < 12) {
                p.location = prefs[KEY_LAST_PLAY_LOCATION, ""].orEmpty()
                p.setPlayers(prefs.getLastPlayPlayers())
                // TODO - only choose if game is auto-sortable
                p.pickStartPlayer(0)
            }
        } else {
            val cursor = getApplication<BggApplication>().contentResolver.query(BggContract.Plays.buildPlayUri(internalId), PlayBuilder.PLAY_PROJECTION, null, null, null)
            cursor?.use {
                it.moveToFirst()
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
        if (originalPlay == null && p != null) originalPlay = p?.copy()
        _play.postValue(p)
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

    fun updateQuantity(quantity: Int) {
        if (play.value?.quantity != quantity) {
            _play.value = play.value?.copy(quantity = quantity)
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

    fun setPlayers(players: List<Player>) {
        _play.value?.setPlayers(players)
        _play.value = play.value?.copy()
    }

    fun addPlayer(player: Player = Player(), position: Int = _play.value?.getPlayerCount() ?: 0) {
        //if (!arePlayersCustomSorted) {
        player.seat = (play.value?.getPlayerCount() ?: 0) + 1
        //}
        _play.value?.replaceOrAddPlayer(player, position)
        _play.value = play.value?.copy()
    }

    fun removePlayer(player: Player) {
        _play.value?.removePlayer(player, true) //, !arePlayersCustomSorted)
        _play.value = play.value?.copy()
    }

    fun isDirty(): Boolean {
        return play.value != originalPlay
    }

    fun clearPositions() {
        play.value?.copy()?.let {
            it.clearPlayerPositions()
            _play.value = it
        }
    }

    fun pickStartPlayer(index: Int) {
        play.value?.copy()?.let {
            it.pickStartPlayer(index)
            _play.value = it
        }
    }

    fun pickRandomStartPlayer() {
        play.value?.copy()?.let {
            it.pickStartPlayer((0 until it.getPlayerCount()).random())
            _play.value = it
        }
    }

    fun randomizePlayerOrder() {
        play.value?.copy()?.let {
            it.randomizePlayerOrder()
            _play.value = it
        }
    }

    fun reorderPlayers(fromSeat: Int, toSeat: Int) {
        play.value?.copy()?.let {
            it.reorderPlayers(fromSeat, toSeat)
            _play.value = it
        }
    }

    fun assignColors(clearExisting: Boolean = false) {
        _play.value?.let {
            if (clearExisting) it.players.forEach { p -> p.color = "" }
            ColorAssignerTask(getApplication(), it).executeAsyncTask()
            // TODO handle event?
        }
    }

    fun logPlay(internalIdToDelete:Long = BggContract.INVALID_ID.toLong()) {
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
