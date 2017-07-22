package com.boardgamegeek.ui.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Games;

public class GameToRefresh {
	public static final String[] PROJECTION = {
		Games.GAME_ID,
		Games.UPDATED,
		Games.POLLS_COUNT,
		Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL
	};

	private static final int UPDATED = 1;
	private static final int POLLS_COUNT = 2;
	private static final int SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL = 3;

	private long syncedTimestampInMillis;
	private int pollsVoteCount;
	private int suggestedPlayerCountPollVoteTotal;

	private GameToRefresh() {
	}

	public static GameToRefresh fromCursor(Cursor cursor) {
		GameToRefresh game = new GameToRefresh();
		game.syncedTimestampInMillis = cursor.getLong(UPDATED);
		game.pollsVoteCount = cursor.getInt(POLLS_COUNT);
		game.suggestedPlayerCountPollVoteTotal = cursor.getInt(SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL);
		return game;
	}

	public long getSyncedTimestampInMillis() {
		return syncedTimestampInMillis;
	}

	public int getPollsVoteCount() {
		return pollsVoteCount + suggestedPlayerCountPollVoteTotal;
	}
}
