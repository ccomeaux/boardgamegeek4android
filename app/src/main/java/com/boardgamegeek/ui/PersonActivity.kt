package com.boardgamegeek.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.boardgamegeek.R
import com.boardgamegeek.entities.Status
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.PersonPagerAdapter
import com.boardgamegeek.ui.viewmodel.PersonViewModel
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast

class PersonActivity : HeroTabActivity() {
    enum class PersonType {
        ARTIST,
        DESIGNER
    }

    private var id = BggContract.INVALID_ID
    private var name = ""
    private var personType = PersonType.DESIGNER
    private var emptyMessageDescription = ""

    private val viewModel: PersonViewModel by lazy {
        ViewModelProviders.of(this).get(PersonViewModel::class.java)
    }

    private val adapter: PersonPagerAdapter by lazy {
        PersonPagerAdapter(supportFragmentManager, this, id, name, personType)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getIntExtra(KEY_PERSON_ID, BggContract.INVALID_ID)
        name = intent.getStringExtra(KEY_PERSON_NAME)
        personType = (intent.getSerializableExtra(KEY_PERSON_TYPE) as PersonType?) ?: PersonType.DESIGNER
        emptyMessageDescription = getString(R.string.title_person).toLowerCase()

        initializeViewPager()
        safelySetTitle(name)

        if (personType == PersonType.ARTIST) {
            viewModel.setArtistId(id)
            emptyMessageDescription = getString(R.string.title_artist).toLowerCase()
        } else if (personType == PersonType.DESIGNER) {
            viewModel.setDesignerId(id)
            emptyMessageDescription = getString(R.string.title_designer).toLowerCase()
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
                    .putContentType("Person")
                    .putContentId(id.toString())
                    .putContentName(name))
        }
    }

    override fun createAdapter(): FragmentPagerAdapter {
        return adapter
    }

    companion object {
        private const val KEY_PERSON_TYPE = "PERSON_TYPE"
        private const val KEY_PERSON_ID = "PERSON_ID"
        private const val KEY_PERSON_NAME = "PERSON_NAME"

        @JvmStatic
        fun startForArtist(context: Context, id: Int, name: String) {
            context.startActivity(createIntentForArtist(context, id, name))
        }

        @JvmStatic
        fun startForDesigner(context: Context, id: Int, name: String) {
            context.startActivity(createIntentForDesigner(context, id, name))
        }

        @JvmStatic
        fun startUpForArtist(context: Context, id: Int, name: String) {
            context.startActivity(createIntentForArtist(context, id, name).clearTask().clearTop())
        }

        @JvmStatic
        fun startUpForDesigner(context: Context, id: Int, name: String) {
            context.startActivity(createIntentForDesigner(context, id, name).clearTask().clearTop())
        }

        private fun createIntentForArtist(context: Context, id: Int, name: String): Intent {
            return context.intentFor<PersonActivity>(
                    KEY_PERSON_ID to id,
                    KEY_PERSON_NAME to name,
                    KEY_PERSON_TYPE to PersonType.ARTIST
            )
        }

        private fun createIntentForDesigner(context: Context, id: Int, name: String): Intent {
            return context.intentFor<PersonActivity>(
                    KEY_PERSON_ID to id,
                    KEY_PERSON_NAME to name,
                    KEY_PERSON_TYPE to PersonType.DESIGNER
            )
        }
    }
}