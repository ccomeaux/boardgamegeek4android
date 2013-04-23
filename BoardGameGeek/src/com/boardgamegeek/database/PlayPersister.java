package com.boardgamegeek.database;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
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

	/*
	 * Save the play while not syncing.
	 */
	public static void save(ContentResolver resolver, Play play) {
		save(resolver, play, false);
	}

	/*
	 * Save the play. If syncing, the play will not be saved if it is a draft.
	 */
	public static int save(ContentResolver resolver, Play play, boolean isSyncing) {
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
				if (play.SyncStatus != Play.SYNC_STATUS_PENDING_UPDATE
					|| play.SyncStatus != Play.SYNC_STATUS_PENDING_DELETE) {
					play.SyncStatus = Play.SYNC_STATUS_IN_PROGRESS;
				}
			} else {
				play.SyncStatus = Play.SYNC_STATUS_SYNCED;
			}
			values.put(Plays.SYNC_STATUS, play.SyncStatus);

			if (play.PlayId == BggContract.INVALID_ID) {
				play.PlayId = getTemporaryId(resolver);
			}
			values.put(Plays.PLAY_ID, play.PlayId);

			if (!values.containsKey(Plays.UPDATED_LIST)) {
				values.put(Plays.UPDATED_LIST, play.Updated);
			}

			batch.add(ContentProviderOperation.newInsert(Plays.CONTENT_URI).withValues(values).build());
		}

		updateOrInsertItem(play, itemObjectIds, batch);
		updateOrInsertPlayers(play, playerUserIds, batch);
		removeUnusedItems(play, itemObjectIds, batch);
		removeUnusedPlayers(play, playerUserIds, batch);
		updateColors(resolver, play);
		updateBuddyNicknames(resolver, play);

		ResolverUtils.applyBatch(resolver, batch);
		LOGI(TAG, "Saved play ID=" + play.PlayId);
		return status;
	}

	/*
	 * Gets an ID to use as a temporary placeholder until the game is synced with the 'Geek.
	 */
	private static int getTemporaryId(ContentResolver resolver) {
		int id = Play.UNSYNCED_PLAY_ID;
		int lastId = ResolverUtils.queryInt(resolver, Plays.CONTENT_URI, "MAX(plays." + Plays.PLAY_ID + ")");
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
		if (play.PlayId == BggContract.INVALID_ID) {
			return false;
		}

		return ResolverUtils.rowExists(resolver, play.uri());
	}

	private static int getCurrentSyncStatus(ContentResolver resolver, Play play) {
		if (play.PlayId == BggContract.INVALID_ID) {
			return Play.SYNC_STATUS_NOT_STORED;
		}
		return ResolverUtils.queryInt(resolver, play.uri(), Plays.SYNC_STATUS, Play.SYNC_STATUS_NOT_STORED);
	}

	private static ContentValues createContentValues(Play play) {
		ContentValues values = new ContentValues();
		values.put(Plays.DATE, play.getDate());
		values.put(Plays.QUANTITY, play.Quantity);
		values.put(Plays.LENGTH, play.Length);
		values.put(Plays.INCOMPLETE, play.Incomplete);
		values.put(Plays.NO_WIN_STATS, play.NoWinStats);
		values.put(Plays.LOCATION, play.Location);
		values.put(Plays.COMMENTS, play.Comments);
		if (play.Updated > 0) {
			values.put(Plays.UPDATED_LIST, play.Updated);
		}
		values.put(Plays.SYNC_STATUS, play.SyncStatus);
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
		int objectId = play.GameId;
		ContentValues values = new ContentValues();
		values.put(PlayItems.NAME, play.GameName);

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

			int userId = player.UserId;
			values.clear();
			values.put(PlayPlayers.USER_ID, userId);
			values.put(PlayPlayers.USER_NAME, player.Username);
			values.put(PlayPlayers.NAME, player.Name);
			values.put(PlayPlayers.START_POSITION, player.getStartingPosition());
			values.put(PlayPlayers.COLOR, player.TeamColor);
			values.put(PlayPlayers.SCORE, player.Score);
			values.put(PlayPlayers.NEW, player.New);
			values.put(PlayPlayers.RATING, player.Rating);
			values.put(PlayPlayers.WIN, player.Win);

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
				batch.add(ContentProviderOperation.newDelete(Plays.buildItemUri(play.PlayId, itemObjectId)).build());
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

	/**
	 * Add the current players' team/colors to the permanent list.
	 */
	private static void updateColors(ContentResolver resolver, Play play) {
		if (!ResolverUtils.rowExists(resolver, BggContract.Games.buildGameUri(play.GameId))) {
			return;
		}

		if (play.getPlayers().size() > 0) {
			List<ContentValues> values = new ArrayList<ContentValues>();
			for (Player player : play.getPlayers()) {
				String color = player.TeamColor;
				if (!TextUtils.isEmpty(color)) {
					ContentValues cv = new ContentValues();
					cv.put(GameColors.COLOR, player.TeamColor);
					values.add(cv);
				}
			}
			if (values.size() > 0) {
				ContentValues[] array = {};
				resolver.bulkInsert(Games.buildColorsUri(play.GameId), values.toArray(array));
			}
		}
	}

	/**
	 * Update Geek buddies' nicknames with the names used here.
	 */
	private static void updateBuddyNicknames(ContentResolver resolver, Play play) {
		if (play.getPlayers().size() > 0) {
			for (Player player : play.getPlayers()) {
				if (!TextUtils.isEmpty(player.Username) && !TextUtils.isEmpty(player.Name)) {
					ContentValues values = new ContentValues();
					values.put(Buddies.PLAY_NICKNAME, player.Name);
					resolver.update(Buddies.CONTENT_URI, values, Buddies.BUDDY_NAME + "=?",
						new String[] { player.Username });
				}
			}
		}
	}
}
