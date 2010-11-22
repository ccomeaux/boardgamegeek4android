package com.boardgamegeek.service;

import java.net.URLEncoder;
import java.text.DateFormat;

import org.apache.http.client.HttpClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddiesHandler;
import com.boardgamegeek.io.RemoteBuddyUserHandler;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;

public class SyncService extends IntentService {
	private final static String TAG = "SyncService";

	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_COMPLETE = 2;
	public static final int STATUS_ERROR = 3;
	public static final String EXTRA_STATUS_RECEIVER = "com.boardgamegeek.extra.STATUS_RECEIVER";

	private static final String BASE_URL = "http://boardgamegeek.com/xmlapi/";
	private static final String BASE_URL_2 = "http://boardgamegeek.com/xmlapi2/";
	private static final int NOTIFICATION_ID = 1;

	private static final int SYNC_GAME_DETAIL_DAYS = 1;
	private static final int SYNC_BUDDY_DETAIL_DAYS = 7;

	private ResultReceiver mResultReceiver;
	private NotificationManager mNotificationManager;
	private HttpClient mHttpClient;
	private ContentResolver mContentResolver;
	private DateFormat mDateFormat;
	private RemoteExecutor mRemoteExecutor;
	private String mUsername;
	private static boolean mUseGzip = true;

	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mHttpClient = HttpUtils.createHttpClient(this, mUseGzip);
		mContentResolver = getContentResolver();
		mDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent(intent=" + intent + ")");

		if (!Intent.ACTION_SYNC.equals(intent.getAction())) {
			Log.w(TAG, "Invalid intent action: " + intent.getAction());
			return;
		}

		mResultReceiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);

		if (!ensureUsername()) {
			return;
		}

		signalStart();

		try {
			final long startTime = System.currentTimeMillis();

			mRemoteExecutor = new RemoteExecutor(mHttpClient, mContentResolver);
			syncCollection();
			syncBuddies();

			Log.d(TAG, "Sync took " + (System.currentTimeMillis() - startTime) + "ms with GZIP "
				+ (mUseGzip ? "on" : "off"));
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			sendError(mResultReceiver, e.toString());
		}

		signalEnd();
		mResultReceiver = null;
	}

	private boolean ensureUsername() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		mUsername = preferences.getString("username", "");
		if (TextUtils.isEmpty(mUsername)) {
			sendError(mResultReceiver, getResources().getString(R.string.pref_warning_username));
			return false;
		}
		return true;
	}

	private void sendError(final ResultReceiver resultReceiver, String errorMessage) {
		if (resultReceiver != null) {
			final Bundle bundle = new Bundle();
			bundle.putString(Intent.EXTRA_TEXT, errorMessage);
			resultReceiver.send(STATUS_ERROR, bundle);
		}
	}

	private void signalStart() {
		if (mResultReceiver != null) {
			mResultReceiver.send(STATUS_RUNNING, Bundle.EMPTY);
		}
		createNotification(R.string.notification_text_start);
	}

	private void signalEnd() {
		if (mResultReceiver != null) {
			mResultReceiver.send(STATUS_COMPLETE, Bundle.EMPTY);
		}
		createNotification(R.string.notification_text_complete, R.string.notification_status_complete);
	}

	private void syncCollection() throws HandlerException {
		syncCollectionList();
		syncCollectionDetail();
		createNotification(R.string.notification_text_collection);
	}

	private void syncCollectionList() throws HandlerException {
		final long startTime = System.currentTimeMillis();

		String[] filters = new String[] { "own", "prevowned", "trade", "want", "wanttoplay", "wanttobuy",
			"wishlist", "preordered" };
		String filterOff = "";
		for (int i = 0; i < filters.length; i++) {
			mRemoteExecutor.executeGet(getCollectionUrl(filters[i]), new RemoteCollectionHandler(startTime));
			filterOff = filterOff + "," + filters[i] + "=0";
		}
		mRemoteExecutor.executeGet(BASE_URL + "collection/" + mUsername + "?" + filterOff.substring(1),
			new RemoteCollectionHandler(startTime));
		mContentResolver
			.delete(Games.CONTENT_URI, Games.UPDATED_LIST + "<?", new String[] { "" + startTime });
	}

	private void syncCollectionDetail() throws HandlerException {
		Cursor cursor = null;
		try {
			int count = 0;
			String ids = "";
			long days = System.currentTimeMillis() - (SYNC_GAME_DETAIL_DAYS * DateUtils.DAY_IN_MILLIS);
			cursor = mContentResolver.query(Games.CONTENT_URI, new String[] { Games.GAME_ID },
				SyncColumns.UPDATED_DETAIL + "<?", new String[] { "" + days }, null);
			while (cursor.moveToNext()) {
				final int id = cursor.getInt(0);
				count++;
				ids = ids + "," + id;
				if (count == 10) {
					fetchGameDetail(ids);
					count = 0;
					ids = "";
				}
			}

			if (count > 0) {
				fetchGameDetail(ids);
			}

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void fetchGameDetail(String ids) throws HandlerException {
		final String url = BASE_URL + "boardgame/" + ids.substring(1) + "?stats=1";
		mRemoteExecutor.executeGet(url, new RemoteGameHandler());
	}

	private String getCollectionUrl(String flag) {
		return BASE_URL + "collection/" + mUsername + (TextUtils.isEmpty(flag) ? "" : "?" + flag + "=1");
	}

	private void syncBuddies() throws HandlerException {
		syncBuddiesList();
		syncBuddiesDetail();
		createNotification(R.string.notification_text_buddies);
	}

	private void syncBuddiesList() throws HandlerException {
		final long startTime = System.currentTimeMillis();
		mRemoteExecutor.executePagedGet(BASE_URL_2 + "user?name=" + mUsername + "&buddies=1",
			new RemoteBuddiesHandler());
		mContentResolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?", new String[] { ""
			+ startTime });
	}

	private void syncBuddiesDetail() throws HandlerException {
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(Buddies.CONTENT_URI, new String[] { Buddies.BUDDY_NAME,
				SyncColumns.UPDATED_DETAIL }, null, null, null);
			RemoteBuddyUserHandler handler = new RemoteBuddyUserHandler();
			while (cursor.moveToNext()) {
				final String name = cursor.getString(0);
				final long lastUpdated = cursor.getLong(1);
				if (DateTimeUtils.howManyDaysOld(lastUpdated) > SYNC_BUDDY_DETAIL_DAYS) {
					final String url = URLEncoder.encode(name);
					mRemoteExecutor.executeGet(BASE_URL_2 + "user?name=" + url, handler);
				} else {
					Log.v(TAG, "Skipping name=" + name + ", updated on " + mDateFormat.format(lastUpdated));
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void createNotification(int messageId) {
		createNotification(messageId, R.string.notification_status_default);
	}

	private void createNotification(int messageId, int statusId) {
		final String message = getResources().getString(messageId);
		final String status = getResources().getString(statusId);

		Notification notification = new Notification(android.R.drawable.stat_notify_sync, message, System
			.currentTimeMillis());
		Intent i = new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction(
			Intent.ACTION_SYNC);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
		notification.setLatestEventInfo(this, getResources().getString(R.string.notification_title), status,
			pi);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
}
