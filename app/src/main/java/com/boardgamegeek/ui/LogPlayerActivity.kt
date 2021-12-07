package com.boardgamegeek.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.BuddyNameAdapter
import com.boardgamegeek.ui.adapter.GameColorAdapter
import com.boardgamegeek.ui.adapter.PlayerNameAdapter
import com.boardgamegeek.ui.dialog.ColorPickerWithListenerDialogFragment
import com.boardgamegeek.ui.viewmodel.LogPlayerViewModel
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.activity_logplayer.*
import kotlinx.coroutines.launch

class LogPlayerActivity : AppCompatActivity(R.layout.activity_logplayer), ColorPickerWithListenerDialogFragment.Listener {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val viewModel by viewModels<LogPlayerViewModel>()

    private var gameName = ""
    private var position = 0
    private var player = PlayPlayerEntity()
    private var originalPlayer: PlayPlayerEntity? = null

    private var userHasShownTeamColor = false
    private var userHasShownPosition = false
    private var userHasShownScore = false
    private var userHasShownRating = false
    private var userHasShownNew = false
    private var userHasShownWin = false
    private var autoPosition = PlayPlayerEntity.SEAT_UNKNOWN
    private var isNewPlayer = false
    private var usedColors: ArrayList<String>? = null
    private val colors = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setDoneCancelActionBarView { v: View ->
            when (v.id) {
                R.id.menu_done -> save()
                R.id.menu_cancel -> cancel()
            }
        }

        val gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID)
        position = intent.getIntExtra(KEY_POSITION, INVALID_POSITION)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        val imageUrl = intent.getStringExtra(KEY_IMAGE_URL).orEmpty()
        val thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        val heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()
        autoPosition = intent.getIntExtra(KEY_AUTO_POSITION, PlayPlayerEntity.SEAT_UNKNOWN)
        val usedColors = intent.getStringArrayExtra(KEY_USED_COLORS)

        viewModel.colors.observe(this, {
            colors.clear()
            colors.addAll(it)
        })

        if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
            userHasShownScore = true
            scoreView.requestFocus()
        }
        isNewPlayer = intent.getBooleanExtra(KEY_NEW_PLAYER, false)

        fab.colorize(intent.getIntExtra(KEY_FAB_COLOR, ContextCompat.getColor(this, R.color.accent)))
        if (savedInstanceState == null) {
            position = intent.getIntExtra(KEY_POSITION, INVALID_POSITION)
            player = intent.getParcelableExtra(KEY_PLAYER) ?: PlayPlayerEntity()
            if (hasAutoPosition()) player = player.copy(startingPosition = autoPosition.toString())
            originalPlayer = player.copy()
        } else {
            player = savedInstanceState.getParcelable(KEY_PLAYER) ?: PlayPlayerEntity()
            userHasShownTeamColor = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_TEAM_COLOR)
            userHasShownPosition = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_POSITION)
            userHasShownScore = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_SCORE)
            userHasShownRating = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_RATING)
            userHasShownNew = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_NEW)
            userHasShownWin = savedInstanceState.getBoolean(KEY_USER_HAS_SHOWN_WIN)
        }

        this.usedColors = if (usedColors == null) arrayListOf() else ArrayList(listOf(*usedColors))
        this.usedColors?.remove(player.color)

        lifecycleScope.launch {
            thumbnailView.safelyLoadImage(imageUrl, thumbnailUrl, heroImageUrl)
        }

        bindUi()
        nameView.setAdapter(PlayerNameAdapter(this))
        usernameView.setAdapter(BuddyNameAdapter(this))
        teamColorView.setAdapter(GameColorAdapter(this, gameId))
        viewModel.setGameId(gameId)
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
        editText.focusWithKeyboard()
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
        positionView.setTextKeepState(player.startingPosition)
        scoreView.setTextKeepState(player.score)
        ratingView.setTextKeepState(if (player.rating == PlayPlayerEntity.DEFAULT_RATING) "" else player.rating.toString())
        newView.isChecked = player.isNew
        winView.isChecked = player.isWin
    }

    private fun setViewVisibility() {
        val prefs = preferences()
        teamColorContainer.isVisible = prefs.showLogPlayerTeamColor() || userHasShownTeamColor || player.color.isNotEmpty()
        positionContainer.isVisible = !hasAutoPosition() && (prefs.showLogPlayerPosition() || userHasShownPosition || player.startingPosition.isNotEmpty())
        scoreContainer.isVisible = prefs.showLogPlayerScore() || userHasShownScore || player.score.isNotEmpty()
        ratingContainer.isVisible = prefs.showLogPlayerRating() || userHasShownRating || player.rating > 0
        newView.isVisible = prefs.showLogPlayerNew() || userHasShownNew || player.isNew
        winView.isVisible = prefs.showLogPlayerWin() || userHasShownWin || player.isWin

        val enableButton = createAddFieldArray().isNotEmpty()
        if (enableButton) fab.show() else fab.hide()
        fabBuffer.isVisible = enableButton
    }

    private fun hasAutoPosition(): Boolean {
        return autoPosition != PlayPlayerEntity.SEAT_UNKNOWN
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
        if (!teamColorContainer.isVisible) list.add(resources.getString(R.string.team_color))
        if (!hasAutoPosition() && !positionContainer.isVisible) list.add(resources.getString(R.string.starting_position))
        if (!scoreContainer.isVisible) list.add(resources.getString(R.string.score))
        if (!ratingContainer.isVisible) list.add(resources.getString(R.string.rating))
        if (!newView.isVisible) list.add(resources.getString(R.string.new_label))
        if (!winView.isVisible) list.add(resources.getString(R.string.win))
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
        player = player.copy(
            name = nameView.text.toString().trim(),
            username = usernameView.text.toString().trim(),
            color = teamColorView.text.toString().trim(),
            startingPosition = positionView.text.toString().trim(),
            score = scoreView.text.toString().trim(),
            rating = ratingView.text.toString().toDoubleOrNull() ?: 0.0,
            isNew = newView.isChecked,
            isWin = winView.isChecked,
        )
    }

    data class LaunchInput(
        val gameId: Int,
        val gameName: String,
        val imageUrl: String,
        val thumbnailUrl: String,
        val heroImageUrl: String,
        val isRequestingToEndPlay: Boolean,
        val fabColor: Int,
        val usedColors: List<String>,
        val autoPosition: Int,
    )

    class AddPlayerContract : ActivityResultContract<LaunchInput, PlayPlayerEntity?>() {
        override fun createIntent(context: Context, input: LaunchInput?): Intent {
            if (input == null) throw IllegalArgumentException("input can't be null")
            val i = Intent().apply {
                setClass(context, LogPlayerActivity::class.java)
                putExtra(KEY_GAME_ID, input.gameId)
                putExtra(KEY_GAME_NAME, input.gameName)
                putExtra(KEY_IMAGE_URL, input.imageUrl)
                putExtra(KEY_THUMBNAIL_URL, input.thumbnailUrl)
                putExtra(KEY_HERO_IMAGE_URL, input.heroImageUrl)
                putExtra(KEY_END_PLAY, input.isRequestingToEndPlay)
                putExtra(KEY_FAB_COLOR, input.fabColor)
                putExtra(KEY_USED_COLORS, input.usedColors.toTypedArray())
                putExtra(KEY_NEW_PLAYER, true)
                putExtra(KEY_AUTO_POSITION, input.autoPosition)
            }
            return i
        }

        override fun parseResult(resultCode: Int, intent: Intent?): PlayPlayerEntity? {
            return if (resultCode == RESULT_OK) {
                val player = intent?.getParcelableExtra(KEY_PLAYER) as? PlayPlayerEntity
                player
            } else null
        }
    }

    class EditPlayerContract : ActivityResultContract<Pair<LaunchInput, Pair<Int, PlayPlayerEntity>>, Pair<Int, PlayPlayerEntity?>>() {
        override fun createIntent(context: Context, input: Pair<LaunchInput, Pair<Int, PlayPlayerEntity>>?): Intent {
            if (input == null) throw IllegalArgumentException("input can't be null")
            return Intent().apply {
                setClass(context, LogPlayerActivity::class.java)
                putExtra(KEY_GAME_ID, input.first.gameId)
                putExtra(KEY_GAME_NAME, input.first.gameName)
                putExtra(KEY_IMAGE_URL, input.first.imageUrl)
                putExtra(KEY_THUMBNAIL_URL, input.first.thumbnailUrl)
                putExtra(KEY_HERO_IMAGE_URL, input.first.heroImageUrl)
                putExtra(KEY_END_PLAY, input.first.isRequestingToEndPlay)
                putExtra(KEY_FAB_COLOR, input.first.fabColor)
                putExtra(KEY_USED_COLORS, input.first.usedColors.toTypedArray())
                putExtra(KEY_NEW_PLAYER, false)
                putExtra(KEY_AUTO_POSITION, input.first.autoPosition)
                putExtra(KEY_POSITION, input.second.first)
                putExtra(KEY_PLAYER, input.second.second)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Pair<Int, PlayPlayerEntity?> {
            return if (resultCode == RESULT_OK) {
                val position = intent?.getIntExtra(KEY_POSITION, INVALID_POSITION) ?: INVALID_POSITION
                val player = intent?.getParcelableExtra(KEY_PLAYER) as? PlayPlayerEntity
                position to player
            } else INVALID_POSITION to null
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
    }
}
