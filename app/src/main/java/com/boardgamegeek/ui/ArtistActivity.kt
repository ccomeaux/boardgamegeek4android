package com.boardgamegeek.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.ArtistPagerAdapter
import com.boardgamegeek.ui.viewmodel.ArtistViewModel
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class ArtistActivity : HeroTabActivity() {
    private var id = BggContract.INVALID_ID
    private var name = ""

    private val viewModel: ArtistViewModel by lazy {
        ViewModelProviders.of(this).get(ArtistViewModel::class.java)
    }

    private val adapter: ArtistPagerAdapter by lazy {
        ArtistPagerAdapter(supportFragmentManager, this, id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getIntExtra(KEY_ARTIST_ID, BggContract.INVALID_ID)
        name = intent.getStringExtra(KEY_ARTIST_NAME)

        initializeViewPager()
        safelySetTitle(name)

        viewModel.setArtistId(id)
        viewModel.artist.observe(this, Observer {
            when {
                it == null -> return@Observer
                it.status == Status.ERROR -> toast(if (it.message.isBlank()) getString(R.string.empty_artist, it.toString()) else it.message)
                it.data == null -> return@Observer
                else -> {
                    it.data.apply {
                        safelySetTitle(name)
                    }
                }
            }
        })
        viewModel.artistImages.observe(this, Observer { resource ->
            resource?.let { entity ->
                if (entity.status == Status.SUCCESS) entity.data?.let {
                    if (it.heroImageUrl.isBlank()) {
                        loadToolbarImage(it.thumbnailUrl)
                    } else {
                        loadToolbarImage(it.heroImageUrl)
                    }
                }
            }
        })

        if (savedInstanceState == null) {
            Answers.getInstance().logContentView(ContentViewEvent()
                    .putContentType("Artist")
                    .putContentId(id.toString())
                    .putContentName(name))
        }
    }

    override fun createAdapter(): FragmentPagerAdapter {
        return adapter
    }

    companion object {
        private const val KEY_ARTIST_ID = "ARTIST_ID"
        private const val KEY_ARTIST_NAME = "ARTIST_NAME"

        @JvmStatic
        fun start(context: Context, id: Int, name: String) {
            context.startActivity<ArtistActivity>(
                    KEY_ARTIST_ID to id,
                    KEY_ARTIST_NAME to name
            )
        }
    }
}