package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.LocationEntity
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.DateTimeUtils
import com.boardgamegeek.util.PreferencesUtils
import java.util.*

class NewPlayViewModel(application: Application) : AndroidViewModel(application) {
    private var gameId: Int = BggContract.INVALID_ID
    private var gameName: String = ""
    private var playDate: Long = Calendar.getInstance().timeInMillis
    private var comments: String = ""

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

    fun setGame(id: Int, name: String) {
        gameId = id
        gameName = name
    }

    fun filterLocations(filter: String) = rawLocations.value?.let { result ->
        locations.value = filterLocations(result, filter)
    }.also { locationFilter = filter }

    private fun filterLocations(list: List<LocationEntity>?, filter: String): List<LocationEntity> {
        val newList = (list?.filter { it.name.isNotBlank() } ?: emptyList()).toMutableList()
        //TODO convert these to extension methods
        val lastPlay = PreferencesUtils.getLastPlayTime(getApplication())
        if (DateTimeUtils.howManyHoursOld(lastPlay) < 3) { // TODO make this longer probably
            newList.find { it.name == PreferencesUtils.getLastPlayLocation(getApplication()) }?.let {
                newList.remove(it)
                newList.add(0, it)
            }
        }
        return newList.filter { it.name.startsWith(filter, true) }
    }

    fun setLocation(name: String) {
        _location.value = name
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
        self?.let {
            newList.add(it)
        }
        // TODO - add PreferencesUtils.getLastPlayPlayers()
        locationPlayers?.let { list ->
            newList.addAll(list.filter { it.username != self?.username }.asIterable())
        }
        allPlayers?.let { list ->
            newList += list.filter {
                it.username != self?.username && !(locationPlayers?.contains(it) ?: true)
            }.asIterable()
        }
        return newList.filter {
            !(addedPlayers
                    ?: emptyList()).contains(it) && (it.name.contains(filter, true) || it.username.contains(filter, true))
        }
    }

    fun setComments(input: String) {
        this.comments = input
    }

    val insertedId = MutableLiveData<Long>()

    fun save() {
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
                updateTimestamp = System.currentTimeMillis(),
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
