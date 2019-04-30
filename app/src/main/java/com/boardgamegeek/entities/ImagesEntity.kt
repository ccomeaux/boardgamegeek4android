package com.boardgamegeek.entities

import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.Image
import com.boardgamegeek.util.ImageUtils
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

interface ImagesEntity {
    val imagesEntityDescription: String
    val imageUrl: String
    val thumbnailUrl: String
    val heroImageUrl: String

    fun maybeRefreshHeroImageUrl(entityTypeDescription: String, started: AtomicBoolean, successListener: (String) -> Unit = {}) {
        val heroImageId = ImageUtils.getImageId(heroImageUrl)
        val thumbnailId = ImageUtils.getImageId(thumbnailUrl)
        if (heroImageId != thumbnailId && started.compareAndSet(false, true)) {
            val call = Adapter.createGeekdoApi().image(thumbnailId)
            call.enqueue(object : retrofit2.Callback<Image> {
                override fun onResponse(call: Call<Image>?, response: Response<Image>?) {
                    if (response?.isSuccessful == true) {
                        val body = response.body()
                        if (body != null) {
                            successListener(body.images.medium.url)
                        } else {
                            Timber.w("Empty body while fetching image $thumbnailId for $entityTypeDescription $imagesEntityDescription")
                        }
                    } else {
                        val message = response?.message() ?: response?.code().toString()
                        Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for $entityTypeDescription $imagesEntityDescription")
                    }
                    started.set(false)
                }

                override fun onFailure(call: Call<Image>?, t: Throwable?) {
                    val message = t?.localizedMessage ?: "Unknown error"
                    Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for $entityTypeDescription $imagesEntityDescription")
                    started.set(false)
                }
            })
        }
    }
}
