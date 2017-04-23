package com.boardgamegeek.ui.model;

public class HIndexEntry {
	private final int playCount;
	private final int rank;
	private final String name;

	private HIndexEntry(Builder builder) {
		playCount = builder.playCount;
		rank = builder.rank;
		name = builder.name;
	}

	public int getPlayCount() {
		return playCount;
	}

	public String getDescription() {
		return String.format("%s (#%s)", name, rank);
	}

	public static final class Builder {
		private int playCount;
		private int rank;
		private String name;

		public Builder() {
		}

		public Builder playCount(int val) {
			playCount = val;
			return this;
		}

		public Builder rank(int val) {
			rank = val;
			return this;
		}

		public Builder name(String val) {
			name = val;
			return this;
		}

		public HIndexEntry build() {
			return new HIndexEntry(this);
		}
	}
}
