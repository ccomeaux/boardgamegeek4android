package com.boardgamegeek.ui.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Games;

import hugo.weaving.DebugLog;

public class Game {
	public static final String[] PROJECTION = {
		Games.GAME_ID,
		Games.STATS_AVERAGE,
		Games.YEAR_PUBLISHED,
		Games.MIN_PLAYERS,
		Games.MAX_PLAYERS,
		Games.PLAYING_TIME,
		Games.MINIMUM_AGE,
		Games.DESCRIPTION,
		Games.STATS_USERS_RATED,
		Games.UPDATED,
		Games.GAME_RANK,
		Games.GAME_NAME,
		Games.THUMBNAIL_URL,
		Games.STATS_BAYES_AVERAGE,
		Games.STATS_MEDIAN,
		Games.STATS_STANDARD_DEVIATION,
		Games.STATS_NUMBER_WEIGHTS,
		Games.STATS_AVERAGE_WEIGHT,
		Games.STATS_NUMBER_OWNED,
		Games.STATS_NUMBER_TRADING,
		Games.STATS_NUMBER_WANTING,
		Games.STATS_NUMBER_WISHING,
		Games.POLLS_COUNT,
		Games.IMAGE_URL,
		Games.SUBTYPE,
		Games.CUSTOM_PLAYER_SORT,
		Games.STATS_NUMBER_COMMENTS,
		Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
		Games.MIN_PLAYING_TIME,
		Games.MAX_PLAYING_TIME,
		Games.STARRED
	};

	private static final int GAME_ID = 0;
	private static final int STATS_AVERAGE = 1;
	private static final int YEAR_PUBLISHED = 2;
	private static final int MIN_PLAYERS = 3;
	private static final int MAX_PLAYERS = 4;
	private static final int PLAYING_TIME = 5;
	private static final int MINIMUM_AGE = 6;
	private static final int DESCRIPTION = 7;
	private static final int STATS_USERS_RATED = 8;
	private static final int UPDATED = 9;
	private static final int GAME_RANK = 10;
	private static final int GAME_NAME = 11;
	private static final int THUMBNAIL_URL = 12;
	private static final int STATS_NUMBER_WEIGHTS = 16;
	private static final int STATS_AVERAGE_WEIGHT = 17;
	private static final int STATS_NUMBER_OWNED = 18;
	private static final int STATS_NUMBER_TRADING = 19;
	private static final int STATS_NUMBER_WANTING = 20;
	private static final int STATS_NUMBER_WISHING = 21;
	private static final int POLLS_COUNT = 22;
	private static final int IMAGE_URL = 23;
	private static final int SUBTYPE = 24;
	private static final int CUSTOM_PLAYER_SORT = 25;
	private static final int STATS_NUMBER_COMMENTS = 26;
	private static final int SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL = 27;
	private static final int MIN_PLAYING_TIME = 28;
	private static final int MAX_PLAYING_TIME = 29;
	private static final int STARRED = 30;

	public String Name;
	public String ThumbnailUrl;
	public String ImageUrl;
	public int Id;
	public double Rating;
	public int YearPublished;
	public int MinPlayers;
	public int MaxPlayers;
	public int PlayingTime;
	public int MinPlayingTime;
	public int MaxPlayingTime;
	public int MinimumAge;
	public String Description;
	public int UsersRated;
	public int UsersCommented;
	public long Updated;
	public int Rank;
	public double AverageWeight;
	public int NumberWeights;
	public int NumberOwned;
	public int NumberTrading;
	public int NumberWanting;
	public int NumberWishing;
	public int PollsCount;
	public String Subtype;
	public boolean CustomPlayerSort;
	public int SuggestedPlayerCountPollVoteTotal;
	public boolean IsFavorite;

	private Game() {
	}

	public static Game fromCursor(Cursor cursor) {
		Game game = new Game();
		game.Name = cursor.getString(GAME_NAME);
		game.ThumbnailUrl = cursor.getString(THUMBNAIL_URL);
		game.ImageUrl = cursor.getString(IMAGE_URL);
		game.Id = cursor.getInt(GAME_ID);
		game.Rating = cursor.getDouble(STATS_AVERAGE);
		game.YearPublished = cursor.getInt(YEAR_PUBLISHED);
		game.MinPlayers = cursor.getInt(MIN_PLAYERS);
		game.MaxPlayers = cursor.getInt(MAX_PLAYERS);
		game.PlayingTime = cursor.getInt(PLAYING_TIME);
		game.MinPlayingTime = cursor.getInt(MIN_PLAYING_TIME);
		game.MaxPlayingTime = cursor.getInt(MAX_PLAYING_TIME);
		game.MinimumAge = cursor.getInt(MINIMUM_AGE);
		game.Description = cursor.getString(DESCRIPTION);
		game.UsersRated = cursor.getInt(STATS_USERS_RATED);
		game.UsersCommented = cursor.getInt(STATS_NUMBER_COMMENTS);
		game.Updated = cursor.getLong(UPDATED);
		game.Rank = cursor.getInt(GAME_RANK);
		game.AverageWeight = cursor.getDouble(STATS_AVERAGE_WEIGHT);
		game.NumberWeights = cursor.getInt(STATS_NUMBER_WEIGHTS);
		game.NumberOwned = cursor.getInt(STATS_NUMBER_OWNED);
		game.NumberTrading = cursor.getInt(STATS_NUMBER_TRADING);
		game.NumberWanting = cursor.getInt(STATS_NUMBER_WANTING);
		game.NumberWishing = cursor.getInt(STATS_NUMBER_WISHING);
		game.PollsCount = cursor.getInt(POLLS_COUNT);
		game.Subtype = cursor.getString(SUBTYPE);
		game.CustomPlayerSort = (cursor.getInt(CUSTOM_PLAYER_SORT) == 1);
		game.SuggestedPlayerCountPollVoteTotal = cursor.getInt(SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL);
		game.IsFavorite = (cursor.getInt(STARRED) == 1);
		return game;
	}

	@DebugLog
	public int getMaxUsers() {
		int max = Math.max(UsersRated, UsersCommented);
		max = Math.max(max, NumberOwned);
		max = Math.max(max, NumberTrading);
		max = Math.max(max, NumberWanting);
		max = Math.max(max, NumberWeights);
		max = Math.max(max, NumberWishing);
		return max;
	}
}
