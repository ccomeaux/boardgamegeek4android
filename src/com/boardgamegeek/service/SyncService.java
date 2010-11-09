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
import android.util.Log;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddiesHandler;
import com.boardgamegeek.io.RemoteBuddyUserHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;

public class SyncService extends IntentService {
	private final static String TAG = "SyncService";

	public static final String EXTRA_STATUS_RECEIVER = "com.boardgamegeek.extra.STATUS_RECEIVER";

	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_COMPLETE = 2;
	public static final int STATUS_ERROR = 3;

	private static final String BASE_URL = "http://boardgamegeek.com/xmlapi2/";
	private static final int NOTIFICATION_ID = 1;

	private ResultReceiver mResultReceiver;
	private NotificationManager mNotificationManager;
	private HttpClient mHttpClient;
	private ContentResolver mContentResolver;
	private DateFormat mDateFormat;
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

		if (!ensureUsername())
			return;

		signalStart();

		try {
			final long startTime = System.currentTimeMillis();

			RemoteExecutor remoteExecutor = new RemoteExecutor(mHttpClient, mContentResolver);
			syncBuddies(remoteExecutor);

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

	private void syncBuddies(RemoteExecutor remoteExecutor) throws HandlerException {
		syncBuddiesList(remoteExecutor);
		syncBuddiesDetail(remoteExecutor);
		createNotification(R.string.notification_text_buddies);
	}

	private void syncBuddiesList(RemoteExecutor remoteExecutor) throws HandlerException {
		final long startTime = System.currentTimeMillis();
		remoteExecutor.executePagedGet(BASE_URL + "user?name=" + mUsername + "&buddies=1",
			new RemoteBuddiesHandler());
		mContentResolver.delete(Buddies.CONTENT_URI,
			Buddies.UPDATED_LIST + "<?",
			new String[] { "" + startTime });
	}

	private void syncBuddiesDetail(RemoteExecutor remoteExecutor) throws HandlerException {
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(Buddies.CONTENT_URI,
				new String[] { Buddies.BUDDY_NAME, SyncColumns.UPDATED_DETAIL },
				null, null, null);
			RemoteBuddyUserHandler handler = new RemoteBuddyUserHandler();
			while (cursor.moveToNext()) {
				final String name = cursor.getString(0);
				final long lastUpdated = cursor.getLong(1);
				if (DateTimeUtils.howManyDaysOld(lastUpdated) > 7){
					final String url = URLEncoder.encode(name);
					remoteExecutor.executeGet(BASE_URL + "user?name=" + url, handler);
				} else {
					Log.d(TAG, "Skipping name=" + name + ", updated on "
						+ mDateFormat.format(lastUpdated));
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

		Notification notification = new Notification(android.R.drawable.stat_notify_sync, message,
			System.currentTimeMillis());
		Intent i = new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction(
			Intent.ACTION_SYNC);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
		notification.setLatestEventInfo(this, getResources().getString(R.string.notification_title), status, pi);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
}
