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
import com.boardgamegeek.databinding.ActivityLogplayerBinding
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.adapter.BuddyNameAdapter
import com.boardgamegeek.ui.adapter.GameColorAdapter
import com.boardgamegeek.ui.adapter.PlayerNameAdapter
import com.boardgamegeek.ui.dialog.ColorPickerWithListenerDialogFragment
import com.boardgamegeek.ui.viewmodel.LogPlayerViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.coroutines.launch

class LogPlayerActivity : AppCompatActivity(), ColorPickerWithListenerDialogFragment.Listener {
    private lateinit var binding: ActivityLogplayerBinding
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val viewModel by viewModels<LogPlayerViewModel>()
    private val playerNameAdapter: PlayerNameAdapter by lazy { PlayerNameAdapter(this) }
    private val buddyNameAdapter: BuddyNameAdapter by lazy { BuddyNameAdapter(this) }
    private val colorAdapter: GameColorAdapter by lazy { GameColorAdapter(this) }

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

        binding = ActivityLogplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        binding.nameView.setOnItemClickListener { _, view, _, _ ->
            binding.usernameView.setText(view.tag as String)
        }
        binding.usernameView.setOnItemClickListener { _, view, _, _ ->
            binding.nameView.setText(view.tag as String)
        }
        binding.teamColorView.doAfterTextChanged {
            binding.colorView.setColorViewValue(it?.toString().asColorRgb())
        }
        binding.colorView.setOnClickListener {
            showAndSurvive(ColorPickerWithListenerDialogFragment.newInstance(colors, binding.teamColorView.text.toString(), usedColors))
        }
        binding.positionContainer.setEndIconOnClickListener {
            onNumberToTextClick(binding.positionContainer, binding.positionView, false)
        }
        binding.scoreContainer.setEndIconOnClickListener {
            onNumberToTextClick(binding.scoreContainer, binding.scoreView, true)
        }
        binding.fab.setOnClickListener { addField() }

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

        viewModel.colors.observe(this) {
            colors.clear()
            colors.addAll(it)
        }
        viewModel.players.observe(this) {
            playerNameAdapter.addPlayers(it)
            buddyNameAdapter.addPlayers(it)
        }
        viewModel.buddies.observe(this) {
            playerNameAdapter.addUsers(it)
            buddyNameAdapter.addUsers(it)
        }
        viewModel.colors.observe(this) {
            colorAdapter.addData(it)
        }

        if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
            userHasShownScore = true
            binding.scoreView.requestFocus()
        }
        isNewPlayer = intent.getBooleanExtra(KEY_NEW_PLAYER, false)

        binding.fab.colorize(intent.getIntExtra(KEY_FAB_COLOR, ContextCompat.getColor(this, R.color.accent)))
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
            binding.thumbnailView.safelyLoadImage(imageUrl, thumbnailUrl, heroImageUrl)
        }

        bindUi()
        binding.nameView.setAdapter(playerNameAdapter)
        binding.usernameView.setAdapter(buddyNameAdapter)
        binding.teamColorView.setAdapter(colorAdapter)
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
        binding.teamColorView.setText(description)
    }

    private fun onNumberToTextClick(til: TextInputLayout, editText: EditText, includeSign: Boolean) {
        if (editText.inputType and InputType.TYPE_CLASS_NUMBER == InputType.TYPE_CLASS_NUMBER) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            til.setEndIconDrawable(R.drawable.ic_baseline_dialpad_24)
        } else {
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or if (includeSign) {
                editText.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED
            } else {
                editText.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED.inv()
            }
            til.setEndIconDrawable(R.drawable.ic_baseline_keyboard_24)
        }
        editText.requestFocusAndKeyboard()
    }

    private fun bindUi() {
        if (hasAutoPosition()) {
            binding.titleView.text = gameName
            binding.subtitleView.text = getString(R.string.generic_player, autoPosition)
        } else {
            binding.headerView.text = gameName
        }
        binding.headerView.isVisible = !hasAutoPosition()
        binding.twoLineContainer.isVisible = hasAutoPosition()

        binding.nameView.setTextKeepState(player.name)
        binding.usernameView.setTextKeepState(player.username)
        binding.teamColorView.setTextKeepState(player.color)
        binding.positionView.setTextKeepState(player.startingPosition)
        binding.scoreView.setTextKeepState(player.score)
        binding.ratingView.setTextKeepState(if (player.rating == PlayPlayerEntity.DEFAULT_RATING) "" else player.rating.toString())
        binding.newView.isChecked = player.isNew
        binding.winView.isChecked = player.isWin
    }

    private fun setViewVisibility() {
        val prefs = preferences()
        binding.teamColorContainer.isVisible = prefs.showLogPlayerTeamColor() || userHasShownTeamColor || player.color.isNotEmpty()
        binding.positionContainer.isVisible =
            !hasAutoPosition() && (prefs.showLogPlayerPosition() || userHasShownPosition || player.startingPosition.isNotEmpty())
        binding.scoreContainer.isVisible = prefs.showLogPlayerScore() || userHasShownScore || player.score.isNotEmpty()
        binding.ratingContainer.isVisible = prefs.showLogPlayerRating() || userHasShownRating || player.rating > 0
        binding.newView.isVisible = prefs.showLogPlayerNew() || userHasShownNew || player.isNew
        binding.winView.isVisible = prefs.showLogPlayerWin() || userHasShownWin || player.isWin

        val enableButton = createAddFieldArray().isNotEmpty()
        if (enableButton) binding.fab.show() else binding.fab.hide()
        binding.fabBuffer.isVisible = enableButton
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
                        binding.teamColorView to binding.teamColorContainer
                    }
                    resources.getString(R.string.starting_position) -> {
                        userHasShownPosition = true
                        binding.positionView to binding.positionContainer
                    }
                    resources.getString(R.string.score) -> {
                        userHasShownScore = true
                        binding.scoreView to binding.scoreContainer
                    }
                    resources.getString(R.string.rating) -> {
                        userHasShownRating = true
                        binding.ratingView to binding.ratingContainer
                    }
                    resources.getString(R.string.new_label) -> {
                        userHasShownNew = true
                        binding.newView.isChecked = true
                        binding.newView to binding.newView
                    }
                    resources.getString(R.string.win) -> {
                        userHasShownWin = true
                        binding.winView.isChecked = true
                        binding.winView to binding.winView
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
                    binding.scrollContainer.post { binding.scrollContainer.smoothScrollTo(0, it.bottom) }
                }
            }.show()
    }

    private fun createAddFieldArray(): Array<CharSequence> {
        val list = mutableListOf<CharSequence>()
        if (!binding.teamColorContainer.isVisible) list.add(resources.getString(R.string.team_color))
        if (!hasAutoPosition() && !binding.positionContainer.isVisible) list.add(resources.getString(R.string.starting_position))
        if (!binding.scoreContainer.isVisible) list.add(resources.getString(R.string.score))
        if (!binding.ratingContainer.isVisible) list.add(resources.getString(R.string.rating))
        if (!binding.newView.isVisible) list.add(resources.getString(R.string.new_label))
        if (!binding.winView.isVisible) list.add(resources.getString(R.string.win))
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
            createDiscardDialog(R.string.player, isNew = isNewPlayer).show()
        }
    }

    private fun captureForm() {
        player = player.copy(
            name = binding.nameView.text.toString().trim(),
            username = binding.usernameView.text.toString().trim(),
            color = binding.teamColorView.text.toString().trim(),
            startingPosition = binding.positionView.text.toString().trim(),
            score = binding.scoreView.text.toString().trim(),
            rating = binding.ratingView.text.toString().toDoubleOrNull() ?: 0.0,
            isNew = binding.newView.isChecked,
            isWin = binding.winView.isChecked,
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
            return Intent(context, LogPlayerActivity::class.java).apply {
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
            return Intent(context, LogPlayerActivity::class.java).apply {
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
