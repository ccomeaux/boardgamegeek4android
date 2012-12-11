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

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.database.PlayPersister;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;

public class SyncPlaysUpload extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlaysUpload.class);

	private Context mContext;
	private HttpClient mClient;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		mContext = context;
		mClient = executor.getHttpClient();

		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		updatePendingPlays(account.name);
		deletePendingPlays();
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_plays_upload;
	}

	private void updatePendingPlays(String username) {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Plays.CONTENT_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE) }, null);
			LOGI(TAG, String.format("Updating %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				Play play = new Play().fromCursor(cursor);

				Cursor c = null;
				try {
					c = mContext.getContentResolver().query(play.playerUri(), null, null, null, null);
					while (c.moveToNext()) {
						play.addPlayer(new Player(c));
					}
				} finally {
					if (c != null) {
						c.close();
					}
				}

				String error = postPlayUpdate(play);
				if (TextUtils.isEmpty(error)) {
					updateContentProvider(play);
					error = syncGame(username, play);

					if (TextUtils.isEmpty(error)) {
						Resources r = mContext.getResources();
						String message = r.getString(R.string.msg_play_updated);
						if (!play.hasBeenSynced()) {
							String countDescription = "";// result.getPlayCountDescription();
							message = String.format(r.getString(R.string.logPlaySuccess), countDescription,
								play.GameName);
						}

						notifyUser(message);
					} else {
						notifyUser(error);
					}
				} else {
					notifyUser(error);
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	// TODO: make this work again
	public String getPlayCountDescription(String result) {
		int playCount = parsePlayCount(result);

		String countDescription = "";
		int quantity = 0;// mPlayQuantity;
		switch (quantity) {
			case 1:
				countDescription = StringUtils.getOrdinal(playCount);
				break;
			case 2:
				countDescription = StringUtils.getOrdinal(playCount - 1) + " & " + StringUtils.getOrdinal(playCount);
				break;
			default:
				countDescription = StringUtils.getOrdinal(playCount - quantity + 1) + " - "
					+ StringUtils.getOrdinal(playCount);
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

	private void deletePendingPlays() {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Plays.CONTENT_URI, null, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) }, null);
			LOGI(TAG, String.format("Deleting %s play(s)", cursor.getCount()));
			while (cursor.moveToNext()) {
				Play play = new Play().fromCursor(cursor);
				PlayPersister pp = new PlayPersister(mContext.getContentResolver(), play);
				if (play.hasBeenSynced()) {
					String error = postPlayDelete(play.PlayId);
					if (TextUtils.isEmpty(error)) {
						pp.delete();
						notifyUser(mContext.getString(R.string.msg_play_deleted));
					} else {
						notifyUser(error);
					}
				} else {
					pp.delete();
					notifyUser(mContext.getString(R.string.msg_play_deleted));
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void notifyUser(String message) {
		// TODO: Sometimes this toast gets stuck until the app closes
		// Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
	}

	protected String postPlayUpdate(Play play) {
		List<NameValuePair> nvps = play.toNameValuePairs();
		UrlEncodedFormEntity entity;
		try {
			entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			LOGE(TAG, "Trying to encode play for update", e);
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
				if (message.startsWith("Plays: <a") || message.startsWith("{\"html\":\"Plays:")) {
					return "";
				} else {
					return "Bad response:\n" + message;
				}
			}
		} catch (ClientProtocolException e) {
			return e.toString();
		} catch (IOException e) {
			return e.toString();
		}
	}

	private void updateContentProvider(Play play) {
		PlayPersister ph = new PlayPersister(mContext.getContentResolver(), play);
		if (play.hasBeenSynced()) {
			play.SyncStatus = Play.SYNC_STATUS_SYNCED;
			ph.save();
		} else {
			ph.delete();
		}
	}

	private String syncGame(String username, Play play) {
		RemoteExecutor re = new RemoteExecutor(mClient, mContext);
		try {
			re.executeGet(HttpUtils.constructPlayUrlSpecific(username, play.GameId, play.getDate()),
				new RemotePlaysHandler());
		} catch (HandlerException e) {
			return e.toString();
		}
		return "";
	}

	public String postPlayDelete(int playId) {
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
			return e.toString();
		} catch (IOException e) {
			return e.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}
}
