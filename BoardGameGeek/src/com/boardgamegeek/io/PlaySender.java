package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.database.PlayPersister;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;

public class PlaySender {
	private static final String TAG = makeLogTag(PlaySender.class);

	private Context mContext;
	private Resources mResources;
	private HttpClient mClient;

	public PlaySender(Context context, CookieStore cookieStore) {
		mContext = context;
		mResources = mContext.getResources();
		mClient = HttpUtils.createHttpClient(mContext, cookieStore);
	}

	public Result sendPlay(Play play) {
		Result result = postPlay(play);

		if (result.hasError() || !result.isValidResponse()) {
			savePending(play);
		} else {
			updateSyncStatus(play);
			result.ErrorMessage = syncGame(play);
		}
		shutdownClient();
		LOGI(TAG, "Sent play with result:\n" + result.toString());
		return result;
	}

	protected Result postPlay(Play play) {
		Result result = new Result(play.Quantity);

		List<NameValuePair> nvps = play.toNameValuePairs();
		UrlEncodedFormEntity entity;
		try {
			entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			result.ErrorMessage = e.toString();
			return result;
		}

		HttpPost post = new HttpPost(BggApplication.siteUrl + "geekplay.php");
		post.setEntity(entity);

		try {
			HttpResponse response = mClient.execute(post);
			if (response == null) {
				result.ErrorMessage = mResources.getString(R.string.logInError) + " : "
						+ mResources.getString(R.string.logInErrorSuffixNoResponse);
			} else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				result.ErrorMessage = mResources.getString(R.string.logInError) + " : "
						+ mResources.getString(R.string.logInErrorSuffixBadResponse) + " " + response.toString() + ".";
			} else {
				result.setResponse(HttpUtils.parseResponse(response));
			}
		} catch (ClientProtocolException e) {
			result.ErrorMessage = e.toString();
		} catch (IOException e) {
			result.ErrorMessage = e.toString();
		}
		return result;
	}

	private void shutdownClient() {
		if (mClient != null && mClient.getConnectionManager() != null) {
			mClient.getConnectionManager().shutdown();
		}
	}

	public class Result {
		public String ErrorMessage;
		private String mResponse;
		private int mPlayQuantity;

		public Result(int playQuantity) {
			mPlayQuantity = playQuantity;
		}

		public void setResponse(String response) {
			mResponse = response;
		}

		public boolean isValidResponse() {
			if (TextUtils.isEmpty(mResponse)) {
				return false;
			}
			return mResponse.startsWith("Plays: <a") || mResponse.startsWith("{\"html\":\"Plays:");
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(ErrorMessage);
		}

		public String getPlayCountDescription() {
			int playCount = parsePlayCount(mResponse);

			String countDescription = "";
			int quantity = mPlayQuantity;
			switch (quantity) {
				case 1:
					countDescription = StringUtils.getOrdinal(playCount);
					break;
				case 2:
					countDescription = StringUtils.getOrdinal(playCount - 1) + " & "
							+ StringUtils.getOrdinal(playCount);
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

		@Override
		public String toString() {
			return "RESPONSE: " + mResponse + "\nERROR: " + ErrorMessage;
		}
	}

	private String syncGame(Play play) {
		RemoteExecutor re = new RemoteExecutor(mClient, mContext.getContentResolver());
		try {
			re.executeGet(HttpUtils.constructPlayUrlSpecific(play.GameId, play.getFormattedDate()),
					new RemotePlaysHandler());
		} catch (HandlerException e) {
			return e.toString();
		}
		return "";
	}

	private void savePending(Play play) {
		LOGI(TAG, "Saving " + play.PlayId + " as pending due to sync problem");
		play.SyncStatus = Play.SYNC_STATUS_PENDING;
		new PlayPersister(mContext.getContentResolver(), play).save();
	}

	private void updateSyncStatus(Play play) {
		PlayPersister ph = new PlayPersister(mContext.getContentResolver(), play);
		if (play.hasBeenSynced()) {
			play.SyncStatus = Play.SYNC_STATUS_SYNCED;
			ph.save();
		} else {
			ph.delete();
		}
	}
}
