package com.boardgamegeek.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Target
import timber.log.Timber
import java.util.*

class LargeIconLoader(private val context: Context, vararg urls: String, private val callback: Callback?) : Target {
    private val imageUrls: Queue<String> = LinkedList()
    private var currentImageUrl: String? = null

    private val requestCreator: RequestCreator
        get() = Picasso.with(context.applicationContext)
                .load(currentImageUrl.ensureHttpsScheme())
                .resize(WEARABLE_ICON_SIZE, WEARABLE_ICON_SIZE)
                .centerCrop()

    init {
        imageUrls.addAll(urls)
    }

    @WorkerThread
    fun executeInBackground() {
        if (imageUrls.size == 0) {
            callback?.onFailedIconLoad()
            return
        }
        currentImageUrl = imageUrls.poll()
        if (currentImageUrl.isNullOrBlank()) {
            executeInBackground()
        } else {
            try {
                onBitmapLoaded(requestCreator.get(), null)
            } catch (e: Exception) {
                Timber.i("Didn't find an image at %s", currentImageUrl)
                executeInBackground()
            }
        }
    }

    @MainThread
    fun executeOnMainThread() {
        if (imageUrls.size == 0) {
            callback?.onFailedIconLoad()
            return
        }
        currentImageUrl = imageUrls.poll()
        if (currentImageUrl.isNullOrBlank()) {
            executeOnMainThread()
        } else {
            requestCreator.into(this)
        }
    }

    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom?) {
        Timber.d("Found an image at %s", currentImageUrl)
        callback?.onSuccessfulIconLoad(bitmap)
    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
        Timber.d("Didn't find an image at %s", currentImageUrl)
        callback?.onFailedIconLoad()
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

    interface Callback {
        fun onSuccessfulIconLoad(bitmap: Bitmap)

        fun onFailedIconLoad()
    }

    companion object {

        private const val WEARABLE_ICON_SIZE = 400
    }
}
