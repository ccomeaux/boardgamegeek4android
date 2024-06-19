@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_PLAYS
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.User
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.work.PlayUploadWorker
import com.boardgamegeek.work.SyncPlaysWorker
import com.boardgamegeek.work.SyncUsersWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    val syncPlays: LiveData<Boolean?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_PLAYS)
    private val oldestSyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_OLDEST_DATE, SyncPrefs.NAME)
    private val newestSyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_PLAYS_NEWEST_DATE, SyncPrefs.NAME)

    private val _playSyncState = MediatorLiveData<Triple<Long, Long, Int>>()
    val playSyncState: LiveData<Triple<Long, Long, Int>>
        get() = _playSyncState

    val syncBuddies: LiveData<Boolean?> = LiveSharedPreference(getApplication(), PREFERENCES_KEY_SYNC_BUDDIES)
    val buddySyncDate: LiveData<Long?> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_BUDDIES, SyncPrefs.NAME)

    private val playWorkInfos = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData(SyncPlaysWorker.UNIQUE_WORK_NAME_AD_HOC)
    private val userWorkInfos = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData(SyncUsersWorker.UNIQUE_WORK_NAME_AD_HOC)

    private val allPlays: LiveData<List<Play>> = liveData {
        emitSource(playRepository.loadAllPlaysFlow().distinctUntilChanged().asLiveData())
    }

    private val numberOfSyncPlays: LiveData<Int> = allPlays.map { list ->
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

    val playSyncProgres: LiveData<PlaySyncProgress> = playWorkInfos.map {
        val workInfo = it?.firstOrNull()
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

    private val buddies: LiveData<List<User>> = liveData {
        emitSource(userRepository.loadBuddiesFlow().distinctUntilChanged().asLiveData())
    }

    fun syncBuddies() {
        SyncUsersWorker.requestSync(getApplication())
    }

    fun cancelBuddies() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork(SyncUsersWorker.UNIQUE_WORK_NAME_AD_HOC)
    }

    val userSyncState: LiveData<UserSyncState> = buddies.map { list ->
        val updatedOrNotLists = list.partition { it.updatedTimestamp == 0L }
        val numberOfUnupdatedBuddies = updatedOrNotLists.first.filter { user -> user.updatedTimestamp == 0L }.size
        val oldestSyncedUser = updatedOrNotLists.second.minBy { it.updatedTimestamp }
        UserSyncState(
            updatedOrNotLists.second.size,
            numberOfUnupdatedBuddies,
            oldestSyncedUser.updatedTimestamp,
        )
    }.distinctUntilChanged()

    val userProgress: LiveData<UserSyncProgress> = userWorkInfos.map {
        val workInfo = it?.firstOrNull()
        if (workInfo?.state == WorkInfo.State.RUNNING) {
            val progress = workInfo.progress
            val stepEnum = when (progress.getInt(SyncUsersWorker.PROGRESS_STEP, SyncUsersWorker.PROGRESS_STEP_UNKNOWN)) {
                SyncUsersWorker.PROGRESS_STEP_BUDDY_LIST -> UserSyncProgressStep.BuddyList
                SyncUsersWorker.PROGRESS_STEP_STALE_USERS -> UserSyncProgressStep.StaleUsers
                SyncUsersWorker.PROGRESS_STEP_UNUPDATED_USERS -> UserSyncProgressStep.UnupdatedUsers
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

    data class UserSyncState(val count: Int, val numberOfUnupdatedBuddies: Int, val oldestUpdatedBuddyTimestamp: Long)

    data class UserSyncProgress(val step: UserSyncProgressStep, val username: String? = null, val progress: Int = 0, val max: Int = 0)

    enum class UserSyncProgressStep {
        NotSyncing,
        BuddyList,
        StaleUsers,
        UnupdatedUsers,
    }
}
