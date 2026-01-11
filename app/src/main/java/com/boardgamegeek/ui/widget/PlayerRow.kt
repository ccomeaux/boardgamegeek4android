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
import com.boardgamegeek.databinding.RowPlayerBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.Player
import java.text.DecimalFormat

class PlayerRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = RowPlayerBinding.inflate(LayoutInflater.from(context), this)
    private val ratingFormat = DecimalFormat("0.0######")

    private val nameTypeface: Typeface
    private val usernameTypeface: Typeface
    private val scoreTypeface: Typeface
    private val nameColor: Int

    init {
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

        nameTypeface = binding.nameView.typeface
        usernameTypeface = binding.usernameView.typeface
        scoreTypeface = binding.scoreView.typeface
        nameColor = binding.nameView.textColors.defaultColor

        binding.ratingButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN)
        binding.scoreButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN)
    }

    fun getMoreButton(): View {
        return binding.moreButton
    }

    fun getDragHandle(): View {
        return binding.dragHandle
    }

    fun setOnScoreListener(l: OnClickListener) {
        binding.scoreButton.setSelectableBackgroundBorderless()
        binding.scoreButton.setOnClickListener(l)
    }

    fun setOnRatingListener(l: OnClickListener) {
        binding.ratingButton.setSelectableBackgroundBorderless()
        binding.ratingButton.setOnClickListener(l)
    }

    fun setOnColorListener(l: OnClickListener) {
        binding.colorView.setSelectableBackgroundBorderless()
        binding.colorView.setOnClickListener(l)
    }

    fun setNameListener(l: OnClickListener) {
        binding.nameContainer.setSelectableBackgroundBorderless()
        binding.nameContainer.setOnClickListener(l)
    }

    fun setOnMoreListener(l: OnClickListener) {
        binding.moreButton.visibility = View.VISIBLE
        binding.moreButton.setOnClickListener(l)
    }

    fun setAutoSort(value: Boolean) {
        binding.dragHandle.visibility = if (value) View.VISIBLE else View.INVISIBLE
    }

    fun setPlayer(player: Player?) {
        if (player == null) {
            binding.colorView.visibility = View.GONE
            binding.seatView.setTextOrHide("")
            binding.nameView.setTextOrHide(resources.getString(R.string.title_player))
            binding.usernameView.setTextOrHide("")
            binding.teamColorView.setTextOrHide("")
            binding.scoreView.setTextOrHide("")
            binding.ratingView.setTextOrHide("")
            binding.ratingButton.visibility = View.GONE
            binding.scoreButton.visibility = View.GONE
        } else {
            binding.seatView.setTextOrHide(player.startingPosition)
            if (player.name.isNullOrEmpty() && player.username.isNullOrEmpty()) {
                val name = if (player.seat == Player.SEAT_UNKNOWN)
                    resources.getString(R.string.title_player)
                else
                    resources.getString(R.string.generic_player, player.seat)
                setText(binding.nameView, name, nameTypeface, player.isNew, player.isWin, true)
                binding.usernameView.visibility = View.GONE
            } else if (player.name.isEmpty()) {
                setText(binding.nameView, player.username, nameTypeface, player.isNew, player.isWin)
                binding.usernameView.visibility = View.GONE
            } else {
                setText(binding.nameView, player.name, nameTypeface, player.isNew, player.isWin)
                setText(binding.usernameView, player.username, usernameTypeface, player.isNew, player.isWin)
            }

            setText(binding.scoreView, player.scoreDescription, scoreTypeface, false, player.isWin)
            binding.scoreButton.visibility = if (player.score.isNullOrEmpty()) View.GONE else View.VISIBLE

            if (player.rating == 0.0) {
                binding.ratingView.isVisible = false
            } else {
                binding.ratingView.setTextOrHide(player.rating.asScore(context, format = ratingFormat))
            }
            binding.ratingButton.visibility = if (player.rating > 0) View.VISIBLE else View.GONE

            binding.startingPositionView.setTextOrHide(player.startingPosition)

            binding.teamColorView.setTextOrHide(player.color)
            val color = player.color.asColorRgb()
            binding.colorView.visibility = View.VISIBLE
            binding.colorView.setColorViewValue(color)
            if (player.seat == Player.SEAT_UNKNOWN) {
                binding.seatView.visibility = View.GONE
            } else {
                binding.seatView.setTextColor(color.getTextColor())
                binding.startingPositionView.visibility = View.GONE
            }
            if (color != Color.TRANSPARENT) {
                binding.teamColorView.visibility = View.GONE
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
