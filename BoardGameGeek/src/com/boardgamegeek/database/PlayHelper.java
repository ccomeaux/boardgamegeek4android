package com.boardgamegeek.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlayHelper {

	private ContentResolver mResolver;
	private Play mPlay;
	private List<Integer> mPlayerUserIds;
	private List<Integer> mItemObjectIds;
	private boolean mIsUpdate;

	public PlayHelper(ContentResolver resolver, Play play) {
		mResolver = resolver;
		mPlay = play;
	}

	public boolean getIsUpdate() {
		return mIsUpdate;
	}

	public void save() {
		mIsUpdate = isUpdate();
		if (mIsUpdate) {
			mItemObjectIds = getIds(Plays.buildItemUri(mPlay.PlayId), PlayItems.OBJECT_ID);

			mResolver.delete(Plays.buildPlayerUri(mPlay.PlayId), PlayPlayers.USER_ID + " IS NULL", null);
			mPlayerUserIds = getIds(Plays.buildPlayerUri(mPlay.PlayId), PlayPlayers.USER_ID);
			mPlayerUserIds = removeDuplicatePlayerIds(mPlay.PlayId, mPlayerUserIds);

			mResolver.update(mPlay.getUri(), getContentValues(), null, null);
		} else {
			ContentValues values = getContentValues();
			values.put(Plays.PLAY_ID, mPlay.PlayId);
			mResolver.insert(Plays.CONTENT_URI, values);
		}

		saveItem();
		savePlayers();
		removeUnusedItems();
		removeUnusedPlayers();
	}

	private boolean isUpdate() {
		Cursor cursor = null;
		try {
			cursor = mResolver.query(mPlay.getUri(), new String[] { BaseColumns._ID }, null, null, null);
			if (cursor.moveToFirst()) {
				return true;
			}
			return false;
		} finally {
			cursor.deactivate();
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
		values.put(Plays.UPDATED_LIST, System.currentTimeMillis());
		return values;
	}

	private List<Integer> getIds(Uri uri, String columnName) {
		List<Integer> list = new ArrayList<Integer>();
		Cursor c = mResolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			while (c.moveToNext()) {
				list.add(c.getInt(0));
			}
		} finally {
			if (c != null) {
				c.close();
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

	private void saveItem() {
		ContentValues values = new ContentValues();
		int objectId = mPlay.GameId;
		values.clear();
		values.put(PlayItems.NAME, mPlay.GameName);

		if (mItemObjectIds != null && mItemObjectIds.remove(new Integer(objectId))) {
			mResolver.update(Plays.buildItemUri(mPlay.PlayId, objectId), values, null, null);
		} else {
			values.put(PlayItems.OBJECT_ID, objectId);
			mResolver.insert(Plays.buildItemUri(mPlay.PlayId), values);
		}
	}

	private void savePlayers() {
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

			if (mPlayerUserIds != null && mPlayerUserIds.remove(new Integer(userId))) {
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
