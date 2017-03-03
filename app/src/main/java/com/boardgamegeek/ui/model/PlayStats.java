package com.boardgamegeek.ui.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.SelectionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class PlayStats {
	public static final String[] PROJECTION = {
		Plays.SUM_QUANTITY,
		Plays.ITEM_NAME,
		Games.GAME_RANK
	};

	private static final int SUM_QUANTITY = 0;
	private static final int GAME_NAME = 1;
	private static final int RANK = 2;

	private static final int MIN_H_INDEX_GAMES = 2;
	private static final int MAX_H_INDEX_GAMES = 6;
	private int numberOfPlays = 0;
	private int numberOfGames = 0;
	private int numberOfQuarters = 0;
	private int numberOfDimes = 0;
	private int numberOfNickels = 0;
	private int hIndex = 0;
	private int hIndexCounter = 1;
	private final List<Pair<String, Integer>> hIndexGames = new ArrayList<>();
	private final Stack<Pair<String, Integer>> hIndexGamesStack = new Stack<>();
	private int postIndexCount = 0;
	private int priorPlayCount;
	private int top100count = 0;

	public static PlayStats fromCursor(Cursor cursor) {
		return new PlayStats(cursor);
	}

	private PlayStats(Cursor cursor) {
		init(cursor);
	}

	private void init(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		do {
			int playCount = cursor.getInt(SUM_QUANTITY);
			String gameName = cursor.getString(GAME_NAME);
			int rank = cursor.getInt(RANK);

			numberOfPlays += playCount;
			numberOfGames++;

			if (playCount >= 25) {
				numberOfQuarters++;
			} else if (playCount >= 10) {
				numberOfDimes++;
			} else if (playCount >= 5) {
				numberOfNickels++;
			}

			if (rank >= 1 && rank <= 100) {
				top100count++;
			}

			if (hIndex == 0) {
				hIndexGamesStack.push(new Pair<>(gameName, playCount));
				if (hIndexCounter > playCount) {
					hIndex = hIndexCounter - 1;
					int preIndexCount = 0;
					while (!hIndexGamesStack.isEmpty()) {
						Pair<String, Integer> game = hIndexGamesStack.pop();
						if (preIndexCount < MIN_H_INDEX_GAMES) {
							hIndexGames.add(0, game);
							if (game.second != hIndex) {
								preIndexCount++;
								priorPlayCount = game.second;
							}
						} else //noinspection StatementWithEmptyBody
							if (preIndexCount >= MAX_H_INDEX_GAMES) {
								//do nothing
							} else if (game.second == priorPlayCount) {
								hIndexGames.add(0, game);
								preIndexCount++;
							}
					}
				}
				hIndexCounter++;
			} else {
				if (postIndexCount < MIN_H_INDEX_GAMES) {
					hIndexGames.add(new Pair<>(gameName, playCount));
					postIndexCount++;
					priorPlayCount = playCount;
				} else //noinspection StatementWithEmptyBody
					if (postIndexCount >= MAX_H_INDEX_GAMES) {
						// do nothing
					} else if (playCount == priorPlayCount) {
						hIndexGames.add(new Pair<>(gameName, playCount));
						postIndexCount++;
					}
			}
		} while (cursor.moveToNext());
	}

	@NonNull
	public static Uri getUri() {
		return Plays.CONTENT_URI.buildUpon()
			.appendQueryParameter(BggContract.QUERY_KEY_GROUP_BY, BggContract.Plays.OBJECT_ID)
			.build();
	}

	@NonNull
	public static String getSelection(Context context) {
		String selection = SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP);

		if (!PreferencesUtils.logPlayStatsIncomplete(context)) {
			selection += " AND " + Plays.INCOMPLETE + "!=?";
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
	public static String[] getSelectionArgs(Context context) {
		List<String> args = new ArrayList<>();

		if (!PreferencesUtils.logPlayStatsIncomplete(context)) {
			args.add("1");
		}

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
	public static String getSortOrder() {
		return Plays.SUM_QUANTITY + " DESC, " + Games.GAME_NAME + " ASC";
	}

	public int getNumberOfPlays() {
		return numberOfPlays;
	}

	public int getNumberOfGames() {
		return numberOfGames;
	}

	public int getNumberOfQuarters() {
		return numberOfQuarters;
	}

	public int getNumberOfDimes() {
		return numberOfDimes;
	}

	public int getNumberOfNickels() {
		return numberOfNickels;
	}

	public int getHIndex() {
		return hIndex;
	}

	public int getTop100Count() {
		return top100count;
	}

	public List<Pair<String, Integer>> getHIndexGames() {
		return hIndexGames;
	}
}
