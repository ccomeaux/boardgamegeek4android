package com.boardgamegeek.io;

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
import com.boardgamegeek.database.PlayHelper;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;

public class PlaySender {
	private Context mContext;
	private Resources mResources;
	private CookieStore mCookieStore;
	private HttpClient mClient;

	public PlaySender(Context context, CookieStore cookieStore) {
		mContext = context;
		mResources = mContext.getResources();
		mCookieStore = cookieStore;
		mClient = HttpUtils.createHttpClient(mContext, mCookieStore);
	}

	public Result sendPlay(Play play) {
		Result result = new Result(play.Quantity);

		UrlEncodedFormEntity entity;
		List<NameValuePair> nvps = play.toNameValuePairs();
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
				if (result.isValidResponse()) {
					updateSyncStatus(play);
					syncGame(play);
				} else {
					savePending(play);
				}
			}
		} catch (ClientProtocolException e) {
			result.ErrorMessage = e.toString();
			savePending(play);
		} catch (IOException e) {
			result.ErrorMessage = e.toString();
			savePending(play);
		} finally {
			if (mClient != null && mClient.getConnectionManager() != null) {
				mClient.getConnectionManager().shutdown();
			}
		}

		return result;
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

	}

	private void syncGame(Play play) throws HandlerException {
		RemoteExecutor re = new RemoteExecutor(mClient, mContext.getContentResolver());
		re.executeGet(HttpUtils.constructPlayUrlSpecific(play.GameId, play.getFormattedDate()),
				new RemotePlaysHandler());
	}

	private void savePending(Play play) {
		play.SyncStatus = Play.SYNC_STATUS_PENDING;
		new PlayHelper(mContext.getContentResolver(), play).save();
	}

	private void updateSyncStatus(Play play) {
		PlayHelper ph = new PlayHelper(mContext.getContentResolver(), play);
		if (play.hasBeenSynced()) {
			play.SyncStatus = Play.SYNC_STATUS_SYNCED;
			ph.save();
		} else {
			ph.delete();
		}
	}
}
