package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.boardgamegeek.entities.HotGameEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.repository.HotnessRepository

class HotnessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HotnessRepository(getApplication())

    val hotness: LiveData<RefreshableResource<List<HotGameEntity>>> = repository.getHotness()
}
