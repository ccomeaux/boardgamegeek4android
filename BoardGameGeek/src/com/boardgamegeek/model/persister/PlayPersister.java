package com.boardgamegeek.model.persister;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.ResolverUtils;

public class PlayPersister {
	private static final String TAG = makeLogTag(PlayPersister.class);

	/**
	 * A new play was inserted into the database
	 */
	public static final int STATUS_INSERT = 1;
	/**
	 * An existing play was updated in the database
	 */
	public static final int STATUS_UPDATE = 2;
	/**
	 * The play is being edited, therefore wasn't saved
	 */
	public static final int STATUS_IN_PROGRESS = 3;
	/**
	 * The play is pending update, therefore wasn't saved
	 */
	public static final int STATUS_PENDING_UPDATE = 4;
	/**
	 * The play is pending delete, therefore wasn't saved
	 */
	public static final int STATUS_PENDING_DELETE = 5;
	/**
	 * The save status is unknown, most likely because it hasn't finished executing yet.
	 */
	public static final int STATUS_UNKNOWN = 0;
	/**
	 * An unexpected error occurred while trying to sync
	 */
	public static final int STATUS_ERROR = -1;

	/*
	 * Delete the play from the content provider.
	 */
	public static boolean delete(ContentResolver resolver, Play play) {
		return resolver.delete(play.uri(), null, null) > 0;
	}

	public static void save(Context context, List<Play> plays, long startTime) {
		int updateCount = 0;
		int insertCount = 0;
		int pendingUpdateCount = 0;
		int pendingDeleteCount = 0;
		int inProgressCount = 0;
		int errorCount = 0;
		if (plays != null) {
			for (Play play : plays) {
				play.updated = startTime;
				int status = PlayPersister.save(context, play, true);
				switch (status) {
					case PlayPersister.STATUS_UPDATE:
						updateCount++;
						break;
					case PlayPersister.STATUS_INSERT:
						insertCount++;
						break;
					case PlayPersister.STATUS_PENDING_UPDATE:
						pendingUpdateCount++;
						break;
					case PlayPersister.STATUS_PENDING_DELETE:
						pendingDeleteCount++;
						break;
					case PlayPersister.STATUS_IN_PROGRESS:
						inProgressCount++;
						break;
					case PlayPersister.STATUS_ERROR:
					case PlayPersister.STATUS_UNKNOWN:
						errorCount++;
						break;
					default:
						break;
				}
			}
		}
		LOGI(
			TAG,
			String
				.format(
					"Updated %1$s, inserted %2$s, skipped %3$s (%4$s pending update, %5$s pending delete, %6$s draft, %7$s errors)",
					updateCount, insertCount, (pendingUpdateCount + pendingDeleteCount + inProgressCount + errorCount),
					pendingUpdateCount, pendingDeleteCount, inProgressCount, errorCount));
	}

	/*
	 * Save the play while not syncing.
	 */
	public static void save(Context context, Play play) {
		save(context, play, false);
	}

	/*
	 * Save the play. If syncing, the play will not be saved if it is a draft.
	 */
	public static int save(Context context, Play play, boolean isSyncing) {
		ContentResolver resolver = context.getContentResolver();

		int status = determineStatus(resolver, play, isSyncing);

		if (status != STATUS_UPDATE && status != STATUS_INSERT) {
			return status;
		}

		ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
		ContentValues values = createContentValues(play);
		List<Integer> itemObjectIds = null;
		List<Integer> playerUserIds = null;

		if (status == STATUS_UPDATE) {
			deletePlayerWithNullUserId(play, batch);
			playerUserIds = ResolverUtils.queryInts(resolver, play.playerUri(), PlayPlayers.USER_ID);
			playerUserIds = removeDuplicatePlayerUserIds(play, playerUserIds, batch);
			itemObjectIds = ResolverUtils.queryInts(resolver, play.itemUri(), PlayItems.OBJECT_ID);
			batch.add(ContentProviderOperation.newUpdate(play.uri()).withValues(values).build());
		} else if (status == STATUS_INSERT) {
			if (!play.hasBeenSynced()) {
				// If a sync isn't pending, mark it as draft
				if (play.syncStatus != Play.SYNC_STATUS_PENDING_UPDATE
					&& play.syncStatus != Play.SYNC_STATUS_PENDING_DELETE) {
					play.syncStatus = Play.SYNC_STATUS_IN_PROGRESS;
				}
			} else {
				play.syncStatus = Play.SYNC_STATUS_SYNCED;
			}
			values.put(Plays.SYNC_STATUS, play.syncStatus);

			if (play.playId == BggContract.INVALID_ID) {
				play.playId = getTemporaryId(resolver);
			}
			values.put(Plays.PLAY_ID, play.playId);

			if (!values.containsKey(Plays.UPDATED_LIST)) {
				values.put(Plays.UPDATED_LIST, play.updated);
			}
			batch.add(ContentProviderOperation.newInsert(Plays.CONTENT_URI).withValues(values).build());
		}

		updateOrInsertItem(play, itemObjectIds, batch);
		updateOrInsertPlayers(play, playerUserIds, batch);
		removeUnusedItems(play, itemObjectIds, batch);
		removeUnusedPlayers(play, playerUserIds, batch);
		updateGameSortOrder(resolver, play);
		updateColors(resolver, play);
		updateBuddyNicknames(resolver, play);

		ResolverUtils.applyBatch(context, batch);
		LOGI(TAG, "Saved play ID=" + play.playId);
		return status;
	}

	/*
	 * Gets an ID to use as a temporary placeholder until the game is synced with the 'Geek.
	 */
	private static int getTemporaryId(ContentResolver resolver) {
		int id = Play.UNSYNCED_PLAY_ID;
		int lastId = ResolverUtils.queryInt(resolver, Plays.CONTENT_SIMPLE_URI, "MAX(plays." + Plays.PLAY_ID + ")");
		if (lastId >= id) {
			id = lastId + 1;
		}
		return id;
	}

	private static int determineStatus(ContentResolver resolver, Play play, boolean isSyncing) {
		int status = STATUS_UNKNOWN;
		if (playExistsInDatabase(resolver, play)) {
			if (isSyncing) {
				// don't replace the play in the database if there are unsynced changes
				int currentSyncStatus = getCurrentSyncStatus(resolver, play);
				if (currentSyncStatus != Play.SYNC_STATUS_SYNCED) {
					if (currentSyncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
						status = STATUS_IN_PROGRESS;
					} else if (currentSyncStatus == Play.SYNC_STATUS_PENDING_UPDATE) {
						status = STATUS_PENDING_UPDATE;
					} else if (currentSyncStatus == Play.SYNC_STATUS_PENDING_DELETE) {
						status = STATUS_PENDING_DELETE;
					} else if (currentSyncStatus == Play.SYNC_STATUS_NOT_STORED) {
						status = STATUS_IN_PROGRESS;
					} else {
						status = STATUS_ERROR;
						LOGE(TAG, "Unknown sync status!");
					}
					LOGI(TAG, "Not saving during the sync due to status=" + status);
				} else {
					status = STATUS_UPDATE;
				}
			} else {
				status = STATUS_UPDATE;
			}
		} else {
			status = STATUS_INSERT;
		}
		return status;
	}

	private static void deletePlayerWithNullUserId(Play play, ArrayList<ContentProviderOperation> batch) {
		batch.add(ContentProviderOperation.newDelete(play.playerUri())
			.withSelection(PlayPlayers.USER_ID + " IS NULL", null).build());
	}

	private static boolean playExistsInDatabase(ContentResolver resolver, Play play) {
		if (play.playId == BggContract.INVALID_ID) {
			return false;
		}

		return ResolverUtils.rowExists(resolver, play.uri());
	}

	private static int getCurrentSyncStatus(ContentResolver resolver, Play play) {
		if (play.playId == BggContract.INVALID_ID) {
			return Play.SYNC_STATUS_NOT_STORED;
		}
		return ResolverUtils.queryInt(resolver, play.uri(), Plays.SYNC_STATUS, Play.SYNC_STATUS_NOT_STORED);
	}

	private static ContentValues createContentValues(Play play) {
		ContentValues values = new ContentValues();
		values.put(Plays.DATE, play.getDate());
		values.put(Plays.QUANTITY, play.quantity);
		values.put(Plays.LENGTH, play.length);
		values.put(Plays.INCOMPLETE, play.Incomplete());
		values.put(Plays.NO_WIN_STATS, play.NoWinStats());
		values.put(Plays.LOCATION, play.location);
		values.put(Plays.COMMENTS, play.comments);
		values.put(Plays.PLAYER_COUNT, play.getPlayerCount());
		if (play.updated > 0) {
			values.put(Plays.UPDATED_LIST, play.updated);
		}
		values.put(Plays.SYNC_STATUS, play.syncStatus);
		// only store start time if there's no length
		values.put(Plays.START_TIME, play.length > 0 ? 0 : play.startTime);
		values.put(Plays.UPDATED, System.currentTimeMillis());
		return values;
	}

	private static List<Integer> removeDuplicatePlayerUserIds(Play play, List<Integer> ids,
		ArrayList<ContentProviderOperation> batch) {
		if (ids == null || ids.size() == 0) {
			return new ArrayList<Integer>();
		}

		List<Integer> uniqueIds = new ArrayList<Integer>();
		List<Integer> idsToDelete = new ArrayList<Integer>();

		for (int i = 0; i < ids.size(); i++) {
			Integer id = ids.get(i);
			if (id != null) {
				if (uniqueIds.contains(id)) {
					idsToDelete.add(id);
				} else {
					uniqueIds.add(id);
				}
			}
		}

		for (Integer id : idsToDelete) {
			batch.add(ContentProviderOperation.newDelete(play.playerUri())
				.withSelection(PlayPlayers.USER_ID + "=?", new String[] { String.valueOf(id) }).build());
			uniqueIds.remove(id);
		}

		return uniqueIds;
	}

	private static void updateOrInsertItem(Play play, List<Integer> itemObjectIds,
		ArrayList<ContentProviderOperation> batch) {
		int objectId = play.gameId;
		ContentValues values = new ContentValues();
		values.put(PlayItems.NAME, play.gameName);

		if (itemObjectIds != null && itemObjectIds.remove(Integer.valueOf(objectId))) {
			batch.add(ContentProviderOperation.newUpdate(play.itemIdUri()).withValues(values).build());
		} else {
			values.put(PlayItems.OBJECT_ID, objectId);
			batch.add(ContentProviderOperation.newInsert(play.itemUri()).withValues(values).build());
		}
	}

	private static void updateOrInsertPlayers(Play play, List<Integer> playerUserIds,
		ArrayList<ContentProviderOperation> batch) {
		ContentValues values = new ContentValues();
		for (Player player : play.getPlayers()) {

			int userId = player.userid;
			values.clear();
			values.put(PlayPlayers.USER_ID, userId);
			values.put(PlayPlayers.USER_NAME, player.username);
			values.put(PlayPlayers.NAME, player.name);
			values.put(PlayPlayers.START_POSITION, player.getStartingPosition());
			values.put(PlayPlayers.COLOR, player.color);
			values.put(PlayPlayers.SCORE, player.score);
			values.put(PlayPlayers.NEW, player.New());
			values.put(PlayPlayers.RATING, player.rating);
			values.put(PlayPlayers.WIN, player.Win());

			if (playerUserIds != null && playerUserIds.remove(Integer.valueOf(userId))) {
				batch.add(ContentProviderOperation.newUpdate(play.playerUri())
					.withSelection(PlayPlayers.USER_ID + "=?", new String[] { String.valueOf(userId) })
					.withValues(values).build());
			} else {
				values.put(PlayPlayers.USER_ID, userId);
				batch.add(ContentProviderOperation.newInsert(play.playerUri()).withValues(values).build());
			}
		}
	}

	private static void removeUnusedItems(Play play, List<Integer> itemObjectIds,
		ArrayList<ContentProviderOperation> batch) {
		if (itemObjectIds != null) {
			for (Integer itemObjectId : itemObjectIds) {
				batch.add(ContentProviderOperation.newDelete(Plays.buildItemUri(play.playId, itemObjectId)).build());
			}
		}
	}

	private static void removeUnusedPlayers(Play play, List<Integer> playerUserIds,
		ArrayList<ContentProviderOperation> batch) {
		if (playerUserIds != null) {
			for (Integer playerUserId : playerUserIds) {
				batch.add(ContentProviderOperation.newDelete(play.playerUri())
					.withSelection(PlayPlayers.USER_ID + "=?", new String[] { String.valueOf(playerUserId) }).build());
			}
		}
	}

	private static void updateGameSortOrder(ContentResolver resolver, Play play) {
		if (play.getPlayerCount() == 0) {
			return;
		}

		Uri gameUri = Games.buildGameUri(play.gameId);
		if (!ResolverUtils.rowExists(resolver, gameUri)) {
			return;
		}

		ContentValues values = new ContentValues(1);
		values.put(Games.CUSTOM_PLAYER_SORT, play.arePlayersCustomSorted() ? 1 : 0);
		resolver.update(gameUri, values, null, null);
	}

	/**
	 * Add the current players' team/colors to the permanent list for the game.
	 */
	private static void updateColors(ContentResolver resolver, Play play) {
		if (play.getPlayerCount() == 0) {
			return;
		}
		if (!ResolverUtils.rowExists(resolver, Games.buildGameUri(play.gameId))) {
			return;
		}

		List<ContentValues> values = new ArrayList<ContentValues>();
		for (Player player : play.getPlayers()) {
			String color = player.color;
			if (!TextUtils.isEmpty(color)) {
				ContentValues cv = new ContentValues();
				cv.put(GameColors.COLOR, color);
				values.add(cv);
			}
		}
		if (values.size() > 0) {
			ContentValues[] array = {};
			resolver.bulkInsert(Games.buildColorsUri(play.gameId), values.toArray(array));
		}
	}

	/**
	 * Update GeekBuddies' nicknames with the names used here.
	 */
	private static void updateBuddyNicknames(ContentResolver resolver, Play play) {
		if (play.getPlayers().size() > 0) {
			for (Player player : play.getPlayers()) {
				if (!TextUtils.isEmpty(player.username) && !TextUtils.isEmpty(player.name)) {
					ContentValues values = new ContentValues();
					values.put(Buddies.PLAY_NICKNAME, player.name);
					resolver.update(Buddies.CONTENT_URI, values, Buddies.BUDDY_NAME + "=?",
						new String[] { player.username });
				}
			}
		}
	}
}
