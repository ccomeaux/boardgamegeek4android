package com.boardgamegeek.model.persister;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PlayPersister {
	/**
	 * An existing play was updated in the database
	 */
	private static final int STATUS_UPDATE = 2;
	/**
	 * The play is being edited, therefore wasn't saved
	 */
	private static final int STATUS_IN_PROGRESS = 3;
	/**
	 * The play is pending update, therefore wasn't saved
	 */
	private static final int STATUS_PENDING_UPDATE = 4;
	/**
	 * The play is pending delete, therefore wasn't saved
	 */
	private static final int STATUS_PENDING_DELETE = 5;
	/**
	 * The save status is unknown, most likely because it hasn't finished executing yet.
	 */
	private static final int STATUS_UNKNOWN = 0;
	/**
	 * An unexpected error occurred while trying to sync
	 */
	private static final int STATUS_ERROR = -1;
	/**
	 * This play is unchanged from what's currently in the database
	 */
	private static final int STATUS_UNCHANGED = -2;

	private final Context context;
	private final ContentResolver resolver;
	private ArrayList<ContentProviderOperation> batch;

	public PlayPersister(Context context) {
		this.context = context;
		resolver = context.getContentResolver();
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
		int errorCount = 0;
		if (plays != null) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			for (Play play : plays) {
				play.updated = startTime;
				long internalId = getInternalId(play);
				if (internalId == BggContract.INVALID_ID) {
					save(play, internalId);
					insertCount++;
				} else {
					int status = determineSyncStatus(play, internalId);
					switch (status) {
						case PlayPersister.STATUS_UPDATE:
							save(play, status);
							updateCount++;
							break;
						case PlayPersister.STATUS_UNCHANGED:
							ContentProviderOperation.Builder builder = ContentProviderOperation
								.newUpdate(Plays.buildPlayUri(internalId))
								.withValue(Plays.UPDATED, startTime)
								.withValue(Plays.UPDATED_LIST, startTime);
							batch.add(builder.build());
							unchangedCount++;
							break;
						case PlayPersister.STATUS_PENDING_UPDATE:
						case PlayPersister.STATUS_PENDING_DELETE:
						case PlayPersister.STATUS_IN_PROGRESS:
							dirtyCount++;
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
			ResolverUtils.applyBatch(context, batch);
		}
		Timber.i("Updated %1$s, inserted %2$s, %6$s unchanged, skipped %3$s (%4$s dirty, %5$s errors)",
			updateCount, insertCount, (dirtyCount + errorCount), dirtyCount, errorCount, unchangedCount);
	}

	/*
	 * Save the play while not syncing.
	 */
	public void save(Play play) {
		long internalId = getInternalId(play);
		save(play, internalId);
	}

	private void save(Play play, long internalId) {
		batch = new ArrayList<>();
		ContentValues values = createContentValues(play);
		List<Integer> playerUserIds = null;

		if (!shouldSave(play)) {
			return;
		}

		String debugMessage;
		if (internalId != BggContract.INVALID_ID) {
			debugMessage = "Updating play ID " + play.playId;
			deletePlayerWithNullUserId(play);
			playerUserIds = determineUniqueUserIds(play);
			batch.add(ContentProviderOperation.newUpdate(Plays.buildPlayUri(internalId)).withValues(values).build());
		} else {
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
				play.playId = getTemporaryId();
			}
			values.put(Plays.PLAY_ID, play.playId);
			debugMessage = "Inserting play ID " + play.playId;

			if (!values.containsKey(Plays.UPDATED_LIST)) {
				values.put(Plays.UPDATED_LIST, play.updated);
			}
			batch.add(ContentProviderOperation.newInsert(Plays.CONTENT_URI).withValues(values).build());
		}

		updateOrInsertPlayers(play, playerUserIds);
		removeUnusedPlayers(play, playerUserIds);
		if (play.syncStatus == Play.SYNC_STATUS_SYNCED || play.syncStatus == Play.SYNC_STATUS_PENDING_UPDATE) {
			updateGameSortOrder(play);
			updateColors(play);
			updateBuddyNicknames(play);
		}

		ResolverUtils.applyBatch(context, batch, debugMessage);
		Timber.i("Saved play ID=%s", play.playId);
	}

	private boolean shouldSave(Play play) {
		if (play == null) {
			return false;
		}
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

	/*
	 * Gets an ID to use as a temporary placeholder until the game is synced with the 'Geek.
	 */

	private int getTemporaryId() {
		int id = Play.UNSYNCED_PLAY_ID;
		int lastId = ResolverUtils.queryInt(resolver, Plays.CONTENT_SIMPLE_URI, "MAX(plays." + Plays.PLAY_ID + ")");
		if (lastId >= id) {
			id = lastId + 1;
		}
		return id;
	}

	private int determineSyncStatus(Play play, long internalId) {
		int status;
		int currentSyncStatus = getCurrentSyncStatus(internalId);
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
				Timber.e("Unknown sync status!");
			}
			Timber.i("Not saving during the sync due to status=%s", status);
		} else {
			int oldSyncHashCode = ResolverUtils.queryInt(resolver, Plays.buildPlayUri(internalId), Plays.SYNC_HASH_CODE);
			int newSyncHashCode = generateSyncHashCode(play);
			if (oldSyncHashCode == newSyncHashCode) {
				status = STATUS_UNCHANGED;
			} else {
				status = STATUS_UPDATE;
			}
		}
		return status;
	}

	private void deletePlayerWithNullUserId(Play play) {
		batch.add(ContentProviderOperation.newDelete(play.playerUri())
			.withSelection(PlayPlayers.USER_ID + " IS NULL", null).build());
	}

	private long getInternalId(Play play) {
		return ResolverUtils.queryLong(resolver,
			Plays.CONTENT_URI,
			Plays._ID,
			BggContract.INVALID_ID,
			Plays.PLAY_ID + "=?",
			new String[] { String.valueOf(play.playId) });
	}

	private int getCurrentSyncStatus(long internalId) {
		if (internalId == BggContract.INVALID_ID) return Play.SYNC_STATUS_NOT_STORED;
		return ResolverUtils.queryInt(resolver, Plays.buildPlayUri(internalId), Plays.SYNC_STATUS, Play.SYNC_STATUS_NOT_STORED);
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
		if (play.updated > 0) {
			values.put(Plays.UPDATED_LIST, play.updated);
		}
		values.put(Plays.SYNC_STATUS, play.syncStatus);
		// only store start time if there's no length
		values.put(Plays.START_TIME, play.length > 0 ? 0 : play.startTime);
		values.put(Plays.UPDATED, System.currentTimeMillis());
		values.put(Plays.SYNC_HASH_CODE, generateSyncHashCode(play));
		return values;
	}

	private List<Integer> determineUniqueUserIds(Play play) {
		List<Integer> ids = ResolverUtils.queryInts(resolver, play.playerUri(), PlayPlayers.USER_ID);

		if (ids == null || ids.size() == 0) {
			return new ArrayList<>();
		}

		List<Integer> uniqueIds = new ArrayList<>();
		List<Integer> idsToDelete = new ArrayList<>();

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

	private void updateOrInsertPlayers(Play play, List<Integer> playerUserIds) {
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

	private void removeUnusedPlayers(Play play, List<Integer> playerUserIds) {
		if (playerUserIds != null) {
			for (Integer playerUserId : playerUserIds) {
				batch.add(ContentProviderOperation.newDelete(play.playerUri())
					.withSelection(PlayPlayers.USER_ID + "=?", new String[] { String.valueOf(playerUserId) }).build());
			}
		}
	}

	private void updateGameSortOrder(Play play) {
		if (play.getPlayerCount() == 0) {
			return;
		}

		Uri gameUri = Games.buildGameUri(play.gameId);
		if (!ResolverUtils.rowExists(resolver, gameUri)) {
			return;
		}

		ContentValues values = new ContentValues(1);
		values.put(Games.CUSTOM_PLAYER_SORT, play.arePlayersCustomSorted());
		resolver.update(gameUri, values, null, null);
	}

	/**
	 * Add the current players' team/colors to the permanent list for the game.
	 */
	private void updateColors(Play play) {
		if (play.getPlayerCount() == 0) {
			return;
		}
		if (!ResolverUtils.rowExists(resolver, Games.buildGameUri(play.gameId))) {
			return;
		}

		List<ContentValues> values = new ArrayList<>();
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
	private void updateBuddyNicknames(Play play) {
		if (play.getPlayers().size() > 0) {
			for (Player player : play.getPlayers()) {
				if (!TextUtils.isEmpty(player.username) && !TextUtils.isEmpty(player.name)) {
					ContentValues values = new ContentValues();
					values.put(Buddies.PLAY_NICKNAME, player.name);
					resolver.update(Buddies.CONTENT_URI, values, Buddies.BUDDY_NAME + "=?", new String[] { player.username });
				}
			}
		}
	}
}
