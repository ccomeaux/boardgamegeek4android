package com.boardgamegeek.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlayHelper {
	private static final String TAG = "PlayHelper";

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
	 * The play is pending a sync, therefore wasn't saved
	 */
	public static final int STATUS_PENDING = 4;
	/**
	 * The save status is unknown, most likely because it hasn't finished executing yet.
	 */
	public static final int STATUS_UNKNOWN = 0;
	/**
	 * An unexpected error occurred while trying to sync
	 */
	public static final int STATUS_ERROR = -1;

	private ContentResolver mResolver;
	private Play mPlay;
	private List<Integer> mPlayerUserIds;
	private List<Integer> mItemObjectIds;
	private int mStatus;

	public PlayHelper(ContentResolver resolver, Play play) {
		mResolver = resolver;
		mPlay = play;
	}

	/**
	 * Get the status.
	 * 
	 * @return A STATUS indicating the results of the save operation.
	 */
	public int getStatus() {
		return mStatus;
	}

	public boolean delete() {
		return mResolver.delete(mPlay.getUri(), null, null) > 0;
	}

	public void save() {
		save(false);
	}

	public void save(boolean isSyncing) {
		mStatus = STATUS_UNKNOWN;
		if (playExistsInDatabase()) {
			if (isSyncing) {
				// don't replace the play in the database if there are unsynced changes
				int currentSyncStatus = getCurrentSyncStatus();
				if (currentSyncStatus != Play.SYNC_STATUS_SYNCED) {
					if (currentSyncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
						mStatus = STATUS_IN_PROGRESS;
					} else if (currentSyncStatus == Play.SYNC_STATUS_PENDING) {
						mStatus = STATUS_PENDING;
					} else {
						mStatus = STATUS_ERROR;
						Log.e(TAG, "Unknown sync status!");
					}
					Log.i(TAG, "Not saving during the sync due to status=" + mStatus);
					return;
				}
			}
			mStatus = STATUS_UPDATE;

			mItemObjectIds = getIds(Plays.buildItemUri(mPlay.PlayId), PlayItems.OBJECT_ID);

			mResolver.delete(Plays.buildPlayerUri(mPlay.PlayId), PlayPlayers.USER_ID + " IS NULL", null);
			mPlayerUserIds = getIds(Plays.buildPlayerUri(mPlay.PlayId), PlayPlayers.USER_ID);
			mPlayerUserIds = removeDuplicatePlayerIds(mPlay.PlayId, mPlayerUserIds);

			mResolver.update(mPlay.getUri(), getContentValues(), null, null);
		} else {
			mStatus = STATUS_INSERT;
			ContentValues values = getContentValues();

			if (mPlay.PlayId == 0) {
				mPlay.PlayId = getTemporaryId();
				// If a sync isn't pending, mark it as in progress
				if (mPlay.SyncStatus != Play.SYNC_STATUS_PENDING) {
					mPlay.SyncStatus = Play.SYNC_STATUS_IN_PROGRESS;
				}
			}
			values.put(Plays.PLAY_ID, mPlay.PlayId);

			if (!values.containsKey(Plays.UPDATED_LIST)) {
				values.put(Plays.UPDATED_LIST, mPlay.Updated);
			}

			mResolver.insert(Plays.CONTENT_URI, values);
		}

		updateOrInsertItem();
		updateOrInsertPlayers();
		removeUnusedItems();
		removeUnusedPlayers();

		Log.i(TAG, "Saved play ID=" + mPlay.PlayId);
	}

	private boolean playExistsInDatabase() {
		Cursor cursor = null;
		try {
			cursor = mResolver.query(mPlay.getUri(), new String[] { BaseColumns._ID }, null, null, null);
			return cursor.getCount() > 0;
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private int getCurrentSyncStatus() {
		Cursor cursor = null;
		try {
			cursor = mResolver.query(mPlay.getUri(), new String[] { Plays.SYNC_STATUS }, null, null, null);
			if (cursor.moveToFirst()) {
				return cursor.getInt(0);
			}
			return -1;
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private int getTemporaryId() {
		Cursor cursor = null;
		int id = Play.UNSYNCED_PLAY_ID;
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

	private ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(Plays.DATE, mPlay.getFormattedDate());
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

	private List<Integer> getIds(Uri uri, String columnName) {
		List<Integer> list = new ArrayList<Integer>();
		Cursor cursor = mResolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			while (cursor.moveToNext()) {
				list.add(cursor.getInt(0));
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return list;
	}

	private List<Integer> removeDuplicatePlayerIds(int playId, List<Integer> ids) {
		if (ids == null || ids.size() == 0) {
			return new ArrayList<Integer>();
		}

		List<Integer> uniqueIds = new ArrayList<Integer>();
		List<Integer> idsToDelete = new ArrayList<Integer>();

		for (int i = 0; i < ids.size(); i++) {
			Integer id = ids.get(i);
			if (uniqueIds.contains(id)) {
				idsToDelete.add(id);
			} else {
				uniqueIds.add(id);
			}
		}

		for (Integer id : idsToDelete) {
			mResolver.delete(Plays.buildPlayerUri(playId), PlayPlayers.USER_ID + "=?",
					new String[] { String.valueOf(id) });
			uniqueIds.remove(id);
		}

		return uniqueIds;
	}

	private void updateOrInsertItem() {
		int objectId = mPlay.GameId;
		ContentValues values = new ContentValues();
		values.put(PlayItems.NAME, mPlay.GameName);

		if (mItemObjectIds != null && mItemObjectIds.remove(Integer.valueOf(objectId))) {
			mResolver.update(Plays.buildItemUri(mPlay.PlayId, objectId), values, null, null);
		} else {
			values.put(PlayItems.OBJECT_ID, objectId);
			mResolver.insert(Plays.buildItemUri(mPlay.PlayId), values);
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
				mResolver.update(Plays.buildPlayerUri(mPlay.PlayId), values, PlayPlayers.USER_ID + "=?",
						new String[] { String.valueOf(userId) });
			} else {
				values.put(PlayPlayers.USER_ID, userId);
				mResolver.insert(Plays.buildPlayerUri(mPlay.PlayId), values);
			}
		}
	}

	private void removeUnusedItems() {
		if (mItemObjectIds != null) {
			for (Integer itemObjectId : mItemObjectIds) {
				mResolver.delete(Plays.buildItemUri(mPlay.PlayId, itemObjectId), null, null);
			}
		}
	}

	private void removeUnusedPlayers() {
		if (mPlayerUserIds != null) {
			for (Integer playerUserId : mPlayerUserIds) {
				mResolver.delete(Plays.buildPlayerUri(mPlay.PlayId), PlayPlayers.USER_ID + "=?",
						new String[] { String.valueOf(playerUserId) });
			}
		}
	}
}
