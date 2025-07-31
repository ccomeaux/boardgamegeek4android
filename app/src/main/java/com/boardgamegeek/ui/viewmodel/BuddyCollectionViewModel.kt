package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.R
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.repository.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BuddyCollectionViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {

    private val usernameAndStatus = MutableLiveData<Pair<String, CollectionStatus>>()

    fun setUsername(username: String) {
        if (usernameAndStatus.value?.first != username)
            usernameAndStatus.value = username to (usernameAndStatus.value?.second ?: DEFAULT_STATUS)
    }

    fun setStatus(status: CollectionStatus) {
        FirebaseAnalytics.getInstance(getApplication()).logEvent("Filter") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "BuddyCollection")
            param("filterType", status.toString())
        }
        if (usernameAndStatus.value?.second != status)
            usernameAndStatus.value = (usernameAndStatus.value?.first.orEmpty()) to status
    }

    val status: LiveData<CollectionStatus> = usernameAndStatus.map {
        it.second
    }

    val collection = usernameAndStatus.switchMap {
        liveData {
            emit(RefreshableResource.refreshing(null))
            if (it.first.isBlank()) {
                emit(RefreshableResource.error(application.getString(R.string.error_null_username), null))
            } else {
                try {
                    val collectionItems = userRepository.refreshCollection(it.first, it.second)
                    val map = collectionItems.groupBy { person -> getSectionHeader(person) }
                    emit(RefreshableResource.success(map))
                } catch (e: Exception) {
                    emit(RefreshableResource.error(e, application))
                }
            }
        }
    }

    private fun getSectionHeader(user: CollectionItem?): String {
        return user?.sortName.firstChar()
    }

    companion object {
        val DEFAULT_STATUS = CollectionStatus.Own
    }
}
