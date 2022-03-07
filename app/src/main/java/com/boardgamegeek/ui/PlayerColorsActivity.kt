package com.boardgamegeek.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.boardgamegeek.R
import com.boardgamegeek.databinding.ActivityPlayerColorsBinding
import com.boardgamegeek.databinding.RowPlayerColorBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.ui.adapter.AutoUpdatableAdapter
import com.boardgamegeek.ui.dialog.PlayerColorPickerDialogFragment
import com.boardgamegeek.ui.viewmodel.PlayerColorsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

class PlayerColorsActivity : BaseActivity() {
    private lateinit var binding: ActivityPlayerColorsBinding
    private var buddyName: String? = null
    private var playerName: String? = null

    private val usedColors = ArrayList<String>()

    private val viewModel by viewModels<PlayerColorsViewModel>()

    private val itemTouchHelper by lazy {
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.material_margin_horizontal).toFloat()
            val deleteIcon = this@PlayerColorsActivity.getBitmap(R.drawable.ic_baseline_delete_24, Color.WHITE)
            val swipePaint by lazy {
                Paint().apply {
                    color = ContextCompat.getColor(this@PlayerColorsActivity, R.color.delete)
                }
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
                return viewModel.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            }

            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                (viewHolder as? PlayerColorsAdapter.ColorViewHolder)?.let {
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        it.onItemDragging()
                    } else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        it.onItemSwiping()
                    }
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
                (viewHolder as? PlayerColorsAdapter.ColorViewHolder)?.onItemClear()
                super.clearView(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: ViewHolder, swipeDir: Int) {
                val color = adapter.getItem(viewHolder.bindingAdapterPosition) ?: return
                val index = viewHolder.bindingAdapterPosition
                Snackbar.make(binding.coordinator, getString(R.string.removed_suffix, color), Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.add(color, index) }
                    .setActionTextColor(ContextCompat.getColor(this@PlayerColorsActivity, R.color.light_blue))
                    .show()
                viewModel.remove(color)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
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
                            deleteIcon.height
                        )
                        iconDst = RectF(
                            itemView.left.toFloat() + horizontalPadding,
                            itemView.top.toFloat() + verticalPadding,
                            min(itemView.left.toFloat() + horizontalPadding + deleteIcon.width.toFloat(), dX),
                            itemView.bottom.toFloat() - verticalPadding
                        )
                    } else {
                        background = RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                        iconSrc = Rect(
                            max(deleteIcon.width + horizontalPadding.toInt() + dX.toInt(), 0),
                            0,
                            deleteIcon.width,
                            deleteIcon.height
                        )
                        iconDst = RectF(
                            max(itemView.right.toFloat() + dX, itemView.right.toFloat() - horizontalPadding - deleteIcon.width.toFloat()),
                            itemView.top.toFloat() + verticalPadding,
                            itemView.right.toFloat() - horizontalPadding,
                            itemView.bottom.toFloat() - verticalPadding
                        )
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

        binding = ActivityPlayerColorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buddyName = intent.getStringExtra(KEY_BUDDY_NAME)
        playerName = intent.getStringExtra(KEY_PLAYER_NAME)

        if (buddyName.isNullOrBlank() && playerName.isNullOrBlank()) {
            Timber.w("Can't launch - missing both buddy name and username.")
            finish()
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = if (buddyName.isNullOrBlank()) playerName else buddyName

        binding.recyclerView.setHasFixedSize(true)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.adapter = adapter

        binding.emptyButton.setOnClickListener {
            viewModel.generate()
        }

        binding.fab.colorize(R.color.primary)
        binding.fab.setOnClickListener {
            PlayerColorPickerDialogFragment.launch(this, usedColors)
        }

        viewModel.setUsername(buddyName)
        if (buddyName.isNullOrEmpty()) {
            viewModel.setPlayerName(playerName)
        } else {
            viewModel.setUsername(buddyName)
        }
        viewModel.colors.observe(this) { colors ->
            usedColors.clear()
            colors?.let { usedColors.addAll(it) }
            adapter.colors = colors.orEmpty()
            binding.progressView.hide()
            binding.emptyView.isVisible = adapter.colors.isEmpty()
            binding.recyclerView.isVisible = adapter.colors.isNotEmpty()
        }

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

    class PlayerColorsAdapter(private val itemTouchHelper: ItemTouchHelper?) : RecyclerView.Adapter<PlayerColorsAdapter.ColorViewHolder>(),
        AutoUpdatableAdapter {
        var colors: List<String> by Delegates.observable(emptyList()) { _, old, new ->
            autoNotify(old, new) { o, n ->
                o == n
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            return ColorViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_player_color, parent, false), itemTouchHelper)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val color = getItem(position) ?: return
            holder.bind(color)
        }

        override fun getItemCount() = colors.size

        override fun getItemId(position: Int) = getItem(position).hashCode().toLong()

        fun getItem(position: Int): String? {
            return colors.getOrNull(position)
        }

        class ColorViewHolder(itemView: View, private val itemTouchHelper: ItemTouchHelper?) : RecyclerView.ViewHolder(itemView) {
            val binding = RowPlayerColorBinding.bind(itemView)

            @SuppressLint("ClickableViewAccessibility")
            fun bind(color: String) {
                binding.titleView.text = color
                binding.colorView.setColorViewValue(color.asColorRgb())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    binding.dragHandle.imageTintList = ColorStateList.valueOf(color.asColorRgb().getTextColor())
                }
                binding.dragHandle.setOnTouchListener { v, event ->
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
                KEY_PLAYER_NAME to playerName,
            )
        }
    }
}
