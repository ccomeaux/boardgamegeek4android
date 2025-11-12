package com.boardgamegeek.ui.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.boardgamegeek.R
import com.boardgamegeek.databinding.RowPollPlayersBinding

class PlayerNumberRow @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = RowPollPlayersBinding.inflate(LayoutInflater.from(context))
    private var totalVoteCount: Int = 0
    private var bestVoteCount: Int = 0
    private var recommendedVoteCount: Int = 0
    private var notRecommendedVoteCount: Int = 0

    val votes: IntArray
        get() {
            val votes = IntArray(3)
            votes[0] = bestVoteCount
            votes[1] = recommendedVoteCount
            votes[2] = notRecommendedVoteCount
            return votes
        }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return if (superState != null) {
            val savedState = SavedState(superState)
            savedState.totalVoteCount = totalVoteCount
            savedState.bestVoteCount = bestVoteCount
            savedState.recommendedVoteCount = recommendedVoteCount
            savedState.notRecommendedVoteCount = notRecommendedVoteCount
            savedState
        } else {
            superState
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        totalVoteCount = ss.totalVoteCount
        bestVoteCount = ss.bestVoteCount
        recommendedVoteCount = ss.recommendedVoteCount
        notRecommendedVoteCount = ss.notRecommendedVoteCount
    }

    fun setText(text: CharSequence) {
        binding.labelView.text = text
    }

    fun setVotes(bestVoteCount: Int, recommendedVoteCount: Int, notRecommendedVoteCount: Int, totalVoteCount: Int) {
        this.bestVoteCount = bestVoteCount
        this.recommendedVoteCount = recommendedVoteCount
        this.notRecommendedVoteCount = notRecommendedVoteCount
        this.totalVoteCount = totalVoteCount
        adjustSegment(binding.bestSegment, bestVoteCount)
        adjustSegment(binding.recommendedSegment, recommendedVoteCount)
        adjustSegment(binding.missingVotesSegment, totalVoteCount - bestVoteCount - recommendedVoteCount - notRecommendedVoteCount)
        adjustSegment(binding.notRecommendedSegment, notRecommendedVoteCount)
        binding.votesView.text = (bestVoteCount + recommendedVoteCount + notRecommendedVoteCount).toString()
    }

    fun showNoVotes(show: Boolean) {
        binding.missingVotesSegment.visibility = if (show) View.VISIBLE else View.GONE
        binding.votesView.visibility = if (show) View.GONE else View.VISIBLE
    }

    fun setHighlight() {
        binding.labelView.setBackgroundResource(R.drawable.highlight)
    }

    fun clearHighlight() {
        @Suppress("DEPRECATION")
        binding.labelView.setBackgroundDrawable(null)
    }

    private fun adjustSegment(segment: View, votes: Int) {
        segment.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, votes.toFloat())
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
