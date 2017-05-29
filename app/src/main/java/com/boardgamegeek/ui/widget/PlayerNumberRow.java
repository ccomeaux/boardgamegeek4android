package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

public class PlayerNumberRow extends LinearLayout {
	@BindView(R.id.label) TextView labelView;
	@BindView(R.id.best) View bestSegment;
	@BindView(R.id.recommended) View recommendedSegment;
	@BindView(R.id.no_votes) View missingVotesSegment;
	@BindView(R.id.not_recommended) View notRecommendedSegment;
	@BindView(R.id.votes) TextView votesView;

	@State int totalVoteCount;
	@State int bestVoteCount;
	@State int recommendedVoteCount;
	@State int notRecommendedVoteCount;

	public PlayerNumberRow(Context context) {
		super(context);
		init(context);
	}

	public PlayerNumberRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		LayoutInflater.from(context).inflate(R.layout.row_poll_players, this);
		ButterKnife.bind(this);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		return Icepick.saveInstanceState(this, super.onSaveInstanceState());
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(Icepick.restoreInstanceState(this, state));
	}

	public void setText(CharSequence text) {
		labelView.setText(text);
	}

	public void setTotal(int voteCount) {
		totalVoteCount = voteCount;
		adjustSegments();
	}

	public void setBest(int voteCount) {
		bestVoteCount = voteCount;
		adjustSegments();
	}

	public void setRecommended(int voteCount) {
		recommendedVoteCount = voteCount;
		adjustSegments();
	}

	public void setNotRecommended(int voteCount) {
		notRecommendedVoteCount = voteCount;
		adjustSegments();
	}

	public void showNoVotes(boolean show) {
		missingVotesSegment.setVisibility(show ? View.VISIBLE : View.GONE);
		votesView.setVisibility(show ? View.GONE : View.VISIBLE);
	}

	public int[] getVotes() {
		int[] votes = new int[3];
		votes[0] = bestVoteCount;
		votes[1] = recommendedVoteCount;
		votes[2] = notRecommendedVoteCount;
		return votes;
	}

	public void setHighlight() {
		labelView.setBackgroundResource(R.drawable.highlight);
	}

	@SuppressWarnings("deprecation")
	public void clearHighlight() {
		labelView.setBackgroundDrawable(null);
	}

	private void adjustSegments() {
		adjustSegment(bestSegment, bestVoteCount);
		adjustSegment(recommendedSegment, recommendedVoteCount);
		adjustSegment(missingVotesSegment, totalVoteCount - bestVoteCount - recommendedVoteCount - notRecommendedVoteCount);
		adjustSegment(notRecommendedSegment, notRecommendedVoteCount);
		votesView.setText(String.valueOf(bestVoteCount + recommendedVoteCount + notRecommendedVoteCount));
	}

	private void adjustSegment(View segment, int votes) {
		segment.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, votes));
	}
}
