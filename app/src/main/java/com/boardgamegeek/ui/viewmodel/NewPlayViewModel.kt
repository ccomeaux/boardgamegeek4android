package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.entities.PlayerEntity
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

    val startTime = MutableLiveData<Long>()

    private val _location = MutableLiveData<String>()
    val location: LiveData<String>
        get() = _location

    private val playRepository = PlayRepository(getApplication())

    private val _currentStep = MutableLiveData<Int>()
    val currentStep: LiveData<Int>
        get() = _currentStep

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
    private val _addedPlayers = MutableLiveData<MutableList<PlayerEntity>>()
    val addedPlayers: LiveData<MutableList<PlayerEntity>>
        get() = _addedPlayers

    init {
        _currentStep.value = STEP_LOCATION
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
        _currentStep.value = STEP_PLAYERS
    }

    fun addPlayer(player: PlayerEntity) {
        val newList = addedPlayers.value ?: mutableListOf()
        newList.add(player)
        _addedPlayers.value = newList
    }

    fun removePlayer(player: PlayerEntity) {
        val newList = addedPlayers.value ?: mutableListOf()
        newList.remove(player)
        _addedPlayers.value = newList
    }

    fun finishAddingPlayers() {
        _currentStep.value = STEP_COMMENTS
    }

    fun filterPlayers(filter: String) = availablePlayers.value?.let {
        availablePlayers.value = filterPlayers(allPlayers.value, playersByLocation.value, addedPlayers.value, filter)
    }.also { playerFilter = filter }

    private fun filterPlayers(allPlayers: List<PlayerEntity>?, locationPlayers: List<PlayerEntity>?, addedPlayers: List<PlayerEntity>?, filter: String): List<PlayerEntity> {
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
            !(addedPlayers ?: emptyList()).contains(it) &&
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

    fun startTimer() {
        if ((startTime.value ?: 0L) == 0L) {
            startTime.value = System.currentTimeMillis()
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
                length = 0,
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

    companion object {
        const val STEP_LOCATION = 1
        const val STEP_PLAYERS = 2
        const val STEP_COMMENTS = 3
    }
}
