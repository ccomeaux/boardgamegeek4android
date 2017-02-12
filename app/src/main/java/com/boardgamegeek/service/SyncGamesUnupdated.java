package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ResolverUtils;

import java.util.List;

/**
 * Syncs all games in the collection that have not been updated completely.
 */
public class SyncGamesUnupdated extends SyncGames {

	public SyncGamesUnupdated(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@NonNull
	@Override
	protected String getIntroLogMessage(int gamesPerFetch) {
		return String.format("Syncing %,d unupdated games in the collection...", gamesPerFetch);
	}

	@NonNull
	@Override
	protected String getExitLogMessage() {
		return "...no more unupdated games";
	}

	@Override
	protected List<String> getGameIds(int gamesPerFetch) {
		return ResolverUtils.queryStrings(context.getContentResolver(), Games.CONTENT_URI,
			Games.GAME_ID, "games." + Games.UPDATED + "=0 OR games." + Games.UPDATED + " IS NULL", null,
			"games." + Games.UPDATED_LIST + " DESC LIMIT " + gamesPerFetch);
	}

	@Override
	protected int getMaxFetchCount() {
		return 20;
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_games_unupdated;
	}
}
