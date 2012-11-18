package com.boardgamegeek.database;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
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

	private ContentResolver mResolver;
	private ArrayList<ContentProviderOperation> mBatch;
	private Play mPlay;
	private List<Integer> mPlayerUserIds;
	private List<Integer> mItemObjectIds;

	public PlayPersister(ContentResolver resolver, Play play) {
		mResolver = resolver;
		mPlay = play;
	}

	/*
	 * Gets an ID to use as a temporary placeholder until the game is synced with the 'Geek.
	 */
	public int getTemporaryId() {
		int id = Play.UNSYNCED_PLAY_ID;
		Cursor cursor = null;
		try {
			cursor = mResolver.query(Plays.CONTENT_URI, new String[] { "MAX(plays." + Plays.PLAY_ID + ")" }, null,
				null, null);
			if (cursor.moveToFirst()) {
				int lastId = cursor.getInt(0);
				if (lastId >= id) {
					id = lastId + 1;
				}
			}
			return id;
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	/*
	 * Delete the play from the content provider.
	 */
	public boolean delete() {
		return mResolver.delete(mPlay.uri(), null, null) > 0;
	}

	/*
	 * Save the play while not syncing.
	 */
	public void save() {
		save(false);
	}

	/*
	 * Save the play. If syncing, the play will not be saved if an edit is in progress.
	 */
	public int save(boolean isSyncing) {
		int status = determineStatus(isSyncing);

		if (status != STATUS_UPDATE && status != STATUS_INSERT) {
			return status;
		}

		mBatch = new ArrayList<ContentProviderOperation>();
		ContentValues values = createContentValues();

		if (status == STATUS_UPDATE) {
			deletePlayerWithNullUserId();
			mPlayerUserIds = ResolverUtils.queryInts(mResolver, mPlay.playerUri(), PlayPlayers.USER_ID);
			mPlayerUserIds = removeDuplicatePlayerIds(mPlay.PlayId, mPlayerUserIds);
			mItemObjectIds = ResolverUtils.queryInts(mResolver, mPlay.itemUri(), PlayItems.OBJECT_ID);
			mBatch.add(ContentProviderOperation.newUpdate(mPlay.uri()).withValues(values).build());
		} else if (status == STATUS_INSERT) {
			if (!mPlay.hasBeenSynced()) {
				// If a sync isn't pending, mark it as in progress
				if (mPlay.SyncStatus != Play.SYNC_STATUS_PENDING_UPDATE) {
					mPlay.SyncStatus = Play.SYNC_STATUS_IN_PROGRESS;
				}
			}
			values.put(Plays.SYNC_STATUS, mPlay.SyncStatus);

			if (mPlay.PlayId == 0) {
				mPlay.PlayId = getTemporaryId();
			}
			values.put(Plays.PLAY_ID, mPlay.PlayId);

			if (!values.containsKey(Plays.UPDATED_LIST)) {
				values.put(Plays.UPDATED_LIST, mPlay.Updated);
			}

			mBatch.add(ContentProviderOperation.newInsert(Plays.CONTENT_URI).withValues(values).build());
		}

		updateOrInsertItem();
		updateOrInsertPlayers();
		removeUnusedItems();
		removeUnusedPlayers();
		updateColors();
		updateBuddyNicknames();

		ResolverUtils.applyBatch(mResolver, mBatch);
		LOGI(TAG, "Saved play ID=" + mPlay.PlayId);
		return status;
	}

	private int determineStatus(boolean isSyncing) {
		int status = STATUS_UNKNOWN;
		if (playExistsInDatabase()) {
			if (isSyncing) {
				// don't replace the play in the database if there are unsynced changes
				int currentSyncStatus = getCurrentSyncStatus();
				if (currentSyncStatus != Play.SYNC_STATUS_SYNCED) {
					if (currentSyncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
						status = STATUS_IN_PROGRESS;
					} else if (currentSyncStatus == Play.SYNC_STATUS_PENDING_UPDATE) {
						status = STATUS_PENDING_UPDATE;
					} else if (currentSyncStatus == Play.SYNC_STATUS_PENDING_DELETE) {
						status = STATUS_PENDING_DELETE;
					} else {
						status = STATUS_ERROR;
						LOGE(TAG, "Unknown sync status!");
					}
					LOGI(TAG, "Not saving during the sync due to status=" + status);
				}
			}
			status = STATUS_UPDATE;
		} else {
			status = STATUS_INSERT;
		}
		return status;
	}

	private void deletePlayerWithNullUserId() {
		mBatch.add(ContentProviderOperation.newDelete(mPlay.playerUri())
			.withSelection(PlayPlayers.USER_ID + " IS NULL", null).build());
	}

	private boolean playExistsInDatabase() {
		if (mPlay.PlayId == 0) {
			return false;
		}

		Cursor cursor = null;
		try {
			cursor = mResolver.query(mPlay.uri(), new String[] { BaseColumns._ID }, null, null, null);
			return cursor.getCount() > 0;
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private int getCurrentSyncStatus() {
		if (mPlay.PlayId == 0) {
			return Play.SYNC_STATUS_NOT_STORED;
		}

		Cursor cursor = null;
		try {
			cursor = mResolver.query(mPlay.uri(), new String[] { Plays.SYNC_STATUS }, null, null, null);
			if (cursor.moveToFirst()) {
				return cursor.getInt(0);
			}
			return Play.SYNC_STATUS_NOT_STORED;
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private ContentValues createContentValues() {
		ContentValues values = new ContentValues();
		values.put(Plays.DATE, mPlay.getDate());
		values.put(Plays.QUANTITY, mPlay.Quantity);
		values.put(Plays.LENGTH, mPlay.Length);
		values.put(Plays.INCOMPLETE, mPlay.Incomplete);
		values.put(Plays.NO_WIN_STATS, mPlay.NoWinStats);
		values.put(Plays.LOCATION, mPlay.Location);
		values.put(Plays.COMMENTS, mPlay.Comments);
		if (mPlay.Updated > 0) {
			values.put(Plays.UPDATED_LIST, mPlay.Updated);
		}
		values.put(Plays.SYNC_STATUS, mPlay.SyncStatus);
		values.put(Plays.UPDATED, System.currentTimeMillis());
		return values;
	}

	private List<Integer> removeDuplicatePlayerIds(int playId, List<Integer> ids) {
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
			mBatch.add(ContentProviderOperation.newDelete(mPlay.playerUri())
				.withSelection(PlayPlayers.USER_ID + "=?", new String[] { String.valueOf(id) }).build());
			uniqueIds.remove(id);
		}

		return uniqueIds;
	}

	private void updateOrInsertItem() {
		int objectId = mPlay.GameId;
		ContentValues values = new ContentValues();
		values.put(PlayItems.NAME, mPlay.GameName);

		if (mItemObjectIds != null && mItemObjectIds.remove(Integer.valueOf(objectId))) {
			mBatch.add(ContentProviderOperation.newUpdate(mPlay.itemIdUri()).withValues(values).build());
		} else {
			values.put(PlayItems.OBJECT_ID, objectId);
			mBatch.add(ContentProviderOperation.newInsert(mPlay.itemUri()).withValues(values).build());
		}
	}

	private void updateOrInsertPlayers() {
		ContentValues values = new ContentValues();
		for (Player player : mPlay.getPlayers()) {

			int userId = player.UserId;
			values.clear();
			values.put(PlayPlayers.USER_ID, userId);
			values.put(PlayPlayers.USER_NAME, player.Username);
			values.put(PlayPlayers.NAME, player.Name);
			values.put(PlayPlayers.START_POSITION, player.StartingPosition);
			values.put(PlayPlayers.COLOR, player.TeamColor);
			values.put(PlayPlayers.SCORE, player.Score);
			values.put(PlayPlayers.NEW, player.New);
			values.put(PlayPlayers.RATING, player.Rating);
			values.put(PlayPlayers.WIN, player.Win);

			if (mPlayerUserIds != null && mPlayerUserIds.remove(Integer.valueOf(userId))) {
				mBatch.add(ContentProviderOperation.newUpdate(mPlay.playerUri())
					.withSelection(PlayPlayers.USER_ID + "=?", new String[] { String.valueOf(userId) })
					.withValues(values).build());
			} else {
				values.put(PlayPlayers.USER_ID, userId);
				mBatch.add(ContentProviderOperation.newInsert(mPlay.playerUri()).withValues(values).build());
			}
		}
	}

	private void removeUnusedItems() {
		if (mItemObjectIds != null) {
			for (Integer itemObjectId : mItemObjectIds) {
				mBatch.add(ContentProviderOperation.newDelete(Plays.buildItemUri(mPlay.PlayId, itemObjectId)).build());
			}
		}
	}

	private void removeUnusedPlayers() {
		if (mPlayerUserIds != null) {
			for (Integer playerUserId : mPlayerUserIds) {
				mBatch.add(ContentProviderOperation.newDelete(mPlay.playerUri())
					.withSelection(PlayPlayers.USER_ID + "=?", new String[] { String.valueOf(playerUserId) }).build());
			}
		}
	}

	/**
	 * Add the current players' team/colors to the permanent list.
	 */
	private void updateColors() {
		if (mPlay.getPlayers().size() > 0) {
			List<ContentValues> values = new ArrayList<ContentValues>();
			for (Player player : mPlay.getPlayers()) {
				String color = player.TeamColor;
				if (!TextUtils.isEmpty(color)) {
					ContentValues cv = new ContentValues();
					cv.put(GameColors.COLOR, player.TeamColor);
					values.add(cv);
				}
			}
			if (values.size() > 0) {
				ContentValues[] array = {};
				mResolver.bulkInsert(Games.buildColorsUri(mPlay.GameId), values.toArray(array));
			}
		}
	}

	/**
	 * Update Geek buddies' nicknames with the names used here.
	 */
	private void updateBuddyNicknames() {
		if (mPlay.getPlayers().size() > 0) {
			for (Player player : mPlay.getPlayers()) {
				if (!TextUtils.isEmpty(player.Username) && !TextUtils.isEmpty(player.Name)) {
					ContentValues values = new ContentValues();
					values.put(Buddies.PLAY_NICKNAME, player.Name);
					mResolver.update(Buddies.CONTENT_URI, values, Buddies.BUDDY_NAME + "=?",
						new String[] { player.Username });
				}
			}
		}
	}
}
