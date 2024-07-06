package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityImageBinding
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.extensions.startActivity
import com.boardgamegeek.util.PaletteTransformation
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import timber.log.Timber

class ImageActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var binding : ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra(KEY_IMAGE_URL)
        if (imageUrl.isNullOrBlank()) {
            Timber.w("Received an empty imageUrl")
            finish()
            return
        }

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Image")
                param(FirebaseAnalytics.Param.ITEM_ID, imageUrl.toUri().lastPathSegment.orEmpty())
            }
        }

        Picasso.with(this)
            .load(imageUrl.ensureHttpsScheme())
            .error(R.drawable.thumbnail_image_empty)
            .fit()
            .centerInside()
            .transform(PaletteTransformation.instance())
            .into(binding.imageView, object : Callback.EmptyCallback() {
                override fun onSuccess() {
                    setBackgroundColor()
                    binding.progressBar.isVisible = false
                }
            })
    }

    private fun setBackgroundColor() {
        val bitmap = (binding.imageView.drawable as BitmapDrawable).bitmap
        val palette = PaletteTransformation.getPalette(bitmap)
        val swatch = palette?.darkMutedSwatch ?: palette?.darkVibrantSwatch ?: palette?.mutedSwatch
        binding.imageView.setBackgroundColor(swatch?.rgb ?: Color.BLACK)
    }

    companion object {
        private const val KEY_IMAGE_URL = "IMAGE_URL"

        fun start(context: Context, imageUrl: String?) {
            if (imageUrl.isNullOrBlank()) {
                Timber.w("Missing the required image URL.")
                return
            }
            context.startActivity<ImageActivity>(KEY_IMAGE_URL to imageUrl)
        }
    }
}
