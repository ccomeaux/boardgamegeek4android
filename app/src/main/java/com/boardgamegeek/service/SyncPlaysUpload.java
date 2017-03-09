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
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.util.Pair;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.PlayDeleteResponse;
import com.boardgamegeek.model.PlaySaveResponse;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.PlaysActivity;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.SelectionBuilder;
import com.boardgamegeek.util.StringUtils;

import java.util.List;

import hugo.weaving.DebugLog;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;

public class SyncPlaysUpload extends SyncUploadTask {
	public static final String GEEK_PLAY_URL = "https://www.boardgamegeek.com/geekplay.php";
	private OkHttpClient httpClient;
	private PlayPersister persister;
	private long currentInternalIdForMessage;
	private int currentGameIdForMessage;
	private String currentGameNameForMessage;

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
	protected Intent getNotificationIntent() {
		return new Intent(context, PlaysActivity.class);
	}

	@DebugLog
	@Override
	protected String getNotificationMessageTag() {
		return NotificationUtils.TAG_UPLOAD_PLAY;
	}

	@DebugLog
	@Override
	protected String getNotificationErrorTag() {
		return NotificationUtils.TAG_UPLOAD_PLAY_ERROR;
	}

	@DebugLog
	@Override
	public void execute(Account account, @NonNull SyncResult syncResult) {
		httpClient = HttpUtils.getHttpClientWithAuth(context);
		persister = new PlayPersister(context);

		deletePendingPlays(syncResult);
		updatePendingPlays(syncResult);
		if (SyncService.isPlaysSyncUpToDate(context)) {
			SyncService.calculateAndUpdateHIndex(context);
		}
	}

	@DebugLog
	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_plays_upload;
	}

	@DebugLog
	private void updatePendingPlays(@NonNull SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Plays.CONTENT_SIMPLE_URI,
				PlayBuilder.PLAY_PROJECTION_WITH_ID,
				Plays.UPDATE_TIMESTAMP + ">0",
				null,
				Plays.UPDATE_TIMESTAMP);
			int playCount = cursor != null ? cursor.getCount() : 0;
			updateProgressNotificationAsPlural(R.plurals.sync_notification_progress_update, playCount, playCount);

			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) break;
				if (wasSleepInterrupted(1000)) break;

				currentInternalIdForMessage = CursorUtils.getLong(cursor, Plays._ID, BggContract.INVALID_ID);
				Play play = PlayBuilder.fromCursor(cursor);
				Cursor playerCursor = PlayBuilder.queryPlayers(context, currentInternalIdForMessage);
				try {
					PlayBuilder.addPlayers(playerCursor, play);
				} finally {
					if (playerCursor != null) playerCursor.close();
				}

				PlaySaveResponse response = postPlayUpdate(play);
				if (response == null) {
					syncResult.stats.numIoExceptions++;
					notifyUploadError(context.getString(R.string.msg_play_update_null_response));
				} else if (response.hasAuthError()) {
					syncResult.stats.numAuthExceptions++;
					Authenticator.clearPassword(context);
					break;
				} else if (response.hasInvalidIdError()) {
					notifyUploadError(PresentationUtils.getText(context, R.string.msg_play_update_bad_id, play.playId));
				} else if (response.hasError()) {
					notifyUploadError(response.getErrorMessage());
				} else if (response.getPlayCount() <= 0) {
					syncResult.stats.numIoExceptions++;
					notifyUploadError(context.getString(R.string.msg_play_update_null_response));
				} else {
					CharSequence message = play.playId > 0 ?
						PresentationUtils.getText(context, R.string.msg_play_updated) :
						PresentationUtils.getText(context, R.string.msg_play_added, getPlayCountDescription(response.getPlayCount(), play.quantity));

					play.playId = response.getPlayId();
					play.dirtyTimestamp = 0;
					play.updateTimestamp = 0;
					play.deleteTimestamp = 0;
					currentGameIdForMessage = play.gameId;
					currentGameNameForMessage = play.gameName;

					notifyUser(play, message);
					persister.save(play, currentInternalIdForMessage, false);

					updateGamePlayCount(play);
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void notifyUser(Play play, CharSequence message) {
		Pair<String, String> imageUrls = queryGameImageUrls(play);
		notifyUser(play.gameName, message, NotificationUtils.getIntegerId(currentInternalIdForMessage), imageUrls.first, imageUrls.second);
	}

	private Pair<String, String> queryGameImageUrls(Play play) {
		Pair<String, String> imageUrls = Pair.create("", "");
		Cursor gameCursor = context.getContentResolver().query(Games.buildGameUri(play.gameId),
			new String[] { Games.IMAGE_URL, Games.THUMBNAIL_URL }, null, null, null);
		try {
			if (gameCursor != null && gameCursor.moveToFirst()) {
				imageUrls = Pair.create(gameCursor.getString(0), gameCursor.getString(1));
			}
		} finally {
			if (gameCursor != null) gameCursor.close();
		}
		return imageUrls;
	}

	@DebugLog
	private void deletePendingPlays(@NonNull SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Plays.CONTENT_SIMPLE_URI,
				PlayBuilder.PLAY_PROJECTION_WITH_ID,
				Plays.DELETE_TIMESTAMP + ">0",
				null,
				Plays.DELETE_TIMESTAMP);
			int playCount = cursor != null ? cursor.getCount() : 0;
			updateProgressNotificationAsPlural(R.plurals.sync_notification_progress_delete, playCount, playCount);

			while (cursor != null && cursor.moveToNext()) {
				if (isCancelled()) break;
				currentInternalIdForMessage = CursorUtils.getLong(cursor, Plays._ID, BggContract.INVALID_ID);
				Play play = PlayBuilder.fromCursor(cursor);
				if (play.playId > 0) {
					if (wasSleepInterrupted(1000)) break;
					PlayDeleteResponse response = postPlayDelete(play.playId);
					if (response == null) {
						syncResult.stats.numIoExceptions++;
						notifyUploadError(context.getString(R.string.msg_play_update_null_response));
					} else if (response.isSuccessful()) {
						deletePlay(currentInternalIdForMessage);
						updateGamePlayCount(play);
						notifyUserOfDelete(R.string.msg_play_deleted, play);
					} else if (response.hasInvalidIdError()) {
						deletePlay(currentInternalIdForMessage);
						notifyUserOfDelete(R.string.msg_play_deleted, play);
					} else if (response.hasAuthError()) {
						syncResult.stats.numAuthExceptions++;
						Authenticator.clearPassword(context);
					} else {
						syncResult.stats.numIoExceptions++;
						notifyUploadError(response.getErrorMessage());
					}
				} else {
					deletePlay(currentInternalIdForMessage);
					notifyUserOfDelete(R.string.msg_play_deleted_draft, play);
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
				String.format("%s=? AND %s", Plays.OBJECT_ID, SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP)),
				new String[] { String.valueOf(play.gameId) },
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
	@Nullable
	private PlaySaveResponse postPlayUpdate(@NonNull Play play) {
		FormBody.Builder builder = new FormBody.Builder()
			.add("ajax", "1")
			.add("action", "save")
			.add("version", "2")
			.add("objecttype", "thing");
		if (play.playId > 0) {
			builder.add("playid", String.valueOf(play.playId));
		}
		builder.add("objectid", String.valueOf(play.gameId))
			.add("playdate", play.getDate())
			.add("dateinput", play.getDate())
			.add("length", String.valueOf(play.length))
			.add("location", play.location)
			.add("quantity", String.valueOf(play.quantity))
			.add("incomplete", play.Incomplete() ? "1" : "0")
			.add("nowinstats", play.NoWinStats() ? "1" : "0")
			.add("comments", play.comments);
		List<Player> players = play.getPlayers();
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			builder
				.add(getMapKey(i, "playerid"), "player_" + i)
				.add(getMapKey(i, "name"), player.name)
				.add(getMapKey(i, "username"), player.username)
				.add(getMapKey(i, "color"), player.color)
				.add(getMapKey(i, "position"), player.startposition)
				.add(getMapKey(i, "score"), player.score)
				.add(getMapKey(i, "rating"), String.valueOf(player.rating))
				.add(getMapKey(i, "new"), String.valueOf(player.new_))
				.add(getMapKey(i, "win"), String.valueOf(player.win));
		}

		Request request = new Builder()
			.url(GEEK_PLAY_URL)
			.post(builder.build())
			.build();
		return new PlaySaveResponse(httpClient, request);
	}

	@DebugLog
	@NonNull
	private static String getMapKey(int index, String key) {
		return "players[" + index + "][" + key + "]";
	}

	@DebugLog
	@Nullable
	private PlayDeleteResponse postPlayDelete(int playId) {
		FormBody.Builder builder = new FormBody.Builder()
			.add("ajax", "1")
			.add("action", "delete")
			.add("playid", String.valueOf(playId))
			.add("finalize", "1");

		Request request = new Builder()
			.url(GEEK_PLAY_URL)
			.post(builder.build())
			.build();
		return new PlayDeleteResponse(httpClient, request);
	}

	/**
	 * Deletes the specified play from the content provider
	 */
	@DebugLog
	private void deletePlay(long internalId) {
		persister.delete(internalId);
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
	private void notifyUserOfDelete(@StringRes int messageId, Play play) {
		notifyUser(play, PresentationUtils.getText(context, messageId, play.gameName));
	}

	@DebugLog
	@Override
	protected Action createMessageAction() {
		if (currentInternalIdForMessage != BggContract.INVALID_ID) {
			Intent intent = ActivityUtils.createRematchIntent(context,
				currentInternalIdForMessage,
				currentGameIdForMessage,
				currentGameNameForMessage, null, null);
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
