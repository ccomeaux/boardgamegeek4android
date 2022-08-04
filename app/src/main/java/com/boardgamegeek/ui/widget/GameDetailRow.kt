package com.boardgamegeek.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.extensions.setOrClearColorFilter
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.GameDetailActivity
import com.boardgamegeek.ui.PersonActivity
import com.boardgamegeek.ui.viewmodel.GameViewModel.ProducerType

class GameDetailRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val oneMore: String by lazy {
        context.getString(R.string.one_more)
    }
    private val someMore: String by lazy {
        context.getString(R.string.some_more)
    }

    private var label: String = ""
    private var icon: Drawable? = null
    private var queryToken: Int = 0
    private var type: ProducerType = ProducerType.UNKNOWN

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_game_detail_row, this)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        context.withStyledAttributes(0, intArrayOf(android.R.attr.selectableItemBackground)) {
            setBackgroundResource(getResourceId(0, 0))
        }

        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.game_row_height)
        orientation = HORIZONTAL

        context.withStyledAttributes(attrs, R.styleable.GameDetailRow, defStyleAttr, defStyleRes) {
            label = getString(R.styleable.GameDetailRow_label).orEmpty()
            icon = getDrawable(R.styleable.GameDetailRow_icon_res)
            queryToken = getInt(R.styleable.GameDetailRow_query_token, BggContract.INVALID_ID)
        }
        findViewById<ImageView>(R.id.iconView).apply {
            isVisible = (icon != null)
            setImageDrawable(icon)
        }

        type = ProducerType.fromInt(queryToken)
    }

    fun clear() {
        findViewById<TextView>(R.id.dataView).text = ""
        setOnClickListener { }
    }

    fun bindData(gameId: Int, gameName: String, list: List<GameDetailEntity>?) {
        clear()
        if (list == null || list.isEmpty()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            if (list.size == 1) {
                list[0].apply {
                    findViewById<TextView>(R.id.dataView).apply {
                        maxLines = 1
                        text = name
                    }
                    findViewById<TextView>(R.id.descriptionView).setTextOrHide(description)
                    setOnClickListener {
                        when (type) {
                            ProducerType.ARTIST -> PersonActivity.startForArtist(context, id, name)
                            ProducerType.DESIGNER -> PersonActivity.startForDesigner(context, id, name)
                            ProducerType.PUBLISHER -> PersonActivity.startForPublisher(context, id, name)
                            ProducerType.EXPANSIONS,
                            ProducerType.BASE_GAMES -> {
                                GameActivity.start(context, id, name)
                            }
                            else -> GameDetailActivity.start(context, label, gameId, gameName, type)
                        }
                    }
                }
            } else {
                findViewById<TextView>(R.id.dataView).apply {
                    maxLines = 2
                    text = generateName(list.map { it.name })
                }
                findViewById<TextView>(R.id.descriptionView).isVisible = false
                setOnClickListener {
                    GameDetailActivity.start(context, label, gameId, gameName, type)
                }
            }
        }
    }

    fun colorize(@ColorInt color: Int) {
        findViewById<ImageView>(R.id.iconView).setOrClearColorFilter(color)
    }

    private fun generateName(names: List<String>): CharSequence {
        return when (names.size) {
            1 -> names[0]
            2 -> "${names[0]} & ${names[1]}"
            else -> {
                val paint = TextPaint()
                val dataView = findViewById<TextView>(R.id.dataView)
                paint.textSize = dataView.textSize
                val avail = (dataView.width * 2).toFloat()
                val summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    TextUtils.listEllipsize(context, names, ", ", paint, avail, R.plurals.more)
                } else {
                    val text = names.joinToString()
                    @Suppress("DEPRECATION")
                    TextUtils.commaEllipsize(text, paint, avail, oneMore, someMore)
                }
                if (summary.isNullOrBlank()) {
                    String.format(someMore, names.size)
                } else {
                    summary
                }
            }
        }
    }
}
