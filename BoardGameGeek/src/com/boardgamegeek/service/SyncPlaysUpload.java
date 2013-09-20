package com.boardgamegeek.service;

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
import android.content.Context;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.database.PlayPersister;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.PlaysActivity;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.url.PlaysUrlBuilder;

public class SyncPlaysUpload extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlaysUpload.class);

	private Context mContext;
	private HttpClient mClient;
	private List<String> mMessages;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		mContext = executor.getContext();
		mClient = executor.getHttpClient();
		mMessages = new ArrayList<String>();

		updatePendingPlays(account.name, syncResult);
		deletePendingPlays(syncResult);
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_plays_upload;
	}

	private void updatePendingPlays(String username, SyncResult syncResult) {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Plays.CONTENT_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE) }, null);
			LOGI(TAG, String.format("Updating %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = new Play().fromCursor(cursor, mContext, true);

				PlayUpdateResponse response = postPlayUpdate(play);
				if (!response.hasError()) {
					updateContentProvider(play, syncResult);
					String error = syncGame(username, play, syncResult);

					if (TextUtils.isEmpty(error)) {
						Resources r = mContext.getResources();
						String message = String.format(
							r.getString(play.hasBeenSynced() ? R.string.msg_play_updated : R.string.msg_play_added),
							getPlayCountDescription(response.count, play.Quantity), play.GameName);
						notifyUser(message);
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
			cursor = mContext.getContentResolver().query(Plays.CONTENT_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) }, null);
			LOGI(TAG, String.format("Deleting %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					break;
				}
				Play play = new Play().fromCursor(cursor);
				if (play.hasBeenSynced()) {
					String error = postPlayDelete(play.PlayId, syncResult);
					if (TextUtils.isEmpty(error)) {
						PlayPersister.delete(mContext.getContentResolver(), play);
						notifyUser(String.format(mContext.getString(R.string.msg_play_deleted), play.GameName));
						// syncResult.stats.numDeletes++;
					} else {
						notifyUser(error);
					}
				} else {
					PlayPersister.delete(mContext.getContentResolver(), play);
					notifyUser(String.format(mContext.getString(R.string.msg_play_deleted_draft), play.GameName));
					// syncResult.stats.numDeletes++;
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private PlayUpdateResponse postPlayUpdate(Play play) {
		List<NameValuePair> nvps = play.toNameValuePairs();
		UrlEncodedFormEntity entity;
		try {
			entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "Trying to encode play for update", e);
			return new PlayUpdateResponse("Couldn't create the HttpEntity");
		}

		HttpPost post = new HttpPost(BggApplication.siteUrl + "geekplay.php");
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

		HttpPost post = new HttpPost(BggApplication.siteUrl + "geekplay.php");
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
	 * Marks the specified play as synced or deleted in the content provider
	 */
	private void updateContentProvider(Play play, SyncResult syncResult) {
		if (play.hasBeenSynced()) {
			play.SyncStatus = Play.SYNC_STATUS_SYNCED;
			PlayPersister.save(mContext.getContentResolver(), play);
			// syncResult.stats.numUpdates++;
		} else {
			PlayPersister.delete(mContext.getContentResolver(), play);
			// syncResult.stats.numDeletes++;
		}
	}

	/**
	 * Syncs the specified game from the 'Geek to the local DB.
	 * 
	 * @return An error message, or blank if no error.
	 */
	private String syncGame(String username, Play play, SyncResult syncResult) {
		RemoteExecutor re = new RemoteExecutor(mClient, mContext);
		try {
			String url = new PlaysUrlBuilder(username).gameId(play.GameId).date(play.getDate()).build();
			re.executeGet(url, new RemotePlaysHandler());
		} catch (IOException e) {
			syncResult.stats.numIoExceptions++;
			return e.toString();
		} catch (XmlPullParserException e) {
			syncResult.stats.numParseExceptions++;
			return e.toString();
		}
		return "";
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

	private void notifyUser(String message) {
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
