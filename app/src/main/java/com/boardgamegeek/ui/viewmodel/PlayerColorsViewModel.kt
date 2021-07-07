package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.entities.PlayerColorEntity
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.util.ColorUtils
import kotlinx.coroutines.launch
import timber.log.Timber

class PlayerColorsViewModel(application: Application) : AndroidViewModel(application) {
    private val playRepository = PlayRepository(getApplication())

    private val _user = MutableLiveData<Pair<String?, Int>>()
    val user: LiveData<Pair<String?, Int>>
        get() = _user

    private val _colors = MutableLiveData<List<PlayerColorEntity>?>()
    val colors: LiveData<List<PlayerColorEntity>?>
        get() = _colors

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            user.value?.let {
                val name = it.first
                _colors.value = when {
                    name == null || name.isBlank() -> null
                    it.second == TYPE_USER -> playRepository.loadUserColors(name)
                    it.second == TYPE_PLAYER -> playRepository.loadPlayerColors(name)
                    else -> null
                }
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
        var order = 1
        val newColors = mutableListOf<PlayerColorEntity>()

        val availableColors = ColorUtils.limitedColorList

        val playerDetail = user.value?.let {
            val name = it.first
            when {
                name == null || name.isBlank() -> null
                it.second == TYPE_USER -> playRepository.loadUserPlayerDetail(name)
                it.second == TYPE_PLAYER -> playRepository.loadNonUserPlayerDetail(name)
                else -> null
            }
        }
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
                val color = PlayerColorEntity(description, order++)
                availableColors.remove(availableColors.find { it.first == description })
                newColors.add(color)
            }
        }

        if (availableColors.isNotEmpty()) {
            availableColors.shuffle()
            for (color in availableColors) {
                newColors.add(PlayerColorEntity(color.first, order++))
            }
        }

        _colors.value = newColors
    }

    fun clear() {
        _colors.value = null
    }

    fun add(description: String) {
        val newColors = mutableListOf<PlayerColorEntity>()
        _colors.value?.let {
            newColors.addAll(it)
        }
        newColors.add(PlayerColorEntity(description, newColors.size + 1))
        _colors.value = newColors
    }

    fun add(color: PlayerColorEntity) {
        val newColors = mutableListOf<PlayerColorEntity>()
        _colors.value?.let {
            newColors.addAll(it)
        }
        for (c in newColors) {
            if (c.sortOrder >= color.sortOrder) {
                Timber.d("Moving %s down!", c.description)
                c.sortOrder = c.sortOrder + 1
            }
        }
        newColors.add(color)
        Timber.d("Re-adding %s!", color)
        _colors.value = newColors
    }

    fun remove(color: PlayerColorEntity) {
        Timber.d("Removing %s!", color)
        val newColors = mutableListOf<PlayerColorEntity>()
        _colors.value?.let {
            newColors.addAll(it)
        }
        newColors.remove(color)
        for (c in newColors) {
            if (c.sortOrder >= color.sortOrder) {
                Timber.d("Moving %s up!", c.description)
                c.sortOrder = c.sortOrder - 1
            }
        }
        _colors.value = newColors
    }

    fun move(fromPosition: Int, toPosition: Int): Boolean {
        _colors.value?.let {
            val colorMoving = it[fromPosition]

            val newColors = mutableListOf<PlayerColorEntity>()
            newColors.addAll(it)

            if (fromPosition < toPosition) {
                // dragging down
                for (color in newColors) {
                    if (color.sortOrder > fromPosition + 1 && color.sortOrder <= toPosition + 1) {
                        Timber.d("Moving %s up!", color.description)
                        color.sortOrder = color.sortOrder - 1
                    }
                }
            } else {
                // dragging up
                for (color in newColors) {
                    if (color.sortOrder >= toPosition + 1 && color.sortOrder < fromPosition + 1) {
                        Timber.d("Moving %s down!", color.description)
                        color.sortOrder = color.sortOrder + 1
                    }
                }
            }

            newColors.find { c -> c.description == colorMoving.description }?.let { c ->
                Timber.d("Moving %s to %d!", c.description, toPosition + 1)
                c.sortOrder = toPosition + 1
            }

            _colors.value = newColors.sortedBy { c -> c.sortOrder }

            return true
        }
        return false
    }

    fun save() {
        _user.value?.let {
            val name = it.first
            when {
                name == null || name.isBlank() -> null
                it.second == TYPE_USER -> playRepository.saveUserColors(name, colors.value)
                it.second == TYPE_PLAYER -> playRepository.savePlayerColors(name, colors.value)
                else -> null
            }
        }
    }

    companion object {
        const val TYPE_USER = 1
        const val TYPE_PLAYER = 2
    }
}
