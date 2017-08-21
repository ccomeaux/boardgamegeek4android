package com.boardgamegeek.ui.model;


import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollPollResults;
import com.boardgamegeek.provider.BggContract.Games;

public class GameSuggestedPlayerCount {
	public static final String[] PROJECTION = {
		GameSuggestedPlayerCountPollPollResults.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
		GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT,
		GameSuggestedPlayerCountPollPollResults.RECOMMENDATION,
	};

	private static final int TOTAL_VOTE_COUNT = 0;
	private static final int PLAYER_COUNT = 1;
	private static final int RECOMMENDATION = 2;

	private int totalVotes;
	private int playerCount;
	private int recommendation;

	private GameSuggestedPlayerCount() {
	}

	public static GameSuggestedPlayerCount fromCursor(Cursor cursor) {
		GameSuggestedPlayerCount gameSuggestedPlayerCount = new GameSuggestedPlayerCount();
		gameSuggestedPlayerCount.totalVotes = cursor.getInt(TOTAL_VOTE_COUNT);
		gameSuggestedPlayerCount.playerCount = cursor.getInt(PLAYER_COUNT);
		gameSuggestedPlayerCount.recommendation = cursor.getInt(RECOMMENDATION);
		return gameSuggestedPlayerCount;
	}

	public static Uri buildUri(int gameId) {
		return Games.buildSuggestedPlayerCountPollResultsUri(gameId);
	}

	public int getTotalVotes() {
		return totalVotes;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public int getRecommendation() {
		return recommendation;
	}
}
