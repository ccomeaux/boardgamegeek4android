package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.dialog.CollectionRatingNumberPadDialogFragment
import kotlinx.android.synthetic.main.widget_rating.view.*
import java.text.DecimalFormat

class RatingView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : ForegroundLinearLayout(context, attrs) {
    private val hideWhenZero: Boolean
    private var isEditMode: Boolean = false
    private var gameId = BggContract.INVALID_ID
    private var collectionId = BggContract.INVALID_ID
    private var internalId = BggContract.INVALID_ID.toLong()

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.widget_rating, this, true)

        visibility = View.GONE
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.edit_row_height)
        orientation = VERTICAL
        setSelectableBackgroundBorderless()

        val a = getContext().obtainStyledAttributes(attrs, R.styleable.RatingView, defStyleAttr, 0)
        try {
            hideWhenZero = a.getBoolean(R.styleable.RatingView_hideWhenZero, false)
        } finally {
            a.recycle()
        }

        setOnClickListener {
            var output = RATING_EDIT_FORMAT.format(ratingView.tag as Double)
            if ("0" == output) output = ""
            val fragment = CollectionRatingNumberPadDialogFragment.newInstance(output)
            (context as? FragmentActivity)?.showAndSurvive(fragment)
        }
    }

    private var rating: Double
        get() {
            return ratingView.tag as Double? ?: return 0.0
        }
        set(value) {
            val constrainedRating = value.coerceIn(0.0, 10.0)
            ratingView.text = constrainedRating.asPersonalRating(context)
            ratingView.tag = constrainedRating
            ratingView.setTextViewBackground(constrainedRating.toColor(ratingColors))
        }

    fun setContent(rating: Double, timestamp: Long, gameId: Int, collectionId: Int, internalId: Long) {
        this.rating = rating
        timestampView.timestamp = timestamp
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
