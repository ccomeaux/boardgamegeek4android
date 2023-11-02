package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.repository.PlayRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerColorsViewModel @Inject constructor(
    application: Application,
    private val playRepository: PlayRepository,
) : AndroidViewModel(application) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())

    private val user = MutableLiveData<Pair<String?, PlayRepository.PlayerType>>()

    private val _colors = MutableLiveData<List<String>>()
    val colors: LiveData<List<String>>
        get() = _colors

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            user.value?.let { (name, type) ->
                val loadedColors = playRepository.loadPlayerColors(name.orEmpty(), type)
                _colors.postValue(loadedColors.map { entity -> entity.description })
            }
        }
    }

    fun setUsername(name: String?) {
        if (user.value?.first != name) user.value = (name to PlayRepository.PlayerType.USER)
        load()
    }

    fun setPlayerName(name: String?) {
        if (user.value?.first != name) user.value = (name to PlayRepository.PlayerType.NON_USER)
        load()
    }

    fun generate() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val availableColors = BggColors.standardColorList.map { it.first }.toMutableList()
                val rankedColors = mutableListOf<String>()

                user.value?.let { (name, type) ->
                    val playedColors =  playRepository.loadPlayerUsedColors(name, type)
                    val sortedColors = playedColors.asSequence()
                        .filter { availableColors.contains(it) } // only include available colors
                        .groupBy { it }
                        .map { it.key to it.value.size }
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .toMutableList()
                    while (sortedColors.isNotEmpty()) {
                        val description = sortedColors.removeAt(0)
                        availableColors.remove(availableColors.find { it == description })
                        rankedColors.add(description)
                    }
                }

                if (availableColors.isNotEmpty()) {
                    rankedColors.addAll(availableColors.shuffled())
                }

                _colors.postValue(rankedColors)
            }
            logEvent("Generate")
        }
    }

    fun clear() {
        _colors.value = emptyList()
        logEvent("Clear")
    }

    fun add(color: String) {
        _colors.value?.let {
            if (!it.contains(color)) {
                val list = it.toMutableList()
                list.add(color)
                _colors.value = list
            }
        }
        logEvent("Add", color)
    }

    fun add(color: String, index: Int) {
        _colors.value?.let {
            if (!it.contains(color)) {
                val list = it.toMutableList()
                list.add(index, color)
                _colors.value = list
            }
        }
        logEvent("AddAtIndex", color)
    }

    fun remove(color: String) {
        _colors.value?.let {
            if (it.contains(color)) {
                val list = it.toMutableList()
                list.remove(color)
                _colors.value = list
            }
        }
        logEvent("Delete", color)
    }

    fun move(fromPosition: Int, toPosition: Int): Boolean {
        _colors.value?.let {
            val newColors = it.toMutableList()
            val movingColor = newColors.removeAt(fromPosition)
            newColors.add(toPosition, movingColor)
            _colors.value = newColors
            return true
        }
        return false
    }

    fun save() {
        viewModelScope.launch {
            user.value?.let { (name, type) ->
                playRepository.savePlayerColors(name, type, colors.value)
            }
        }
    }

    private fun logEvent(action: String, color: String? = null) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
            param("Action", action)
            color?.let { param("Color", it) }
        }
    }
}
