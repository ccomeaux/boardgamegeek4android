package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.model.Play
import com.boardgamegeek.repository.PlayRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocationPlaysViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val location = MutableLiveData<String>()

    private val _updateMessage = MutableLiveData<Event<String>>()
    val updateMessage: LiveData<Event<String>>
        get() = _updateMessage

    fun setLocation(name: String) {
        this.location.value = name
    }

    val plays = location.switchMap {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        playRepository.loadPlaysByLocationFlow(it).distinctUntilChanged().asLiveData().map { list ->
            list.groupBy { play ->
                if (play.dateInMillis == Play.UNKNOWN_DATE)
                    DEFAULT_HEADER
                else
                    dateFormat.format(play.dateInMillis)!!
            }
        }
    }

    fun renameLocation(oldLocationName: String, newLocationName: String) {
        viewModelScope.launch {
            val internalIds = playRepository.renameLocation(oldLocationName, newLocationName)
            playRepository.enqueueUploadRequest(internalIds)
            _updateMessage.value = Event(
                getApplication<BggApplication>().resources.getQuantityString(
                    R.plurals.msg_play_location_change,
                    internalIds.size,
                    internalIds.size,
                    oldLocationName,
                    newLocationName
                )
            )
            setLocation(newLocationName)
            FirebaseAnalytics.getInstance(getApplication()).logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Location")
                param("Action", "Edit")
            }
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "?"
    }
}