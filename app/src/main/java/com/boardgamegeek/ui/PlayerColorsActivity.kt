package com.boardgamegeek.ui

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayerColorEntity
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.extensions.setColorViewValue
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.dialog.PlayerColorPickerDialogFragment
import com.boardgamegeek.ui.viewmodel.PlayerColorsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.activity_player_colors.*
import kotlinx.android.synthetic.main.row_player_color.view.*
import org.jetbrains.anko.startActivity
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

class PlayerColorsActivity : BaseActivity() {
    private var buddyName: String? = null
    private var playerName: String? = null

    private val usedColors = ArrayList<String>()

    private val viewModel by viewModels<PlayerColorsViewModel>()

    private val itemTouchHelper by lazy {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.material_margin_horizontal).toFloat()
            val deleteIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_delete_white)
            val swipePaint: Paint by lazy {
                val swipePaint = Paint()
                swipePaint.color = ContextCompat.getColor(this@PlayerColorsActivity, R.color.medium_blue)
                swipePaint
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                return viewModel.move(fromPosition, toPosition)
            }

            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                val colorViewHolder = viewHolder as? PlayerColorsAdapter.ColorViewHolder?
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    colorViewHolder?.onItemDragging()
                } else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    colorViewHolder?.onItemSwiping()
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
                val colorViewHolder = viewHolder as? PlayerColorsAdapter.ColorViewHolder?
                colorViewHolder?.onItemClear()
                super.clearView(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: ViewHolder, swipeDir: Int) {
                val color = adapter.getItem(viewHolder.adapterPosition) ?: return
                Snackbar.make(coordinator, getString(R.string.removed_suffix, color.description), Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo) {
                            viewModel.add(color)
                            firebaseAnalytics.logEvent("DataManipulation") {
                                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                                param("Action", "UndoDelete")
                                param("Color", color.description)
                            }
                        }
                        .setActionTextColor(ContextCompat.getColor(this@PlayerColorsActivity, R.color.light_blue))
                        .show()
                viewModel.remove(color)
                firebaseAnalytics.logEvent("DataManipulation") {
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                    param("Action", "Delete")
                    param("Color", color.description)
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView

                    // fade and slide item
                    val width = itemView.width.toFloat()
                    val alpha = 1.0f - abs(dX) / width
                    itemView.alpha = alpha
                    itemView.translationX = dX

                    // show background with delete icon
                    val verticalPadding = ((itemView.height - deleteIcon.height) / 2).toFloat()
                    val background: RectF
                    val iconSrc: Rect
                    val iconDst: RectF

                    if (dX > 0) {
                        background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), dX, itemView.bottom.toFloat())
                        iconSrc = Rect(
                                0,
                                0,
                                min((dX - itemView.left.toFloat() - horizontalPadding).toInt(), deleteIcon.width),
                                deleteIcon.height)
                        iconDst = RectF(
                                itemView.left.toFloat() + horizontalPadding,
                                itemView.top.toFloat() + verticalPadding,
                                min(itemView.left.toFloat() + horizontalPadding + deleteIcon.width.toFloat(), dX),
                                itemView.bottom.toFloat() - verticalPadding)
                    } else {
                        background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                        iconSrc = Rect(
                                max(deleteIcon.width + horizontalPadding.toInt() + dX.toInt(), 0),
                                0,
                                deleteIcon.width,
                                deleteIcon.height)
                        iconDst = RectF(
                                max(itemView.right.toFloat() + dX, itemView.right.toFloat() - horizontalPadding - deleteIcon.width.toFloat()),
                                itemView.top.toFloat() + verticalPadding,
                                itemView.right.toFloat() - horizontalPadding,
                                itemView.bottom.toFloat() - verticalPadding)
                    }
                    c.drawRect(background, swipePaint)
                    c.drawBitmap(deleteIcon, iconSrc, iconDst, swipePaint)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
    }

    private val adapter: PlayerColorsAdapter by lazy {
        PlayerColorsAdapter(itemTouchHelper)
    }

    override val optionsMenuId = R.menu.player_colors

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player_colors)

        buddyName = intent.getStringExtra(KEY_BUDDY_NAME)
        playerName = intent.getStringExtra(KEY_PLAYER_NAME)

        if (buddyName.isNullOrBlank() && playerName.isNullOrBlank()) {
            Timber.w("Can't launch - missing both buddy name and username.")
            finish()
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = if (buddyName.isNullOrBlank()) playerName else buddyName

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.adapter = adapter

        emptyButton.setOnClickListener {
            firebaseAnalytics.logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                param("Action", "Generate")
            }
            viewModel.generate()
        }

        fab.setOnClickListener {
            PlayerColorPickerDialogFragment.launch(this, usedColors)
        }

        viewModel.setUsername(buddyName)
        if (buddyName.isNullOrEmpty()) {
            viewModel.setPlayerName(playerName)
        } else {
            viewModel.setUsername(buddyName)
        }
        viewModel.colors.observe(this, Observer { playerColorEntities ->
            usedColors.clear()
            if (playerColorEntities != null) {
                usedColors.addAll(playerColorEntities.map { it.description })
            }

            adapter.colors = playerColorEntities ?: emptyList()
            progressView.hide()
            if (playerColorEntities == null || playerColorEntities.isEmpty()) {
                emptyView.fadeIn()
                recyclerView.fadeOut()
            } else {
                emptyView.fadeOut()
                recyclerView.fadeIn()
            }
        })

        if (savedInstanceState == null) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                param(FirebaseAnalytics.Param.ITEM_ID, buddyName.orEmpty())
                param(FirebaseAnalytics.Param.ITEM_NAME, playerName.orEmpty())
            }
        }
    }

    override fun onStop() {
        viewModel.save()
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> this.createThemedBuilder()
                    .setMessage(R.string.are_you_sure_clear_colors)
                    .setPositiveButton(R.string.clear) { _, _ ->
                        firebaseAnalytics.logEvent("DataManipulation") {
                            param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                            param("Action", "Clear")
                        }
                        viewModel.clear()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setCancelable(true)
                    .show()
            android.R.id.home -> {
                BuddyActivity.startUp(this, buddyName, playerName)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class PlayerColorsAdapter(private val itemTouchHelper: ItemTouchHelper?) : RecyclerView.Adapter<PlayerColorsAdapter.ColorViewHolder>(), AutoUpdatableAdapter {
        var colors: List<PlayerColorEntity> by Delegates.observable(emptyList()) { _, old, new ->
            autoNotify(old, new) { o, n ->
                o.description == n.description || o.sortOrder == n.sortOrder
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            return ColorViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_player_color, parent, false), itemTouchHelper)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val color = getItem(position) ?: return
            holder.bind(color)
        }

        override fun getItemCount(): Int {
            return colors.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        fun getItem(position: Int): PlayerColorEntity? {
            return colors.find { it.sortOrder == position + 1 }
        }

        class ColorViewHolder(itemView: View, private val itemTouchHelper: ItemTouchHelper?) : RecyclerView.ViewHolder(itemView) {
            fun bind(color: PlayerColorEntity) {
                itemView.titleView.text = color.description
                itemView.colorView.setColorViewValue(color.rgb)
                itemView.dragHandle.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper?.startDrag(this@ColorViewHolder)
                    } else if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    false
                }
            }

            fun onItemDragging() {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.light_blue_transparent))
            }

            fun onItemSwiping() {
                itemView.setBackgroundColor(Color.WHITE)
            }

            fun onItemClear() {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    companion object {
        private const val KEY_BUDDY_NAME = "BUDDY_NAME"
        private const val KEY_PLAYER_NAME = "PLAYER_NAME"

        fun start(context: Context, buddyName: String?, playerName: String?) {
            context.startActivity<PlayerColorsActivity>(
                    KEY_BUDDY_NAME to buddyName,
                    KEY_PLAYER_NAME to playerName
            )
        }
    }
}
