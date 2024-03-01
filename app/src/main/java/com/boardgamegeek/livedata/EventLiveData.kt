package com.boardgamegeek.livedata

import androidx.lifecycle.LiveData
import java.lang.Exception

class EventLiveData: LiveData<Event<String>>()  {
    fun setMessage(message: String) {
        super.setValue(Event(message))
    }

    fun setMessage(exception: Throwable) {
        super.setValue(Event(exception.localizedMessage.orEmpty()))
    }
    fun postMessage(message: String) {
        super.postValue(Event(message))
    }

    fun postMessage(exception: Throwable) {
        super.postValue(Event(exception.localizedMessage.orEmpty()))
    }
}
