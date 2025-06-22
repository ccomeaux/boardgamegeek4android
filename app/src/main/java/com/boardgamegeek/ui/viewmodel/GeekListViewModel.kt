package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.model.GeekList
import com.boardgamegeek.model.RefreshableResource
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.GeekListRepository
import com.boardgamegeek.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class GeekListViewModel @Inject constructor(
    application: Application,
    private val geekListRepository: GeekListRepository,
    private val imageRepository: ImageRepository,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    private val _geekListId = MutableLiveData<Int>()

    fun setId(geekListId: Int) {
        if (_geekListId.value != geekListId) _geekListId.value = geekListId
    }

    val geekList: LiveData<RefreshableResource<GeekList>> = _geekListId.switchMap { id ->
        liveData {
            emit(RefreshableResource.refreshing(latestValue?.data))
            if (id == BggContract.INVALID_ID) {
                emit(RefreshableResource.error("Invalid ID!"))
            } else {
                try {
                    val geekList = geekListRepository.getGeekList(id)
                    emit(RefreshableResource.refreshing(geekList))
                    val itemsWithImages = geekList.items.toMutableList()
                    itemsWithImages.forEachIndexed { index, it ->
                        if (it.thumbnailUrls == null || it.heroImageUrls == null) {
                            val urlPair = if (it.imageId == 0) {
                                val url = gameRepository.fetchGameThumbnail(it.objectId)
                                listOf(url.orEmpty()) to listOf(url.orEmpty())
                            } else {
                                val urls = imageRepository.getImageUrls(it.imageId)
                                urls[ImageRepository.ImageType.THUMBNAIL] to urls[ImageRepository.ImageType.HERO]
                            }
                            itemsWithImages[index] = it.copy(thumbnailUrls = urlPair.first, heroImageUrls = urlPair.second)
                            emit(RefreshableResource.refreshing(geekList.copy(items = itemsWithImages)))
                            delay(500)
                        }
                    }
                    emit(RefreshableResource.success(geekList.copy(items = itemsWithImages)))
                } catch (e: Exception) {
                    emit(RefreshableResource.error(e, application))
                }
            }
        }
    }
}
