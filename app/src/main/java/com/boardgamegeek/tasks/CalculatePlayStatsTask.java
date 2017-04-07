package com.boardgamegeek.tasks;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import android.support.v4.util.Pair;

import com.boardgamegeek.events.PlayStatsUpdatedEvent;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.model.PlayStats;
import com.boardgamegeek.ui.model.PlayStats.Builder;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.SelectionBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CalculatePlayStatsTask extends AsyncTask<Void, Void, PlayStats> {
	public static final String[] PROJECTION = {
		Plays.SUM_QUANTITY,
		Plays.ITEM_NAME,
		Games.GAME_RANK,
		Games.GAME_ID
	};

	private static final int SUM_QUANTITY = 0;
	private static final int GAME_NAME = 1;
	private static final int RANK = 2;
	private static final int GAME_ID = 3;

	private final Context context;
	private static final int MIN_H_INDEX_GAMES = 2;
	private static final int MAX_H_INDEX_GAMES = 6;
	private int numberOfPlays = 0;
	private int numberOfPlayedGames = 0;
	private int numberOfQuarters = 0;
	private int numberOfDimes = 0;
	private int numberOfNickels = 0;
	private int numberOfZeroes = 0;
	private final List<Integer> ownedPlayCounts = new ArrayList<>();
	private int gameHIndex = 0;
	private final List<Pair<String, Integer>> hIndexGames = new ArrayList<>();
	private int top100count = 0;
	private double totalCdf;
	private boolean isOwnedSynced;
	private final double lambda;

	public CalculatePlayStatsTask(Context context) {
		this.context = context.getApplicationContext();
		lambda = Math.log(0.1) / -10;
	}

	@Override
	protected PlayStats doInBackground(Void... params) {
		isOwnedSynced = PreferencesUtils.isSyncStatus(context, BggService.COLLECTION_QUERY_STATUS_OWN);
		boolean isPlayedSynced = PreferencesUtils.isSyncStatus(context, BggService.COLLECTION_QUERY_STATUS_PLAYED);

		Set<Integer> ownedGameIds = getOwnedGameIds();

		Cursor cursor = context.getContentResolver().query(
			getUri(isOwnedSynced && isPlayedSynced),
			PROJECTION,
			getGameSelection(),
			getGameSelectionArgs(),
			getGameSortOrder());

		try {
			if (cursor != null) calculateGameStats(cursor, ownedGameIds);
		} finally {
			if (cursor != null) cursor.close();
		}

		PlayStats playStats = new Builder()
			.numberOfPlays(numberOfPlays)
			.numberOfPlayedGames(numberOfPlayedGames)
			.numberOfNickels(numberOfNickels)
			.numberOfDimes(numberOfDimes)
			.numberOfQuarters(numberOfQuarters)
			.gameHIndex(gameHIndex)
			.hIndexGames(getHIndexGames())
			.friendless(getFriendless())
			.utilization(getUtilization())
			.cfm(getCfm())
			.top100count(top100count)
			.build();

		PreferencesUtils.updatePlayStats(context, playStats);

		return playStats;
	}

	private void calculateGameStats(Cursor cursor, Set<Integer> ownedGameIds) {
		int hIndexCounter = 0;
		while (cursor.moveToNext()) {
			int playCount = cursor.getInt(SUM_QUANTITY);
			String gameName = cursor.getString(GAME_NAME);
			int rank = cursor.getInt(RANK);
			int gameId = cursor.getInt(GAME_ID);

			numberOfPlays += playCount;
			if (ownedGameIds.contains(gameId)) {
				ownedPlayCounts.add(playCount);
				totalCdf += MathUtils.cdf(playCount, lambda);
			}
			if (playCount > 0) numberOfPlayedGames++;

			if (playCount >= 25) numberOfQuarters++;
			else if (playCount >= 10) numberOfDimes++;
			else if (playCount >= 5) numberOfNickels++;
			else if (playCount == 0) numberOfZeroes++;

			if (playCount > 0 && rank >= 1 && rank <= 100) top100count++;

			hIndexGames.add(Pair.create(gameName, playCount));
			if (gameHIndex == 0) {
				hIndexCounter++;
				if (hIndexCounter > playCount) {
					gameHIndex = hIndexCounter - 1;
				}
			}
		}
		if (gameHIndex == 0) gameHIndex = hIndexCounter - 1;
	}

	@NonNull
	private Set<Integer> getOwnedGameIds() {
		Set<Integer> ownedGameIds = new ArraySet<>();
		if (isOwnedSynced) {
			Cursor collectionCursor = context.getContentResolver().query(
				Collection.CONTENT_URI,
				new String[] { Collection.GAME_ID },
				Collection.STATUS_OWN + "=1",
				null,
				null);
			try {
				while (collectionCursor != null && collectionCursor.moveToNext()) {
					ownedGameIds.add(collectionCursor.getInt(0));
				}
			} finally {
				if (collectionCursor != null) collectionCursor.close();
			}
		}
		return ownedGameIds;
	}

	@Override
	protected void onPostExecute(PlayStats playStats) {
		EventBus.getDefault().post(new PlayStatsUpdatedEvent(playStats));
	}

	@NonNull
	private static Uri getUri(boolean byGames) {
		return byGames ?
			Games.CONTENT_PLAYS_URI.buildUpon().build() :
			Plays.CONTENT_URI.buildUpon()
				.appendQueryParameter(BggContract.QUERY_KEY_GROUP_BY, Plays.OBJECT_ID)
				.build();
	}

	@NonNull
	private String getGameSelection() {
		String selection = SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP);

		if (!PreferencesUtils.logPlayStatsIncomplete(context)) {
			selection += " AND " + SelectionBuilder.whereZeroOrNull(Plays.INCOMPLETE);
		}

		if (!PreferencesUtils.logPlayStatsExpansions(context) &&
			!PreferencesUtils.logPlayStatsAccessories(context)) {
			selection += " AND (" + Games.SUBTYPE + "=? OR " + Games.SUBTYPE + " IS NULL)";
		} else if (!PreferencesUtils.logPlayStatsExpansions(context) ||
			!PreferencesUtils.logPlayStatsAccessories(context)) {
			selection += " AND (" + Games.SUBTYPE + "!=? OR " + Games.SUBTYPE + " IS NULL)";
		}

		return selection;
	}

	@NonNull
	private String[] getGameSelectionArgs() {
		List<String> args = new ArrayList<>();

		if (!PreferencesUtils.logPlayStatsExpansions(context) &&
			!PreferencesUtils.logPlayStatsAccessories(context)) {
			args.add(BggService.THING_SUBTYPE_BOARDGAME);
		} else if (!PreferencesUtils.logPlayStatsExpansions(context)) {
			args.add(BggService.THING_SUBTYPE_BOARDGAME_EXPANSION);
		} else if (!PreferencesUtils.logPlayStatsAccessories(context)) {
			args.add(BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
		}

		return args.toArray(new String[args.size()]);
	}

	@NonNull
	private static String getGameSortOrder() {
		return Plays.SUM_QUANTITY + " DESC, " + Games.GAME_SORT_NAME + " ASC";
	}

	private List<Pair<String, Integer>> getHIndexGames() {
		if (hIndexGames == null || hIndexGames.size() == 0) return new ArrayList<>();

		List<Pair<String, Integer>> games = new ArrayList<>();
		int indexAbove = -1;
		int indexBelow = -1;
		for (int i = 0; i < hIndexGames.size(); i++) {
			Pair<String, Integer> game = hIndexGames.get(i);
			if (game.second > gameHIndex) {
				indexAbove = i;
			} else if (game.second == gameHIndex) {
				games.add(game);
			} else if (game.second < gameHIndex) {
				indexBelow = i;
				break;
			}
		}

		int count = 0;
		int previousPlayCount = -1;
		if (indexBelow > -1) {
			for (int i = indexBelow; i < hIndexGames.size(); i++) {
				Pair<String, Integer> game = hIndexGames.get(i);
				if (count >= MAX_H_INDEX_GAMES) {
					break;
				} else if (count < MIN_H_INDEX_GAMES) {
					games.add(game);
					count++;
					previousPlayCount = game.second;
				} else if (game.second == previousPlayCount) {
					games.add(game);
					count++;
				} else {
					break;
				}
			}
		}

		count = 0;
		previousPlayCount = -1;
		if (indexAbove > -1) {
			for (int i = indexAbove; i >= 0; i--) {
				Pair<String, Integer> game = hIndexGames.get(i);
				if (count >= MAX_H_INDEX_GAMES) {
					break;
				} else if (count < MIN_H_INDEX_GAMES) {
					games.add(0, game);
					count++;
					previousPlayCount = game.second;
				} else if (game.second == previousPlayCount) {
					games.add(0, game);
					count++;
				} else {
					break;
				}
			}
		}

		return games;
	}

	private int getFriendless() {
		if (!isOwnedSynced) return PlayStats.INVALID_FRIENDLESS;
		if (ownedPlayCounts == null || ownedPlayCounts.size() == 0) return 0;
		int numberOfTens = numberOfDimes + numberOfQuarters;
		if (numberOfTens == ownedPlayCounts.size()) {
			return ownedPlayCounts.get(ownedPlayCounts.size() - 1);
		} else {
			int friendless = ownedPlayCounts.get(ownedPlayCounts.size() - numberOfTens - 1);
			if (friendless == 0)
				return numberOfTens - numberOfZeroes;
			return friendless;
		}
	}

	private double getUtilization() {
		if (!isOwnedSynced) return PlayStats.INVALID_UTILIZATION;
		if (ownedPlayCounts == null || ownedPlayCounts.size() == 0) return 0;
		return totalCdf / ownedPlayCounts.size();
	}

	private double getCfm() {
		if (!isOwnedSynced) return PlayStats.INVALID_CFM;
		if (ownedPlayCounts == null || ownedPlayCounts.size() == 0) return 0;
		return MathUtils.invcdf(totalCdf / ownedPlayCounts.size(), lambda);
	}
}
