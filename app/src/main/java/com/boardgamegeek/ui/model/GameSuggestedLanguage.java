package com.boardgamegeek.ui.model;


import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.PollFragment;

public class GameSuggestedLanguage {
	public static final String[] PROJECTION = {
		GamePollResultsResult.POLL_RESULTS_RESULT_VOTES,
		GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL,
		GamePolls.POLL_TOTAL_VOTES
	};

	public static final String SORT =
		GamePollResultsResult.POLL_RESULTS_SORT_INDEX + " ASC, " + GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX;

	private static final int POLL_RESULTS_RESULT_VOTES = 0;
	private static final int POLL_RESULTS_RESULT_LEVEL = 1;
	private static final int POLL_TOTAL_VOTES = 2;

	private int totalVotes;
	private int level;
	private int votes;

	private GameSuggestedLanguage() {
	}

	public static GameSuggestedLanguage fromCursor(Cursor cursor) {
		GameSuggestedLanguage gameSuggestedPlayerLanguage = new GameSuggestedLanguage();
		gameSuggestedPlayerLanguage.totalVotes = cursor.getInt(POLL_TOTAL_VOTES);
		gameSuggestedPlayerLanguage.level = (cursor.getInt(POLL_RESULTS_RESULT_LEVEL) - 1) % 5 + 1;
		gameSuggestedPlayerLanguage.votes = cursor.getInt(POLL_RESULTS_RESULT_VOTES);
		return gameSuggestedPlayerLanguage;
	}

	public static Uri buildUri(int gameId) {
		return Games.buildPollResultsResultUri(gameId, PollFragment.LANGUAGE_DEPENDENCE);
	}

	public int getTotalVotes() {
		return totalVotes;
	}

	public int getLevel() {
		return level;
	}

	public int getVotes() {
		return votes;
	}
}
