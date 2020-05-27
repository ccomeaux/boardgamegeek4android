package com.boardgamegeek.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff.Mode
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Player
import kotlinx.android.synthetic.main.row_player.view.*
import java.text.DecimalFormat

class PlayerRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {
    private val ratingFormat = DecimalFormat("0.0######")

    private val nameTypeface: Typeface
    private val usernameTypeface: Typeface
    private val scoreTypeface: Typeface
    private val nameColor: Int

    init {
        LayoutInflater.from(context).inflate(R.layout.row_player, this)

        isBaselineAligned = false
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.player_row_height)
        orientation = HORIZONTAL
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.material_margin_horizontal)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.padding_standard)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        setSelectableBackground()

        nameTypeface = nameView.typeface
        usernameTypeface = usernameView.typeface
        scoreTypeface = scoreView.typeface
        nameColor = nameView.textColors.defaultColor

        ratingButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN)
        scoreButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN)
    }

    fun getMoreButton(): View {
        return moreButton
    }

    fun getDragHandle(): View {
        return dragHandle
    }

    fun setOnScoreListener(l: OnClickListener) {
        scoreButton.setSelectableBackgroundBorderless()
        scoreButton.setOnClickListener(l)
    }

    fun setOnRatingListener(l: OnClickListener) {
        ratingButton.setSelectableBackgroundBorderless()
        ratingButton.setOnClickListener(l)
    }

    fun setOnColorListener(l: OnClickListener) {
        colorView.setSelectableBackgroundBorderless()
        colorView.setOnClickListener(l)
    }

    fun setNameListener(l: OnClickListener) {
        nameContainer.setSelectableBackgroundBorderless()
        nameContainer.setOnClickListener(l)
    }

    fun setOnMoreListener(l: OnClickListener) {
        moreButton.visibility = View.VISIBLE
        moreButton.setOnClickListener(l)
    }

    fun setAutoSort(value: Boolean) {
        dragHandle.isVisible = value
    }

    fun setPlayer(player: Player?) {
        if (player == null) {
            colorView.visibility = View.GONE
            seatView.setTextOrHide("")
            nameView.setTextOrHide(resources.getString(R.string.title_player))
            usernameView.setTextOrHide("")
            teamColorView.setTextOrHide("")
            scoreView.setTextOrHide("")
            ratingView.setTextOrHide("")
            ratingButton.visibility = View.GONE
            scoreButton.visibility = View.GONE
        } else {
            seatView.setTextOrHide(player.startingPosition)
            if (player.name.isNullOrEmpty() && player.username.isNullOrEmpty()) {
                val name = if (player.seat == Player.SEAT_UNKNOWN)
                    resources.getString(R.string.title_player)
                else
                    resources.getString(R.string.generic_player, player.seat)
                setText(nameView, name, nameTypeface, player.isNew, player.isWin, true)
                usernameView.visibility = View.GONE
            } else if (player.name.isEmpty()) {
                setText(nameView, player.username, nameTypeface, player.isNew, player.isWin)
                usernameView.visibility = View.GONE
            } else {
                setText(nameView, player.name, nameTypeface, player.isNew, player.isWin)
                setText(usernameView, player.username, usernameTypeface, player.isNew, player.isWin)
            }

            setText(scoreView, player.scoreDescription, scoreTypeface, false, player.isWin)
            scoreButton.visibility = if (player.score.isNullOrEmpty()) View.GONE else View.VISIBLE

            if (player.rating == 0.0) {
                ratingView.isVisible = false
            } else {
                ratingView.setTextOrHide(player.rating.asScore(context, format = ratingFormat))
            }
            ratingButton.visibility = if (player.rating > 0) View.VISIBLE else View.GONE

            startingPositionView.setTextOrHide(player.startingPosition)

            teamColorView.setTextOrHide(player.color)
            val color = player.color.asColorRgb()
            colorView.visibility = View.VISIBLE
            colorView.setColorViewValue(color)
            if (player.seat == Player.SEAT_UNKNOWN) {
                seatView.visibility = View.GONE
            } else {
                seatView.setTextColor(color.getTextColor())
                startingPositionView.visibility = View.GONE
            }
            if (color != Color.TRANSPARENT) {
                teamColorView.visibility = View.GONE
            }
        }
    }

    private fun setText(textView: TextView, text: String, tf: Typeface, italic: Boolean, bold: Boolean, isSecondary: Boolean = false) {
        textView.setTextOrHide(text)
        if (text.isNotEmpty()) {
            when {
                italic && bold -> textView.setTypeface(tf, Typeface.BOLD_ITALIC)
                italic -> textView.setTypeface(tf, Typeface.ITALIC)
                bold -> textView.setTypeface(tf, Typeface.BOLD)
                else -> textView.setTypeface(tf, Typeface.NORMAL)
            }
            when {
                isSecondary -> textView.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                else -> textView.setTextColor(nameColor)
            }
        }
    }
}
