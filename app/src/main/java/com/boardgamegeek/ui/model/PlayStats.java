package com.boardgamegeek.ui.model;

import java.util.ArrayList;
import java.util.List;

public class PlayStats {
	public static final int INVALID_FRIENDLESS = Integer.MIN_VALUE;
	public static final double INVALID_UTILIZATION = -1.0;
	public static final double INVALID_CFM = -1.0;

	private int numberOfPlays = 0;
	private int numberOfPlayedGames = 0;
	private int numberOfQuarters = 0;
	private int numberOfDimes = 0;
	private int numberOfNickels = 0;
	private int gameHIndex = 0;
	private List<HIndexEntry> hIndexGames = new ArrayList<>();
	private int friendless = INVALID_FRIENDLESS;
	private double utilization = INVALID_UTILIZATION;
	private double cfm = 0.0;
	private int top100count = 0;

	private PlayStats(Builder builder) {
		numberOfPlays = builder.numberOfPlays;
		numberOfPlayedGames = builder.numberOfPlayedGames;
		numberOfQuarters = builder.numberOfQuarters;
		numberOfDimes = builder.numberOfDimes;
		numberOfNickels = builder.numberOfNickels;
		gameHIndex = builder.gameHIndex;
		hIndexGames = builder.hIndexGames;
		friendless = builder.friendless;
		utilization = builder.utilization;
		cfm = builder.cfm;
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

	public int getGameHIndex() {
		return gameHIndex;
	}

	public int getFriendless() {
		return friendless;
	}

	public double getUtilization() {
		return utilization;
	}

	public double getCfm() {
		return cfm;
	}

	public int getTop100Count() {
		return top100count;
	}

	public List<HIndexEntry> getHIndexGames() {
		return hIndexGames;
	}

	public static final class Builder {
		private int numberOfPlays;
		private int numberOfPlayedGames;
		private int numberOfQuarters;
		private int numberOfDimes;
		private int numberOfNickels;
		private int gameHIndex;
		private List<HIndexEntry> hIndexGames = new ArrayList<>();
		private int friendless;
		private double utilization;
		private double cfm;
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

		public Builder gameHIndex(int val) {
			gameHIndex = val;
			return this;
		}

		public Builder hIndexGames(List<HIndexEntry> val) {
			hIndexGames = val;
			return this;
		}

		public Builder friendless(int val) {
			friendless = val;
			return this;
		}

		public Builder utilization(double val) {
			utilization = val;
			return this;
		}

		public Builder cfm(double val) {
			cfm = val;
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
