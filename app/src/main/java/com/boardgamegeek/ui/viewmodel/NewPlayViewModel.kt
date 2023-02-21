package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class NewPlayViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val gameRepository = GameRepository(getApplication(), playRepository)
    private val prefs: SharedPreferences by lazy { application.preferences() }

    private var gameId = MutableLiveData<Int>()
    private var gameName = MutableLiveData<String>()

    private val steps = ArrayDeque<Step>()

    private val _currentStep = MutableLiveData<Step>()
    val currentStep: LiveData<Step>
        get() = _currentStep

    private var _playDate = MutableLiveData<Long>()
    val playDate: LiveData<Long>
        get() = _playDate

    private val _startTime = MutableLiveData<Long>()
    val startTime: LiveData<Long>
        get() = _startTime

    private val _lengthInMillis = MutableLiveData<Long>()
    val length: LiveData<Int> = Transformations.map(_lengthInMillis) {
        (it / 60_000).toInt()
    }

    private var _comments: String = ""
    val comments: String
        get() = _comments

    private val _location = MutableLiveData<String>()
    val location: LiveData<String>
        get() = _location

    // Locations
    val locations = MediatorLiveData<List<LocationEntity>>()
    private var locationFilter = ""
    private val rawLocations: LiveData<List<LocationEntity>> = liveData {
        emit(playRepository.loadLocations(PlayDao.LocationSortBy.PLAY_COUNT))
    }

    // Players
    val availablePlayers = MediatorLiveData<List<PlayerEntity>>()
    private val _allPlayers = liveData { emit(playRepository.loadPlayersByLocation()) }
    private val playersByLocation: LiveData<List<PlayerEntity>> = location.switchMap {
        liveData {
            emit(playRepository.loadPlayersByLocation(it))
        }
    }
    private val playerFavoriteColors: LiveData<Map<PlayerEntity, String>> = liveData {
        emit(playRepository.loadPlayerFavoriteColors())
    }
    private val playerFilter = MutableLiveData<String>()
    private val _addedPlayers = MutableLiveData<MutableList<PlayerEntity>>()
    val addedPlayers = MediatorLiveData<List<NewPlayPlayerEntity>>()
    private val playerColorMap = MutableLiveData<MutableMap<String, String>>()
    private val playerFavoriteColorMap = mutableMapOf<String, List<PlayerColorEntity>>()
    val selectedColors = MediatorLiveData<List<String>>()
    private val playerSortMap = MutableLiveData<MutableMap<String, Int>>()
    private val playerMightBeNewMap = mutableMapOf<String, Boolean>()
    private val playerIsNewMap = MutableLiveData<MutableMap<String, Boolean>>()
    val mightBeNewPlayers = MediatorLiveData<List<NewPlayPlayerEntity>>()
    private val playerWinMap = MutableLiveData<MutableMap<String, Boolean>>()
    private val playerScoresMap = MutableLiveData<MutableMap<String, String>>()

    val gameColors = gameId.switchMap { gameId ->
        liveData {
            emit(if (gameId == BggContract.INVALID_ID) null else gameRepository.getPlayColors(gameId).filter { it.isNotBlank() })
        }
    }

    init {
        addStep(INITIAL_STEP)

        locations.addSource(rawLocations) { result ->
            result?.let { locations.value = filterLocations(result, locationFilter) }
        }

        availablePlayers.addSource(_allPlayers) { result ->
            result?.let {
                availablePlayers.value = assembleAvailablePlayers(allPlayers = result)
            }
        }
        availablePlayers.addSource(playersByLocation) { result ->
            result?.let {
                availablePlayers.value = assembleAvailablePlayers(locationPlayers = result)
            }
        }
        availablePlayers.addSource(_addedPlayers) { result ->
            result?.let {
                availablePlayers.value = assembleAvailablePlayers(addedPlayers = result)
            }
        }
        availablePlayers.addSource(playerFavoriteColors) { result ->
            result?.let {
                availablePlayers.value = assembleAvailablePlayers(favoriteColors = result)
            }
        }
        availablePlayers.addSource(playerFilter) { result ->
            result?.let {
                availablePlayers.value = assembleAvailablePlayers(filter = result)
            }
        }

        addedPlayers.addSource(_addedPlayers) { list ->
            list?.let {
                assemblePlayers(addedPlayers = it)
            }
        }
        addedPlayers.addSource(playerColorMap) { map ->
            map?.let {
                assemblePlayers(playerColors = it)
            }
        }
        addedPlayers.addSource(playerSortMap) { map ->
            map?.let {
                assemblePlayers(playerSort = it)
            }
        }
        addedPlayers.addSource(playerIsNewMap) { map ->
            map?.let {
                assemblePlayers(playerIsNew = it)
            }
        }
        addedPlayers.addSource(playerWinMap) { map ->
            map?.let {
                assemblePlayers(playerWin = it)
            }
        }
        addedPlayers.addSource(playerScoresMap) { map ->
            map?.let {
                assemblePlayers(playerScores = it)
            }
        }
        addedPlayers.addSource(gameColors) { list ->
            list?.let {
                assemblePlayers(gameColorList = it)
            }
        }

        mightBeNewPlayers.addSource(addedPlayers) { list ->
            list?.let {
                assembleMightBeNewPlayers(list)
            }
        }

        selectedColors.addSource(addedPlayers) { result ->
            selectedColors.value = result.map { it.color }
        }
    }

    private val _insertedId = MutableLiveData<Long>()
    val insertedId: LiveData<Long>
        get() = _insertedId

    fun setGame(id: Int, name: String) {
        gameId.value = id
        gameName.value = name
    }

    val game: LiveData<GameEntity?> = gameId.switchMap {
        liveData {
            emit(
                when (it) {
                    BggContract.INVALID_ID -> null
                    else -> gameRepository.loadGame(it)
                }
            )
        }
    }

    fun filterLocations(filter: String) = rawLocations.value?.let { result ->
        locations.value = filterLocations(result, filter)
    }.also { locationFilter = filter }

    fun setDate(date: Long) {
        if (_playDate.value != date) _playDate.value = date
        addStep(Step.LOCATION)
    }

    private fun filterLocations(list: List<LocationEntity>?, filter: String): List<LocationEntity> {
        val newList = (list?.filter { it.name.isNotBlank() }.orEmpty()).toMutableList()
        if (isLastPlayRecent()) {
            newList.find { it.name == prefs[KEY_LAST_PLAY_LOCATION, ""] }?.let {
                newList.remove(it)
                newList.add(0, it)
            }
        }
        return newList.filter { it.name.startsWith(filter, true) }
    }

    fun setLocation(name: String) {
        if (_location.value != name) _location.value = name
        addStep(Step.ADD_PLAYERS)
    }

    fun addPlayer(player: PlayerEntity) {
        val newList = _addedPlayers.value ?: mutableListOf()
        if (!newList.contains(player)) {
            viewModelScope.launch {
                newList.add(player)

                playerFavoriteColorMap[player.id] = if (player.isUser()) {
                    playRepository.loadUserColors(player.username)
                } else {
                    playRepository.loadPlayerColors(player.name)
                }
                assemblePlayers()

                val plays = playRepository.loadPlaysByPlayer(
                    player.playerName,
                    gameId.value ?: BggContract.INVALID_ID,
                    player.isUser()
                )
                playerMightBeNewMap[player.id] = plays.sumOf { it.quantity } == 0
                assembleMightBeNewPlayers()

                _addedPlayers.value = newList
            }
        }
    }

    fun removePlayer(player: NewPlayPlayerEntity) {
        val removedPlayer = PlayerEntity(player.name, player.username)

        val newList = _addedPlayers.value ?: mutableListOf()
        newList.remove(removedPlayer).let { _addedPlayers.value = newList }

        playerFavoriteColorMap.remove(removedPlayer.id)

        val newColorMap = playerColorMap.value ?: mutableMapOf()
        newColorMap.remove(removedPlayer.id)?.let { playerColorMap.value = newColorMap }

        playerMightBeNewMap.remove(removedPlayer.id)
        assembleMightBeNewPlayers()

        val newSortMap = playerSortMap.value ?: mutableMapOf()
        if (newSortMap.isNotEmpty()) {
            newSortMap.remove(removedPlayer.id)?.let { value ->
                newSortMap.forEach {
                    if (it.value > value) newSortMap[it.key] = it.value - 1
                }
            }
            playerSortMap.value = newSortMap
        }
    }

    fun finishAddingPlayers() {
        addStep(
            if ((_addedPlayers.value?.size ?: 0) > 0)
                Step.PLAYERS_COLOR
            else
                Step.COMMENTS
        )
    }

    fun addColorToPlayer(playerIndex: Int, color: String) {
        val colorMap = playerColorMap.value ?: mutableMapOf()
        val player = _addedPlayers.value?.getOrNull(playerIndex)
        if (player != null) {
            colorMap[player.id] = color
            playerColorMap.value = colorMap
        }
    }

    fun finishPlayerColors() {
        addStep(Step.PLAYERS_SORT)
    }

    fun clearSortOrder() {
        playerSortMap.value = mutableMapOf()
    }

    fun randomizePlayers() {
        val sortMap = mutableMapOf<String, Int>()
        val playerCount = _addedPlayers.value?.size ?: 0
        val collection = (1..playerCount).toMutableSet()
        _addedPlayers.value?.forEach { playerEntity ->
            val sortOrder = collection.random()
            sortMap[playerEntity.id] = sortOrder
            collection.remove(sortOrder)
        }
        playerSortMap.value = sortMap
    }

    fun randomizeStartPlayer() {
        val sortMap = mutableMapOf<String, Int>()
        val playerCount = _addedPlayers.value?.size ?: 0
        var sortOrder = (1..playerCount).random()
        _addedPlayers.value?.forEach { playerEntity ->
            sortMap[playerEntity.id] = sortOrder
            sortOrder += 1
            if (sortOrder > playerCount) sortOrder -= playerCount
        }
        playerSortMap.value = sortMap
    }

    fun selectStartPlayer(index: Int) {
        val playerCount = _addedPlayers.value?.size ?: 0
        val sortMap = playerSortMap.value ?: mutableMapOf()
        if (sortMap.isNotEmpty()) {
            sortMap.forEach {
                sortMap[it.key] = (it.value + playerCount - index - 1) % playerCount + 1
            }
        } else {
            _addedPlayers.value?.forEachIndexed { i, playerEntity ->
                sortMap[playerEntity.id] = (i + playerCount - index) % playerCount + 1
            }
        }
        playerSortMap.value = sortMap
    }

    fun movePlayer(fromPosition: Int, toPosition: Int): Boolean {
        val oldMap = playerSortMap.value ?: mutableMapOf()
        if (oldMap.isNotEmpty()) {
            val newMap = mutableMapOf<String, Int>()
            if (fromPosition < toPosition) { // dragging down
                for (seat in oldMap) {
                    newMap[seat.key] = seat.value - if (seat.value in (fromPosition + 2)..(toPosition + 1)) 1 else 0
                }
            } else { // dragging up
                for (seat in oldMap) {
                    newMap[seat.key] = seat.value + if (seat.value in (toPosition + 1)..fromPosition) 1 else 0
                }
            }
            oldMap.filter { it.value == fromPosition + 1 }.keys.firstOrNull()?.let {
                newMap[it] = toPosition + 1
            }
            playerSortMap.value = newMap
            return true
        }
        return false
    }

    fun finishPlayerSort() {
        val step = when {
            playerMightBeNewMap.values.any { it } -> Step.PLAYERS_NEW
            (startTime.value ?: 0L) == 0L -> Step.PLAYERS_WIN
            else -> Step.COMMENTS
        }
        addStep(step)
    }

    fun addIsNewToPlayer(playerId: String, isNew: Boolean) {
        val isNewMap = playerIsNewMap.value ?: mutableMapOf()
        isNewMap[playerId] = isNew
        playerIsNewMap.value = isNewMap
    }

    fun finishPlayerIsNew() {
        addStep(if ((startTime.value ?: 0L) == 0L) Step.PLAYERS_WIN else Step.COMMENTS)
    }

    fun addWinToPlayer(playerId: String, isWin: Boolean) {
        val winMap = playerWinMap.value ?: mutableMapOf()
        winMap[playerId] = isWin
        playerWinMap.value = winMap
    }

    private val scoreFormat = DecimalFormat("0.#########")

    fun addScoreToPlayer(playerId: String, score: Double) {
        val scoreMap = playerScoresMap.value ?: mutableMapOf()
        scoreMap[playerId] = scoreFormat.format(score)
        playerScoresMap.value = scoreMap
    }

    fun finishPlayerWin() {
        addStep(Step.COMMENTS)
    }

    fun filterPlayers(filter: String) {
        playerFilter.value = filter
    }

    private fun assembleAvailablePlayers(
        allPlayers: List<PlayerEntity>? = _allPlayers.value,
        locationPlayers: List<PlayerEntity>? = playersByLocation.value,
        addedPlayers: List<PlayerEntity>? = _addedPlayers.value,
        filter: String? = playerFilter.value,
        favoriteColors: Map<PlayerEntity, String>? = playerFavoriteColors.value,
    ): List<PlayerEntity> {
        val newList = mutableListOf<PlayerEntity>()
        // show players in this order:
        // 1. me
        val self = allPlayers?.find { it.username == prefs[AccountPreferences.KEY_USERNAME, ""] }
        self?.let { newList.add(it) }
        //  2. last played at this location
        if (isLastPlayRecent() && location.value == prefs[KEY_LAST_PLAY_LOCATION, ""]) {
            val lastPlayers = prefs.getLastPlayPlayerEntities()
            lastPlayers.forEach { lastPlayer ->
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
        val filteredList = newList.filter {
            !(addedPlayers.orEmpty()).contains(it) && (it.name.contains(filter.orEmpty(), true) || it.username.contains(filter.orEmpty(), true))
        }
        val favColors = favoriteColors.orEmpty()
        filteredList.forEach { player ->
            if (player.isUser()) {
                favColors.keys.find { it.isUser() && it.username == player.username }?.let { key ->
                    player.favoriteColor = favColors[key].asColorRgb()
                }
            } else {
                favColors.keys.find { !it.isUser() && it.name == player.name }?.let { key ->
                    player.favoriteColor = favColors[key].asColorRgb()
                }
            }
        }
        return filteredList
    }

    private fun assemblePlayers(
        addedPlayers: List<PlayerEntity> = _addedPlayers.value.orEmpty(),
        playerColors: Map<String, String> = playerColorMap.value.orEmpty(),
        favoriteColorsMap: Map<String, List<PlayerColorEntity>> = playerFavoriteColorMap,
        playerSort: Map<String, Int> = playerSortMap.value.orEmpty(),
        playerIsNew: Map<String, Boolean> = playerIsNewMap.value.orEmpty(),
        playerWin: Map<String, Boolean> = playerWinMap.value.orEmpty(),
        playerScores: Map<String, String> = playerScoresMap.value.orEmpty(),
        gameColorList: List<String> = gameColors.value.orEmpty(),
    ) {
        val players = mutableListOf<NewPlayPlayerEntity>()
        addedPlayers.forEach { playerEntity ->
            val newPlayer = NewPlayPlayerEntity(playerEntity).apply {
                color = playerColors[id].orEmpty()
                val favoriteForPlayer = favoriteColorsMap[id]?.map { it.description }.orEmpty()
                val rankedChoices = favoriteForPlayer
                    .filter { gameColorList.contains(it) }
                    .filterNot { playerColors.containsValue(it) }
                    .toMutableList()
                rankedChoices += gameColorList
                    .filterNot { favoriteForPlayer.contains(it) }
                    .filterNot { playerColors.containsValue(it) }
                favoriteColorsForGame = rankedChoices
                favoriteColor = favoriteForPlayer.firstOrNull()
                sortOrder = playerSort[id]?.toString().orEmpty()
                isNew = playerIsNew[id] ?: false
                isWin = playerWin[id] ?: false
                score = playerScores[id].orEmpty()
            }
            players.add(newPlayer)
        }
        this.addedPlayers.value = players
    }

    private fun assembleMightBeNewPlayers(
        players: List<NewPlayPlayerEntity> = mightBeNewPlayers.value.orEmpty(),
        newMap: MutableMap<String, Boolean> = playerMightBeNewMap
    ) {
        mightBeNewPlayers.value = players.filter { newMap[it.id] ?: false }
    }

    private fun isLastPlayRecent(): Boolean {
        val lastPlayTime = prefs[KEY_LAST_PLAY_TIME, 0L] ?: 0L
        return !lastPlayTime.isOlderThan(6, TimeUnit.HOURS)
    }

    fun setComments(input: String) {
        this._comments = input
    }

    fun toggleTimer() {
        val lengthInMillis = _lengthInMillis.value ?: 0L
        if ((startTime.value ?: 0L) == 0L) {
            _startTime.value = System.currentTimeMillis() - lengthInMillis
            _lengthInMillis.value = 0
        } else {
            _lengthInMillis.value =
                System.currentTimeMillis() - (startTime.value ?: 0L) + lengthInMillis
            _startTime.value = 0L
        }
    }

    fun save() {
        viewModelScope.launch {
            val startTime = startTime.value ?: 0L
            val players = _addedPlayers.value.orEmpty().map { player ->
                PlayPlayerEntity(
                    player.name,
                    player.username,
                    (playerSortMap.value.orEmpty())[player.id]?.toString().orEmpty(),
                    color = (playerColorMap.value.orEmpty())[player.id].orEmpty(),
                    isNew = (playerIsNewMap.value.orEmpty())[player.id] ?: false,
                    isWin = (playerWinMap.value.orEmpty())[player.id] ?: false,
                    score = (playerScoresMap.value.orEmpty())[player.id].orEmpty(),
                )
            }
            val play = PlayEntity(
                BggContract.INVALID_ID.toLong(),
                BggContract.INVALID_ID,
                PlayEntity.millisToRawDate(playDate.value ?: System.currentTimeMillis()),
                gameId.value ?: BggContract.INVALID_ID,
                gameName.value.orEmpty(),
                quantity = 1,
                length = if (startTime == 0L) length.value ?: 0 else 0,
                location = location.value.orEmpty(),
                incomplete = false,
                noWinStats = false,
                comments = _comments,
                syncTimestamp = 0,
                initialPlayerCount = _addedPlayers.value?.size ?: 0,
                startTime = startTime,
                updateTimestamp = if (startTime == 0L) System.currentTimeMillis() else 0L,
                dirtyTimestamp = System.currentTimeMillis(),
                _players = players,
            )

            _insertedId.value = playRepository.save(play)
        }
    }

    private fun addStep(step: Step) {
        steps += step
        _currentStep.value = step
    }

    fun previousPage() {
        steps.removeLastOrNull()
        _currentStep.value = steps.lastOrNull()
    }

    enum class Step {
        DATE,
        LOCATION,
        ADD_PLAYERS,
        PLAYERS_COLOR,
        PLAYERS_SORT,
        PLAYERS_NEW,
        PLAYERS_WIN,
        COMMENTS
    }

    companion object {
        val INITIAL_STEP = Step.DATE
    }
}
