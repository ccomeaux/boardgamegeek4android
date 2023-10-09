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
    private val categoryRepository: CategoryRepository,
    private val collectionItemRepository: CollectionItemRepository,
    private val collectionViewRepository: CollectionViewRepository,
    private val designerRepository: DesignerRepository,
    private val gameRepository: GameRepository,
    private val imageRepository: ImageRepository,
    private val mechanicRepository: MechanicRepository,
    private val playRepository: PlayRepository,
    private val publisherRepository: PublisherRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    fun clearAllData() {
        viewModelScope.launch {
            gameRepository.delete()
            designerRepository.deleteAll()
            artistRepository.deleteAll()
            publisherRepository.deleteAll()
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
