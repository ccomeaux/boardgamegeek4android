package com.boardgamegeek.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.text.TextUtils;

import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.events.PlayStatsUpdatedEvent;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.model.HIndexEntry;
import com.boardgamegeek.ui.model.PlayStats;
import com.boardgamegeek.ui.model.PlayStats.Builder;
import com.boardgamegeek.ui.model.Player;
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

	@SuppressLint("StaticFieldLeak") @Nullable private final Context context;
	private static final int MIN_H_INDEX_ENTRIES = 3;
	private static final int MAX_H_INDEX_ENTRIES = 6;
	private int numberOfPlays;
	private int numberOfPlayedGames;
	private int numberOfQuarters;
	private int numberOfDimes;
	private int numberOfNickels;
	private final List<Integer> ownedGamePlayCountsSorted = new ArrayList<>();
	private int numberOfOwnedGamesThatHaveEarnedTheirKeep;
	private int numberOfOwnedUnplayedGames;
	private int gameHIndex = 0;
	private final List<HIndexEntry> hIndexGames = new ArrayList<>();
	private int playerHIndex;
	private final List<HIndexEntry> hIndexPlayers = new ArrayList<>();
	private int top100count;
	private double totalCdf;
	private boolean isOwnedSynced;
	private final double lambda;

	public CalculatePlayStatsTask(@Nullable Context context) {
		this.context = context == null ? null : context.getApplicationContext();
		lambda = Math.log(0.1) / -10;
	}

	@Override
	protected PlayStats doInBackground(Void... params) {
		reset();

		if (context != null) {
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

			Cursor playerCursor = context.getContentResolver().query(
				Plays.buildPlayersByUniquePlayerUri(),
				Player.PROJECTION,
				getPlayerSelection(context),
				getPlayerSelectionArgs(context),
				getPlayerSortOrder());

			try {
				if (playerCursor != null) calculatePlayerStats(playerCursor);
			} finally {
				if (playerCursor != null) playerCursor.close();
			}
		}

		PlayStats playStats = new Builder()
			.numberOfPlays(numberOfPlays)
			.numberOfPlayedGames(numberOfPlayedGames)
			.numberOfNickels(numberOfNickels)
			.numberOfDimes(numberOfDimes)
			.numberOfQuarters(numberOfQuarters)
			.gameHIndex(gameHIndex)
			.hIndexGames(getHIndexGames())
			.playerHIndex(playerHIndex)
			.hIndexPlayers(getHIndexPlayers())
			.friendless(getFriendless())
			.utilization(getUtilization())
			.cfm(getCfm())
			.top100count(top100count)
			.build();

		PreferencesUtils.updatePlayStats(context, playStats);

		return playStats;
	}

	private void reset() {
		numberOfPlays = 0;
		numberOfPlayedGames = 0;
		numberOfQuarters = 0;
		numberOfDimes = 0;
		numberOfNickels = 0;
		ownedGamePlayCountsSorted.clear();
		numberOfOwnedGamesThatHaveEarnedTheirKeep = 0;
		numberOfOwnedUnplayedGames = 0;
		gameHIndex = 0;
		hIndexGames.clear();
		playerHIndex = 0;
		hIndexPlayers.clear();
		top100count = 0;
		totalCdf = 0.0;
	}

	private void calculateGameStats(Cursor cursor, Set<Integer> ownedGameIds) {
		final int PLAY_COUNT_TO_EARN_KEEP = 10;
		int hIndexCounter = 0;
		while (cursor.moveToNext()) {
			int playCount = cursor.getInt(SUM_QUANTITY);
			String gameName = cursor.getString(GAME_NAME);
			int rank = cursor.getInt(RANK);
			int gameId = cursor.getInt(GAME_ID);

			numberOfPlays += playCount;
			if (playCount > 0) numberOfPlayedGames++;
			if (playCount >= 25) numberOfQuarters++;
			else if (playCount >= 10) numberOfDimes++;
			else if (playCount >= 5) numberOfNickels++;

			if (ownedGameIds.contains(gameId)) {
				ownedGamePlayCountsSorted.add(playCount);
				totalCdf += MathUtils.cdf(playCount, lambda);
				if (playCount == 0) numberOfOwnedUnplayedGames++;
				if (playCount >= PLAY_COUNT_TO_EARN_KEEP)
					numberOfOwnedGamesThatHaveEarnedTheirKeep++;
			}

			if (playCount > 0 && rank >= 1 && rank <= 100) top100count++;

			if (playCount > 0) {
				hIndexCounter++;
				hIndexGames.add(new HIndexEntry.Builder()
					.name(gameName)
					.rank(hIndexCounter)
					.playCount(playCount)
					.build());
				if (gameHIndex == 0) {
					if (hIndexCounter > playCount) {
						gameHIndex = hIndexCounter - 1;
					}
				}
			}
		}
		if (gameHIndex == 0) gameHIndex = hIndexCounter;
	}

	private void calculatePlayerStats(Cursor playerCursor) {
		int hIndexCounter = 0;
		while (playerCursor.moveToNext()) {
			Player player = Player.fromCursor(playerCursor);
			if (player.getPlayCount() > 0) {
				hIndexCounter++;
				hIndexPlayers.add(new HIndexEntry.Builder()
					.name(player.getDescription())
					.rank(hIndexCounter)
					.playCount(player.getPlayCount())
					.build());
				if (playerHIndex == 0) {
					if (hIndexCounter > player.getPlayCount()) {
						playerHIndex = hIndexCounter - 1;
					}
				}
			}
		}
		if (playerHIndex == 0) playerHIndex = hIndexCounter;
	}

	@NonNull
	private Set<Integer> getOwnedGameIds() {
		Set<Integer> ownedGameIds = new ArraySet<>();
		if (isOwnedSynced && context != null) {
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
			Games.CONTENT_PLAYS_URI :
			Plays.CONTENT_URI.buildUpon()
				.appendQueryParameter(BggContract.QUERY_KEY_GROUP_BY, Plays.OBJECT_ID)
				.build();
	}

	@NonNull
	private String getGameSelection() {
		StringBuilder selection = new StringBuilder(SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP));

		if (!PreferencesUtils.logPlayStatsIncomplete(context)) {
			selection.append(" AND ").append(SelectionBuilder.whereZeroOrNull(Plays.INCOMPLETE));
		}

		if (!PreferencesUtils.logPlayStatsExpansions(context) &&
			!PreferencesUtils.logPlayStatsAccessories(context)) {
			selection.append(" AND (").append(Games.SUBTYPE).append("=? OR ").append(Games.SUBTYPE).append(" IS NULL)");
		} else if (!PreferencesUtils.logPlayStatsExpansions(context) ||
			!PreferencesUtils.logPlayStatsAccessories(context)) {
			selection.append(" AND (").append(Games.SUBTYPE).append("!=? OR ").append(Games.SUBTYPE).append(" IS NULL)");
		}

		return selection.toString();
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

	@NonNull
	private static String getPlayerSelection(Context context) {
		StringBuilder sb = new StringBuilder(SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP));

		if (!TextUtils.isEmpty(AccountUtils.getUsername(context))) {
			sb
				.append(" AND ")
				.append(PlayPlayers.USER_NAME)
				.append("!=?");
		}

		if (!PreferencesUtils.logPlayStatsIncomplete(context)) {
			sb.append(" AND ").append(SelectionBuilder.whereZeroOrNull(Plays.INCOMPLETE));
		}

		return sb.toString();
	}

	@Nullable
	private static String[] getPlayerSelectionArgs(Context context) {
		final String username = AccountUtils.getUsername(context);
		if (TextUtils.isEmpty(username)) return null;
		List<String> args = new ArrayList<>();
		args.add(username);
		return args.toArray(new String[args.size()]);
	}

	@NonNull
	private static String getPlayerSortOrder() {
		return PlayPlayers.SUM_QUANTITY + " DESC, " + PlayPlayers.NAME + BggContract.COLLATE_NOCASE;
	}

	private List<HIndexEntry> getHIndexGames() {
		return getHIndexEntries(hIndexGames, gameHIndex);
	}

	private List<HIndexEntry> getHIndexPlayers() {
		return getHIndexEntries(hIndexPlayers, playerHIndex);
	}

	private List<HIndexEntry> getHIndexEntries(List<HIndexEntry> hIndexEntries, int hIndex) {
		if (hIndexEntries == null || hIndexEntries.size() == 0) return new ArrayList<>();

		List<HIndexEntry> entries = new ArrayList<>();
		int indexAbove = -1;
		int indexBelow = -1;
		for (int i = 0; i < hIndexEntries.size(); i++) {
			HIndexEntry entry = hIndexEntries.get(i);
			if (entry.getPlayCount() > hIndex) {
				indexAbove = i;
			} else if (entry.getPlayCount() == hIndex) {
				entries.add(entry);
			} else if (entry.getPlayCount() < hIndex) {
				indexBelow = i;
				break;
			}
		}

		int count = 0;
		int previousPlayCount = -1;
		if (indexBelow > -1) {
			for (int i = indexBelow; i < hIndexEntries.size(); i++) {
				HIndexEntry entry = hIndexEntries.get(i);
				if (count >= MAX_H_INDEX_ENTRIES) {
					break;
				} else if (count < MIN_H_INDEX_ENTRIES) {
					entries.add(entry);
					count++;
					previousPlayCount = entry.getPlayCount();
				} else if (entry.getPlayCount() == previousPlayCount) {
					entries.add(entry);
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
				HIndexEntry entry = hIndexEntries.get(i);
				if (count >= MAX_H_INDEX_ENTRIES) {
					break;
				} else if (count < MIN_H_INDEX_ENTRIES) {
					entries.add(0, entry);
					count++;
					previousPlayCount = entry.getPlayCount();
				} else if (entry.getPlayCount() == previousPlayCount) {
					entries.add(0, entry);
					count++;
				} else {
					break;
				}
			}
		}

		return entries;
	}

	private int getNumberOfOwnedPlayedGames() {
		return ownedGamePlayCountsSorted == null ? 0 : ownedGamePlayCountsSorted.size();
	}

	private int getFriendless() {
		if (!isOwnedSynced) return PlayStats.INVALID_FRIENDLESS;
		if (getNumberOfOwnedPlayedGames() == 0) return 0;
		if (numberOfOwnedGamesThatHaveEarnedTheirKeep >= ownedGamePlayCountsSorted.size()) {
			return ownedGamePlayCountsSorted.get(ownedGamePlayCountsSorted.size() - 1);
		} else {
			int friendless = ownedGamePlayCountsSorted.get(ownedGamePlayCountsSorted.size() - numberOfOwnedGamesThatHaveEarnedTheirKeep - 1);
			if (friendless == 0)
				return numberOfOwnedGamesThatHaveEarnedTheirKeep - numberOfOwnedUnplayedGames;
			return friendless;
		}
	}

	private double getUtilization() {
		if (!isOwnedSynced) return PlayStats.INVALID_UTILIZATION;
		if (getNumberOfOwnedPlayedGames() == 0) return 0;
		return totalCdf / getNumberOfOwnedPlayedGames();
	}

	private double getCfm() {
		if (!isOwnedSynced) return PlayStats.INVALID_CFM;
		if (getNumberOfOwnedPlayedGames() == 0) return 0;
		return MathUtils.invcdf(totalCdf / getNumberOfOwnedPlayedGames(), lambda);
	}
}
