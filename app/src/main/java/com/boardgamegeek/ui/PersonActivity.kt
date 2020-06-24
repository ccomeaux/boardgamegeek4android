package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.extensions.linkToBgg
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.PersonPagerAdapter
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import java.util.*

class PersonActivity : HeroTabActivity() {
    enum class PersonType {
        ARTIST,
        DESIGNER,
        PUBLISHER
    }

    private var id = BggContract.INVALID_ID
    private var name = ""
    private var personType = PersonType.DESIGNER
    private var emptyMessageDescription = ""

    private val viewModel by viewModels<PersonViewModel>()

    private val adapter: PersonPagerAdapter by lazy {
        PersonPagerAdapter(this, id, name, personType)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getIntExtra(KEY_PERSON_ID, BggContract.INVALID_ID)
        name = intent.getStringExtra(KEY_PERSON_NAME)
        personType = (intent.getSerializableExtra(KEY_PERSON_TYPE) as PersonType?) ?: PersonType.DESIGNER
        emptyMessageDescription = getString(R.string.title_person).toLowerCase(Locale.getDefault())

        initializeViewPager()
        safelySetTitle(name)

        emptyMessageDescription = when (personType) {
            PersonType.ARTIST -> {
                viewModel.setArtistId(id)
                getString(R.string.title_artist).toLowerCase(Locale.getDefault())
            }
            PersonType.DESIGNER -> {
                viewModel.setDesignerId(id)
                getString(R.string.title_designer).toLowerCase(Locale.getDefault())
            }
            PersonType.PUBLISHER -> {
                viewModel.setPublisherId(id)
                getString(R.string.title_publisher).toLowerCase(Locale.getDefault())
            }
        }

        viewModel.details.observe(this, Observer {
            when {
                it == null -> return@Observer
                it.status == Status.ERROR -> toast(if (it.message.isBlank()) getString(R.string.empty_person, emptyMessageDescription) else it.message)
                it.data == null -> return@Observer
                else -> {
                    it.data.apply {
                        safelySetTitle(name)
                    }
                }
            }
        })
        viewModel.images.observe(this, Observer { resource ->
            resource?.let { entity ->
                entity.data?.let {
                    if (it.heroImageUrl.isBlank()) {
                        loadToolbarImage(it.thumbnailUrl)
                    } else {
                        loadToolbarImage(it.heroImageUrl)
                    }
                }
            }
        })

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Person")
                param(FirebaseAnalytics.Param.ITEM_ID, id.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, name)
            }
        }
    }

    override val optionsMenuId = R.menu.person

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_view -> {
                val path = when (personType) {
                    PersonType.DESIGNER -> "boardgamedesigner"
                    PersonType.ARTIST -> "boardgameartist"
                    PersonType.PUBLISHER -> "boardgamepublisher"
                }
                linkToBgg(path, id)
                return true
            }
            android.R.id.home -> {
                when (personType) {
                    PersonType.DESIGNER -> startActivity(intentFor<DesignersActivity>().clearTop())
                    PersonType.ARTIST -> startActivity(intentFor<ArtistsActivity>().clearTop())
                    PersonType.PUBLISHER -> startActivity(intentFor<PublishersActivity>().clearTop())
                }
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun createAdapter() = adapter

    override fun getPageTitle(position: Int) = adapter.getPageTitle(position)

    companion object {
        private const val KEY_PERSON_TYPE = "PERSON_TYPE"
        private const val KEY_PERSON_ID = "PERSON_ID"
        private const val KEY_PERSON_NAME = "PERSON_NAME"

        fun startForArtist(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.ARTIST))
        }

        fun startForDesigner(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.DESIGNER))
        }

        fun startForPublisher(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.PUBLISHER))
        }

        fun startUpForArtist(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.ARTIST).clearTask().clearTop())
        }

        fun startUpForDesigner(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.DESIGNER).clearTask().clearTop())
        }

        fun startUpForPublisher(context: Context, id: Int, name: String) {
            context.startActivity(createIntent(context, id, name, PersonType.PUBLISHER).clearTask().clearTop())
        }

        private fun createIntent(context: Context, id: Int, name: String, personType: PersonType): Intent {
            return context.intentFor<PersonActivity>(
                    KEY_PERSON_ID to id,
                    KEY_PERSON_NAME to name,
                    KEY_PERSON_TYPE to personType
            )
        }
    }
}