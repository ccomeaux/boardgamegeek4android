package com.boardgamegeek.model.persister;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Play.Subtype;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PlayPersister {
	private final Context context;
	private final ContentResolver resolver;
	private final ArrayList<ContentProviderOperation> batch;

	public PlayPersister(Context context) {
		this.context = context;
		resolver = context.getContentResolver();
		batch = new ArrayList<>();
	}

	/*
	 * Delete the play from the content provider.
	 */
	public boolean delete(long internalId) {
		return resolver.delete(Plays.buildPlayUri(internalId), null, null) > 0;
	}

	public void save(List<Play> plays, long startTime) {
		int updateCount = 0;
		int insertCount = 0;
		int unchangedCount = 0;
		int dirtyCount = 0;
		if (plays != null) {
			for (Play play : plays) {
				play.updated = startTime;
				PlaySyncCandidate candidate = PlaySyncCandidate.find(resolver, play.playId);
				if (candidate.getInternalId() == BggContract.INVALID_ID) {
					insert(play);
					insertCount++;
				} else {
					if (candidate.getDirtyTimestamp() > 0) {
						Timber.i("Not saving during the sync; modification in progress.");
						dirtyCount++;
					} else if (candidate.getDeleteTimestamp() > 0) {
						Timber.i("Not saving during the sync; set to delete.");
						dirtyCount++;
					} else if (candidate.getUpdateTimestamp() > 0) {
						Timber.i("Not saving during the sync; set to update.");
						dirtyCount++;
					} else if (candidate.getSyncHashCode() == generateSyncHashCode(play)) {
						updateSyncTimestamp(candidate.getInternalId(), startTime);
						unchangedCount++;
					} else {
						update(play, candidate.getInternalId());
						updateCount++;
					}
				}
			}
		}

		Timber.i("Updated %1$s, inserted %2$s, %3$s unchanged, %4$s dirty", updateCount, insertCount, unchangedCount, dirtyCount);
	}

	/*
	 * Save the play while not syncing.
	 */
	public void save(Play play) {
		PlaySyncCandidate candidate = PlaySyncCandidate.find(resolver, play.playId);
		save(play, candidate.getInternalId());
	}

	private void insert(Play play) {
		save(play, BggContract.INVALID_ID);
	}

	private void update(Play play, long internalId) {
		save(play, internalId);
	}

	private void save(Play play, long internalId) {
		if (play == null) return;
		if (!isBoardgameSubtype(play)) return;

		batch.clear();
		ContentValues values = createContentValues(play);

		String debugMessage;
		if (internalId != BggContract.INVALID_ID) {
			debugMessage = "Updating play _ID " + internalId;
			batch.add(ContentProviderOperation
				.newUpdate(Plays.buildPlayUri(internalId))
				.withValues(values)
				.build());
		} else {
			debugMessage = "Inserting new play";
			batch.add(ContentProviderOperation
				.newInsert(Plays.CONTENT_URI)
				.withValues(values)
				.build());
		}

		// Players
		deletePlayerWithEmptyUserNameInBatch(internalId);
		List<String> existingPlayerIds = removeDuplicateUserNamesFromBatch(internalId);
		addPlayersToBatch(play, existingPlayerIds, internalId);
		removeUnusedPlayersFromBatch(internalId, existingPlayerIds);

		if (play.playId > 0 || play.updateTimestamp > 0) {
			saveGamePlayerSortOrderToBatch(play);
			updateColorsInBatch(play);
			saveBuddyNicknamesToBatch(play);
		}

		ResolverUtils.applyBatch(context, batch, debugMessage);
		Timber.i("Saved play ID=%s", play.playId);
	}

	private void updateSyncTimestamp(long internalId, long startTime) {
		batch.clear();
		ContentProviderOperation.Builder builder = ContentProviderOperation
			.newUpdate(Plays.buildPlayUri(internalId))
			.withValue(Plays.UPDATED_LIST, startTime);
		batch.add(builder.build());
		ResolverUtils.applyBatch(context, batch);
	}

	private boolean isBoardgameSubtype(Play play) {
		if (play.subtypes == null || play.subtypes.isEmpty()) {
			return true;
		}
		for (Subtype subtype : play.subtypes) {
			if (subtype.value.startsWith("boardgame")) {
				return true;
			}
		}
		return false;
	}

	private static int generateSyncHashCode(Play play) {
		StringBuilder sb = new StringBuilder();
		sb.append(play.getDate()).append("\n");
		sb.append(play.quantity).append("\n");
		sb.append(play.length).append("\n");
		sb.append(play.Incomplete()).append("\n");
		sb.append(play.NoWinStats()).append("\n");
		sb.append(play.location).append("\n");
		sb.append(play.comments).append("\n");
		for (Player player : play.getPlayers()) {
			sb.append(player.username).append("\n");
			sb.append(player.userid).append("\n");
			sb.append(player.name).append("\n");
			sb.append(player.startposition).append("\n");
			sb.append(player.color).append("\n");
			sb.append(player.score).append("\n");
			sb.append(player.New()).append("\n");
			sb.append(player.rating).append("\n");
			sb.append(player.Win()).append("\n");
		}
		return sb.toString().hashCode();
	}

	private static ContentValues createContentValues(Play play) {
		ContentValues values = new ContentValues();
		values.put(Plays.DATE, play.getDate());
		values.put(Plays.ITEM_NAME, play.gameName);
		values.put(Plays.OBJECT_ID, play.gameId);
		values.put(Plays.QUANTITY, play.quantity);
		values.put(Plays.LENGTH, play.length);
		values.put(Plays.INCOMPLETE, play.Incomplete());
		values.put(Plays.NO_WIN_STATS, play.NoWinStats());
		values.put(Plays.LOCATION, play.location);
		values.put(Plays.COMMENTS, play.comments);
		values.put(Plays.PLAYER_COUNT, play.getPlayerCount());
		values.put(Plays.UPDATED_LIST, play.updated);
		values.put(Plays.START_TIME, play.length > 0 ? 0 : play.startTime); // only store start time if there's no length
		values.put(Plays.SYNC_HASH_CODE, generateSyncHashCode(play));
		values.put(Plays.DELETE_TIMESTAMP, play.deleteTimestamp);
		values.put(Plays.UPDATE_TIMESTAMP, play.updateTimestamp);
		values.put(Plays.DIRTY_TIMESTAMP, play.dirtyTimestamp);
		return values;
	}

	private void deletePlayerWithEmptyUserNameInBatch(long internalId) {
		if (internalId == BggContract.INVALID_ID) return;
		batch.add(ContentProviderOperation
			.newDelete(Plays.buildPlayerUri(internalId))
			.withSelection(String.format("%1$s IS NULL OR %1$s=''", PlayPlayers.USER_NAME), null)
			.build());
	}

	private List<String> removeDuplicateUserNamesFromBatch(long internalId) {
		if (internalId == BggContract.INVALID_ID) return new ArrayList<>(0);
		List<String> userNames = ResolverUtils.queryStrings(resolver, Plays.buildPlayerUri(internalId), PlayPlayers.USER_NAME);

		if (userNames == null || userNames.size() == 0) {
			return new ArrayList<>();
		}

		List<String> uniqueUserNames = new ArrayList<>();
		List<String> userNamesToDelete = new ArrayList<>();

		for (int i = 0; i < userNames.size(); i++) {
			String userName = userNames.get(i);
			if (!TextUtils.isEmpty(userName)) {
				if (uniqueUserNames.contains(userName)) {
					userNamesToDelete.add(userName);
				} else {
					uniqueUserNames.add(userName);
				}
			}
		}

		for (String userName : userNamesToDelete) {
			batch.add(ContentProviderOperation
				.newDelete(Plays.buildPlayerUri(internalId))
				.withSelection(PlayPlayers.USER_NAME + "=?", new String[] { userName })
				.build());
			uniqueUserNames.remove(userName);
		}

		return uniqueUserNames;
	}

	private void addPlayersToBatch(Play play, List<String> playerUserNames, long internalId) {
		for (Player player : play.getPlayers()) {

			String userName = player.username;
			ContentValues values = new ContentValues();
			values.put(PlayPlayers.USER_ID, player.userid);
			values.put(PlayPlayers.USER_NAME, userName);
			values.put(PlayPlayers.NAME, player.name);
			values.put(PlayPlayers.START_POSITION, player.getStartingPosition());
			values.put(PlayPlayers.COLOR, player.color);
			values.put(PlayPlayers.SCORE, player.score);
			values.put(PlayPlayers.NEW, player.New());
			values.put(PlayPlayers.RATING, player.rating);
			values.put(PlayPlayers.WIN, player.Win());

			if (playerUserNames != null && playerUserNames.remove(userName)) {
				batch.add(ContentProviderOperation
					.newUpdate(Plays.buildPlayerUri(internalId))
					.withSelection(PlayPlayers.USER_NAME + "=?", new String[] { userName })
					.withValues(values).build());
			} else {
				values.put(PlayPlayers.USER_NAME, userName);
				if (internalId == BggContract.INVALID_ID) {
					batch.add(ContentProviderOperation
						.newInsert(Plays.buildPlayerUri())
						.withValueBackReference(PlayPlayers._PLAY_ID, 0)
						.withValues(values)
						.build());
				} else {
					batch.add(ContentProviderOperation
						.newInsert(Plays.buildPlayerUri(internalId))
						.withValues(values)
						.build());
				}
			}
		}
	}

	private void removeUnusedPlayersFromBatch(long internalId, List<String> playerUserNames) {
		if (internalId == BggContract.INVALID_ID) return;
		if (playerUserNames != null) {
			for (String playerUserName : playerUserNames) {
				batch.add(ContentProviderOperation
					.newDelete(Plays.buildPlayerUri(internalId))
					.withSelection(PlayPlayers.USER_NAME + "=?", new String[] { playerUserName })
					.build());
			}
		}
	}

	/**
	 * Determine if the players are customer sorted or not, and save it to the game.
	 */
	private void saveGamePlayerSortOrderToBatch(Play play) {
		// We can't determine the sort order without players
		if (play.getPlayerCount() == 0) return;

		// We can't save the sort order if we aren't storing the game
		Uri gameUri = Games.buildGameUri(play.gameId);
		if (!ResolverUtils.rowExists(resolver, gameUri)) return;

		batch.add(ContentProviderOperation
			.newUpdate(gameUri)
			.withValue(Games.CUSTOM_PLAYER_SORT, play.arePlayersCustomSorted())
			.build());
	}

	/**
	 * Add the current players' team/colors to the permanent list for the game.
	 */
	private void updateColorsInBatch(Play play) {
		// There are no players, so there are no colors to save
		if (play.getPlayerCount() == 0) return;

		// We can't save the colors if we aren't storing the game
		if (!ResolverUtils.rowExists(resolver, Games.buildGameUri(play.gameId))) return;

		for (Player player : play.getPlayers()) {
			String color = player.color;
			if (!TextUtils.isEmpty(color)) {
				if (!ResolverUtils.rowExists(resolver, Games.buildColorsUri(play.gameId, color))) {
					batch.add(ContentProviderOperation
						.newInsert(Games.buildColorsUri(play.gameId))
						.withValue(GameColors.COLOR, color)
						.build());
				}
			}
		}
	}

	/**
	 * Update GeekBuddies' nicknames with the names used here.
	 */
	private void saveBuddyNicknamesToBatch(Play play) {
		// There are no players, so there are no nicknames to save
		if (play.getPlayerCount() == 0) return;

		for (Player player : play.getPlayers()) {
			if (!TextUtils.isEmpty(player.username) && !TextUtils.isEmpty(player.name)) {
				batch.add(ContentProviderOperation
					.newUpdate(Buddies.CONTENT_URI)
					.withSelection(Buddies.BUDDY_NAME + "=?", new String[] { player.username })
					.withValue(Buddies.PLAY_NICKNAME, player.name)
					.build());
			}
		}
	}

	static class PlaySyncCandidate {
		public static final PlaySyncCandidate NULL = new PlaySyncCandidate() {
			@Override
			public long getInternalId() {
				return BggContract.INVALID_ID;
			}

			@Override
			public int getSyncHashCode() {
				return 0;
			}

			@Override
			public long getDeleteTimestamp() {
				return 0;
			}

			@Override
			public long getDirtyTimestamp() {
				return 0;
			}

			@Override
			public long getUpdateTimestamp() {
				return 0;
			}
		};

		public static final String[] PROJECTION = {
			Plays._ID,
			Plays.SYNC_HASH_CODE,
			Plays.DELETE_TIMESTAMP,
			Plays.UPDATE_TIMESTAMP,
			Plays.DIRTY_TIMESTAMP
		};

		private long internalId;
		private int syncHashCode;
		private long deleteTimestamp;
		private long updateTimestamp;
		private long dirtyTimestamp;

		public static PlaySyncCandidate find(ContentResolver resolver, int playId) {
			Cursor cursor = resolver.query(Plays.CONTENT_URI,
				PROJECTION,
				Plays.PLAY_ID + "=?",
				new String[] { String.valueOf(playId) },
				null);
			try {
				if (cursor != null && cursor.moveToFirst()) {
					return fromCursor(cursor);
				}
				return NULL;
			} finally {
				if (cursor != null) cursor.close();
			}

		}

		public static PlaySyncCandidate fromCursor(Cursor cursor) {
			PlaySyncCandidate psc = new PlaySyncCandidate();
			psc.internalId = CursorUtils.getLong(cursor, Plays._ID, BggContract.INVALID_ID);
			psc.syncHashCode = CursorUtils.getInt(cursor, Plays.SYNC_HASH_CODE);
			psc.deleteTimestamp = CursorUtils.getLong(cursor, Plays.DELETE_TIMESTAMP);
			psc.updateTimestamp = CursorUtils.getLong(cursor, Plays.UPDATE_TIMESTAMP);
			psc.dirtyTimestamp = CursorUtils.getLong(cursor, Plays.DIRTY_TIMESTAMP);
			return psc;
		}

		public long getInternalId() {
			return internalId;
		}

		public int getSyncHashCode() {
			return syncHashCode;
		}

		public long getDeleteTimestamp() {
			return deleteTimestamp;
		}

		public long getUpdateTimestamp() {
			return updateTimestamp;
		}

		public long getDirtyTimestamp() {
			return dirtyTimestamp;
		}
	}
}
