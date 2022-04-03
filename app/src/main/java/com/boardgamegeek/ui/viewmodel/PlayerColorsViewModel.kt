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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerColorsViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())
    private val playRepository = PlayRepository(getApplication())

    private val _user = MutableLiveData<Pair<String?, Int>>()
    val user: LiveData<Pair<String?, Int>>
        get() = _user

    private val _colors = MutableLiveData<List<String>>()
    val colors: LiveData<List<String>>
        get() = _colors

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            user.value?.let {
                val name = it.first
                val loadedColors = when {
                    name == null || name.isBlank() -> null
                    it.second == TYPE_USER -> playRepository.loadUserColors(name)
                    it.second == TYPE_PLAYER -> playRepository.loadPlayerColors(name)
                    else -> null
                }
                _colors.postValue(loadedColors?.map { entity -> entity.description } ?: emptyList())
            }
        }
    }

    fun setUsername(name: String?) {
        if (_user.value?.first != name) _user.value = (name to TYPE_USER)
        load()
    }

    fun setPlayerName(name: String?) {
        if (_user.value?.first != name) _user.value = (name to TYPE_PLAYER)
        load()
    }

    fun generate() {
        viewModelScope.launch {
            val availableColors = BggColors.standardColorList.toMutableList()
            val playerDetail = user.value?.let {
                val name = it.first
                when {
                    name == null || name.isBlank() -> null
                    it.second == TYPE_USER -> playRepository.loadUserPlayerDetail(name)
                    it.second == TYPE_PLAYER -> playRepository.loadNonUserPlayerDetail(name)
                    else -> null
                }
            }
            withContext(Dispatchers.Default) {
                val newColors = mutableListOf<String>()
                if (playerDetail != null) {
                    val colorNames = availableColors.map { it.first }
                    val playedColors = playerDetail.asSequence()
                        .filter { colorNames.contains(it.color) } // only include known colors
                        .groupBy { it.color }
                        .map { it.key to it.value.size }
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .toMutableList()
                    while (playedColors.isNotEmpty()) {
                        val description = playedColors.removeAt(0)
                        availableColors.remove(availableColors.find { it.first == description })
                        newColors.add(description)
                    }
                }

                if (availableColors.isNotEmpty()) {
                    availableColors.shuffle()
                    for (color in availableColors) {
                        newColors += color.first
                    }
                }

                _colors.postValue(newColors)
            }
            firebaseAnalytics.logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                param("Action", "Generate")
            }
        }
    }

    fun clear() {
        _colors.value = emptyList()
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
            param("Action", "Clear")
        }
    }

    fun add(color: String) {
        _colors.value?.let {
            if (!it.contains(color)) {
                val list = it.toMutableList()
                list.add(color)
                _colors.value = list
            }
        }
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
            param("Action", "Add")
            param("Color", color)
        }
    }

    fun add(color: String, index: Int) {
        _colors.value?.let {
            if (!it.contains(color)) {
                val list = it.toMutableList()
                list.add(index, color)
                _colors.value = list
            }
        }
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
            param("Action", "AddAtIndex")
            param("Color", color)
        }
    }

    fun remove(color: String) {
        _colors.value?.let {
            if (it.contains(color)) {
                val list = it.toMutableList()
                list.remove(color)
                _colors.value = list
            }
        }
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
            param("Action", "Delete")
            param("Color", color)
        }
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
        _user.value?.let {
            viewModelScope.launch {
                val name = it.first.orEmpty()
                if (name.isNotBlank()) {
                    when (it.second) {
                        TYPE_USER -> playRepository.saveUserColors(name, colors.value)
                        TYPE_PLAYER -> playRepository.savePlayerColors(name, colors.value)
                    }
                }
            }
        }
    }

    companion object {
        const val TYPE_USER = 1
        const val TYPE_PLAYER = 2
    }
}
