package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.mappers.mapToEnum
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.Game
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.User
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.getCompleteCollectionTimestampKey
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.work.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    application: Application,
    private val collectionRepository: GameCollectionRepository,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    private val prefs: SharedPreferences by lazy { application.preferences() }

    val syncCollectionStatuses: LiveData<Set<CollectionStatus>?> = collectionStatusLiveData(getApplication())

    val collectionCompleteTimestamp: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_COLLECTION_COMPLETE, SyncPrefs.NAME, defaultValue = null)
    val collectionPartialTimestamp: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_COLLECTION_PARTIAL, SyncPrefs.NAME, defaultValue = null)
    val collectionCompleteCurrentTimestamp: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_COLLECTION_COMPLETE_CURRENT, SyncPrefs.NAME, defaultValue = null)

    val syncPlays: LiveData<Boolean?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS, defaultValue = null)
    private val oldestSyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE, SyncPrefs.NAME, defaultValue = null)
    private val newestSyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE, SyncPrefs.NAME, defaultValue = null)

    private val _playSyncState = MediatorLiveData<Triple<Long, Long, Int>>()
    val playSyncState: LiveData<Triple<Long, Long, Int>>
        get() = _playSyncState

    val syncBuddies: LiveData<Boolean?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_BUDDIES, defaultValue = null)
    val buddySyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_BUDDIES, SyncPrefs.NAME, defaultValue = null)

    private val collectionWorkInfos = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData(SyncCollectionWorker.UNIQUE_WORK_NAME_AD_HOC)
    private val playWorkInfos = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData(SyncPlaysWorker.UNIQUE_WORK_NAME_AD_HOC)
    private val userWorkInfos = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData(SyncUsersWorker.UNIQUE_WORK_NAME_AD_HOC)

    private val collectionItemsToUpload = liveData {
        emitSource(collectionRepository.loadItemsPendingUploadAsFlow().distinctUntilChanged().asLiveData())
    }
    val numberOfCollectionItemsToUpload = collectionItemsToUpload.map { it.size }

    private val games = liveData {
        emitSource(gameRepository.loadAllAsFlow().distinctUntilChanged().asLiveData())
    }

    val numberOfUnsyncedGames = games.map { list ->
        list.filter { it?.updated == 0L }.size
    }

    private val allPlays: LiveData<List<Play>> = liveData {
        emitSource(playRepository.loadAllPlaysFlow().distinctUntilChanged().asLiveData())
    }

    private val numberOfSyncPlays = allPlays.map { list ->
        list.filter { it.isSynced }.size
    }

    init {
        _playSyncState.addSource(numberOfSyncPlays) {
            it?.let { size ->
                newestSyncDate.value?.let { new ->
                    oldestSyncDate.value?.let { old ->
                        numberOfSyncPlays.value?.let {
                            _playSyncState.postValue(Triple(old, new, size))
                        }
                    }
                }
            }
        }
        _playSyncState.addSource(oldestSyncDate) {
            it?.let { old ->
                newestSyncDate.value?.let { new ->
                    numberOfSyncPlays.value?.let { size ->
                        _playSyncState.postValue(Triple(old, new, size))
                    }
                }
            }
        }
        _playSyncState.addSource(newestSyncDate) {
            it?.let { new ->
                oldestSyncDate.value?.let { old ->
                    numberOfSyncPlays.value?.let { size ->
                        _playSyncState.postValue(Triple(old, new, size))
                    }
                }
            }
        }
    }

    fun collectionStatusCompleteTimestamp(status: CollectionStatus): LiveData<Long?> {
        return if (status == CollectionStatus.Unknown) MutableLiveData(null)
        else LiveSharedPreference(getApplication(), getCompleteCollectionTimestampKey(null, status), SyncPrefs.NAME, defaultValue = null)
    }

    fun collectionStatusAccessoryCompleteTimestamp(status: CollectionStatus): LiveData<Long?> {
        return LiveSharedPreference(getApplication(), getCompleteCollectionTimestampKey(Game.Subtype.BoardGameAccessory, status), SyncPrefs.NAME, defaultValue = null)
    }

    fun syncCollection(status: CollectionStatus = CollectionStatus.Unknown) {
        SyncCollectionWorker.requestSync(getApplication(), status)
    }

    fun cancelCollection() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(SyncCollectionWorker.UNIQUE_WORK_NAME_AD_HOC)
    }

    fun uploadCollection() {
        CollectionUploadWorker.buildRequest(getApplication())
    }

    fun modifyCollectionStatus(status: CollectionStatus, add: Boolean) {
        if (add) prefs.addSyncStatus(status)
        else prefs.removeSyncStatus(status)
    }

    val collectionSyncProgress: LiveData<CollectionSyncProgress> = collectionWorkInfos.map {
        val workInfo = it.firstOrNull()
        if (workInfo?.state == WorkInfo.State.RUNNING) {
            val step = when (workInfo.progress.getInt(SyncCollectionWorker.PROGRESS_KEY_STEP, SyncCollectionWorker.PROGRESS_STEP_UNKNOWN)) {
                SyncCollectionWorker.PROGRESS_STEP_COLLECTION_COMPLETE -> CollectionSyncProgressStep.CompleteCollection
                SyncCollectionWorker.PROGRESS_STEP_COLLECTION_PARTIAL -> CollectionSyncProgressStep.PartialCollection
                SyncCollectionWorker.PROGRESS_STEP_COLLECTION_STALE -> CollectionSyncProgressStep.StaleCollection
                SyncCollectionWorker.PROGRESS_STEP_COLLECTION_DELETE -> CollectionSyncProgressStep.DeleteCollection
                SyncCollectionWorker.PROGRESS_STEP_GAMES_REMOVE -> CollectionSyncProgressStep.RemoveGames
                SyncCollectionWorker.PROGRESS_STEP_GAMES_STALE -> CollectionSyncProgressStep.StaleGames
                SyncCollectionWorker.PROGRESS_STEP_GAMES_NEW -> CollectionSyncProgressStep.NewGames
                else -> CollectionSyncProgressStep.NotSyncing
            }
            val subtype = when (workInfo.progress.getInt(SyncCollectionWorker.PROGRESS_KEY_SUBTYPE, SyncCollectionWorker.PROGRESS_SUBTYPE_NONE)) {
                SyncCollectionWorker.PROGRESS_SUBTYPE_ALL -> CollectionSyncProgressSubtype.All
                SyncCollectionWorker.PROGRESS_SUBTYPE_ACCESSORY -> CollectionSyncProgressSubtype.Accessory
                else -> CollectionSyncProgressSubtype.None
            }
            val status = workInfo.progress.getString(SyncCollectionWorker.PROGRESS_KEY_STATUS)
            CollectionSyncProgress(step, subtype, status.mapToEnum())
        } else {
            CollectionSyncProgress()
        }
    }.distinctUntilChanged()

    val playSyncProgress: LiveData<PlaySyncProgress> = playWorkInfos.map {
        val workInfo = it.firstOrNull()
        if (workInfo?.state == WorkInfo.State.RUNNING) {
            val step = when (workInfo.progress.getInt(SyncPlaysWorker.PROGRESS_STEP, SyncPlaysWorker.PROGRESS_STEP_UNKNOWN)) {
                SyncPlaysWorker.PROGRESS_STEP_NEW -> PlaySyncProgressStep.New
                SyncPlaysWorker.PROGRESS_STEP_OLD -> PlaySyncProgressStep.Old
                SyncPlaysWorker.PROGRESS_STEP_STATS -> PlaySyncProgressStep.Stats
                else -> PlaySyncProgressStep.NotSyncing
            }
            val minDate = workInfo.progress.getLong(SyncPlaysWorker.PROGRESS_MIN_DATE, 0L)
            val maxDate = workInfo.progress.getLong(SyncPlaysWorker.PROGRESS_MAX_DATE, 0L)
            val page = workInfo.progress.getInt(SyncPlaysWorker.PROGRESS_PAGE, 1)
            val action = when (workInfo.progress.getInt(SyncPlaysWorker.PROGRESS_ACTION, SyncPlaysWorker.PROGRESS_ACTION_UNKNOWN)) {
                SyncPlaysWorker.PROGRESS_ACTION_WAITING -> PlaySyncProgressAction.Waiting
                SyncPlaysWorker.PROGRESS_ACTION_DOWNLOADING -> PlaySyncProgressAction.Downloading
                SyncPlaysWorker.PROGRESS_ACTION_SAVING -> PlaySyncProgressAction.Saving
                SyncPlaysWorker.PROGRESS_ACTION_DELETING -> PlaySyncProgressAction.Deleting
                else -> PlaySyncProgressAction.None
            }
            PlaySyncProgress(step, minDate, maxDate, page, action)
        } else {
            PlaySyncProgress(PlaySyncProgressStep.NotSyncing)
        }
    }.distinctUntilChanged()

    val numberOfPlaysToBeUpdated: LiveData<Int> = allPlays.map { list ->
        list.filter { it.updateTimestamp > 0L }.size
    }

    val numberOfPlaysToBeDeleted: LiveData<Int> = allPlays.map { list ->
        list.filter { it.deleteTimestamp > 0L }.size
    }

    fun syncPlays() {
        SyncPlaysWorker.requestSync(getApplication())
    }

    fun cancelPlays() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(SyncPlaysWorker.UNIQUE_WORK_NAME_AD_HOC)
    }

    fun uploadPlays() {
        PlayUploadWorker.requestSync(getApplication())
    }

    private val users: LiveData<List<User>> = liveData {
        emitSource(userRepository.loadUsersFlow().distinctUntilChanged().asLiveData())
    }

    fun syncBuddies() {
        SyncUsersWorker.requestSync(getApplication())
    }

    fun cancelBuddies() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(SyncUsersWorker.UNIQUE_WORK_NAME_AD_HOC)
    }

    val userSyncState: LiveData<UserSyncState> = users.map { list ->
        val updatedOrNotLists = list.partition { it.updatedTimestamp == 0L }
        val numberOfUnupdatedUsers = updatedOrNotLists.first.size
        val oldestSyncedUser = updatedOrNotLists.second.minByOrNull { it.updatedTimestamp }
        UserSyncState(
            updatedOrNotLists.second.size,
            numberOfUnupdatedUsers,
            oldestSyncedUser?.updatedTimestamp,
        )
    }.distinctUntilChanged()

    val userProgress: LiveData<UserSyncProgress> = userWorkInfos.map {
        val workInfo = it.firstOrNull()
        if (workInfo?.state == WorkInfo.State.RUNNING) {
            val progress = workInfo.progress
            val stepEnum = when (progress.getInt(SyncUsersWorker.PROGRESS_STEP, SyncUsersWorker.PROGRESS_STEP_UNKNOWN)) {
                SyncUsersWorker.PROGRESS_STEP_BUDDY_LIST -> UserSyncProgressStep.BuddyList
                SyncUsersWorker.PROGRESS_STEP_STALE_BUDDIES -> UserSyncProgressStep.StaleBuddies
                SyncUsersWorker.PROGRESS_STEP_NEW_BUDDIES -> UserSyncProgressStep.NewBuddies
                SyncUsersWorker.PROGRESS_STEP_STALE_PLAYERS -> UserSyncProgressStep.StalePlayers
                SyncUsersWorker.PROGRESS_STEP_NEW_PLAYERS -> UserSyncProgressStep.NewPlayers
                else -> UserSyncProgressStep.NotSyncing
            }
            UserSyncProgress(
                stepEnum,
                progress.getString(SyncUsersWorker.PROGRESS_USERNAME),
                progress.getInt(SyncUsersWorker.PROGRESS_INDEX, 0),
                progress.getInt(SyncUsersWorker.PROGRESS_TOTAL, 0),
            )
        } else {
            UserSyncProgress(UserSyncProgressStep.NotSyncing)
        }
    }.distinctUntilChanged()

    data class CollectionSyncProgress(
        val step: CollectionSyncProgressStep = CollectionSyncProgressStep.NotSyncing,
        val subtype: CollectionSyncProgressSubtype = CollectionSyncProgressSubtype.None,
        val status: CollectionStatus = CollectionStatus.Unknown,
    )

    enum class CollectionSyncProgressStep {
        NotSyncing,
        CompleteCollection,
        PartialCollection,
        StaleCollection,
        DeleteCollection,
        RemoveGames,
        NewGames,
        StaleGames,
    }

    enum class CollectionSyncProgressSubtype {
        None,
        All,
        Accessory,
    }

    data class PlaySyncProgress(
        val step: PlaySyncProgressStep,
        val minDate: Long = 0L,
        val maxDate: Long = 0L,
        val page: Int = 1,
        val action: PlaySyncProgressAction = PlaySyncProgressAction.None,
    )

    enum class PlaySyncProgressStep {
        NotSyncing,
        New,
        Old,
        Stats,
    }

    enum class PlaySyncProgressAction {
        None,
        Waiting,
        Downloading,
        Saving,
        Deleting,
    }

    data class UserSyncState(val count: Int, val numberOfUnupdatedUsers: Int, val oldestUpdatedUserTimestamp: Long?)

    data class UserSyncProgress(val step: UserSyncProgressStep, val username: String? = null, val progress: Int = 0, val max: Int = 0)

    enum class UserSyncProgressStep {
        NotSyncing,
        BuddyList,
        StaleBuddies,
        NewBuddies,
        StalePlayers,
        NewPlayers,
    }
}
