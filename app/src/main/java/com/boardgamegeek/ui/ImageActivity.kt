package com.boardgamegeek.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.util.PaletteTransformation
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_image.*
import org.jetbrains.anko.startActivity
import timber.log.Timber

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image)

        val imageUrl = intent.getStringExtra(KEY_IMAGE_URL)
        if (imageUrl.isNullOrBlank()) {
            Timber.w("Received an empty imageUrl")
            finish()
            return
        }

        if (savedInstanceState == null) {
            val imageId = Uri.parse(imageUrl).lastPathSegment
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("Image")
                    .putContentId(imageId))
        }

        Picasso.with(this)
                .load(imageUrl.ensureHttpsScheme())
                .error(R.drawable.thumbnail_image_empty)
                .fit()
                .centerInside()
                .transform(PaletteTransformation.instance())
                .into(imageView, object : Callback.EmptyCallback() {
                    override fun onSuccess() {
                        setBackgroundColor()
                        progressBar.visibility = View.GONE
                    }
                })
    }

    private fun setBackgroundColor() {
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val palette = PaletteTransformation.getPalette(bitmap)
        val swatch: Palette.Swatch? = palette.darkMutedSwatch ?: palette.darkVibrantSwatch ?: palette.mutedSwatch
        imageView.setBackgroundColor(swatch?.rgb ?: Color.BLACK)
    }

    companion object {
        private const val KEY_IMAGE_URL = "IMAGE_URL"

        @JvmStatic
        fun start(context: Context, imageUrl: String?) {
            if (imageUrl.isNullOrBlank()) {
                Timber.w("Missing the required image URL.")
                return
            }
            context.startActivity<ImageActivity>(KEY_IMAGE_URL to imageUrl)
        }
    }
}