package com.boardgamegeek.ui.widget

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.ColorInt
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.setOrClearColorFilter
import com.boardgamegeek.ui.GameActivity
import com.boardgamegeek.ui.GameDetailActivity
import kotlinx.android.synthetic.main.widget_game_detail_row.view.*

class GameDetailRow @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    private val oneMore: String by lazy {
        context.getString(R.string.one_more)
    }
    private val someMore: String by lazy {
        context.getString(R.string.some_more)
    }

    private var label: String? = null
    private var icon: Drawable? = null
    private var queryToken: Int = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_game_detail_row, this, true)

        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        val sa = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        try {
            setBackgroundResource(sa.getResourceId(0, 0))
        } finally {
            sa.recycle()
        }

        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.game_detail_row_height)
        orientation = LinearLayout.HORIZONTAL

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GameDetailRow)
            try {
                label = a.getString(R.styleable.GameDetailRow_label)
                icon = a.getDrawable(R.styleable.GameDetailRow_icon_res)
                queryToken = a.getInt(R.styleable.GameDetailRow_query_token, BggContract.INVALID_ID)
            } finally {
                a.recycle()
            }
        }
        iconView.visibility = if (icon == null) View.GONE else View.VISIBLE
        iconView.setImageDrawable(icon)
    }

    fun clear() {
        dataView.text = ""
        setOnClickListener { }
    }

    fun bindData(gameId: Int, gameName: String, list: List<Pair<Int, String>>?) {
        if (list == null || list.isEmpty()) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            clear()
            dataView.text = setDescription(list.map { it.second })
            setOnClickListener {
                if (list.size == 1) {
                    val id = list[0].first
                    when (queryToken) {
                        resources.getInteger(R.integer.query_token_designers) -> context.startActivity(Intent(Intent.ACTION_VIEW, Designers.buildDesignerUri(id)))
                        resources.getInteger(R.integer.query_token_artists) -> context.startActivity(Intent(Intent.ACTION_VIEW, Artists.buildArtistUri(id)))
                        resources.getInteger(R.integer.query_token_publishers) -> context.startActivity(Intent(Intent.ACTION_VIEW, Publishers.buildPublisherUri(id)))
                        resources.getInteger(R.integer.query_token_expansions), resources.getInteger(R.integer.query_token_base_games) ->
                            GameActivity.start(context, id, list[0].second)
                        else -> GameDetailActivity.start(context, label, gameId, gameName, queryToken)
                    }
                } else {
                    GameDetailActivity.start(context, label, gameId, gameName, queryToken)
                }
            }
        }
    }

    fun colorize(@ColorInt color: Int) {
        iconView.setOrClearColorFilter(color)
    }

    private fun setDescription(names: List<String>): CharSequence {
        return when (names.size) {
            1 -> names[0]
            2 -> "${names[0]} & ${names[1]}"
            else -> {
                val paint = TextPaint()
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
