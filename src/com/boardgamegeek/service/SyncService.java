package com.boardgamegeek.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.util.HttpUtils;

public class SyncService extends IntentService {
	private final static String TAG = "SyncService";

	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_COMPLETE = 2;
	public static final int STATUS_ERROR = 3;
	public static final String EXTRA_STATUS_RECEIVER = "com.boardgamegeek.extra.STATUS_RECEIVER";
	public static final String BASE_URL = "http://boardgamegeek.com/xmlapi/";
	public static final String BASE_URL_2 = "http://boardgamegeek.com/xmlapi2/";

	private static final int NOTIFICATION_ID = 1;
	private static boolean mUseGzip = true;

	private NotificationManager mNotificationManager;
	private HttpClient mHttpClient;
	private List<SyncTask> mTasks = new ArrayList<SyncTask>();
	private ResultReceiver mResultReceiver;
	private RemoteExecutor mRemoteExecutor;

	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mHttpClient = HttpUtils.createHttpClient(this, mUseGzip);

		mTasks.add(new SyncCollectionList());
		mTasks.add(new SyncCollectionDetail());
		mTasks.add(new SyncBuddiesList());
		mTasks.add(new SyncBuddiesDetail());
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

			mRemoteExecutor = new RemoteExecutor(mHttpClient, getContentResolver());

			for (SyncTask task : mTasks) {
				task.execute(mRemoteExecutor, this);
				createNotification(task.getNotification());
			}

			Log.d(TAG, "Sync took " + (System.currentTimeMillis() - startTime) + "ms with GZIP "
				+ (mUseGzip ? "on" : "off"));
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			sendError(e.toString());
		}

		signalEnd();
		mResultReceiver = null;
	}

	private boolean ensureUsername() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String username = preferences.getString("username", "");
		if (TextUtils.isEmpty(username)) {
			sendError(getResources().getString(R.string.pref_warning_username));
			return false;
		}
		return true;
	}

	private void sendError(String errorMessage) {
		if (mResultReceiver != null) {
			final Bundle bundle = new Bundle();
			bundle.putString(Intent.EXTRA_TEXT, errorMessage);
			mResultReceiver.send(STATUS_ERROR, bundle);
		}
		mNotificationManager.cancel(NOTIFICATION_ID);
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
		createNotification(R.string.notification_text_complete, R.string.notification_status_complete, true);
	}

	private void createNotification(int messageId) {
		createNotification(messageId, R.string.notification_status_default, false);
	}

	private void createNotification(int messageId, int statusId, boolean includeIntent) {
		final String message = getResources().getString(messageId);
		final String status = getResources().getString(statusId);

		Notification notification = new Notification(android.R.drawable.stat_notify_sync, message, System
			.currentTimeMillis());

		Intent i = null;
		if (includeIntent) {
			i = new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction(
				Intent.ACTION_SYNC);
		}
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

		notification.setLatestEventInfo(this, getResources().getString(R.string.notification_title), status,
			pi);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
}
