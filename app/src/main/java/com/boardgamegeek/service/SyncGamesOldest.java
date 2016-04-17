package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ResolverUtils;

import java.util.List;

/**
 * Syncs a number of games that haven't been updated in a long time.
 */
public class SyncGamesOldest extends SyncGames {
	public SyncGamesOldest(Context context, BoardGameGeekService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@NonNull
	@Override
	protected String getIntroLogMessage() {
		return "Syncing oldest games in the collection...";
	}

	@NonNull
	@Override
	protected String getExitLogMessage() {
		return "...found no old games to update (this should only happen with empty collections)";
	}

	@Override
	protected List<String> getGameIds(int gamesPerFetch) {
		return ResolverUtils.queryStrings(context.getContentResolver(), Games.CONTENT_URI,
			Games.GAME_ID, null, null, "games." + Games.UPDATED + " LIMIT " + gamesPerFetch);
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_games_oldest;
	}
}
