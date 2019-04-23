package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.entities.PersonGameEntity
import com.boardgamegeek.entities.PersonImagesEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository
import com.boardgamegeek.repository.DesignerRepository

class PersonViewModel(application: Application) : AndroidViewModel(application) {
    enum class PersonType {
        ARTIST,
        DESIGNER
    }

    private val artistRepository = ArtistRepository(getApplication())
    private val designerRepository = DesignerRepository(getApplication())

    private val _person = MutableLiveData<Pair<PersonType, Int>>()
    val person: LiveData<Pair<PersonType, Int>>
        get() = _person

    fun setArtistId(artistId: Int) {
        if (_person.value?.first != PersonType.ARTIST && _person.value?.second != artistId) _person.value = PersonType.ARTIST to artistId
    }

    fun setDesignerId(designerId: Int) {
        if (_person.value?.first != PersonType.DESIGNER && _person.value?.second != designerId) _person.value = PersonType.DESIGNER to designerId
    }

    val details: LiveData<RefreshableResource<PersonEntity>> = Transformations.switchMap(_person) { person ->
        when (person.second) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.first) {
                    PersonType.ARTIST -> artistRepository.loadArtist(person.second)
                    PersonType.DESIGNER -> designerRepository.loadDesigner(person.second)
                }
            }
        }
    }

    val images: LiveData<RefreshableResource<PersonImagesEntity>> = Transformations.switchMap(_person) { person ->
        when (person.second) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.first) {
                    PersonType.ARTIST -> artistRepository.loadArtistImages(person.second)
                    PersonType.DESIGNER -> designerRepository.loadDesignerImages(person.second)
                }
            }
        }
    }

    val collection: LiveData<List<PersonGameEntity>> = Transformations.switchMap(_person) { person ->
        when (person.second) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> {
                when (person.first) {
                    PersonType.ARTIST -> artistRepository.loadCollection(person.second)
                    PersonType.DESIGNER -> designerRepository.loadCollection(person.second)
                }
            }
        }
    }

    fun refresh() {
        _person.value?.let { _person.value = it.first to it.second }
    }
}