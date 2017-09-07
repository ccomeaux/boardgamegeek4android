package com.boardgamegeek.util;


public class PlayerCountRecommendation {
	public static final int BEST = 2;
	public static final int RECOMMENDED = 1;
	public static final int UNKNOWN = 0;
	public static final int NOT_RECOMMENDED = -1;

	private final int bestVoteCount;
	private final int recommendedVoteCount;
	private final int notRecommendVoteCount;

	private PlayerCountRecommendation(Builder builder) {
		bestVoteCount = builder.bestVoteCount;
		recommendedVoteCount = builder.recommendedVoteCount;
		notRecommendVoteCount = builder.notRecommendVoteCount;
	}

	private int halfTotalVoteCount() {
		return (int) ((bestVoteCount + recommendedVoteCount + notRecommendVoteCount) * 0.5);
	}

	public int calculate() {
		if (halfTotalVoteCount() == 0) return UNKNOWN;
		if (bestVoteCount > halfTotalVoteCount()) {
			return BEST;
		} else if ((bestVoteCount + recommendedVoteCount) > halfTotalVoteCount()) {
			return RECOMMENDED;
		} else if (notRecommendVoteCount > halfTotalVoteCount()) {
			return NOT_RECOMMENDED;
		}
		return UNKNOWN;
	}

	public static final class Builder {
		private int bestVoteCount;
		private int recommendedVoteCount;
		private int notRecommendVoteCount;

		public Builder() {
		}

		public Builder bestVoteCount(int val) {
			bestVoteCount = val;
			return this;
		}

		public Builder recommendedVoteCount(int val) {
			recommendedVoteCount = val;
			return this;
		}

		public Builder notRecommendVoteCount(int val) {
			notRecommendVoteCount = val;
			return this;
		}

		public PlayerCountRecommendation build() {
			return new PlayerCountRecommendation(this);
		}
	}
}
