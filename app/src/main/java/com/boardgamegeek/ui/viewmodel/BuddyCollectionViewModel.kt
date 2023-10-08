package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.entities.CollectionItem
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.repository.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BuddyCollectionViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {

    private val usernameAndStatus = MutableLiveData<Pair<String, String>>()

    fun setUsername(username: String) {
        if (usernameAndStatus.value?.first != username)
            usernameAndStatus.value = username to (usernameAndStatus.value?.second ?: DEFAULT_STATUS)
    }

    fun setStatus(status: String) {
        FirebaseAnalytics.getInstance(getApplication()).logEvent("Filter") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
            param("filterType", status)
        }
        if (usernameAndStatus.value?.second != status) usernameAndStatus.value =
            (usernameAndStatus.value?.first.orEmpty()) to status
    }

    val status: LiveData<String> = usernameAndStatus.map {
        it.second
    }

    val collection: LiveData<RefreshableResource<List<CollectionItem>>> =
        usernameAndStatus.switchMap {
            liveData {
                emit(RefreshableResource.refreshing(null))
                try {
                    emit(RefreshableResource.success(userRepository.refreshCollection(it.first, it.second)))
                } catch (e: Exception) {
                    emit(RefreshableResource.error(e, application))
                }
            }
        }

    companion object {
        const val DEFAULT_STATUS = "own"
    }
}
