package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.GameColors
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.ui.adapter.BuddyNameAdapter
import com.boardgamegeek.ui.adapter.GameColorAdapter
import com.boardgamegeek.ui.adapter.PlayerNameAdapter
import com.boardgamegeek.ui.dialog.ColorPickerWithListenerDialogFragment
import com.boardgamegeek.util.HelpUtils
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import com.boardgamegeek.util.ShowcaseViewWizard
import com.boardgamegeek.util.ToolbarUtils
import com.boardgamegeek.util.UIUtils
import com.github.amlcurran.showcaseview.targets.Target
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.activity_logplayer.*
import org.jetbrains.anko.defaultSharedPreferences
import java.util.*

class LogPlayerActivity : AppCompatActivity(), ColorPickerWithListenerDialogFragment.Listener {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var gameName = ""
    private var position = 0
    private var player = Player()
    private var originalPlayer: Player? = null

    private var showcaseWizard: ShowcaseViewWizard? = null
    private var userHasShownTeamColor = false
    private var userHasShownPosition = false
    private var userHasShownScore = false
    private var userHasShownRating = false
    private var userHasShownNew = false
    private var userHasShownWin = false
    private var autoPosition = Player.SEAT_UNKNOWN
    private var isNewPlayer = false
    private var usedColors: ArrayList<String>? = null
    private var colors = arrayListOf<String>()

    @SuppressLint("HandlerLeak")
    private inner class QueryHandler(cr: ContentResolver?) : AsyncQueryHandler(cr) {
        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            if (cursor == null) {
                return
            }
            if (isFinishing) {
                cursor.close()
                return
            }
            cursor.use { c ->
                if (c.moveToFirst()) {
                    colors = ArrayList()
                    do {
                        colors.add(cursor.getString(0))
                    } while (c.moveToNext())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logplayer)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        nameView.setOnItemClickListener { _, view, _, _ ->
            usernameView.setText(view.tag as String)
        }
        usernameView.setOnItemClickListener { _, view, _, _ ->
            nameView.setText(view.tag as String)
        }
        teamColorView.doAfterTextChanged {
            colorView.setColorViewValue(it?.toString().asColorRgb())
        }
        colorView.setOnClickListener {
            showAndSurvive(ColorPickerWithListenerDialogFragment.newInstance(colors, teamColorView.text.toString(), usedColors))
        }
        positionContainer.setEndIconOnClickListener {
            onNumberToTextClick(positionContainer, positionView, false)
        }
        scoreContainer.setEndIconOnClickListener {
            onNumberToTextClick(scoreContainer, scoreView, true)
        }
        fab.setOnClickListener { addField() }

        ToolbarUtils.setDoneCancelActionBarView(this) { v: View ->
            when (v.id) {
                R.id.menu_done -> save()
                R.id.menu_cancel -> cancel()
            }
        }

        val gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        position = intent.getIntExtra(KEY_POSITION, INVALID_POSITION)
        gameName = intent.getStringExtra(KEY_GAME_NAME)
        val imageUrl = intent.getStringExtra(KEY_IMAGE_URL) ?: ""
        val thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL) ?: ""
        val heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL) ?: ""
        autoPosition = intent.getIntExtra(KEY_AUTO_POSITION, Player.SEAT_UNKNOWN)
        val usedColors = intent.getStringArrayExtra(KEY_USED_COLORS)

        if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
            userHasShownScore = true
            scoreView.requestFocus()
        }
        isNewPlayer = intent.getBooleanExtra(KEY_NEW_PLAYER, false)

        fab.colorize(intent.getIntExtra(KEY_FAB_COLOR, ContextCompat.getColor(this, R.color.accent)))
        if (savedInstanceState == null) {
            player = intent.getParcelableExtra(KEY_PLAYER) ?: Player()
            if (hasAutoPosition()) {
                player.seat = autoPosition
            }
            originalPlayer = Player(player)
        } else {
            player = savedInstanceState.getParcelable(KEY_PLAYER) ?: Player()
            userHasShownTeamColor = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_TEAM_COLOR)
            userHasShownPosition = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_POSITION)
            userHasShownScore = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_SCORE)
            userHasShownRating = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_RATING)
            userHasShownNew = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_NEW)
            userHasShownWin = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_WIN)
        }

        this.usedColors = if (usedColors == null) ArrayList() else ArrayList(listOf(*usedColors))
        this.usedColors?.remove(player.color)

        thumbnailView.safelyLoadImage(imageUrl, thumbnailUrl, heroImageUrl)
        bindUi()
        QueryHandler(contentResolver).startQuery(0, null, Games.buildColorsUri(gameId), arrayOf(GameColors.COLOR), null, null, null)
        nameView.setAdapter(PlayerNameAdapter(this))
        usernameView.setAdapter(BuddyNameAdapter(this))
        teamColorView.setAdapter(GameColorAdapter(this, gameId))
        setUpShowcaseViewWizard()
        showcaseWizard?.maybeShowHelp()
    }

    override fun onResume() {
        super.onResume()
        setViewVisibility()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_PLAYER, player)
        outState.putBoolean(KEY_USER_HAS_SHOWN_TEAM_COLOR, userHasShownTeamColor)
        outState.putBoolean(KEY_USER_HAS_SHOWN_POSITION, userHasShownPosition)
        outState.putBoolean(KEY_USER_HAS_SHOWN_SCORE, userHasShownScore)
        outState.putBoolean(KEY_USER_HAS_SHOWN_RATING, userHasShownRating)
        outState.putBoolean(KEY_USER_HAS_SHOWN_NEW, userHasShownNew)
        outState.putBoolean(KEY_USER_HAS_SHOWN_WIN, userHasShownWin)
    }

    override fun onBackPressed() {
        cancel()
    }

    override fun onColorSelected(description: String, color: Int, requestCode: Int) {
        teamColorView.setText(description)
    }

    private fun onNumberToTextClick(til: TextInputLayout, editText: EditText, includeSign: Boolean) {
        if (editText.inputType and InputType.TYPE_CLASS_NUMBER == InputType.TYPE_CLASS_NUMBER) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            til.setEndIconDrawable(R.drawable.ic_dialpad)
        } else {
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or if (includeSign) {
                editText.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED
            } else {
                editText.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED.inv()
            }
            til.setEndIconDrawable(R.drawable.ic_keyboard)
        }
        UIUtils.focusWithKeyboard(editText)
    }

    private fun setUpShowcaseViewWizard() {
        showcaseWizard = ShowcaseViewWizard(this, HelpUtils.HELP_LOGPLAYER_KEY, HELP_VERSION)
        showcaseWizard?.addTarget(R.string.help_logplayer, Target.NONE)
    }

    private fun bindUi() {
        if (hasAutoPosition()) {
            titleView.text = gameName
            subtitleView.text = getString(R.string.generic_player, autoPosition)
        } else {
            headerView.text = gameName
        }
        headerView.isVisible = !hasAutoPosition()
        twoLineContainer.isVisible = hasAutoPosition()

        nameView.setTextKeepState(player.name)
        usernameView.setTextKeepState(player.username)
        teamColorView.setTextKeepState(player.color)
        if (player.startingPosition != null) {
            positionView.setTextKeepState(player.startingPosition)
        }
        scoreView.setTextKeepState(player.score)
        ratingView.setTextKeepState(if (player.rating == Player.DEFAULT_RATING) "" else player.rating.toString())
        newView.isChecked = player.isNew
        winView.isChecked = player.isWin
    }

    private fun setViewVisibility() {
        teamColorContainer.isVisible = !shouldHideTeamColor()
        positionContainer.isVisible = !hasAutoPosition() && !shouldHidePosition()
        scoreContainer.isVisible = !shouldHideScore()
        ratingContainer.isVisible = !shouldHideRating()
        newView.isVisible = !shouldHideNew()
        winView.isVisible = !shouldHideWin()

        val enableButton = createAddFieldArray().isNotEmpty()
        if (enableButton) {
            fab.show()
        } else {
            fab.hide()
        }
        fabBuffer.isVisible = enableButton
    }

    private fun shouldHideTeamColor(): Boolean {
        return !defaultSharedPreferences.showLogPlayerTeamColor() && !userHasShownTeamColor && player.color.isNullOrEmpty()
    }

    private fun shouldHidePosition(): Boolean {
        return !defaultSharedPreferences.showLogPlayerPosition() && !userHasShownPosition && player.startingPosition.isNullOrEmpty()
    }

    private fun shouldHideScore(): Boolean {
        return !defaultSharedPreferences.showLogPlayerScore() && !userHasShownScore && player.score.isNullOrEmpty()
    }

    private fun shouldHideRating(): Boolean {
        return !defaultSharedPreferences.showLogPlayerRating() && !userHasShownRating && player.rating <= 0
    }

    private fun shouldHideNew(): Boolean {
        return !defaultSharedPreferences.showLogPlayerNew() && !userHasShownNew && !player.isNew
    }

    private fun shouldHideWin(): Boolean {
        return !defaultSharedPreferences.showLogPlayerWin() && !userHasShownWin && !player.isWin
    }

    private fun hasAutoPosition(): Boolean {
        return autoPosition != Player.SEAT_UNKNOWN
    }

    private fun addField() {
        val array = createAddFieldArray()
        if (array.isEmpty()) return
        AlertDialog.Builder(this).setTitle(R.string.add_field)
                .setItems(array) { _, which ->
                    val selection = array[which].toString()
                    val views = when (selection) {
                        resources.getString(R.string.team_color) -> {
                            userHasShownTeamColor = true
                            teamColorView to teamColorContainer
                        }
                        resources.getString(R.string.starting_position) -> {
                            userHasShownPosition = true
                            positionView to positionContainer
                        }
                        resources.getString(R.string.score) -> {
                            userHasShownScore = true
                            scoreView to scoreContainer
                        }
                        resources.getString(R.string.rating) -> {
                            userHasShownRating = true
                            ratingView to ratingContainer
                        }
                        resources.getString(R.string.new_label) -> {
                            userHasShownNew = true
                            newView.isChecked = true
                            newView to newView
                        }
                        resources.getString(R.string.win) -> {
                            userHasShownWin = true
                            winView.isChecked = true
                            winView to winView
                        }
                        else -> null to null
                    }
                    firebaseAnalytics.logEvent("AddField") {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Player")
                        param(FirebaseAnalytics.Param.ITEM_NAME, selection)
                    }
                    setViewVisibility()
                    views.first?.requestFocus()
                    views.second?.let {
                        scrollContainer.post { scrollContainer.smoothScrollTo(0, it.bottom) }
                    }
                }.show()
    }

    private fun createAddFieldArray(): Array<CharSequence> {
        val list = mutableListOf<CharSequence>()
        if (shouldHideTeamColor()) {
            list.add(resources.getString(R.string.team_color))
        }
        if (!hasAutoPosition() && shouldHidePosition()) {
            list.add(resources.getString(R.string.starting_position))
        }
        if (shouldHideScore()) {
            list.add(resources.getString(R.string.score))
        }
        if (shouldHideRating()) {
            list.add(resources.getString(R.string.rating))
        }
        if (shouldHideNew()) {
            list.add(resources.getString(R.string.new_label))
        }
        if (shouldHideWin()) {
            list.add(resources.getString(R.string.win))
        }
        return list.toTypedArray()
    }

    private fun save() {
        captureForm()
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(KEY_PLAYER, player)
            putExtra(KEY_POSITION, position)
        })
        finish()
    }

    private fun cancel() {
        captureForm()
        if (player == originalPlayer) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else {
            createDiscardDialog(this, R.string.player, isNew = isNewPlayer).show()
        }
    }

    private fun captureForm() {
        player.apply {
            name = nameView.text.toString().trim()
            username = usernameView.text.toString().trim()
            color = teamColorView.text.toString().trim()
            startingPosition = positionView.text.toString().trim()
            score = scoreView.text.toString().trim()
            rating = ratingView.text.toString().toDoubleOrNull() ?: 0.0
            isNew = newView.isChecked
            isWin = winView.isChecked
        }
    }

    companion object {
        const val KEY_GAME_ID = "GAME_ID"
        const val KEY_GAME_NAME = "GAME_NAME"
        const val KEY_IMAGE_URL = "IMAGE_URL"
        const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        const val KEY_AUTO_POSITION = "AUTO_POSITION"
        const val KEY_USED_COLORS = "USED_COLORS"
        const val KEY_END_PLAY = "SCORE_SHOWN"
        const val KEY_PLAYER = "PLAYER"
        const val KEY_FAB_COLOR = "FAB_COLOR"
        const val KEY_POSITION = "POSITION"
        const val KEY_NEW_PLAYER = "NEW_PLAYER"
        const val INVALID_POSITION = -1
        const val KEY_USER_HAS_SHOWN_TEAM_COLOR = "USER_HAS_SHOWN_TEAM_COLOR"
        const val KEY_USER_HAS_SHOWN_POSITION = "USER_HAS_SHOWN_POSITION"
        const val KEY_USER_HAS_SHOWN_SCORE = "USER_HAS_SHOWN_SCORE"
        const val KEY_USER_HAS_SHOWN_RATING = "USER_HAS_SHOWN_RATING"
        const val KEY_USER_HAS_SHOWN_NEW = "USER_HAS_SHOWN_NEW"
        const val KEY_USER_HAS_SHOWN_WIN = "USER_HAS_SHOWN_WIN"
        private const val HELP_VERSION = 2
    }
}
