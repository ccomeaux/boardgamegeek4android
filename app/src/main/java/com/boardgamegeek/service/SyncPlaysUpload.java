package com.boardgamegeek.service;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.PlayDeleteConverter;
import com.boardgamegeek.io.PlaySaveConverter;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.PlayPostResponse;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.PlaysActivity;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.List;
import java.util.Map;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class SyncPlaysUpload extends SyncUploadTask {
	private LocalBroadcastManager broadcastManager;
	private BggService bggSaveService;
	private BggService bggDeleteService;
	private PlayPersister persister;
	private Play currentPlayForMessage;

	@DebugLog
	public SyncPlaysUpload(Context context, BggService service) {
		super(context, service);
	}

	@DebugLog
	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_PLAYS_UPLOAD;
	}

	@DebugLog
	@Override
	protected int getNotificationTitleResId() {
		return R.string.sync_notification_title_play_upload;
	}

	@DebugLog
	@Override
	protected Class<?> getNotificationIntentClass() {
		return PlaysActivity.class;
	}

	@DebugLog
	@Override
	protected int getNotificationErrorId() {
		return NotificationUtils.ID_SYNC_PLAY_UPLOAD_ERROR;
	}

	@DebugLog
	@Override
	protected int getNotificationMessageId() {
		return NotificationUtils.ID_SYNC_PLAY_UPLOAD;
	}

	@DebugLog
	@StringRes
	@Override
	protected int getUploadSummaryWithSize() {
		return R.string.sync_notification_plays_upload_summary;
	}

	@DebugLog
	@Override
	public void execute(Account account, @NonNull SyncResult syncResult) {
		bggSaveService = Adapter.createForPost(context, new PlaySaveConverter());
		bggDeleteService = Adapter.createForPost(context, new PlayDeleteConverter());
		broadcastManager = LocalBroadcastManager.getInstance(context);
		persister = new PlayPersister(context);

		updatePendingPlays(syncResult);
		deletePendingPlays(syncResult);
		SyncService.hIndex(context);
	}

	@DebugLog
	@Override
	public int getNotification() {
		return R.string.sync_notification_plays_upload;
	}

	@DebugLog
	private void updatePendingPlays(@NonNull SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Plays.CONTENT_SIMPLE_URI,
				null,
				Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE) },
				null);
			String detail = String.format("Uploading %s play(s)", cursor != null ? cursor.getCount() : 0);
			Timber.i(detail);
			showNotification(detail);

			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = PlayBuilder.fromCursor(cursor, context, true);

				PlayPostResponse response = postPlayUpdate(play);
				if (!response.hasError()) {
					String message = play.hasBeenSynced() ?
						context.getString(R.string.msg_play_updated) :
						context.getString(R.string.msg_play_added, getPlayCountDescription(response.getPlayCount(), play.quantity));
					currentPlayForMessage = play;
					currentPlayForMessage.playId = response.getPlayId();
					notifyUser(StringUtils.boldSecondString(message, play.gameName));

					// delete the old plays
					int oldPlayId = play.playId;
					deletePlay(play);

					// then save play as a new record
					play.playId = response.getPlayId();
					play.syncStatus = Play.SYNC_STATUS_SYNCED;
					persister.save(context, play);

					PreferencesUtils.putNewPlayId(context, oldPlayId, play.playId);
					Intent intent = new Intent(SyncService.ACTION_PLAY_ID_CHANGED);
					broadcastManager.sendBroadcast(intent);

					updateGamePlayCount(play);
				} else if (response.hasInvalidIdError()) {
					notifyUser(StringUtils.boldSecondString(context.getString(R.string.msg_play_update_bad_id), String.valueOf(play.playId)));
				} else if (response.hasAuthError()) {
					syncResult.stats.numAuthExceptions++;
					Authenticator.clearPassword(context);
					break;
				} else {
					syncResult.stats.numIoExceptions++;
					notifyUploadError(response.getErrorMessage());
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	@DebugLog
	private void deletePendingPlays(@NonNull SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Plays.CONTENT_SIMPLE_URI,
				null,
				Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) },
				null);
			String detail = String.format("Deleting %s play(s)", cursor != null ? cursor.getCount() : 0);
			Timber.i(detail);
			showNotification(detail);

			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = PlayBuilder.fromCursor(cursor);
				if (play.hasBeenSynced()) {
					PlayPostResponse response = postPlayDelete(play.playId);
					if (!response.hasError()) {
						deletePlay(play);
						updateGamePlayCount(play);
						notifyUserOfDelete(R.string.msg_play_deleted, play.gameName);
					} else if (response.hasInvalidIdError()) {
						deletePlay(play);
						notifyUserOfDelete(R.string.msg_play_deleted, play.gameName);
					} else if (response.hasAuthError()) {
						syncResult.stats.numAuthExceptions++;
						Authenticator.clearPassword(context);
					} else {
						syncResult.stats.numIoExceptions++;
						notifyUploadError(response.getErrorMessage());
					}
				} else {
					deletePlay(play);
					notifyUserOfDelete(R.string.msg_play_deleted_draft, play.gameName);
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	@DebugLog
	private void updateGamePlayCount(@NonNull Play play) {
		ContentResolver resolver = context.getContentResolver();
		Cursor cursor = null;
		try {
			cursor = resolver.query(Plays.CONTENT_SIMPLE_URI,
				new String[] { Plays.SUM_QUANTITY },
				PlayItems.OBJECT_ID + "=? AND " + Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(play.gameId), String.valueOf(Play.SYNC_STATUS_SYNCED) },
				null);
			if (cursor != null && cursor.moveToFirst()) {
				int newPlayCount = cursor.getInt(0);
				ContentValues values = new ContentValues();
				values.put(Collection.NUM_PLAYS, newPlayCount);
				resolver.update(Games.buildGameUri(play.gameId), values, null, null);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@DebugLog
	private PlayPostResponse postPlayUpdate(@NonNull Play play) {
		Map<String, String> form = new ArrayMap<>();
		form.put("ajax", "1");
		form.put("action", "save");
		form.put("version", "2");
		form.put("objecttype", "thing");
		if (play.hasBeenSynced()) {
			form.put("playid", String.valueOf(play.playId));
		}
		form.put("objectid", String.valueOf(play.gameId));
		form.put("playdate", play.getDate());
		form.put("dateinput", play.getDate()); // TODO: ask Aldie what this is
		form.put("length", String.valueOf(play.length));
		form.put("location", play.location);
		form.put("quantity", String.valueOf(play.quantity));
		form.put("incomplete", play.Incomplete() ? "1" : "0");
		form.put("nowinstats", play.NoWinStats() ? "1" : "0");
		form.put("comments", play.comments);
		List<Player> players = play.getPlayers();
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			form.put(getMapKey(i, "playerid"), "player_" + i);
			form.put(getMapKey(i, "name"), player.name);
			form.put(getMapKey(i, "username"), player.username);
			form.put(getMapKey(i, "color"), player.color);
			form.put(getMapKey(i, "position"), player.startposition);
			form.put(getMapKey(i, "score"), player.score);
			form.put(getMapKey(i, "rating"), String.valueOf(player.rating));
			form.put(getMapKey(i, "new"), String.valueOf(player.new_));
			form.put(getMapKey(i, "win"), String.valueOf(player.win));
		}

		try {
			return bggSaveService.geekPlay(form);
		} catch (Exception e) {
			return new PlayPostResponse(e);
		}
	}

	@DebugLog
	@NonNull
	private static String getMapKey(int index, String key) {
		return "players[" + index + "][" + key + "]";
	}

	@DebugLog
	private PlayPostResponse postPlayDelete(int playId) {
		Map<String, String> form = new ArrayMap<>();
		form.put("ajax", "1");
		form.put("action", "delete");
		form.put("playid", String.valueOf(playId));
		form.put("finalize", "1");

		try {
			return bggDeleteService.geekPlay(form);
		} catch (Exception e) {
			return new PlayPostResponse(e);
		}
	}

	/**
	 * Deletes the specified play from the content provider
	 */
	@DebugLog
	private void deletePlay(Play play) {
		persister.delete(play);
	}

	@DebugLog
	private String getPlayCountDescription(int count, int quantity) {
		String countDescription;
		switch (quantity) {
			case 1:
				countDescription = StringUtils.getOrdinal(count);
				break;
			case 2:
				countDescription = StringUtils.getOrdinal(count - 1) + " & " + StringUtils.getOrdinal(count);
				break;
			default:
				countDescription = StringUtils.getOrdinal(count - quantity + 1) + " - " + StringUtils.getOrdinal(count);
				break;
		}
		return countDescription;
	}

	@DebugLog
	private void notifyUserOfDelete(int messageId, String gameName) {
		notifyUser(StringUtils.boldSecondString(context.getString(messageId), gameName));
	}

	@DebugLog
	@Override
	protected Action createMessageAction() {
		if (currentPlayForMessage != null) {
			Intent intent = ActivityUtils.createRematchIntent(context,
				currentPlayForMessage.playId,
				currentPlayForMessage.gameId,
				currentPlayForMessage.gameName, null, null);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			NotificationCompat.Action.Builder builder = new NotificationCompat.Action.Builder(
				R.drawable.ic_replay_black_24dp,
				context.getString(R.string.rematch),
				pendingIntent);
			return builder.build();
		}
		return null;
	}
}
