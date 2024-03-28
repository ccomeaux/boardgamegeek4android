package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import com.boardgamegeek.R
import com.boardgamegeek.model.Status
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.PersonPagerAdapter
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@AndroidEntryPoint
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
    private var automaticRefreshTimestamp = 0L

    private val viewModel by viewModels<PersonViewModel>()

    private val adapter: PersonPagerAdapter by lazy {
        PersonPagerAdapter(this, id, name, personType)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getIntExtra(KEY_PERSON_ID, BggContract.INVALID_ID)
        name = intent.getStringExtra(KEY_PERSON_NAME).orEmpty()
        personType = intent.getSerializableCompat(KEY_PERSON_TYPE) ?: PersonType.DESIGNER
        emptyMessageDescription = getString(R.string.title_person).lowercase(Locale.getDefault())

        initializeViewPager()
        safelySetTitle(name)

        emptyMessageDescription = when (personType) {
            PersonType.ARTIST -> {
                viewModel.setArtistId(id)
                getString(R.string.title_artist).lowercase(Locale.getDefault())
            }
            PersonType.DESIGNER -> {
                viewModel.setDesignerId(id)
                getString(R.string.title_designer).lowercase(Locale.getDefault())
            }
            PersonType.PUBLISHER -> {
                viewModel.setPublisherId(id)
                getString(R.string.title_publisher).lowercase(Locale.getDefault())
            }
        }

        viewModel.details.observe(this) {
            if (it?.status == Status.ERROR) {
                toast(it.message.ifBlank { getString(R.string.empty_person, emptyMessageDescription) })
            }
            it?.data?.let { person ->
                if (it.status == Status.SUCCESS && automaticRefreshTimestamp.isOlderThan(3.hours)) {
                    automaticRefreshTimestamp = System.currentTimeMillis()
                    if (person.updatedTimestamp?.time.isOlderThan(1.days) ||
                        person.imagesUpdatedTimestamp?.time.isOlderThan(1.days)
                    ) {
                        viewModel.refresh()
                    }
                }
                safelySetTitle(person.name)
                loadToolbarImage(person.heroImageUrls)
            }
        }

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
                @Suppress("SpellCheckingInspection")
                val path = when (personType) {
                    PersonType.DESIGNER -> "boardgamedesigner"
                    PersonType.ARTIST -> "boardgameartist"
                    PersonType.PUBLISHER -> "boardgamepublisher"
                }
                linkToBgg(path, id)
            }
            android.R.id.home -> {
                when (personType) {
                    PersonType.DESIGNER -> startActivity(intentFor<DesignersActivity>().clearTop())
                    PersonType.ARTIST -> startActivity(intentFor<ArtistsActivity>().clearTop())
                    PersonType.PUBLISHER -> startActivity(intentFor<PublishersActivity>().clearTop())
                }
                finish()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
                KEY_PERSON_TYPE to personType,
            )
        }
    }
}
