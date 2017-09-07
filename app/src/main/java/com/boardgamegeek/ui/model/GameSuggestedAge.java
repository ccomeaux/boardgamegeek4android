package com.boardgamegeek.ui.model;


import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;

public class GameSuggestedAge {
	public static final String[] PROJECTION = {
		GamePollResultsResult.POLL_RESULTS_RESULT_VOTES,
		GamePollResultsResult.POLL_RESULTS_RESULT_VALUE,
		GamePolls.POLL_TOTAL_VOTES
	};

	public static final String SORT =
		GamePollResultsResult.POLL_RESULTS_SORT_INDEX + " ASC, " + GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX;

	private static final int POLL_RESULTS_RESULT_VOTES = 0;
	private static final int POLL_RESULTS_RESULT_VALUE = 1;
	private static final int POLL_TOTAL_VOTES = 2;

	private int totalVotes;
	private String value;
	private int votes;

	private GameSuggestedAge() {
	}

	public static GameSuggestedAge fromCursor(Cursor cursor) {
		GameSuggestedAge gameSuggestedPlayerAge = new GameSuggestedAge();
		gameSuggestedPlayerAge.totalVotes = cursor.getInt(POLL_TOTAL_VOTES);
		gameSuggestedPlayerAge.value = cursor.getString(POLL_RESULTS_RESULT_VALUE);
		gameSuggestedPlayerAge.votes = cursor.getInt(POLL_RESULTS_RESULT_VOTES);
		return gameSuggestedPlayerAge;
	}

	public static Uri buildUri(int gameId) {
		return Games.buildPollResultsResultUri(gameId, BggContract.POLL_TYPE_SUGGESTED_PLAYER_AGE);
	}

	public int getTotalVotes() {
		return totalVotes;
	}

	public String getValue() {
		return value;
	}

	public int getVotes() {
		return votes;
	}
}
