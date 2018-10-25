package com.boardgamegeek.ui

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.getQuantityText
import com.boardgamegeek.extensions.setBggColors
import com.boardgamegeek.extensions.setColorViewValue
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.tasks.sync.SyncUserTask
import com.boardgamegeek.tasks.sync.SyncUserTask.CompletedEvent
import com.boardgamegeek.ui.dialog.EditTextDialogFragment
import com.boardgamegeek.ui.dialog.UpdateBuddyNicknameDialogFragment
import com.boardgamegeek.ui.model.Buddy
import com.boardgamegeek.ui.model.Player
import com.boardgamegeek.ui.model.PlayerColor
import com.boardgamegeek.util.DialogUtils
import com.boardgamegeek.util.ImageUtils.loadThumbnail
import com.boardgamegeek.util.SelectionBuilder
import com.boardgamegeek.util.TaskUtils
import kotlinx.android.synthetic.main.fragment_buddy.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx
import timber.log.Timber

class BuddyFragment : Fragment(), LoaderCallbacks<Cursor> {
    private var buddyName: String? = null
    private var playerName: String? = null
    private var isRefreshing: Boolean = false
    private var hasBeenRefreshed: Boolean = false //TODO store in state
    private var defaultTextColor: Int = 0
    private var lightTextColor: Int = 0

    private val isUser
        get() = buddyName?.isNotBlank() == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buddyName = arguments?.getString(KEY_BUDDY_NAME) ?: ""
        playerName = arguments?.getString(KEY_PLAYER_NAME) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_buddy, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        buddyInfoView.visibility = if (isUser) View.VISIBLE else View.GONE
        collectionCard.visibility = if (isUser) View.VISIBLE else View.GONE
        updatedView.visibility = if (isUser) View.VISIBLE else View.GONE

        if (isUser) {
            swipeRefresh.setOnRefreshListener {
                requestRefresh()
            }
            swipeRefresh.setBggColors()
            swipeRefresh.isEnabled = true
        } else {
            swipeRefresh.isEnabled = false
        }

        defaultTextColor = nicknameView.textColors.defaultColor
        lightTextColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

        nicknameView.setOnClickListener {
            val nickname = nicknameView.text.toString()
            if (isUser) {
                val dialogFragment = UpdateBuddyNicknameDialogFragment.newInstance(nickname)
                DialogUtils.showFragment(act, dialogFragment, "edit_nickname")
            } else {
                val editTextDialogFragment = EditTextDialogFragment.newInstance(R.string.title_edit_player, nickname)
                DialogUtils.showFragment(act, editTextDialogFragment, "edit_player")
            }
        }

        playsRoot.setOnClickListener {
            if (isUser) {
                BuddyPlaysActivity.start(ctx, buddyName)
            } else {
                PlayerPlaysActivity.start(ctx, playerName)
            }
        }

        collectionRoot.setOnClickListener {
            BuddyCollectionActivity.start(ctx, buddyName)
        }

        colorsRoot.setOnClickListener {
            PlayerColorsActivity.start(ctx, buddyName, playerName)
        }

        if (isUser) {
            LoaderManager.getInstance(this).restartLoader(TOKEN, null, this)
        } else {
            nicknameView.setTextColor(defaultTextColor)
            nicknameView.text = playerName
        }
        LoaderManager.getInstance(this).restartLoader(PLAYS_TOKEN, null, this)
        LoaderManager.getInstance(this).restartLoader(COLORS_TOKEN, null, this)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    private fun updateRefreshStatus(value: Boolean) {
        isRefreshing = value
        swipeRefresh?.post { swipeRefresh?.isRefreshing = isRefreshing }
    }

    override fun onCreateLoader(id: Int, data: Bundle?): Loader<Cursor> {
        return when (id) {
            TOKEN -> CursorLoader(requireContext(), Buddies.buildBuddyUri(buddyName), Buddy.projection, null, null, null)
            PLAYS_TOKEN -> if (isUser) {
                CursorLoader(requireContext(),
                        Plays.buildPlayersByUniqueUserUri(),
                        Player.PROJECTION,
                        PlayPlayers.USER_NAME + "=? AND " + SelectionBuilder.whereZeroOrNull(Plays.NO_WIN_STATS),
                        arrayOf(buddyName), null)

            } else {
                CursorLoader(requireContext(),
                        Plays.buildPlayersByUniquePlayerUri(),
                        Player.PROJECTION,
                        "(" + PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL) AND play_players." + PlayPlayers.NAME + "=?",
                        arrayOf("", playerName), null)
            }
            COLORS_TOKEN -> CursorLoader(requireContext(),
                    if (isUser) PlayerColors.buildUserUri(buddyName) else PlayerColors.buildPlayerUri(playerName),
                    PlayerColor.PROJECTION, null, null, null)
            else -> {
                throw RuntimeException()
            }
        }
    }

    override fun onLoadFinished(@NonNull loader: Loader<Cursor>, cursor: Cursor) {
        if (activity == null) return

        when (loader.id) {
            TOKEN -> onBuddyQueryComplete(cursor)
            PLAYS_TOKEN -> onPlaysQueryComplete(cursor)
            COLORS_TOKEN -> onColorsQueryComplete(cursor)
            else -> cursor.close()
        }
    }

    override fun onLoaderReset(@NonNull loader: Loader<Cursor>) {}

    private fun onBuddyQueryComplete(cursor: Cursor?) {
        if (cursor == null || !cursor.moveToFirst()) {
            requestRefresh()
            return
        }

        val buddy = Buddy.fromCursor(cursor)

        avatarView.loadThumbnail(buddy.avatarUrl, R.drawable.person_image_empty)
        fullNameView.text = buddy.fullName
        usernameView.text = buddyName
        if (buddy.nickName.isBlank()) {
            nicknameView.setTextColor(lightTextColor)
            nicknameView.text = buddy.firstName
        } else {
            nicknameView.setTextColor(defaultTextColor)
            nicknameView.text = buddy.nickName
        }
        updatedView.timestamp = buddy.updated
    }

    private fun onPlaysQueryComplete(cursor: Cursor?) {
        if (cursor == null || !cursor.moveToFirst()) {
            return
        }

        val player = Player.fromCursor(cursor)
        val playCount = player.playCount
        val winCount = player.winCount
        if (playCount > 0 || winCount > 0) {
            playsCard.isVisible = true
            playsView.text = ctx.getQuantityText(R.plurals.winnable_plays_suffix, playCount, playCount)
            winsView.text = ctx.getQuantityText(R.plurals.wins_suffix, winCount, winCount)
            winPercentageView.text = getString(R.string.percentage, (winCount.toDouble() / playCount * 100).toInt())
        } else {
            playsCard.isGone = true
        }
    }

    private fun onColorsQueryComplete(cursor: Cursor?) {
        colorContainer.removeAllViews()

        colorContainer.isVisible = (cursor?.count ?: 0) > 0

        if (cursor?.moveToFirst() == true) {
            var count = 0
            do {
                count++
                val view = createViewToBeColored()
                val color = PlayerColor.fromCursor(cursor)
                view.setColorViewValue(color.color.asColorRgb())
                colorContainer.addView(view)
            } while (count < 3 && cursor.moveToNext())
        }

    }

    private fun createViewToBeColored(): ImageView {
        val view = ImageView(ctx)
        val size = resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small)
        val margin = resources.getDimensionPixelSize(R.dimen.color_circle_diameter_small_margin)
        view.layoutParams = LayoutParams(size, size).apply {
            setMargins(margin)
        }
        return view
    }

    private fun requestRefresh() {
        if (!isRefreshing) {
            if (hasBeenRefreshed) {
                updateRefreshStatus(false)
            } else {
                forceRefresh()
            }
        }
    }

    private fun forceRefresh() {
        if (isUser) {
            updateRefreshStatus(true)
            TaskUtils.executeAsyncTask(SyncUserTask(activity, buddyName))
        } else {
            Timber.w("Something tried to refresh a player that wasn't a user!")
        }
        hasBeenRefreshed = true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: CompletedEvent) {
        if (event.username == buddyName) {
            updateRefreshStatus(false)
        }
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        private const val PLAYS_TOKEN = 1
        private const val COLORS_TOKEN = 2
        private const val TOKEN = 0

        fun newInstance(username: String?, playerName: String?): BuddyFragment {
            return BuddyFragment().apply {
                arguments = bundleOf(
                        KEY_BUDDY_NAME to username,
                        KEY_PLAYER_NAME to playerName)
            }
        }
    }
}
