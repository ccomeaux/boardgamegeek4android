package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.repository.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository = ArtistRepository(getApplication())
    private val categoryRepository = CategoryRepository(getApplication())
    private val collectionItemRepository = CollectionItemRepository(getApplication())
    private val collectionViewRepository = CollectionViewRepository(getApplication())
    private val designerRepository = DesignerRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())
    private val imageRepository = ImageRepository(getApplication())
    private val mechanicRepository = MechanicRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())
    private val publisherRepository = PublisherRepository(getApplication())
    private val userRepository = UserRepository(getApplication())

    fun clearAllData() {
        viewModelScope.launch {
            gameRepository.delete()
            designerRepository.delete()
            artistRepository.delete()
            publisherRepository.delete()
            categoryRepository.delete()
            mechanicRepository.delete()
            collectionViewRepository.delete()
            playRepository.deletePlays()
            userRepository.deleteUsers()
            imageRepository.delete()
        }
    }

    fun resetCollectionItems() {
        viewModelScope.launch {
            collectionItemRepository.resetCollectionItems()
        }
    }

    fun resetPlays() {
        viewModelScope.launch {
            playRepository.resetPlays()
        }
    }

    fun resetUsers() {
        viewModelScope.launch {
            userRepository.resetUsers()
        }
    }
}
