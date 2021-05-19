package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.content.*
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.dialog.*
import com.boardgamegeek.ui.viewmodel.LogPlayViewModel
import com.boardgamegeek.ui.widget.DatePickerDialogFragment
import com.boardgamegeek.ui.widget.PlayerRow
import com.boardgamegeek.util.*
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_logplay.*
import kotlinx.android.synthetic.main.activity_logplay.commentsView
import kotlinx.android.synthetic.main.activity_logplay.incompleteView
import kotlinx.android.synthetic.main.activity_logplay.lengthView
import kotlinx.android.synthetic.main.activity_logplay.locationView
import kotlinx.android.synthetic.main.activity_logplay.noWinStatsView
import kotlinx.android.synthetic.main.activity_logplay.playersLabel
import kotlinx.android.synthetic.main.activity_logplay.quantityView
import kotlinx.android.synthetic.main.activity_logplay.thumbnailView
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class LogPlayActivity : AppCompatActivity(R.layout.activity_logplay) {
    private val viewModel by viewModels<LogPlayViewModel>()
    private val firebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }
    private val playAdapter: PlayAdapter by lazy { PlayAdapter() }
    private val locationAdapter: LocationAdapter by lazy { LocationAdapter(this) }

    private var internalId = INVALID_ID.toLong()
    private var gameId = INVALID_ID
    private var gameName: String = ""
    private var isRequestingToEndPlay = false
    private var isRequestingRematch = false
    private var isChangingGame = false
    private var thumbnailUrl: String = ""
    private var imageUrl: String = ""
    private var heroImageUrl: String = ""

    private var internalIdToDelete = INVALID_ID.toLong()
    private var play: Play? = null
    private var lastRemovedPlayer: Player? = null
    private val gameColors = ArrayList<String>()
    private val availablePlayers = mutableListOf<PlayerEntity>()

    @ColorInt
    private var fabColor = Color.TRANSPARENT
    private val swipePaint = Paint()
    private val deleteIcon: Bitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.ic_delete_white) }
    private val editIcon: Bitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.ic_edit_white) }
    private var horizontalPadding = 0f
    private var itemTouchHelper: ItemTouchHelper? = null
    private var isUserShowingLocation = false
    private var isUserShowingLength = false
    private var isUserShowingQuantity = false
    private var isUserShowingIncomplete = false
    private var isUserShowingNoWinStats = false
    private var isUserShowingComments = false
    private var isUserShowingPlayers = false
    private var shouldDeletePlayOnActivityCancel = false
    private var arePlayersCustomSorted = false
    private var isLaunchingActivity = false
    private var shouldSaveOnPause = true

    private var dateInMillis: Long? = null
    private var startTime: Long = 0L
    private var length: Int = 0
    private var playersHaveColors = false
    private var playersHaveStartingPositions = false
    private var playersAreCustomSorted = true
    private var playerCount = 0
    private var playerDescriptions = listOf<String>()
    private var usedColors = listOf<String>()

    private fun wireUi() {
        val datePickerDialogTag = "DATE_PICKER_DIALOG"
        dateButton.setOnClickListener {
            (supportFragmentManager.findFragmentByTag(datePickerDialogTag) as DatePickerDialogFragment?
                    ?: DatePickerDialogFragment()).apply {
                setOnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    viewModel.updateDate(year, monthOfYear, dayOfMonth)
                }
                setCurrentDateInMillis(dateInMillis ?: System.currentTimeMillis())
                supportFragmentManager.executePendingTransactions()
                this@LogPlayActivity.showAndSurvive(this, datePickerDialogTag)
            }
        }

        if (locationView.adapter == null) locationView.setAdapter(locationAdapter)
        locationView.doAfterTextChanged { viewModel.updateLocation(it.toString().trim()) }

        lengthView.doAfterTextChanged {
            viewModel.updateLength(it.toString().trim().toIntOrNull() ?: 0)
        }
        timerButton.setOnClickListener {
            if (length == 0) {
                viewModel.startTimer()
            } else {
                DialogUtils.createThemedBuilder(this@LogPlayActivity)
                        .setMessage(R.string.are_you_sure_timer_reset)
                        .setPositiveButton(R.string.continue_) { _, _ -> viewModel.resumeTimer() }
                        .setNegativeButton(R.string.reset) { _, _ -> viewModel.startTimer() }
                        .setCancelable(true)
                        .show()
            }
        }
        timerOffButton.setOnClickListener {
            isRequestingToEndPlay = true
            viewModel.endTimer()
            cancelNotification()
            lengthView.apply {
                this.selectAll()
                this.focusWithKeyboard()
            }
        }

        quantityView.doAfterTextChanged {
            it?.let { viewModel.updateQuantity(it.toString().trim().toIntOrNull()) }
        }

        incompleteView.setOnCheckedChangeListener { _, isChecked -> viewModel.updateIncomplete(isChecked) }

        noWinStatsView.setOnCheckedChangeListener { _, isChecked -> viewModel.updateNoWinStats(isChecked) }

        commentsView.doAfterTextChanged { viewModel.updateComments(it.toString()) }

        assignColorsButton.setOnClickListener {
            if (playersHaveColors) {
                val builder = AlertDialog.Builder(this@LogPlayActivity)
                        .setTitle(R.string.title_clear_colors)
                        .setMessage(R.string.msg_clear_colors)
                        .setCancelable(true)
                        .setNegativeButton(R.string.keep) { _: DialogInterface?, _: Int -> viewModel.assignColors() }
                        .setPositiveButton(R.string.clear) { _: DialogInterface?, _: Int -> viewModel.assignColors(true) }
                builder.show()
            } else {
                viewModel.assignColors()
            }
        }
        playerSortButton.setOnClickListener {
            val popup = PopupMenu(this@LogPlayActivity, it)
            popup.inflate(if (!arePlayersCustomSorted && playerCount > 1) R.menu.log_play_player_sort else R.menu.log_play_player_sort_short)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_custom_player_order -> {
                        if (arePlayersCustomSorted) {
                            logPlayerOrder("NotCustom")
                            if (playersHaveStartingPositions && playersAreCustomSorted) {
                                DialogUtils.createConfirmationDialog(this@LogPlayActivity,
                                        R.string.are_you_sure_player_sort_custom_off,
                                        { _: DialogInterface?, _: Int -> autoSortPlayers() },
                                        R.string.sort)
                                        .show()
                            } else {
                                autoSortPlayers()
                            }
                        } else {
                            logPlayerOrder("Custom")
                            if (playersHaveStartingPositions) {
                                val builder = AlertDialog.Builder(this@LogPlayActivity)
                                        .setMessage(R.string.message_custom_player_order)
                                        .setPositiveButton(R.string.keep) { _: DialogInterface?, _: Int ->
                                            arePlayersCustomSorted = true
                                        }
                                        .setNegativeButton(R.string.clear) { _: DialogInterface?, _: Int ->
                                            arePlayersCustomSorted = true
                                            viewModel.clearPositions()
                                        }
                                        .setCancelable(true)
                                builder.show()
                            }
                        }
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_pick_start_player -> {
                        logPlayerOrder("Prompt")
                        AlertDialog.Builder(this)
                                .setTitle(R.string.title_pick_start_player)
                                .setItems(playerDescriptions.toTypedArray()) { _, which: Int ->
                                    viewModel.pickStartPlayer(which)
                                }
                                .show()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_random_start_player -> {
                        logPlayerOrder("RandomStarter")
                        viewModel.pickRandomStartPlayer()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_random_player_order -> {
                        logPlayerOrder("Random")
                        viewModel.randomizePlayerOrder()
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            popup.show()
        }

        addPlayerButton.setOnClickListener {
            if (preferences()[LOG_EDIT_PLAYER_PROMPTED, false] == true) {
                addPlayers(preferences()[LOG_EDIT_PLAYER, false] ?: false)
            } else {
                promptToEditPlayers()
            }
        }
    }

    private fun addPlayers(editPlayer: Boolean) {
        if (editPlayer) {
            if (!showPlayersToAddDialog()) {
                addNewPlayer()
            }
        } else {
            viewModel.addPlayer(resort = !arePlayersCustomSorted)
            //recyclerView.smoothScrollToPosition(playAdapter.itemCount) // TODO do this when the player adapter updates
        }
    }

    private fun promptToEditPlayers() {
        AlertDialog.Builder(this)
                .setTitle(R.string.pref_edit_player_prompt_title)
                .setMessage(R.string.pref_edit_player_prompt_message)
                .setCancelable(true)
                .setPositiveButton(R.string.pref_edit_player_prompt_positive, onPromptClickListener(true))
                .setNegativeButton(R.string.pref_edit_player_prompt_negative, onPromptClickListener(false))
                .create().show()
        preferences()[LOG_EDIT_PLAYER_PROMPTED] = true
    }

    private fun onPromptClickListener(value: Boolean): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            preferences()[LOG_EDIT_PLAYER] = value
            addPlayers(value)
        }
    }

    private fun bindHeader() {
        headerView.text = gameName
        thumbnailView.safelyLoadImage(imageUrl, thumbnailUrl, heroImageUrl, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                headerView.setBackgroundResource(R.color.black_overlay_light)
                updateColors(palette.getIconSwatch().rgb)
            }

            override fun onFailedImageLoad() {
                updateColors(ContextCompat.getColor(this@LogPlayActivity, R.color.accent))
            }

            private fun updateColors(color: Int) {
                fabColor = color
                fab.colorize(color)
                fab.post { fab.show() }
                ViewCompat.setBackgroundTintList(addPlayerButton, ColorStateList.valueOf(color))
            }
        })
    }

    private fun bindDate(play: Play) {
        dateButton.text = play.getDateForDisplay(this)
    }

    private fun bindLocation(play: Play) {
        locationView.setTextKeepState(play.location)
        locationFrame.isVisible = !play.location.isNullOrEmpty() || isUserShowingLocation || preferences().showLogPlayLocation()
    }

    private fun bindLength() {
        when {
            startTime > 0L -> {
                timer.startTimerWithSystemTime(startTime)
                lengthGroup.isInvisible = true // This keeps the constraint layout happy
                timerGroup.isVisible = true
            }
            length > 0 -> {
                lengthView.setTextKeepState(if (length == Play.LENGTH_DEFAULT) "" else length.toString())
                timerButton.isEnabled = (dateInMillis
                        ?: 0 + (length * DateUtils.MINUTE_IN_MILLIS)).isToday()
                lengthGroup.isVisible = true
                timerGroup.isVisible = false
            }
            else -> {
                lengthGroup.isVisible = isUserShowingLength || preferences().showLogPlayLength()
                timerGroup.isVisible = false
            }
        }
    }

    private fun bindQuantity(play: Play) {
        quantityView.setTextKeepState(play.quantity.toString())
        quantityFrame.isVisible = play.quantity != Play.QUANTITY_DEFAULT || isUserShowingQuantity || preferences().showLogPlayQuantity()
    }

    private fun bindIncomplete(play: Play) {
        incompleteView.isChecked = play.incomplete
        incompleteView.isVisible = play.incomplete || isUserShowingIncomplete || preferences().showLogPlayIncomplete()
    }

    private fun bindNoWinStats(play: Play) {
        noWinStatsView.isChecked = play.noWinStats
        noWinStatsView.isVisible = play.noWinStats || isUserShowingNoWinStats || preferences().showLogPlayNoWinStats()
    }

    private fun bindComments(play: Play) {
        commentsView.setTextKeepState(play.comments.orEmpty())
        commentsFrame.isVisible = !play.comments.isNullOrEmpty() || isUserShowingComments || preferences().showLogPlayComments()
    }

    private fun bindPlayerHeader(playerCount: Int) {
        val showPlayers = isUserShowingPlayers || preferences().showLogPlayPlayerList() || playerCount != 0
        playerHeader.isVisible = showPlayers
        playersLabel.text = if (playerCount <= 0) getString(R.string.title_players) else getString(R.string.title_players_with_count, playerCount)
        assignColorsButton.isEnabled = playerCount > 0
        recyclerView.isVisible = showPlayers
        addPlayerFrame.isVisible = showPlayers
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDoneCancelActionBarView { v: View ->
            when (v.id) {
                R.id.menu_done -> {
                    if (startTime > 0L) {
                        toast(R.string.msg_saving_draft)
                        viewModel.saveDraft()
                    } else {
                        toast(R.string.msg_logging_play)
                        viewModel.logPlay(internalIdToDelete)
                        cancelNotification()
                    }
                    setResult(RESULT_OK)
                    finish()
                }
                R.id.menu_cancel -> cancel()
            }
        }

        horizontalPadding = resources.getDimension(R.dimen.material_margin_horizontal)
        fab.setOnClickListener { addField() }

        recyclerView.setHasFixedSize(false)
        recyclerView.adapter = playAdapter

        swipePaint.color = ContextCompat.getColor(this, R.color.medium_blue)
        val deleteColor = ContextCompat.getColor(this@LogPlayActivity, R.color.delete)
        val editColor = ContextCompat.getColor(this@LogPlayActivity, R.color.edit)
        itemTouchHelper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                        if (viewHolder is PlayAdapter.PlayerViewHolder && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                            val itemView = viewHolder.itemView

                            // fade and slide item
                            val width = itemView.width.toFloat()
                            val alpha = 1.0f - abs(dX) / width
                            itemView.alpha = alpha
                            itemView.translationX = dX

                            // show background with an icon
                            val icon = if (dX > 0) {
                                deleteIcon
                            } else {
                                editIcon
                            }
                            val verticalPadding = (itemView.height - icon.height) / 2f
                            val background: RectF
                            val iconSrc: Rect
                            val iconDst: RectF
                            if (dX > 0) {
                                swipePaint.color = deleteColor
                                background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat())
                                iconSrc = Rect(
                                        0,
                                        0,
                                        (dX - itemView.left - horizontalPadding).toInt().coerceAtMost(icon.width),
                                        icon.height)
                                iconDst = RectF(
                                        itemView.left.toFloat() + horizontalPadding,
                                        itemView.top.toFloat() + verticalPadding,
                                        (itemView.left + horizontalPadding + icon.width).coerceAtMost(dX),
                                        itemView.bottom.toFloat() - verticalPadding)
                            } else {
                                swipePaint.color = editColor
                                background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                                iconSrc = Rect(
                                        (icon.width + horizontalPadding.toInt() + dX.toInt()).coerceAtLeast(0),
                                        0,
                                        icon.width,
                                        icon.height)
                                iconDst = RectF(
                                        (itemView.right.toFloat() + dX).coerceAtLeast(itemView.right.toFloat() - horizontalPadding - icon.width),
                                        itemView.top.toFloat() + verticalPadding,
                                        itemView.right.toFloat() - horizontalPadding,
                                        itemView.bottom.toFloat() - verticalPadding)
                            }
                            c.drawRect(background, swipePaint)
                            c.drawBitmap(icon, iconSrc, iconDst, swipePaint)
                        }
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                        if (swipeDir == ItemTouchHelper.RIGHT) {
                            lastRemovedPlayer = playAdapter.getPlayer(viewHolder.adapterPosition)
                            lastRemovedPlayer?.let { player ->
                                coordinatorLayout.indefiniteSnackbar(
                                        getString(R.string.msg_player_deleted, player.description.ifEmpty { getString(R.string.title_player) }),
                                        getString(R.string.undo)) {
                                    lastRemovedPlayer?.let { viewModel.addPlayer(player, resort = !arePlayersCustomSorted) }
                                }
                                viewModel.removePlayer(player, !arePlayersCustomSorted)
                            }
                        } else {
                            editPlayer(viewHolder.adapterPosition)
                            // (viewHolder as? PlayAdapter.PlayerViewHolder)?.onItemClear() // This should be called in `override fun clearView`
                            //playAdapter.notifyDataSetChanged() // Need to clear the swiped player
                        }
                        clearView(recyclerView, viewHolder)
                    }

                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                        if (target !is PlayAdapter.PlayerViewHolder) return false
                        val fromPosition = viewHolder.adapterPosition
                        val toPosition = target.adapterPosition
                        viewModel.reorderPlayers(fromPosition + 1, toPosition + 1)
                        return true
                    }

                    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                        (viewHolder as? PlayAdapter.PlayerViewHolder)?.onItemClear()
                        super.clearView(recyclerView, viewHolder)
                    }

                    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) (viewHolder as? PlayAdapter.PlayerViewHolder)?.onItemDragging()
                        super.onSelectedChanged(viewHolder, actionState)
                    }

                    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                        return if (arePlayersCustomSorted) makeMovementFlags(0, getSwipeDirs(recyclerView, viewHolder)) else super.getMovementFlags(recyclerView, viewHolder)
                    }

                    override fun isLongPressDragEnabled(): Boolean {
                        return false
                    }
                })
        itemTouchHelper?.attachToRecyclerView(recyclerView)

        internalId = intent.getLongExtra(KEY_ID, INVALID_ID.toLong())
        gameId = intent.getIntExtra(KEY_GAME_ID, INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        isRequestingToEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false)
        isRequestingRematch = intent.getBooleanExtra(KEY_REMATCH, false)
        isChangingGame = intent.getBooleanExtra(KEY_CHANGE_GAME, false)
        thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        imageUrl = intent.getStringExtra(KEY_IMAGE_URL).orEmpty()
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()

        val action = if (internalId == INVALID_ID.toLong()) "Action" else "Edit"
        FirebaseAnalytics.getInstance(this).logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
            param("Action", action)
            param("GameName", gameName)
        }

        if (gameId <= 0) {
            val message = "Can't log a play without a game ID."
            Timber.w(message)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
        savedInstanceState?.let {
            internalId = it.getLong(KEY_INTERNAL_ID, INVALID_ID.toLong())
            isUserShowingLocation = it.getBoolean(KEY_IS_USER_SHOWING_LOCATION)
            isUserShowingLength = it.getBoolean(KEY_IS_USER_SHOWING_LENGTH)
            isUserShowingQuantity = it.getBoolean(KEY_IS_USER_SHOWING_QUANTITY)
            isUserShowingIncomplete = it.getBoolean(KEY_IS_USER_SHOWING_INCOMPLETE)
            isUserShowingNoWinStats = it.getBoolean(KEY_IS_USER_SHOWING_NO_WIN_STATS)
            isUserShowingComments = it.getBoolean(KEY_IS_USER_SHOWING_COMMENTS)
            isUserShowingPlayers = it.getBoolean(KEY_IS_USER_SHOWING_PLAYERS)
            shouldDeletePlayOnActivityCancel = it.getBoolean(KEY_SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL)
            arePlayersCustomSorted = it.getBoolean(KEY_ARE_PLAYERS_CUSTOM_SORTED)
        }

        wireUi()

        viewModel.colors.observe(this) {
            gameColors.clear()
            gameColors.addAll(it)
        }
        viewModel.playersByLocation.observe(this) {
            availablePlayers.clear()
            availablePlayers.addAll(it)
        }
        viewModel.internalId.observe(this) { internalId = it }
        viewModel.play.observe(this) {
            this.play = it
            dateInMillis = it.dateInMillis
            length = it.length
            startTime = it.startTime
            playersHaveColors = it.players.any { player -> player.color.isNotBlank() }
            playersHaveStartingPositions = it.players.any { player -> player.startingPosition.isNotBlank() }
            playersAreCustomSorted = it.arePlayersCustomSorted()
            playerCount = it.getPlayerCount()
            playerDescriptions = it.players.mapIndexed { i, p ->
                p.description.ifEmpty { String.format(resources.getString(R.string.generic_player), i + 1) }
            }
            usedColors = it.players.map { p -> p.color }
            arePlayersCustomSorted = if (it.getPlayerCount() > 0) {
                it.arePlayersCustomSorted()
            } else {
                intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)
            }

            bindHeader()
            bindDate(it)
            bindLocation(it)
            bindLength()
            bindQuantity(it)
            bindIncomplete(it)
            bindNoWinStats(it)
            bindComments(it)
            bindPlayerHeader(it.getPlayerCount())
            playAdapter.submit(it.players)
            if (isRequestingToEndPlay) {
                // TODO every time?
                cancelNotification()
            } else {
                if (it.hasStarted() && internalId != INVALID_ID.toLong()) {
                    this.launchPlayingNotification(internalId,
                            it.gameName,
                            it.location.orEmpty(),
                            it.getPlayerCount(),
                            it.startTime,
                            thumbnailUrl, imageUrl, heroImageUrl)
                }
            }
            progressView.hide()
        }
        if (internalId != INVALID_ID.toLong()) {
            // Editing or copying an existing play
            shouldDeletePlayOnActivityCancel = false
            if (isRequestingRematch || isChangingGame) {
                shouldDeletePlayOnActivityCancel = true
            }
        } else {
            // Starting a new play
            shouldDeletePlayOnActivityCancel = true
            arePlayersCustomSorted = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)
        }
        viewModel.loadPlay(internalId, gameId, gameName)

//        if (isRequestingRematch) {
//            play = rematch(play!!)
//            internalId = INVALID_ID.toLong()
//        } else if (isChangingGame) {
//            play = play?.copy(playId = INVALID_ID)
//            internalIdToDelete = internalId
//            internalId = INVALID_ID.toLong()
//        }

        fab.postDelayed({ fab.show() }, 2000)
    }

    override fun onResume() {
        super.onResume()
        isLaunchingActivity = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_INTERNAL_ID, internalId)
        outState.putBoolean(KEY_IS_USER_SHOWING_LOCATION, isUserShowingLocation)
        outState.putBoolean(KEY_IS_USER_SHOWING_LENGTH, isUserShowingLength)
        outState.putBoolean(KEY_IS_USER_SHOWING_QUANTITY, isUserShowingQuantity)
        outState.putBoolean(KEY_IS_USER_SHOWING_INCOMPLETE, isUserShowingIncomplete)
        outState.putBoolean(KEY_IS_USER_SHOWING_NO_WIN_STATS, isUserShowingNoWinStats)
        outState.putBoolean(KEY_IS_USER_SHOWING_COMMENTS, isUserShowingComments)
        outState.putBoolean(KEY_IS_USER_SHOWING_PLAYERS, isUserShowingPlayers)
        outState.putBoolean(KEY_SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL, shouldDeletePlayOnActivityCancel)
        outState.putBoolean(KEY_ARE_PLAYERS_CUSTOM_SORTED, arePlayersCustomSorted)
    }

    override fun onPause() {
        super.onPause()
        locationAdapter.changeCursor(null)
        if (shouldSaveOnPause && !isLaunchingActivity) {
            viewModel.saveDraft() // TODO already called when back is pressed
        }
    }

    override fun onBackPressed() {
        viewModel.saveDraft()
        toast(R.string.msg_saving_draft)
        setResult(RESULT_OK)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            (data?.getParcelableExtra(LogPlayerActivity.KEY_PLAYER) as? Player)?.let { player ->
                val position = data.getIntExtra(LogPlayerActivity.KEY_POSITION, LogPlayerActivity.INVALID_POSITION)
                when (requestCode) {
                    REQUEST_ADD_PLAYER -> {
                        viewModel.addPlayer(player, resort = !arePlayersCustomSorted)
                        addNewPlayer()
                    }
                    REQUEST_EDIT_PLAYER -> if (position == LogPlayerActivity.INVALID_POSITION) {
                        Timber.w("Invalid player position after edit")
                    } else {
                        viewModel.editPlayer(player, position, resort = !arePlayersCustomSorted)
                    }
                    else -> Timber.w("Received invalid request code: %d", requestCode)
                }
            }
        }
        // TODO scroll to the position of the last edited or added player, even when cancelling
    }

    private fun cancel() {
        shouldSaveOnPause = false
        if (viewModel.isDirty()) {
            if (shouldDeletePlayOnActivityCancel) {
                DialogUtils.createDiscardDialog(this, R.string.play, true, true) { viewModel.deletePlay() }.show()
            } else {
                DialogUtils.createDiscardDialog(this, R.string.play, false).show()
            }
        } else {
            if (shouldDeletePlayOnActivityCancel) {
                viewModel.deletePlay()
            }
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun addField() {
        val array = createAddFieldArray()
        if (array.isEmpty()) return
        AlertDialog.Builder(this).setTitle(R.string.add_field)
                .setItems(array) { _, which: Int ->
                    val selection = array[which].toString()
                    when (selection) {
                        resources.getString(R.string.location) -> {
                            isUserShowingLocation = true
                            locationFrame.isVisible = true
                            locationView.requestFocus()
                        }
                        resources.getString(R.string.length) -> {
                            isUserShowingLength = true
                            lengthGroup.isVisible = true
                            lengthView.requestFocus()
                        }
                        resources.getString(R.string.quantity) -> {
                            isUserShowingQuantity = true
                            quantityFrame.isVisible = true
                            quantityView.requestFocus()
                        }
                        resources.getString(R.string.incomplete) -> {
                            isUserShowingIncomplete = true
                            viewModel.updateIncomplete(true)
                        }
                        resources.getString(R.string.noWinStats) -> {
                            isUserShowingNoWinStats = true
                            viewModel.updateNoWinStats(true)
                        }
                        resources.getString(R.string.comments) -> {
                            isUserShowingComments = true
                            commentsFrame.isVisible = true
                            commentsView.requestFocus()
                        }
                        resources.getString(R.string.title_players) -> {
                            isUserShowingPlayers = true
                            bindPlayerHeader(playerCount)
                        }
                    }
                    firebaseAnalytics.logEvent("AddField") {
                        param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                        param(FirebaseAnalytics.Param.ITEM_NAME, selection)
                    }
                    invalidateOptionsMenu()
                }.show()
    }

    private fun createAddFieldArray(): Array<CharSequence> {
        val list = mutableListOf<CharSequence>()
        if (!locationFrame.isVisible) list.add(getString(R.string.location))
        if (!lengthFrame.isVisible || !timer.isVisible) list.add(getString(R.string.length))
        if (!quantityFrame.isVisible) list.add(getString(R.string.quantity))
        if (!incompleteView.isVisible) list.add(getString(R.string.incomplete))
        if (!noWinStatsView.isVisible) list.add(getString(R.string.noWinStats))
        if (!commentsFrame.isVisible) list.add(getString(R.string.comments))
        if (!playerHeader.isVisible) list.add(getString(R.string.title_players))
        return list.toTypedArray()
    }

    internal class LocationAdapter(context: Context) : AutoCompleteAdapter(context, Plays.LOCATION, Plays.buildLocationsUri(), PlayLocations.SORT_BY_SUM_QUANTITY, Plays.SUM_QUANTITY) {
        override val defaultSelection: String
            get() = "${Plays.LOCATION}<>''"
    }

    private fun showPlayersToAddDialog(): Boolean {
        val playersToAdd = mutableListOf<PlayerEntity>()
        AlertDialog.Builder(this)
                .setTitle(R.string.title_add_players)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.addPlayers(playersToAdd, !arePlayersCustomSorted)
                }
                .setNeutralButton(R.string.more) { _, _ ->
                    viewModel.addPlayers(playersToAdd, !arePlayersCustomSorted)
                    addNewPlayer()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(availablePlayers.map { it.description }.toTypedArray<CharSequence>(), null) { _, which, isChecked ->
                    val player = availablePlayers[which]
                    if (isChecked) {
                        playersToAdd.add(player)
                    } else {
                        playersToAdd.remove(player)
                    }
                }
                .create()
                .show()
        return true
    }

    private fun autoSortPlayers() {
        arePlayersCustomSorted = false
        viewModel.pickStartPlayer(0)
    }

    private fun addNewPlayer() {
        val intent = Intent()
        if (!arePlayersCustomSorted) {
            intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, playerCount + 1)
        }
        editPlayer(intent, REQUEST_ADD_PLAYER)
    }

    private fun editPlayer(position: Int) {
        val player = playAdapter.getPlayer(position)
        val intent = Intent().apply {
            putExtra(LogPlayerActivity.KEY_PLAYER, player)
            putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay)
            if (!arePlayersCustomSorted && player != null) {
                putExtra(LogPlayerActivity.KEY_AUTO_POSITION, player.seat)
            }
            putExtra(LogPlayerActivity.KEY_POSITION, position)
        }
        editPlayer(intent, REQUEST_EDIT_PLAYER)
    }

    private fun editPlayer(intent: Intent, requestCode: Int) {
        isLaunchingActivity = true
        intent.apply {
            setClass(this@LogPlayActivity, LogPlayerActivity::class.java)
            putExtra(LogPlayerActivity.KEY_GAME_ID, gameId)
            putExtra(LogPlayerActivity.KEY_GAME_NAME, gameName)
            putExtra(LogPlayerActivity.KEY_IMAGE_URL, imageUrl)
            putExtra(LogPlayerActivity.KEY_THUMBNAIL_URL, thumbnailUrl)
            putExtra(LogPlayerActivity.KEY_HERO_IMAGE_URL, heroImageUrl)
            putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay)
            putExtra(LogPlayerActivity.KEY_FAB_COLOR, fabColor)
            putExtra(LogPlayerActivity.KEY_USED_COLORS, usedColors.toTypedArray())
            putExtra(LogPlayerActivity.KEY_NEW_PLAYER, requestCode == REQUEST_ADD_PLAYER)
        }
        startActivityForResult(intent, requestCode)
    }

    private fun cancelNotification() {
        cancel(TAG_PLAY_TIMER, internalId)
    }

    private class Diff(private val oldList: List<Player>, private val newList: List<Player>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val o = oldList[oldItemPosition]
            val n = newList[newItemPosition]
            return o == n
        }
    }

    private class DraggingDiff(private val oldList: List<Player>, private val newList: List<Player>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }
    }

    inner class PlayAdapter : RecyclerView.Adapter<PlayAdapter.PlayerViewHolder>() {
        var isDragging = false

        private var players: List<Player> = emptyList()

        fun submit(players: List<Player>) {
            val oldPlayers = this.players
            this.players = mutableListOf<Player>().apply { addAll(players) }.toList()
            val diffCallback = if (isDragging) DraggingDiff(oldPlayers, this.players) else Diff(oldPlayers, this.players)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(this)
        }

        init {
            // TODO re-enable to make drag and drop work
            // setHasStableIds(true)
        }

        override fun getItemCount(): Int {
            return players.size
        }

        override fun getItemId(position: Int): Long {
            return players.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            return PlayerViewHolder(this@LogPlayActivity)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            holder.bind(position)
        }

        fun getPlayer(position: Int): Player? {
            return players.getOrNull(position)
        }

        inner class PlayerViewHolder(context: Context) : RecyclerView.ViewHolder(PlayerRow(context)) {
            private val row: PlayerRow = itemView as PlayerRow

            init {
                row.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            fun onItemDragging() {
                isDragging = true
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.light_blue_transparent))
            }

            fun onItemClear() {
                isDragging = false
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            @SuppressLint("ClickableViewAccessibility")
            fun bind(position: Int) {
                row.setAutoSort(!arePlayersCustomSorted)
                row.setPlayer(getPlayer(position))
                row.setNameListener { editPlayer(position) }
                row.setOnMoreListener {
                    players.getOrNull(position)?.let { player ->
                        PopupMenu(this@LogPlayActivity, row.getMoreButton()).apply {
                            inflate(R.menu.log_play_player)
                            menu.findItem(R.id.win)?.isChecked = player.isWin
                            menu.findItem(R.id.new_)?.isChecked = player.isNew
                            setOnMenuItemClickListener { item: MenuItem ->
                                when (item.itemId) {
                                    R.id.win -> {
                                        viewModel.win(!item.isChecked, position)
                                        true
                                    }
                                    R.id.new_ -> {
                                        viewModel.new(!item.isChecked, position)
                                        true
                                    }
                                    else -> false
                                }
                            }
                            show()
                        }
                    }
                }
                row.getDragHandle().setOnTouchListener { _: View?, event: MotionEvent ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper?.startDrag(this@PlayerViewHolder)
                        true
                    } else false
                }
                row.setOnColorListener {
                    players.getOrNull(position)?.let { player ->
                        val usedColors = players.filter { it != player }.map { it.color } as ArrayList<String>
                        LogPlayPlayerColorPickerDialogFragment.launch(
                                this@LogPlayActivity,
                                player.description,
                                gameColors,
                                player.color,
                                usedColors,
                                adapterPosition)
                    }
                }
                row.setOnRatingListener {
                    players.getOrNull(position)?.let { player ->
                        val fragment = LogPlayPlayerRatingNumberPadDialogFragment.newInstance(
                                position,
                                player.ratingDescription,
                                player.color,
                                player.description
                        )
                        DialogUtils.showFragment(this@LogPlayActivity, fragment, "rating_dialog")
                    }
                }
                row.setOnScoreListener {
                    players.getOrNull(position)?.let { player ->
                        val fragment = LogPlayPlayerScoreNumberPadDialogFragment.newInstance(
                                position,
                                player.score,
                                player.color,
                                player.description)
                        DialogUtils.showFragment(this@LogPlayActivity, fragment, "score_dialog")
                    }
                }
            }
        }
    }

    private fun logPlayerOrder(order: String) {
        firebaseAnalytics.logEvent("LogPlayPlayerOrder") {
            param("Order", order)
        }
    }

    companion object {
        private const val KEY_ID = "ID"
        private const val KEY_GAME_ID = "GAME_ID"
        private const val KEY_GAME_NAME = "GAME_NAME"
        private const val KEY_IMAGE_URL = "IMAGE_URL"
        private const val KEY_THUMBNAIL_URL = "THUMBNAIL_URL"
        private const val KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL"
        private const val KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT"
        private const val KEY_END_PLAY = "END_PLAY"
        private const val KEY_REMATCH = "REMATCH"
        private const val KEY_CHANGE_GAME = "CHANGE_GAME"
        private const val KEY_INTERNAL_ID = "INTERNAL_ID"
        private const val KEY_IS_USER_SHOWING_LOCATION = "IS_USER_SHOWING_LOCATION"
        private const val KEY_IS_USER_SHOWING_LENGTH = "IS_USER_SHOWING_LENGTH"
        private const val KEY_IS_USER_SHOWING_QUANTITY = "IS_USER_SHOWING_QUANTITY"
        private const val KEY_IS_USER_SHOWING_INCOMPLETE = "IS_USER_SHOWING_INCOMPLETE"
        private const val KEY_IS_USER_SHOWING_NO_WIN_STATS = "IS_USER_SHOWING_NO_WIN_STATS"
        private const val KEY_IS_USER_SHOWING_COMMENTS = "IS_USER_SHOWING_COMMENTS"
        private const val KEY_IS_USER_SHOWING_PLAYERS = "IS_USER_SHOWING_PLAYERS"
        private const val KEY_SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL = "SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL"
        private const val KEY_ARE_PLAYERS_CUSTOM_SORTED = "ARE_PLAYERS_CUSTOM_SORTED"
        private const val REQUEST_ADD_PLAYER = 1
        private const val REQUEST_EDIT_PLAYER = 2

        fun logPlay(context: Context, gameId: Int, gameName: String, thumbnailUrl: String = "", imageUrl: String = "", heroImageUrl: String = "", customPlayerSort: Boolean = false) {
            context.startActivity(createIntent(context, INVALID_ID.toLong(), gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort))
        }

        fun editPlay(context: Context, internalId: Long, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String) {
            context.startActivity(createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, false))
        }

        fun endPlay(context: Context, internalId: Long, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String) {
            context.startActivity(createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, false).also {
                it.putExtra(KEY_END_PLAY, true)
            })
        }

        fun rematch(context: Context, internalId: Long, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String, customPlayerSort: Boolean) {
            context.startActivity(createRematchIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort))
        }

        fun changeGame(context: Context, internalId: Long, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String) {
            context.startActivity(createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, false).also {
                it.putExtra(KEY_CHANGE_GAME, true)
            })
        }

        fun createRematchIntent(context: Context, internalId: Long, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String, customPlayerSort: Boolean): Intent {
            return createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort).also {
                it.putExtra(KEY_REMATCH, true)
            }
        }

        private fun createIntent(context: Context, internalId: Long, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String, customPlayerSort: Boolean): Intent {
            return context.intentFor<LogPlayActivity>(
                    KEY_ID to internalId,
                    KEY_GAME_ID to gameId,
                    KEY_GAME_NAME to gameName,
                    KEY_THUMBNAIL_URL to thumbnailUrl,
                    KEY_IMAGE_URL to imageUrl,
                    KEY_HERO_IMAGE_URL to heroImageUrl,
                    KEY_CUSTOM_PLAYER_SORT to customPlayerSort,
            )
        }
    }
}
