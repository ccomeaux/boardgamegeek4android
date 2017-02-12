package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;

/**
 * Syncs a number of games that haven't been updated in a long time.
 */
public class SyncGamesOldest extends SyncGames {
	public SyncGamesOldest(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@NonNull
	@Override
	protected String getIntroLogMessage(int gamesPerFetch) {
		return String.format("Syncing %,d oldest games in the collection...", gamesPerFetch);
	}

	@NonNull
	@Override
	protected String getExitLogMessage() {
		return "...found no old games to update (this should only happen with empty collections)";
	}
	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_games_oldest;
	}
}
