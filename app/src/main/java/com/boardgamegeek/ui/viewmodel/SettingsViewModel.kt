package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val artistRepository: ArtistRepository,
    private val designerRepository: DesignerRepository,
    private val playRepository: PlayRepository,
    private val publisherRepository: PublisherRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    private val categoryRepository = CategoryRepository(getApplication())
    private val collectionItemRepository = CollectionItemRepository(getApplication())
    private val collectionViewRepository = CollectionViewRepository(getApplication())
    private val gameRepository = GameRepository(getApplication(), playRepository)
    private val imageRepository = ImageRepository(getApplication())
    private val mechanicRepository = MechanicRepository(getApplication())

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
