package com.boardgamegeek.ui.widget

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.dialog.CollectionRatingNumberPadDialogFragment
import java.text.DecimalFormat

class RatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ForegroundLinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var hideWhenZero: Boolean = false
    private var isEditMode: Boolean = false
    private var gameId = BggContract.INVALID_ID
    private var collectionId = BggContract.INVALID_ID
    private var internalId = BggContract.INVALID_ID.toLong()

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_rating, this)

        visibility = View.GONE
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.edit_row_height)
        orientation = VERTICAL
        setSelectableBackground(android.R.attr.selectableItemBackgroundBorderless)

        context.withStyledAttributes(attrs, R.styleable.RatingView, defStyleAttr, defStyleRes) {
            hideWhenZero = getBoolean(R.styleable.RatingView_hideWhenZero, false)
        }

        setOnClickListener {
            var output = RATING_EDIT_FORMAT.format(findViewById<TextView>(R.id.ratingView).tag as Double)
            if ("0" == output) output = ""
            val fragment = CollectionRatingNumberPadDialogFragment.newInstance(output)
            unwrapContext(context)?.showAndSurvive(fragment)
        }
    }

    private fun unwrapContext(context: Context): FragmentActivity? {
        while (context !is FragmentActivity && context is ContextWrapper) {
            return context.baseContext as? FragmentActivity
        }
        return context as? FragmentActivity
    }

    private var rating: Double
        get() {
            return findViewById<TextView>(R.id.ratingView).tag as Double? ?: return 0.0
        }
        set(value) {
            val constrainedRating = value.coerceIn(0.0, 10.0)
            findViewById<TextView>(R.id.ratingView).apply {
                text = constrainedRating.asPersonalRating(context)
                tag = constrainedRating
                setTextViewBackground(constrainedRating.toColor(BggColors.ratingColors))
            }
        }

    fun setContent(rating: Double, timestamp: Long, gameId: Int, collectionId: Int, internalId: Long) {
        this.rating = rating
        findViewById<TimestampView>(R.id.timestampView).timestamp = timestamp
        this.gameId = gameId
        this.collectionId = collectionId
        this.internalId = internalId
        setEditMode()
    }

    fun enableEditMode(enable: Boolean) {
        isEditMode = enable
        setEditMode()
    }

    private fun setEditMode() {
        isClickable = isEditMode
        isVisible = isEditMode || !hideWhenZero || rating != 0.0
    }

    companion object {
        private val RATING_EDIT_FORMAT = DecimalFormat("0.#")
    }
}
