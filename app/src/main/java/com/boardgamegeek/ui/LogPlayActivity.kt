package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.content.*
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.view.*
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.events.ColorAssignmentCompleteEvent
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Play
import com.boardgamegeek.model.Player
import com.boardgamegeek.model.builder.PlayBuilder
import com.boardgamegeek.model.builder.PlayBuilder.addPlayers
import com.boardgamegeek.model.builder.PlayBuilder.fromCursor
import com.boardgamegeek.model.builder.PlayBuilder.rematch
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.service.SyncService.Companion.sync
import com.boardgamegeek.tasks.ColorAssignerTask
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter
import com.boardgamegeek.ui.dialog.ColorPickerWithListenerDialogFragment
import com.boardgamegeek.ui.dialog.ColorPickerWithListenerDialogFragment.Companion.newInstance
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment
import com.boardgamegeek.ui.dialog.PlayRatingNumberPadDialogFragment
import com.boardgamegeek.ui.dialog.PlayRatingNumberPadDialogFragment.Companion.newInstance
import com.boardgamegeek.ui.dialog.ScoreNumberPadDialogFragment
import com.boardgamegeek.ui.widget.DatePickerDialogFragment
import com.boardgamegeek.ui.widget.PlayerRow
import com.boardgamegeek.util.*
import com.boardgamegeek.util.ImageUtils.safelyLoadImage
import com.boardgamegeek.util.PaletteUtils.getIconSwatch
import com.github.amlcurran.showcaseview.targets.Target
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_logplay.*
import kotlinx.android.synthetic.main.fragment_play.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.io.use
import kotlin.math.abs

class LogPlayActivity : AppCompatActivity(R.layout.activity_logplay), ColorPickerWithListenerDialogFragment.Listener, ScoreNumberPadDialogFragment.Listener, PlayRatingNumberPadDialogFragment.Listener {
    private val firebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }
    private val prefs: SharedPreferences by lazy { preferences() } // TODO - just use `preferences()` directly
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
    private var queryHandler: QueryHandler? = null
    private var outstandingQueries = TOKEN_UNINITIALIZED
    private var play: Play? = null
    private var originalPlay: Play? = null
    private var addPlayersBuilder: AlertDialog.Builder? = null
    private var lastRemovedPlayer: Player? = null
    private val playersToAdd = mutableListOf<Player>()
    private val userNames = mutableListOf<String>()
    private val names = mutableListOf<String>()
    private val gameColors = ArrayList<String>()

    private var showcaseWizard: ShowcaseViewWizard? = null

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

    @SuppressLint("HandlerLeak")
    private inner class QueryHandler(cr: ContentResolver?) : AsyncQueryHandler(cr) {
        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            // If the query didn't return a cursor for some reason return
            if (cursor == null) {
                return
            }

            // If the Activity is finishing, then close the cursor
            if (isFinishing) {
                cursor.close()
                return
            }
            when (token) {
                TOKEN_PLAY -> {
                    if (cursor.count == 0) {
                        // The cursor is empty. This can happen if the play was deleted
                        cursor.close()
                        finish()
                        return
                    }
                    cursor.use {
                        it.moveToFirst()
                        play = fromCursor(it)
                    }
                    if (isRequestingToEndPlay) {
                        play?.end()
                    }
                    if (outstandingQueries and TOKEN_PLAYERS != 0) {
                        queryHandler?.startQuery(TOKEN_PLAYERS, null, Plays.buildPlayerUri(internalId), PlayBuilder.PLAYER_PROJECTION, null, null, null)
                    }
                    setModelIfDone(token)
                }
                TOKEN_PLAYERS -> {
                    play?.let {
                        cursor.use { c ->
                            addPlayers(c, it)
                        }
                        arePlayersCustomSorted = if (it.getPlayerCount() > 0) {
                            it.arePlayersCustomSorted()
                        } else {
                            intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)
                        }
                        setModelIfDone(token)
                    }
                }
                TOKEN_COLORS -> {
                    if (cursor.count == 0) {
                        cursor.close()
                    } else {
                        cursor.use { c ->
                            if (c.moveToFirst()) {
                                gameColors.clear()
                                do {
                                    gameColors.add(c.getString(0))
                                } while (c.moveToNext())
                            }
                        }
                    }
                    setModelIfDone(token)
                }
                else -> cursor.close()
            }
        }
    }

    private fun setModelIfDone(queryType: Int) {
        synchronized(this) {
            outstandingQueries = outstandingQueries and queryType.inv()
            if (outstandingQueries == 0) {
                if (play == null) {
                    // create a new play
                    play = Play(gameId, gameName)
                    val lastPlay = prefs.getLastPlayTime()
                    if (lastPlay.howManyHoursOld() < 12) {
                        play?.location = prefs.getLastPlayLocation()
                        play?.setPlayers(prefs.getLastPlayPlayers().orEmpty())
                        play?.pickStartPlayer(0)
                    }
                }
                if (isRequestingRematch) {
                    play = rematch(play!!)
                    internalId = INVALID_ID.toLong()
                } else if (isChangingGame) {
                    play = play?.copy(playId = INVALID_ID)
                    internalIdToDelete = internalId
                    internalId = INVALID_ID.toLong()
                }
                originalPlay = play?.copy()
                finishDataLoad()
            }
        }
    }

    private fun finishDataLoad() {
        outstandingQueries = 0
        if (isRequestingToEndPlay) {
            cancelNotification()
        } else {
            maybeShowNotification()
        }

        headerView.text = gameName
        fabColor = ContextCompat.getColor(this@LogPlayActivity, R.color.accent)
        thumbnailView.safelyLoadImage(imageUrl, thumbnailUrl, heroImageUrl, object : ImageUtils.Callback {
            override fun onSuccessfulImageLoad(palette: Palette?) {
                thumbnailView.setBackgroundResource(R.color.black_overlay_light)
                fabColor = getIconSwatch(palette).rgb
                fab.colorize(fabColor)
                fab.post { fab.show() }
                bindPlayerHeader(play!!)
            }

            override fun onFailedImageLoad() {
                fab.show()
            }
        })

        play?.let { bindDate(it) }
        dateButton.setOnClickListener {
            val datePickerFragment = supportFragmentManager.findFragmentByTag(DATE_PICKER_DIALOG_TAG) as DatePickerDialogFragment?
                    ?: DatePickerDialogFragment()
            datePickerFragment.setOnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                play?.setDate(year, monthOfYear, dayOfMonth)
                play?.let { bindDate(it) }
            }
            supportFragmentManager.executePendingTransactions()
            datePickerFragment.setCurrentDateInMillis(play?.dateInMillis
                    ?: System.currentTimeMillis())
            showAndSurvive(datePickerFragment, DATE_PICKER_DIALOG_TAG)
        }

        if (locationView.adapter == null) locationView.setAdapter(locationAdapter)
        play?.let { bindLocation(it) }
        locationView.doAfterTextChanged { play?.location = it.toString().trim() }

        play?.let { bindLength(it) }
        lengthView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
//            if (hasFocus) {
//                canEdit = true
//            } else if (canEdit && !hasFocus) {
                //canEdit = false
                play?.length = lengthView.toString().trim().toIntOrNull() ?: 0
                play?.let { bindLength(it) }
            }
        }
        timerButton.setOnClickListener {
            if (play?.hasStarted() == true) {
                isRequestingToEndPlay = true
                logTimer("Off")
                play?.end()
                play?.let { bindLength(it) }
                lengthView.setTextKeepState(if (play?.length == Play.LENGTH_DEFAULT) "" else play?.length.toString())
                cancelNotification()
                if (play?.length ?: 0 > 0) {
                    lengthView.apply {
                        this.selectAll()
                        this.focusWithKeyboard()
                    }
                }
            } else {
                if (play?.length == 0) {
                    startTimer(play)
                } else {
                    DialogUtils.createThemedBuilder(this@LogPlayActivity)
                            .setMessage(R.string.are_you_sure_timer_reset)
                            .setPositiveButton(R.string.continue_) { _, _ -> resumeTimer(play) }
                            .setNegativeButton(R.string.reset) { _, _ -> startTimer(play) }
                            .setCancelable(true)
                            .show()
                }
            }
        }

        play?.let { bindQuantity(it) }
        quantityView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                play?.quantity = quantityView.text?.trim().toString().toIntOrNull() ?: 1
                bindQuantity(play!!)
            }
        }

        play?.let { bindIncomplete(it) }
        incompleteView.setOnCheckedChangeListener { _, isChecked -> play?.incomplete = isChecked }

        play?.let { bindNoWinStats(it) }
        noWinStatsView.setOnCheckedChangeListener { _, isChecked -> play?.noWinStats = isChecked }

        play?.let { bindComments(it) }
        commentsView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) play?.comments = commentsView.toString()
        }

        play?.let { bindPlayerHeader(it) }
        assignColorsButton.setOnClickListener {
            if (play?.hasColors() == true) {
                val builder = AlertDialog.Builder(this@LogPlayActivity)
                        .setTitle(R.string.title_clear_colors)
                        .setMessage(R.string.msg_clear_colors)
                        .setCancelable(true)
                        .setNegativeButton(R.string.keep) { _: DialogInterface?, _: Int -> ColorAssignerTask(this@LogPlayActivity, play!!).executeAsyncTask() }
                        .setPositiveButton(R.string.clear) { _: DialogInterface?, _: Int ->
                            play?.players?.forEach { it.color = "" }
                            ColorAssignerTask(this@LogPlayActivity, play!!).executeAsyncTask()
                        }
                builder.show()
            } else {
                ColorAssignerTask(this@LogPlayActivity, play!!).executeAsyncTask()
            }
        }
        playerSortButton.setOnClickListener {
            val popup = PopupMenu(this@LogPlayActivity, it)
            popup.inflate(if (!arePlayersCustomSorted && (play?.getPlayerCount()
                            ?: 0) > 1) R.menu.log_play_player_sort else R.menu.log_play_player_sort_short)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_custom_player_order -> {
                        if (arePlayersCustomSorted) {
                            logPlayerOrder("NotCustom")
                            if (play?.hasStartingPositions() == true && play?.arePlayersCustomSorted() == true) {
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
                            if (play?.hasStartingPositions() == true) {
                                val builder = AlertDialog.Builder(this@LogPlayActivity)
                                        .setMessage(R.string.message_custom_player_order)
                                        .setPositiveButton(R.string.keep) { _: DialogInterface?, _: Int ->
                                            arePlayersCustomSorted = true
                                            playAdapter.notifyPlayersChanged()
                                        }
                                        .setNegativeButton(R.string.clear) { _: DialogInterface?, _: Int ->
                                            arePlayersCustomSorted = true
                                            play?.clearPlayerPositions()
                                            playAdapter.notifyPlayersChanged()
                                        }
                                        .setCancelable(true)
                                builder.show()
                            }
                        }
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_pick_start_player -> {
                        logPlayerOrder("Prompt")
                        promptPickStartPlayer()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_random_start_player -> {
                        logPlayerOrder("RandomStarter")
                        play?.let { p -> p.pickStartPlayer((0 until p.getPlayerCount()).random()) }
                        playAdapter.notifyPlayersChanged()
                        notifyStartPlayer()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_random_player_order -> {
                        logPlayerOrder("Random")
                        play?.randomizePlayerOrder()
                        playAdapter.notifyPlayersChanged()
                        notifyStartPlayer()
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
            popup.show()
        }

        addPlayerButton.setOnClickListener {
            if (prefs.getEditPlayerPrompted()) {
                addPlayers(prefs.getEditPlayer())
            } else {
                promptToEditPlayers()
            }
        }

        // XXX

        playAdapter.notifyDataSetChanged()
        progressView.hide()
        recyclerView.isVisible = true
    }

    private fun addPlayers(editPlayer: Boolean) {
        if (editPlayer) {
            if (!showPlayersToAddDialog()) {
                addNewPlayer()
            }
        } else {
            val player = Player()
            if (!arePlayersCustomSorted) {
                player.seat = play!!.getPlayerCount() + 1
            }
            play?.addPlayer(player)
            playAdapter.notifyPlayerAdded(play!!.getPlayerCount())
            recyclerView.smoothScrollToPosition(playAdapter.itemCount)
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
        prefs.putEditPlayerPrompted()
    }

    private fun onPromptClickListener(value: Boolean): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _, _ ->
            prefs.putEditPlayer(value)
            addPlayers(value)
        }
    }

    private fun bindDate(play: Play) {
        dateButton.text = play.getDateForDisplay(this)
    }

    private fun bindLocation(play: Play) {
        locationFrame.isVisible = !shouldHideLocation()
        locationView.setTextKeepState(play.location)
    }

    private fun bindLength(play: Play) {
        if (shouldHideLength()) {
            lengthGroup.isVisible = false
        } else {
            lengthGroup.isVisible = true
            lengthView.isVisible = true
            lengthView.setTextKeepState(if (play.length == Play.LENGTH_DEFAULT) "" else play.length.toString())
            UIUtils.startTimerWithSystemTime(timer, play.startTime)
            if (play.hasStarted()) {
                lengthFrame.visibility = View.INVISIBLE
                timer.visibility = View.VISIBLE
            } else {
                lengthView.visibility = View.VISIBLE
                timer.visibility = View.GONE
            }
            val endTime = play.dateInMillis + (play.length * DateUtils.MINUTE_IN_MILLIS)
            when {
                play.hasStarted() -> {
                    timerButton.isEnabled = true
                    timerButton.setImageResource(R.drawable.ic_timer_off)
                }
                endTime.isToday() -> {
                    timerButton.isEnabled = true
                    timerButton.setImageResource(R.drawable.ic_timer)
                }
                else -> {
                    timerButton.isEnabled = false
                }
            }
        }
    }

    private fun bindQuantity(play: Play) {
        quantityFrame.isVisible = !shouldHideQuantity()
        quantityView.setTextKeepState(if (play.quantity == Play.QUANTITY_DEFAULT) "" else play.quantity.toString())
    }

    private fun bindIncomplete(play: Play) {
        incompleteView.isVisible = !shouldHideIncomplete()
        incompleteView.isChecked = play.incomplete
    }

    private fun bindNoWinStats(play: Play) {
        noWinStatsView.isVisible = !shouldHideIncomplete()
        noWinStatsView.isChecked = play.noWinStats
    }

    private fun bindComments(play: Play) {
        commentsFrame.isVisible = !shouldHideComments()
        commentsView.setTextKeepState(play.comments)
    }

    private fun bindPlayerHeader(play: Play) {
        playerHeader.isVisible = !shouldHidePlayers()
        val playerCount = play.getPlayerCount()
        playersLabel.text = if (playerCount <= 0) getString(R.string.title_players) else getString(R.string.title_players_with_count, playerCount)
        assignColorsButton.isEnabled = playerCount > 0
        // playerSortButton.isEnabled = !arePlayersCustomSorted && playerCount > 1
        ViewCompat.setBackgroundTintList(addPlayerButton, ColorStateList.valueOf(fabColor))
    }

    private val DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDoneCancelActionBarView { v: View ->
            when (v.id) {
                R.id.menu_done -> if (play != null && outstandingQueries == 0) {
                    if (play?.hasStarted() == true) {
                        saveDraft(true)
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        logPlay()
                    }
                } else {
                    cancel()
                }
                R.id.menu_cancel -> cancel()
            }
        }

        horizontalPadding = resources.getDimension(R.dimen.material_margin_horizontal)
        fab.setOnClickListener { addField() }

        recyclerView.setHasFixedSize(false)
        recyclerView.adapter = playAdapter

        swipePaint.color = ContextCompat.getColor(this, R.color.medium_blue)
        itemTouchHelper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                        if (viewHolder !is PlayAdapter.PlayerViewHolder) return
                        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
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
                                swipePaint.color = ContextCompat.getColor(this@LogPlayActivity, R.color.delete)
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
                                swipePaint.color = ContextCompat.getColor(this@LogPlayActivity, R.color.edit)
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
                            lastRemovedPlayer?.let {
                                coordinatorLayout.indefiniteSnackbar(
                                        getString(R.string.msg_player_deleted, it.description.ifEmpty { getString(R.string.title_player) }),
                                        getString(R.string.undo)) {
                                    lastRemovedPlayer?.let { p ->
                                        play?.addPlayer(p)
                                        playAdapter.notifyPlayersChanged()
                                    }
                                }
                                play?.removePlayer(it, !arePlayersCustomSorted)
                                playAdapter.notifyPlayerRemoved(viewHolder.adapterPosition)
                            }
                        } else {
                            editPlayer(viewHolder.adapterPosition)
                        }
                    }

                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                        if (target !is PlayAdapter.PlayerViewHolder) return false
                        val fromPosition = viewHolder.adapterPosition
                        val toPosition = target.adapterPosition
                        return if (play?.reorderPlayers(fromPosition + 1, toPosition + 1) == true) {
                            playAdapter.notifyItemMoved(fromPosition, toPosition)
                            true
                        } else {
                            longToast("Something went wrong")
                            false
                        }
                    }

                    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                        super.clearView(recyclerView, viewHolder)
                        viewHolder.itemView.setBackgroundColor(0)
                        playAdapter.notifyPlayersChanged()
                    }

                    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                        // We only want the active item to change
                        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                            viewHolder?.itemView?.setBackgroundColor(ContextCompat.getColor(this@LogPlayActivity, R.color.light_blue_transparent))
                        }
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
        queryHandler = QueryHandler(contentResolver)

        internalId = intent.getLongExtra(KEY_ID, INVALID_ID.toLong())
        gameId = intent.getIntExtra(KEY_GAME_ID, INVALID_ID)
        gameName = intent.getStringExtra(KEY_GAME_NAME).orEmpty()
        isRequestingToEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false)
        isRequestingRematch = intent.getBooleanExtra(KEY_REMATCH, false)
        isChangingGame = intent.getBooleanExtra(KEY_CHANGE_GAME, false)
        thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL).orEmpty()
        imageUrl = intent.getStringExtra(KEY_IMAGE_URL).orEmpty()
        heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL).orEmpty()

        if (gameId <= 0) {
            val message = "Can't log a play without a game ID."
            Timber.w(message)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
        savedInstanceState?.let {
            play = it.getParcelable(KEY_PLAY)
            originalPlay = it.getParcelable(KEY_ORIGINAL_PLAY)
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
        startQuery()
        setUpShowcaseViewWizard()
        showcaseWizard?.maybeShowHelp()
        fab.postDelayed({ fab.show() }, 2000)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        isLaunchingActivity = false
        playAdapter.notifyDataSetChanged()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (outstandingQueries == 0) {
            outState.putParcelable(KEY_PLAY, play)
            outState.putParcelable(KEY_ORIGINAL_PLAY, originalPlay)
        }
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
            saveDraft(false)
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onBackPressed() {
        saveDraft(true)
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
                        play?.addPlayer(player)
                        maybeShowNotification()
                        addNewPlayer()
                    }
                    REQUEST_EDIT_PLAYER -> if (position == LogPlayerActivity.INVALID_POSITION) {
                        Timber.w("Invalid player position after edit")
                    } else {
                        play?.replaceOrAddPlayer(player, position)
                        playAdapter.notifyPlayerChanged(position)
                        recyclerView.smoothScrollToPosition(position)
                    }
                    else -> Timber.w("Received invalid request code: %d", requestCode)
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            playAdapter.notifyPlayersChanged()
            recyclerView.smoothScrollToPosition(playAdapter.itemCount)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ColorAssignmentCompleteEvent) {
        firebaseAnalytics.logEvent("LogPlayColorAssignment", null)
        EventBus.getDefault().removeStickyEvent(event)
        if (event.isSuccessful) {
            playAdapter.notifyPlayersChanged()
        }
        if (event.messageId != 0) {
            Snackbar.make(coordinatorLayout, event.messageId, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setUpShowcaseViewWizard() {
        showcaseWizard = ShowcaseViewWizard(this, HelpUtils.HELP_LOGPLAY_KEY, HELP_VERSION)
        showcaseWizard?.addTarget(R.string.help_logplay, Target.NONE)
    }

    private fun shouldHideLocation(): Boolean {
        return play != null && !prefs.showLogPlayLocation() && !isUserShowingLocation && play?.location.isNullOrEmpty()
    }

    private fun shouldHideLength(): Boolean {
        return play != null && !prefs.showLogPlayLength() && !isUserShowingLength && play!!.length <= 0 && !play!!.hasStarted()
    }

    private fun shouldHideQuantity(): Boolean {
        return play != null && !prefs.showLogPlayQuantity() && !isUserShowingQuantity && play!!.quantity <= 1
    }

    private fun shouldHideIncomplete(): Boolean {
        return play != null && !prefs.showLogPlayIncomplete() && !isUserShowingIncomplete && !play!!.incomplete
    }

    private fun shouldHideNoWinStats(): Boolean {
        return play != null && !prefs.showLogPlayNoWinStats() && !isUserShowingNoWinStats && !play!!.noWinStats
    }

    private fun shouldHideComments(): Boolean {
        return play != null && !prefs.showLogPlayComments() && !isUserShowingComments && play?.comments.isNullOrEmpty()
    }

    private fun shouldHidePlayers(): Boolean {
        return play != null && !prefs.showLogPlayPlayerList() && !isUserShowingPlayers && play!!.getPlayerCount() == 0
    }

    private fun startQuery() {
        if (play != null) {
            // we already have the play from the saved instance
            finishDataLoad()
        } else {
            outstandingQueries = TOKEN_COLORS
            if (internalId != INVALID_ID.toLong()) {
                // Editing or copying an existing play, so retrieve it
                shouldDeletePlayOnActivityCancel = false
                outstandingQueries = outstandingQueries or (TOKEN_PLAY or TOKEN_PLAYERS)
                if (isRequestingRematch || isChangingGame) {
                    shouldDeletePlayOnActivityCancel = true
                }
                queryHandler?.startQuery(TOKEN_PLAY, null, Plays.buildPlayUri(internalId), PlayBuilder.PLAY_PROJECTION, null, null, null)
            } else {
                // Starting a new play
                shouldDeletePlayOnActivityCancel = true
                arePlayersCustomSorted = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false)
            }
            queryHandler?.startQuery(TOKEN_COLORS, null, Games.buildColorsUri(gameId), arrayOf(GameColors.COLOR), null, null, null)
        }
    }

    private fun logPlay() {
        play?.let {
            val now = System.currentTimeMillis()
            it.updateTimestamp = now
            it.deleteTimestamp = 0
            it.dirtyTimestamp = now
            if (save()) {
                if (internalIdToDelete != INVALID_ID.toLong()) {
                    val playRepository = PlayRepository((application as BggApplication))
                    playRepository.markAsDeleted(internalIdToDelete, null)
                }
                // TODO move this to !it.isSynced
                if (it.playId == 0 && (it.dateInMillis.isToday() || (now - it.length * DateUtils.MINUTE_IN_MILLIS).isToday())) {
                    prefs.putLastPlayTime(now)
                    prefs.putLastPlayLocation(it.location)
                    prefs.putLastPlayPlayers(it.players)
                }
                cancelNotification()
                triggerUpload()
                Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show()
            }
        }
        setResult(RESULT_OK)
        finish()
    }

    private fun saveDraft(showToast: Boolean) {
        if (play == null) return
        play?.let {
            it.dirtyTimestamp = System.currentTimeMillis()
            it.deleteTimestamp = 0
            if (save()) {
                if (showToast) toast(R.string.msg_saving_draft)
                maybeShowNotification()
            }
        }
    }

    private fun save(): Boolean {
        if (play == null) return false
        shouldSaveOnPause = false
        recyclerView.findFocus()?.clearFocus()
        internalId = PlayPersister(this).save(play, internalId, true)
        return true
    }

    private fun cancel() {
        shouldSaveOnPause = false
        if (play == null) {
            setResult(RESULT_CANCELED)
            finish()
        } else if (play == originalPlay) {
            if (shouldDeletePlayOnActivityCancel) {
                deletePlay()
            }
            setResult(RESULT_CANCELED)
            finish()
        } else {
            if (shouldDeletePlayOnActivityCancel) {
                DialogUtils.createDiscardDialog(this, R.string.play, true, true) { deletePlay() }.show()
            } else {
                DialogUtils.createDiscardDialog(this, R.string.play, false).show()
            }
        }
    }

    private fun deletePlay() {
        play?.updateTimestamp = 0
        play?.deleteTimestamp = System.currentTimeMillis()
        play?.dirtyTimestamp = 0
        if (save()) {
            triggerUpload()
            cancelNotification()
        }
    }

    private fun triggerUpload() {
        sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD)
    }

    private fun addField() {
        val array = createAddFieldArray()
        if (array.isEmpty()) return
        AlertDialog.Builder(this).setTitle(R.string.add_field)
                .setItems(array) { _: DialogInterface?, which: Int ->
                    val selection = array[which].toString()
                    when (selection) {
                        resources.getString(R.string.location) -> {
                            isUserShowingLocation = true
                            play?.let { bindLocation(it) }
                        }
                        resources.getString(R.string.length) -> {
                            isUserShowingLength = true
                            play?.let { bindLength(it) }
                        }
                        resources.getString(R.string.quantity) -> {
                            isUserShowingQuantity = true
                            play?.let { bindQuantity(it) }
                        }
                        resources.getString(R.string.incomplete) -> {
                            isUserShowingIncomplete = true
                            play?.incomplete = true
                            play?.let { bindIncomplete(it) }
                        }
                        resources.getString(R.string.noWinStats) -> {
                            isUserShowingNoWinStats = true
                            play?.noWinStats = true
                            play?.let { bindNoWinStats(it) }
                        }
                        resources.getString(R.string.comments) -> {
                            isUserShowingComments = true
                            play?.let { bindComments(it) }
                        }
                        resources.getString(R.string.title_players) -> {
                            isUserShowingPlayers = true
                            play?.let { bindPlayerHeader(it) }
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
        if (shouldHideLocation()) list.add(getString(R.string.location))
        if (shouldHideLength()) list.add(getString(R.string.length))
        if (shouldHideQuantity()) list.add(getString(R.string.quantity))
        if (shouldHideIncomplete()) list.add(getString(R.string.incomplete))
        if (shouldHideNoWinStats()) list.add(getString(R.string.noWinStats))
        if (shouldHideComments()) list.add(getString(R.string.comments))
        if (shouldHidePlayers()) list.add(getString(R.string.title_players))
        return list.toTypedArray()
    }

    private fun containsPlayer(username: String, name: String): Boolean {
        return play?.players?.find {
            (username.isNotBlank() && username == it.username) ||
                    (name.isNotBlank() && username.isBlank() && name == it.name)
        } != null
    }

    private fun showPlayersToAddDialog(): Boolean {
        if (addPlayersBuilder == null) {
            addPlayersBuilder = AlertDialog.Builder(this).setTitle(R.string.title_add_players)
                    .setPositiveButton(android.R.string.ok, addPlayersButtonClickListener())
                    .setNeutralButton(R.string.more, addPlayersButtonClickListener())
                    .setNegativeButton(android.R.string.cancel, null)
        }
        playersToAdd.clear()
        userNames.clear()
        names.clear()
        val descriptions = mutableListOf<String>()
//        val sel = if (play?.location.isNullOrEmpty()) Pair<String?, Array<String?>?>(null, null) else "${Plays.LOCATION}=?" to arrayOf(play!!.location)
        val sel = play?.location?.let {
            "${Plays.LOCATION}=?" to arrayOf(it)
        }
        contentResolver.query(Plays.buildPlayersByUniqueNameUri(),
                arrayOf(PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.DESCRIPTION, PlayPlayers.COUNT, PlayPlayers.UNIQUE_NAME),
                sel?.first,
                sel?.second,
                PlayPlayers.SORT_BY_COUNT
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val username = cursor.getString(1)
                val name = cursor.getString(2)
                if (!containsPlayer(username, name)) {
                    userNames.add(username)
                    names.add(name)
                    descriptions.add(cursor.getString(3))
                }
            }
        }
        if (descriptions.size == 0) {
            return false
        }
        addPlayersBuilder?.setMultiChoiceItems(descriptions.toTypedArray<CharSequence>(), null) { _, which, isChecked ->
            val player = Player(name = names[which], username = userNames[which])
            if (isChecked) {
                playersToAdd.add(player)
            } else {
                playersToAdd.remove(player)
            }
        }?.create()?.show()
        return true
    }

    internal class LocationAdapter(context: Context) : AutoCompleteAdapter(context, Plays.LOCATION, Plays.buildLocationsUri(), PlayLocations.SORT_BY_SUM_QUANTITY, Plays.SUM_QUANTITY) {
        //String.format("%1$s='' OR %1s$ IS NULL", Plays.LOCATION);
        override val defaultSelection: String
            get() = "${Plays.LOCATION}<>''" //String.format("%1$s='' OR %1s$ IS NULL", Plays.LOCATION);
    }

    private fun addPlayersButtonClickListener(): DialogInterface.OnClickListener {
        return DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
            play?.let {
                it.setPlayers(playersToAdd)
                if (!arePlayersCustomSorted) {
                    it.pickStartPlayer(0)
                }
                playAdapter.notifyPlayersChanged()
                if (which == DialogInterface.BUTTON_NEUTRAL) {
                    addNewPlayer()
                }
            }
        }
    }

    private fun autoSortPlayers() {
        arePlayersCustomSorted = false
        play?.pickStartPlayer(0)
        playAdapter.notifyPlayersChanged()
    }

    private fun promptPickStartPlayer() {
        val array = createArrayOfPlayerDescriptions()
        AlertDialog.Builder(this).setTitle(R.string.title_pick_start_player)
                .setItems(array) { _: DialogInterface?, which: Int ->
                    play?.pickStartPlayer(which)
                    notifyStartPlayer()
                    playAdapter.notifyPlayersChanged()
                }
                .show()
    }

    private fun createArrayOfPlayerDescriptions(): Array<CharSequence> {
        val playerPrefix = resources.getString(R.string.generic_player)
        val list: MutableList<CharSequence> = ArrayList()
        for (i in 0 until play!!.getPlayerCount()) {
            val p = play!!.players[i]
            list.add(p.description.ifEmpty { String.format(playerPrefix, i + 1) })
        }
        return list.toTypedArray()
    }

    private fun notifyStartPlayer() {
        play?.getPlayerAtSeat(1)?.let {
            val name = it.description.ifEmpty { String.format(resources.getString(R.string.generic_player), 1) }
            Snackbar.make(coordinatorLayout, String.format(resources.getString(R.string.notification_start_player), name), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun addNewPlayer() {
        val intent = Intent()
        if (!arePlayersCustomSorted) {
            intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, play!!.getPlayerCount() + 1)
        }
        editPlayer(intent, REQUEST_ADD_PLAYER)
    }

    private fun editPlayer(position: Int) {
        val player = playAdapter.getPlayer(position)
        val intent = Intent()
        intent.putExtra(LogPlayerActivity.KEY_PLAYER, player)
        intent.putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay)
        if (!arePlayersCustomSorted && player != null) {
            intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, player.seat)
        }
        intent.putExtra(LogPlayerActivity.KEY_POSITION, position)
        editPlayer(intent, REQUEST_EDIT_PLAYER)
        playAdapter.notifyPlayerChanged(position)
    }

    private fun editPlayer(intent: Intent, requestCode: Int) {
        isLaunchingActivity = true
        intent.setClass(this@LogPlayActivity, LogPlayerActivity::class.java)
        intent.putExtra(LogPlayerActivity.KEY_GAME_ID, play!!.gameId)
        intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, play!!.gameName)
        intent.putExtra(LogPlayerActivity.KEY_IMAGE_URL, imageUrl)
        intent.putExtra(LogPlayerActivity.KEY_THUMBNAIL_URL, thumbnailUrl)
        intent.putExtra(LogPlayerActivity.KEY_HERO_IMAGE_URL, heroImageUrl)
        intent.putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay)
        intent.putExtra(LogPlayerActivity.KEY_FAB_COLOR, fabColor)
        val colors: MutableList<String> = ArrayList()
        for ((_, _, color) in play!!.players) {
            colors.add(color)
        }
        intent.putExtra(LogPlayerActivity.KEY_USED_COLORS, colors.toTypedArray())
        intent.putExtra(LogPlayerActivity.KEY_NEW_PLAYER, requestCode == REQUEST_ADD_PLAYER)
        startActivityForResult(intent, requestCode)
    }

    private fun maybeShowNotification() {
        if (play?.hasStarted() == true && internalId != INVALID_ID.toLong()) {
            this.launchPlayingNotification(internalId, play!!.gameName, play!!.location!!, play!!.getPlayerCount(), play!!.startTime, thumbnailUrl, imageUrl, heroImageUrl)
        }
    }

    private fun cancelNotification() {
        cancel(TAG_PLAY_TIMER, internalId)
    }

    override fun onColorSelected(description: String, color: Int, requestCode: Int) {
        play?.players?.get(requestCode)?.color = description
        playAdapter.notifyPlayerChanged(requestCode)
    }

    override fun onNumberPadDone(output: Double, requestCode: Int) {
        play?.let {
            val position = requestCode / 2
            val player = it.players[position]
            if (requestCode % 2 == 0) {
                player.score = SCORE_FORMAT.format(output)
                val highScore = it.highScore
                for (p in it.players) {
                    p.isWin = (p.score.toDoubleOrNull() ?: Double.NaN) == highScore
                }
                playAdapter.notifyPlayersChanged()
            } else {
                player.rating = output
                playAdapter.notifyPlayerChanged(position)
            }
        }
    }

    private fun startTimer(play: Play?) {
        logTimer("On")
        play?.start()
        play?.let { bindLength(it) }
        maybeShowNotification()
    }

    private fun resumeTimer(play: Play?) {
        logTimer("On")
        play?.resume()
        play?.let { bindLength(it) }
        maybeShowNotification()
    }

    private fun logTimer(state: String) {
        firebaseAnalytics.logEvent("LogPlayTimer") {
            param("State", state)
        }
    }

    inner class PlayAdapter : RecyclerView.Adapter<PlayAdapter.PlayerViewHolder>() {
        init {
            setHasStableIds(false)
        }

        override fun getItemCount(): Int {
            return play?.getPlayerCount() ?: 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            return PlayerViewHolder(this@LogPlayActivity)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            holder.bind(position)
        }

        fun getPlayer(position: Int): Player? {
            return play?.players?.getOrNull(position)
        }

        fun notifyPlayersChanged() {
            Handler().post {
                bindPlayerHeader(play!!)
                notifyItemRangeChanged(0, play!!.getPlayerCount())
            }
            maybeShowNotification()
        }

        fun notifyPlayerChanged(playerPosition: Int) {
            Handler().post { notifyItemChanged(playerPosition) }
        }

        fun notifyPlayerAdded(playerPosition: Int) {
            Handler().post {
                bindPlayerHeader(play!!)
                notifyItemInserted(playerPosition)
                notifyItemRangeChanged(playerPosition + 1, play!!.getPlayerCount() - playerPosition - 1)
            }
            maybeShowNotification()
        }

        fun notifyPlayerRemoved(playerPosition: Int) {
            Handler().post {
                bindPlayerHeader(play!!)
                notifyItemRemoved(playerPosition)
                notifyItemRangeChanged(playerPosition + 1, play!!.getPlayerCount() - playerPosition)
            }
            maybeShowNotification()
        }

        inner class PlayerViewHolder(context: Context) : RecyclerView.ViewHolder(PlayerRow(context)) {
            private val row: PlayerRow = itemView as PlayerRow

            @SuppressLint("ClickableViewAccessibility")
            fun bind(position: Int) {
                row.setAutoSort(!arePlayersCustomSorted)
                row.setPlayer(getPlayer(position))
                row.setNameListener { editPlayer(position) }
                row.setOnMoreListener {
                    if (position < 0 || position > play!!.players.size) return@setOnMoreListener
                    val player = play!!.players[position]
                    val popup = PopupMenu(this@LogPlayActivity, row.getMoreButton())
                    popup.inflate(R.menu.log_play_player)
                    popup.menu.findItem(R.id.new_).isChecked = player.isNew
                    popup.menu.findItem(R.id.win).isChecked = player.isWin
                    popup.setOnMenuItemClickListener { item: MenuItem ->
                        when (item.itemId) {
                            R.id.new_ -> {
                                player.isNew = !item.isChecked
                                bind(position)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.win -> {
                                player.isWin = !item.isChecked
                                bind(position)
                                return@setOnMenuItemClickListener true
                            }
                        }
                        false
                    }
                    popup.show()
                }
                row.getDragHandle().setOnTouchListener { _: View?, event: MotionEvent ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper?.startDrag(this@PlayerViewHolder)
                        return@setOnTouchListener true
                    }
                    false
                }
                row.setOnColorListener {
                    val player = play!!.players[position]
                    val usedColors = ArrayList<String>()
                    for (p in play!!.players) {
                        if (p !== player) usedColors.add(p.color)
                    }
                    val fragment = newInstance(gameColors, player.color, usedColors, position)
                    fragment.show(supportFragmentManager, "color_picker")
                }
                row.setOnRatingListener {
                    val player = play!!.players[position]
                    val fragment: NumberPadDialogFragment = newInstance(
                            position * 2 + 1,
                            player.ratingDescription,
                            player.color,
                            player.description
                    )
                    DialogUtils.showFragment(this@LogPlayActivity, fragment, "rating_dialog")
                }
                row.setOnScoreListener {
                    val player = play!!.players[position]
                    val fragment = ScoreNumberPadDialogFragment.newInstance(
                            position * 2,
                            player.score,
                            player.color,
                            player.description)
                    DialogUtils.showFragment(this@LogPlayActivity, fragment, "score_dialog")
                }
            }

            init {
                row.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
        private const val KEY_PLAY = "PLAY"
        private const val KEY_ORIGINAL_PLAY = "ORIGINAL_PLAY"
        private const val KEY_IS_USER_SHOWING_LOCATION = "IS_USER_SHOWING_LOCATION"
        private const val KEY_IS_USER_SHOWING_LENGTH = "IS_USER_SHOWING_LENGTH"
        private const val KEY_IS_USER_SHOWING_QUANTITY = "IS_USER_SHOWING_QUANTITY"
        private const val KEY_IS_USER_SHOWING_INCOMPLETE = "IS_USER_SHOWING_INCOMPLETE"
        private const val KEY_IS_USER_SHOWING_NO_WIN_STATS = "IS_USER_SHOWING_NO_WIN_STATS"
        private const val KEY_IS_USER_SHOWING_COMMENTS = "IS_USER_SHOWING_COMMENTS"
        private const val KEY_IS_USER_SHOWING_PLAYERS = "IS_USER_SHOWING_PLAYERS"
        private const val KEY_SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL = "SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL"
        private const val KEY_ARE_PLAYERS_CUSTOM_SORTED = "ARE_PLAYERS_CUSTOM_SORTED"
        private const val HELP_VERSION = 3
        private const val REQUEST_ADD_PLAYER = 1
        private const val REQUEST_EDIT_PLAYER = 2
        private const val TOKEN_PLAY = 1
        private const val TOKEN_PLAYERS = 1 shl 1
        private const val TOKEN_COLORS = 1 shl 2
        private const val TOKEN_UNINITIALIZED = 1 shl 15
        private val SCORE_FORMAT = DecimalFormat("0.#########")

        fun logPlay(context: Context, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String, customPlayerSort: Boolean) {
            context.startActivity(createIntent(context, INVALID_ID.toLong(), gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort))
        }

        fun editPlay(context: Context, internalId: Long, gameId: Int, gameName: String, thumbnailUrl: String, imageUrl: String, heroImageUrl: String) {
            // TODO move this to onCreate?
            FirebaseAnalytics.getInstance(context).logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Play")
                param("Action", "Edit")
                param("GameName", gameName)
            }
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