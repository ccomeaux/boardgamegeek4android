package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.getLastPlayLocation
import com.boardgamegeek.extensions.getLastPlayPlayers
import com.boardgamegeek.extensions.getLastPlayTime
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import java.util.*
import java.util.concurrent.TimeUnit

class NewPlayViewModel(application: Application) : AndroidViewModel(application) {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var playDate: Long = Calendar.getInstance().timeInMillis
    private var comments: String = ""

    private val playRepository = PlayRepository(getApplication())

    private val _currentStep = MutableLiveData<Step>()
    val currentStep: LiveData<Step>
        get() = _currentStep

    private val _startTime = MutableLiveData<Long>()
    val startTime: LiveData<Long>
        get() = _startTime

    private val _lengthInMillis = MutableLiveData<Long>()
    val length: LiveData<Int> = Transformations.map(_lengthInMillis) {
        (it / 60_000).toInt()
    }

    private val _location = MutableLiveData<String>()
    val location: LiveData<String>
        get() = _location

    // Locations
    val locations = MediatorLiveData<List<LocationEntity>>()
    private var locationFilter = ""
    private val rawLocations = playRepository.loadLocations(PlayDao.LocationSortBy.PLAY_COUNT)

    // Players
    val availablePlayers = MediatorLiveData<List<PlayerEntity>>()
    private val allPlayers = playRepository.loadPlayersByLocation()
    private val playersByLocation: LiveData<List<PlayerEntity>> = Transformations.switchMap(location) {
        playRepository.loadPlayersByLocation(it)
    }
    private var playerFilter = ""
    private val _addedPlayers = MutableLiveData<MutableList<NewPlayPlayerEntity>>()
    val addedPlayers: LiveData<MutableList<NewPlayPlayerEntity>>
        get() = _addedPlayers

    init {
        _currentStep.value = Step.LOCATION
        playDate = Calendar.getInstance().timeInMillis

        locations.addSource(rawLocations) { result ->
            result?.let { locations.value = filterLocations(result, locationFilter) }
        }

        availablePlayers.addSource(allPlayers) { result ->
            result?.let { availablePlayers.value = filterPlayers(result, playersByLocation.value, addedPlayers.value, playerFilter) }
        }
        availablePlayers.addSource(playersByLocation) { result ->
            result?.let { availablePlayers.value = filterPlayers(allPlayers.value, result, addedPlayers.value, playerFilter) }
        }
        availablePlayers.addSource(_addedPlayers) { result ->
            result?.let { availablePlayers.value = filterPlayers(allPlayers.value, playersByLocation.value, result, playerFilter) }
        }
    }

    val insertedId = MutableLiveData<Long>()

    fun setGame(id: Int, name: String) {
        gameId = id
        gameName = name
    }

    fun filterLocations(filter: String) = rawLocations.value?.let { result ->
        locations.value = filterLocations(result, filter)
    }.also { locationFilter = filter }

    private fun filterLocations(list: List<LocationEntity>?, filter: String): List<LocationEntity> {
        val newList = (list?.filter { it.name.isNotBlank() } ?: emptyList()).toMutableList()
        if (isLastPlayRecent()) {
            newList.find { it.name == getApplication<BggApplication>().getLastPlayLocation() }?.let {
                newList.remove(it)
                newList.add(0, it)
            }
        }
        return newList.filter { it.name.startsWith(filter, true) }
    }

    fun setLocation(name: String) {
        if (_location.value != name) _location.value = name
        _currentStep.value = Step.PLAYERS
    }

    fun addPlayer(player: PlayerEntity) {
        val newList = addedPlayers.value ?: mutableListOf()
        val newPlayer = NewPlayPlayerEntity(player)
        if (!newList.contains(newPlayer))
            newList.add(newPlayer)
        _addedPlayers.value = newList
    }

    fun removePlayer(player: NewPlayPlayerEntity) {
        val newList = _addedPlayers.value ?: mutableListOf()
        newList.remove(player)
        _addedPlayers.value = newList
    }

    fun finishAddingPlayers() {
        _currentStep.value = Step.PLAYERS_COLOR
    }

    fun filterPlayers(filter: String) = availablePlayers.value?.let {
        availablePlayers.value = filterPlayers(allPlayers.value, playersByLocation.value, addedPlayers.value, filter)
    }.also { playerFilter = filter }

    private fun filterPlayers(allPlayers: List<PlayerEntity>?, locationPlayers: List<PlayerEntity>?, addedPlayers: List<NewPlayPlayerEntity>?, filter: String): List<PlayerEntity> {
        val self = allPlayers?.find { it.username == AccountUtils.getUsername(getApplication()) }
        val newList = mutableListOf<PlayerEntity>()
        // show players in this order:
        // 1. me
        self?.let {
            newList.add(it)
        }
        //  2. last played at this location
        if (isLastPlayRecent() && location.value == getApplication<BggApplication>().getLastPlayLocation()) {
            val lastPlayers = getApplication<BggApplication>().getLastPlayPlayers()
            lastPlayers?.forEach { lastPlayer ->
                allPlayers?.find { it == lastPlayer && !newList.contains(it) }?.let {
                    newList.add(it)
                }
            }
        }
        // 3. previously played at this location
        locationPlayers?.let { list ->
            newList.addAll(list.filter { !newList.contains(it) }.asIterable())
        }
        // 4. all other players
        allPlayers?.let { list ->
            newList += list.filter {
                (self == null || it.username != self.username) && !(newList.contains(it))
            }.asIterable()
        }
        // then filter out added players and those not matching the current filter
        return newList.filter {
            val p = NewPlayPlayerEntity(it)
            !(addedPlayers ?: emptyList()).contains(p) &&
                    (it.name.contains(filter, true) || it.username.contains(filter, true))
        }
    }

    private fun isLastPlayRecent(): Boolean {
        val lastPlayTime = getApplication<BggApplication>().getLastPlayTime()
        return !lastPlayTime.isOlderThan(6, TimeUnit.HOURS)
    }

    fun setComments(input: String) {
        this.comments = input
    }

    fun toggleTimer() {
        val lengthInMillis = _lengthInMillis.value ?: 0L
        if (startTime.value ?: 0L == 0L) {
            _startTime.value = System.currentTimeMillis() - lengthInMillis
            _lengthInMillis.value = 0
        } else {
            _lengthInMillis.value = System.currentTimeMillis() - (startTime.value ?: 0L) + lengthInMillis
            _startTime.value = 0L
        }
    }

    fun save() {
        val startTime = startTime.value ?: 0L
        val play = PlayEntity(
                BggContract.INVALID_ID.toLong(),
                BggContract.INVALID_ID,
                PlayEntity.currentDate(),
                gameId,
                gameName,
                quantity = 1,
                length = if (startTime == 0L) length.value ?: 0 else 0,
                location = location.value ?: "",
                incomplete = false,
                noWinStats = false,
                comments = comments,
                syncTimestamp = 0,
                playerCount = _addedPlayers.value?.size ?: 0,
                startTime = startTime,
                updateTimestamp = if (startTime == 0L) System.currentTimeMillis() else 0L,
                dirtyTimestamp = System.currentTimeMillis()
        )

        for (player in _addedPlayers.value ?: mutableListOf()) {
            play.addPlayer(PlayPlayerEntity(player.name, player.username))
        }

        playRepository.save(play, insertedId)
    }

    enum class Step {
        LOCATION,
        PLAYERS,
        PLAYERS_COLOR,
        COMMENTS
    }
}
