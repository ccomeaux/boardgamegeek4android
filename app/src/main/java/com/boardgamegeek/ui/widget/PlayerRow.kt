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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.entities.PlayPlayerEntity
import com.boardgamegeek.extensions.*
import java.text.DecimalFormat

class PlayerRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
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

        findViewById<TextView>(R.id.nameView).apply {
            nameTypeface = typeface
            nameColor = textColors.defaultColor
        }
        usernameTypeface = findViewById<TextView>(R.id.usernameView).typeface
        scoreTypeface = findViewById<TextView>(R.id.scoreView).typeface

        findViewById<ImageView>(R.id.ratingButton).setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN)
        findViewById<ImageView>(R.id.scoreButton).setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN)
    }

    fun getMoreButton(): View {
        return findViewById<ImageView>(R.id.moreButton) as View
    }

    fun getDragHandle(): View {
        return findViewById<ImageView>(R.id.dragHandle) as View
    }

    fun setOnScoreListener(l: OnClickListener) {
        findViewById<ImageView>(R.id.scoreButton).apply {
            setSelectableBackgroundBorderless()
            setOnClickListener(l)
        }
    }

    fun setOnRatingListener(l: OnClickListener) {
        findViewById<ImageView>(R.id.ratingButton).apply {
            setSelectableBackgroundBorderless()
            setOnClickListener(l)
        }
    }

    fun setOnColorListener(l: OnClickListener) {
        findViewById<ImageView>(R.id.colorView).apply {
            setSelectableBackgroundBorderless()
            setOnClickListener(l)
        }
    }

    fun setNameListener(l: OnClickListener) {
        findViewById<LinearLayout>(R.id.nameContainer).apply {
            setSelectableBackgroundBorderless()
            setOnClickListener(l)
        }
    }

    fun setOnMoreListener(l: OnClickListener) {
        findViewById<ImageView>(R.id.moreButton).apply {
            isVisible = true
            setOnClickListener(l)
        }
    }

    fun setAutoSort(value: Boolean) {
        findViewById<ImageView>(R.id.dragHandle).isVisible = value
    }

    fun setPlayer(player: PlayPlayerEntity?) {
        val colorView = findViewById<ImageView>(R.id.colorView)
        val seatView = findViewById<TextView>(R.id.seatView)
        val nameView = findViewById<TextView>(R.id.nameView)
        val usernameView = findViewById<TextView>(R.id.usernameView)
        val teamColorView = findViewById<TextView>(R.id.teamColorView)
        val scoreView = findViewById<TextView>(R.id.scoreView)
        val startingPositionView = findViewById<TextView>(R.id.startingPositionView)

        val ratingView = findViewById<TextView>(R.id.ratingView)
        val ratingButton = findViewById<ImageView>(R.id.ratingButton)
        val scoreButton = findViewById<ImageView>(R.id.scoreButton)

        if (player == null) {
            colorView.isVisible = false
            seatView.setTextOrHide("")
            nameView.setTextOrHide(resources.getString(R.string.title_player))
            usernameView.setTextOrHide("")
            teamColorView.setTextOrHide("")
            scoreView.setTextOrHide("")
            ratingView.setTextOrHide("")
            ratingButton.isVisible = false
            scoreButton.isVisible = false
        } else {
            seatView.setTextOrHide(player.startingPosition)
            if (player.name.isEmpty() && player.username.isEmpty()) {
                val name = if (player.seat == PlayPlayerEntity.SEAT_UNKNOWN)
                    resources.getString(R.string.title_player)
                else
                    resources.getString(R.string.generic_player, player.seat)
                setText(nameView, name, nameTypeface, player.isNew, player.isWin, true)
                usernameView.isVisible = false
            } else if (player.name.isEmpty()) {
                setText(nameView, player.username, nameTypeface, player.isNew, player.isWin)
                usernameView.isVisible = false
            } else {
                setText(nameView, player.name, nameTypeface, player.isNew, player.isWin)
                setText(usernameView, player.username, usernameTypeface, player.isNew, player.isWin)
            }

            val scoreDescription = player.score.toDoubleOrNull()?.asScore() ?: player.score
            setText(scoreView, scoreDescription, scoreTypeface, false, player.isWin)
            scoreButton.isVisible = player.score.isNotBlank()

            if (player.rating == 0.0) {
                ratingView.isVisible = false
            } else {
                ratingView.setTextOrHide(player.rating.asScore(context, format = ratingFormat))
            }
            ratingButton.isVisible = player.rating > 0

            startingPositionView.setTextOrHide(player.startingPosition)

            teamColorView.setTextOrHide(player.color)
            val color = player.color.asColorRgb()
            colorView.isVisible = true
            colorView.setColorViewValue(color)
            if (player.seat == PlayPlayerEntity.SEAT_UNKNOWN) {
                seatView.isVisible = false
            } else {
                seatView.setTextColor(color.getTextColor())
                startingPositionView.isVisible = false
            }
            if (color != Color.TRANSPARENT) {
                teamColorView.isVisible = false
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
