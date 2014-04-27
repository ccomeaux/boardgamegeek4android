package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysParser;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.PlaysActivity;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

public class SyncPlaysUpload extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlaysUpload.class);

	private Context mContext;
	private HttpClient mClient;
	private List<CharSequence> mMessages;
	private LocalBroadcastManager mBroadcaster;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		mContext = executor.getContext();
		mClient = executor.getHttpClient();
		mMessages = new ArrayList<CharSequence>();
		mBroadcaster = LocalBroadcastManager.getInstance(mContext);

		updatePendingPlays(account.name, syncResult);
		deletePendingPlays(syncResult);
		SyncService.hIndex(executor.getContext());
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_plays_upload;
	}

	private void updatePendingPlays(String username, SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Plays.CONTENT_SIMPLE_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE) }, null);
			LOGI(TAG, String.format("Updating %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = PlayBuilder.fromCursor(cursor, mContext, true);

				PlayUpdateResponse response = postPlayUpdate(play);
				if (!response.hasError()) {
					setStatusToSynced(play);
					String error = syncGame(username, play, syncResult);

					if (TextUtils.isEmpty(error)) {
						increaseGamePlayCount(play);
						if (!play.hasBeenSynced()) {
							deletePlay(play, syncResult);
						}

						String message = play.hasBeenSynced() ? mContext.getString(R.string.msg_play_updated)
							: mContext.getString(R.string.msg_play_added,
								getPlayCountDescription(response.count, play.quantity));
						notifyUser(StringUtils.boldSecondString(message, play.gameName));
					} else {
						notifyUser(error);
					}
				} else if (response.hasAuthError()) {
					syncResult.stats.numAuthExceptions++;
					Authenticator.clearPassword(mContext);
					break;
				} else if (response.hasBadIdError()) {
					PlayPersister.delete(mContext.getContentResolver(), play);
					notifyUser(mContext.getResources().getString(R.string.msg_play_update_bad_id));
				} else {
					syncResult.stats.numIoExceptions++;
					notifyUser(response.error);
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void deletePendingPlays(SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Plays.CONTENT_SIMPLE_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) }, null);
			LOGI(TAG, String.format("Deleting %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = PlayBuilder.fromCursor(cursor);
				if (play.hasBeenSynced()) {
					String error = postPlayDelete(play.playId, syncResult);
					if (TextUtils.isEmpty(error)) {
						decreaseGamePlayCount(play);
						PlayPersister.delete(mContext.getContentResolver(), play);
						notifyUser(StringUtils.boldSecondString(mContext.getString(R.string.msg_play_deleted),
							play.gameName));
						// syncResult.stats.numDeletes++;
					} else {
						notifyUser(error);
					}
				} else {
					PlayPersister.delete(mContext.getContentResolver(), play);
					notifyUser(StringUtils.boldSecondString(mContext.getString(R.string.msg_play_deleted_draft),
						play.gameName));
					// syncResult.stats.numDeletes++;
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void increaseGamePlayCount(Play play) {
		updateGamePlayCount(play, true);
	}

	private void decreaseGamePlayCount(Play play) {
		updateGamePlayCount(play, false);
	}

	private void updateGamePlayCount(Play play, boolean add) {
		ContentResolver resolver = mContext.getContentResolver();
		Uri uri = Games.buildGameUri(play.gameId);
		Cursor cursor = resolver.query(uri, new String[] { Games.NUM_PLAYS }, null, null, null);
		if (cursor.moveToFirst()) {
			int newPlayCount = cursor.getInt(0);
			if (add) {
				newPlayCount += play.quantity;
			} else {
				newPlayCount -= play.quantity;
				if (newPlayCount < 0) {
					newPlayCount = 0;
				}
			}
			ContentValues values = new ContentValues();
			values.put(Collection.NUM_PLAYS, newPlayCount);
			resolver.update(uri, values, null, null);
		}
	}

	private PlayUpdateResponse postPlayUpdate(Play play) {
		List<NameValuePair> nvps = toNameValuePairs(play);
		UrlEncodedFormEntity entity;
		try {
			entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "Trying to encode play for update", e);
			return new PlayUpdateResponse("Couldn't create the HttpEntity");
		}

		HttpPost post = new HttpPost(HttpUtils.SITE_URL + "geekplay.php");
		post.setEntity(entity);

		try {
			Resources r = mContext.getResources();
			HttpResponse response = mClient.execute(post);
			if (response == null) {
				return new PlayUpdateResponse(r.getString(R.string.logInError) + " : "
					+ r.getString(R.string.logInErrorSuffixNoResponse));
			} else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return new PlayUpdateResponse(r.getString(R.string.logInError) + " : "
					+ r.getString(R.string.logInErrorSuffixBadResponse) + " " + response.toString() + ".");
			} else {
				String message = HttpUtils.parseResponse(response);
				if (message.startsWith("Plays: <a") || message.startsWith("{\"html\":\"Plays:")) {
					return new PlayUpdateResponse(parsePlayCount(message));
				} else {
					return new PlayUpdateResponse("Bad response:\n" + message);
				}
			}
		} catch (ClientProtocolException e) {
			return new PlayUpdateResponse(e);
		} catch (IOException e) {
			return new PlayUpdateResponse(e);
		}
	}

	private String postPlayDelete(int playId, SyncResult syncResult) {
		UrlEncodedFormEntity entity = null;
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ajax", "1"));
		nvps.add(new BasicNameValuePair("action", "delete"));
		nvps.add(new BasicNameValuePair("playid", String.valueOf(playId)));
		try {
			entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "Trying to encode play for deletion", e);
			return "Couldn't create the HttpEntity";
		}

		HttpPost post = new HttpPost(HttpUtils.SITE_URL + "geekplay.php");
		post.setEntity(entity);

		try {
			Resources r = mContext.getResources();
			HttpResponse response = mClient.execute(post);
			if (response == null) {
				return r.getString(R.string.logInError) + " : " + r.getString(R.string.logInErrorSuffixNoResponse);
			} else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return r.getString(R.string.logInError) + " : " + r.getString(R.string.logInErrorSuffixBadResponse)
					+ " " + response.toString() + ".";
			} else {
				String message = HttpUtils.parseResponse(response);
				if (message.contains("<title>Plays ") || message.contains("That play doesn't exist")) {
					// TODO: only needed if play is a draft
					PreferencesUtils.removeNewPlayId(mContext, playId);
					return "";
				} else {
					return "Bad response:\n" + message;
				}
			}
		} catch (ClientProtocolException e) {
			syncResult.stats.numIoExceptions++;
			return e.toString();
		} catch (IOException e) {
			syncResult.stats.numIoExceptions++;
			return e.toString();
		} catch (Exception e) {
			syncResult.stats.numAuthExceptions++;
			return e.toString();
		}
	}

	/**
	 * Marks the specified play as synced in the content provider
	 */
	private void setStatusToSynced(Play play) {
		play.syncStatus = Play.SYNC_STATUS_SYNCED;
		PlayPersister.save(mContext.getContentResolver(), play);
		// syncResult.stats.numUpdates++;
	}

	/**
	 * Deletes the specified play from the content provider
	 */
	private void deletePlay(Play play, SyncResult syncResult) {
		PlayPersister.delete(mContext.getContentResolver(), play);
		// syncResult.stats.numDeletes++;
	}

	/**
	 * Syncs the specified game from the 'Geek to the local DB.
	 * 
	 * @return An error message, or blank if no error.
	 */
	private String syncGame(String username, Play play, SyncResult syncResult) {
		RemoteExecutor re = new RemoteExecutor(mClient, mContext);
		try {
			RemotePlaysParser parser = new RemotePlaysParser(username).setGameId(play.gameId).setDate(play.getDate());
			re.executeGet(parser);

			if (!play.hasBeenSynced()) {
				int newPlayId = getTranslatedPlayId(play, parser.getPlays());
				PreferencesUtils.putNewPlayId(mContext, play.playId, newPlayId);
				Intent intent = new Intent(SyncService.ACTION_PLAY_ID_CHANGED);
				mBroadcaster.sendBroadcast(intent);
			}

			PlayPersister.save(mContext.getContentResolver(), parser.getPlays());
		} catch (IOException e) {
			syncResult.stats.numIoExceptions++;
			return e.toString();
		} catch (XmlPullParserException e) {
			syncResult.stats.numParseExceptions++;
			return e.toString();
		}
		return "";
	}

	private int getTranslatedPlayId(Play play, List<Play> parsedPlays) {
		if (parsedPlays == null || parsedPlays.size() == 0) {
			return BggContract.INVALID_ID;
		}

		int latestPlayId = BggContract.INVALID_ID;

		for (Play parsedPlay : parsedPlays) {
			if ((play.playId != parsedPlay.playId) && (play.gameId == parsedPlay.gameId)
				&& (play.getDate() == parsedPlay.getDate()) && (play.Incomplete() == parsedPlay.Incomplete())
				&& (play.NoWinStats() == parsedPlay.NoWinStats())
				&& (play.getPlayerCount() == parsedPlay.getPlayerCount())) {
				if (parsedPlay.playId > latestPlayId) {
					latestPlayId = parsedPlay.playId;
				}
			}
		}

		return latestPlayId;
	}

	private List<NameValuePair> toNameValuePairs(Play play) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ajax", "1"));
		nvps.add(new BasicNameValuePair("action", "save"));
		nvps.add(new BasicNameValuePair("version", "2"));
		nvps.add(new BasicNameValuePair("objecttype", "thing"));
		if (play.hasBeenSynced()) {
			nvps.add(new BasicNameValuePair("playid", String.valueOf(play.playId)));
		}
		nvps.add(new BasicNameValuePair("objectid", String.valueOf(play.gameId)));
		nvps.add(new BasicNameValuePair("playdate", play.getDate()));
		nvps.add(new BasicNameValuePair("dateinput", play.getDate())); // TODO: ask Aldie what this is
		nvps.add(new BasicNameValuePair("length", String.valueOf(play.length)));
		nvps.add(new BasicNameValuePair("location", play.location));
		nvps.add(new BasicNameValuePair("quantity", String.valueOf(play.quantity)));
		nvps.add(new BasicNameValuePair("incomplete", play.Incomplete() ? "1" : "0"));
		nvps.add(new BasicNameValuePair("nowinstats", play.NoWinStats() ? "1" : "0"));
		nvps.add(new BasicNameValuePair("comments", play.comments));

		List<Player> players = play.getPlayers();
		for (int i = 0; i < players.size(); i++) {
			nvps.addAll(players.get(i).toNameValuePairs(i));
		}

		LOGD(TAG, nvps.toString());
		return nvps;
	}

	private String getPlayCountDescription(int count, int quantity) {
		String countDescription = "";
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

	private int parsePlayCount(String result) {
		int start = result.indexOf(">");
		int end = result.indexOf("<", start);
		int playCount = StringUtils.parseInt(result.substring(start + 1, end), 1);
		return playCount;
	}

	private void notifyUser(CharSequence message) {
		mMessages.add(message);

		NotificationCompat.Builder builder = createNotificationBuilder().setContentText(message);

		if (mMessages.size() == 1) {
			NotificationCompat.BigTextStyle detail = new NotificationCompat.BigTextStyle(builder);
			detail.bigText(message);
		} else {
			NotificationCompat.InboxStyle detail = new NotificationCompat.InboxStyle(builder);
			detail.setSummaryText(String.format(mContext.getString(R.string.sync_notification_upload_summary),
				mMessages.size()));
			for (int i = mMessages.size() - 1; i >= 0; i--) {
				detail.addLine(mMessages.get(i));
			}
		}
		NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC_PLAY_UPLOAD, builder);
	}

	private NotificationCompat.Builder createNotificationBuilder() {
		return NotificationUtils.createNotificationBuilder(mContext, R.string.sync_notification_title_play_upload,
			PlaysActivity.class);
	}

	private static class PlayUpdateResponse {
		int count;
		String error;

		PlayUpdateResponse(int count) {
			this.count = count;
		}

		PlayUpdateResponse(String error) {
			this.error = error;
		}

		PlayUpdateResponse(Exception e) {
			this.error = e.toString();
		}

		boolean hasError() {
			return !TextUtils.isEmpty(error);
		}

		boolean hasAuthError() {
			return hasError() && error.contains("You must login to save plays");
		}

		boolean hasBadIdError() {
			return hasError() && error.contains("You are not permitted to edit this play.");
		}
	}
}
