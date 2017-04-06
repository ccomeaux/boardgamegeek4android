package com.boardgamegeek.ui.model;

import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class PlayStats {
	public static final int INVALID_FRIENDLESS = Integer.MIN_VALUE;

	private int numberOfPlays = 0;
	private int numberOfPlayedGames = 0;
	private int numberOfQuarters = 0;
	private int numberOfDimes = 0;
	private int numberOfNickels = 0;
	private int hIndex = 0;
	private int friendless = 0;
	private List<Pair<String, Integer>> hIndexGames = new ArrayList<>();
	private int top100count = 0;

	private PlayStats(Builder builder) {
		numberOfPlays = builder.numberOfPlays;
		numberOfPlayedGames = builder.numberOfPlayedGames;
		numberOfQuarters = builder.numberOfQuarters;
		numberOfDimes = builder.numberOfDimes;
		numberOfNickels = builder.numberOfNickels;
		hIndex = builder.hIndex;
		hIndexGames = builder.hIndexGames;
		friendless = builder.friendless;
		top100count = builder.top100count;
	}

	public int getNumberOfPlays() {
		return numberOfPlays;
	}

	public int getNumberOfGames() {
		return numberOfPlayedGames;
	}

	public int getNumberOfQuarters() {
		return numberOfQuarters;
	}

	public int getNumberOfDimes() {
		return numberOfDimes;
	}

	public int getNumberOfNickels() {
		return numberOfNickels;
	}

	public int getHIndex() {
		return hIndex;
	}

	public int getFriendless() {
		return friendless;
	}

	public int getTop100Count() {
		return top100count;
	}

	public List<Pair<String, Integer>> getHIndexGames() {
		return hIndexGames;
	}

	public static final class Builder {
		private int numberOfPlays;
		private int numberOfPlayedGames;
		private int numberOfQuarters;
		private int numberOfDimes;
		private int numberOfNickels;
		private int hIndex;
		private List<Pair<String, Integer>> hIndexGames = new ArrayList<>();
		private int friendless;
		private int top100count;

		public Builder() {
		}

		public Builder numberOfPlays(int val) {
			numberOfPlays = val;
			return this;
		}

		public Builder numberOfPlayedGames(int val) {
			numberOfPlayedGames = val;
			return this;
		}

		public Builder numberOfQuarters(int val) {
			numberOfQuarters = val;
			return this;
		}

		public Builder numberOfDimes(int val) {
			numberOfDimes = val;
			return this;
		}

		public Builder numberOfNickels(int val) {
			numberOfNickels = val;
			return this;
		}

		public Builder hIndex(int val) {
			hIndex = val;
			return this;
		}

		public Builder hIndexGames(List<Pair<String, Integer>> val) {
			hIndexGames = val;
			return this;
		}

		public Builder friendless(int val) {
			friendless = val;
			return this;
		}

		public Builder top100count(int val) {
			top100count = val;
			return this;
		}

		public PlayStats build() {
			return new PlayStats(this);
		}
	}
}
