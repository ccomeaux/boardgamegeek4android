package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.entities.ArtistEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ArtistRepository

class ArtistViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepository = ArtistRepository(getApplication())

    private val _artistId = MutableLiveData<Int>()

    fun setArtistId(artistId: Int) {
        if (_artistId.value != artistId) _artistId.value = artistId
    }

    val artist: LiveData<RefreshableResource<ArtistEntity>> = Transformations.switchMap(_artistId) { artistId ->
        when (artistId) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameRepository.loadArtist(artistId)
        }
    }

    fun refresh() {
        _artistId.value?.let { _artistId.value = it }
    }
}