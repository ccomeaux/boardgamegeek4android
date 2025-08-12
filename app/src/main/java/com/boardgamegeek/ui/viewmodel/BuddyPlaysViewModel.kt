package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.repository.PlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BuddyPlaysViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {

    private val username = MutableLiveData<String>()

    fun setUsername(username: String) {
        this.username.value = username
    }

    val plays = username.switchMap {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        playRepository.loadPlaysByUsernameFlow(it).distinctUntilChanged().asLiveData().map { list ->
            list.groupBy { play ->
                if (play.dateInMillis == Play.UNKNOWN_DATE)
                    DEFAULT_HEADER
                else
                    dateFormat.format(play.dateInMillis)!!
            }
        }
    }

    companion object {
        private const val DEFAULT_HEADER = "?"
    }
}