package com.boardgamegeek.ui.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.boardgamegeek.R

class PlayerNumberRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var totalVoteCount: Int = 0
    private var bestVoteCount: Int = 0
    private var recommendedVoteCount: Int = 0
    private var notRecommendedVoteCount: Int = 0
    private val actualVotes
        get() = bestVoteCount + recommendedVoteCount + notRecommendedVoteCount

    init {
        LayoutInflater.from(context).inflate(R.layout.row_poll_players, this)
    }

    val votes: IntArray
        get() {
            val votes = IntArray(3)
            votes[0] = bestVoteCount
            votes[1] = recommendedVoteCount
            votes[2] = notRecommendedVoteCount
            return votes
        }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()?.let {
            SavedState(it).also { savedState ->
                savedState.totalVoteCount = totalVoteCount
                savedState.bestVoteCount = bestVoteCount
                savedState.recommendedVoteCount = recommendedVoteCount
                savedState.notRecommendedVoteCount = notRecommendedVoteCount
            }
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        (state as? SavedState)?.let {
            super.onRestoreInstanceState(it.superState)
            totalVoteCount = it.totalVoteCount
            bestVoteCount = it.bestVoteCount
            recommendedVoteCount = it.recommendedVoteCount
            notRecommendedVoteCount = it.notRecommendedVoteCount
        } ?: super.onRestoreInstanceState(state)
    }

    fun setText(text: CharSequence) {
        findViewById<TextView>(R.id.labelView).text = text
    }

    fun setVotes(bestVoteCount: Int, recommendedVoteCount: Int, notRecommendedVoteCount: Int, totalVoteCount: Int) {
        this.bestVoteCount = bestVoteCount
        this.recommendedVoteCount = recommendedVoteCount
        this.notRecommendedVoteCount = notRecommendedVoteCount
        this.totalVoteCount = totalVoteCount
        if (actualVotes == 0) {
            adjustSegment(findViewById(R.id.missingVotesSegment), 1)
        } else {
            adjustSegment(findViewById(R.id.bestSegment), bestVoteCount)
            adjustSegment(findViewById(R.id.recommendedSegment), recommendedVoteCount)
            adjustSegment(findViewById(R.id.missingVotesSegment), totalVoteCount - bestVoteCount - recommendedVoteCount - notRecommendedVoteCount)
            adjustSegment(findViewById(R.id.notRecommendedSegment), notRecommendedVoteCount)
        }
        findViewById<View>(R.id.missingVotesSegment).isVisible = actualVotes == 0
        findViewById<TextView>(R.id.votesView).text = actualVotes.toString()
    }

    fun showNoVotes(show: Boolean) {
        findViewById<View>(R.id.missingVotesSegment).isVisible = show || actualVotes == 0
        findViewById<TextView>(R.id.votesView).isVisible = !show
    }

    fun setHighlight() {
        findViewById<TextView>(R.id.labelView).setBackgroundResource(R.drawable.bg_highlight)
    }

    fun clearHighlight() {
        @Suppress("DEPRECATION")
        findViewById<TextView>(R.id.labelView).setBackgroundDrawable(null)
    }

    private fun adjustSegment(segment: View, votes: Int) {
        segment.layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, votes.toFloat())
    }

    internal class SavedState : BaseSavedState {
        internal var totalVoteCount: Int = 0
        internal var bestVoteCount: Int = 0
        internal var recommendedVoteCount: Int = 0
        internal var notRecommendedVoteCount: Int = 0

        constructor(superState: Parcelable) : super(superState)

        constructor(source: Parcel) : super(source) {
            totalVoteCount = source.readInt()
            bestVoteCount = source.readInt()
            recommendedVoteCount = source.readInt()
            notRecommendedVoteCount = source.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(totalVoteCount)
            out.writeInt(bestVoteCount)
            out.writeInt(recommendedVoteCount)
            out.writeInt(notRecommendedVoteCount)
        }

        companion object {
            @JvmField
            @Suppress("unused")
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
