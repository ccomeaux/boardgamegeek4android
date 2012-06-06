package com.boardgamegeek.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.boardgamegeek.BggApplication;
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

	public static final String KEY_SYNC_TYPE = "KEY_SYNC_TYPE";
	public static final int SYNC_TYPE_ALL = 0;
	public static final int SYNC_TYPE_COLLECTION = 1;
	public static final int SYNC_TYPE_PLAYS = 2;
	public static final int SYNC_TYPE_BUDDIES = 3;

	private static final int NOTIFICATION_ID = 1;
	private static boolean mUseGzip = true;

	private boolean mIsRunning = false;
	private NotificationManager mNotificationManager;
	private HttpClient mHttpClient;
	private SparseArray<List<SyncTask>> mTaskList = new SparseArray<List<SyncTask>>();
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

		List<SyncTask> tasks = new ArrayList<SyncTask>();

		if (BggApplication.getInstance().getSyncStatuses() != null
				&& BggApplication.getInstance().getSyncStatuses().length > 0) {
			List<SyncTask> list = new ArrayList<SyncTask>(2);
			list.add(new SyncCollectionList());
			list.add(new SyncCollectionDetail());
			tasks.addAll(list);
			mTaskList.put(SYNC_TYPE_COLLECTION, list);
		}

		if (BggApplication.getInstance().getSyncPlays()) {
			List<SyncTask> list = new ArrayList<SyncTask>(1);
			list.add(new SyncPlays());
			tasks.addAll(list);
			mTaskList.put(SYNC_TYPE_PLAYS, list);
		}

		if (BggApplication.getInstance().getSyncBuddies()) {
			List<SyncTask> list = new ArrayList<SyncTask>(2);
			list.add(new SyncBuddiesList());
			list.add(new SyncBuddiesDetail());
			tasks.addAll(list);
			mTaskList.put(SYNC_TYPE_BUDDIES, list);
		}

		mTaskList.put(SYNC_TYPE_ALL, tasks);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent(intent=" + intent + ")");

		if (!Intent.ACTION_SYNC.equals(intent.getAction())) {
			Log.w(TAG, "Invalid intent action: " + intent.getAction());
			return;
		}

		mResultReceiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);

		List<SyncTask> tasks = mTaskList.get(intent.getIntExtra(KEY_SYNC_TYPE, 0));
		if (tasks == null) {
			tasks = mTaskList.get(SYNC_TYPE_ALL);
		}

		if (mIsRunning) {
			sendResultToReceiver(STATUS_RUNNING);
			return;
		}

		if (!ensureUsername()) {
			return;
		}

		mIsRunning = true;
		final long startTime = System.currentTimeMillis();
		BggApplication.getInstance().putSyncTimestamp(startTime);
		signalStart();

		try {

			mRemoteExecutor = new RemoteExecutor(mHttpClient, getContentResolver());

			for (SyncTask task : tasks) {
				createNotification(task.getNotification());
				task.execute(mRemoteExecutor, this);
				if (task.isBggDown()) {
					String message = getResources().getString(R.string.notification_bgg_down);
					Log.d(TAG, message);
					sendError(message);
					break;
				}
			}

			Log.d(TAG, "Sync took " + (System.currentTimeMillis() - startTime) + "ms with GZIP "
					+ (mUseGzip ? "on" : "off"));
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			sendError(e.toString());
		} finally {
			BggApplication.getInstance().putSyncTimestamp(0);
			signalEnd();
			mResultReceiver = null;
			mIsRunning = false;
		}
	}

	private boolean ensureUsername() {
		String username = BggApplication.getInstance().getUserName();
		if (TextUtils.isEmpty(username)) {
			sendError(getResources().getString(R.string.pref_warning_username));
			return false;
		}
		return true;
	}

	private void sendError(String errorMessage) {
		sendResultToReceiver(STATUS_ERROR, errorMessage);
		mNotificationManager.cancel(NOTIFICATION_ID);
	}

	private void signalStart() {
		sendResultToReceiver(STATUS_RUNNING);
		createNotification(R.string.notification_text_start);
	}

	private void signalEnd() {
		sendResultToReceiver(STATUS_COMPLETE);
		createNotification(R.string.notification_text_complete, true);
	}

	private void sendResultToReceiver(int resultCode) {
		sendResultToReceiver(resultCode, null);
	}

	private void sendResultToReceiver(int resultCode, String message) {
		if (mResultReceiver != null) {
			Bundle bundle = Bundle.EMPTY;
			if (!TextUtils.isEmpty(message)) {
				bundle = new Bundle();
				bundle.putString(Intent.EXTRA_TEXT, message);
			}
			mResultReceiver.send(resultCode, bundle);
		}
	}

	private void createNotification(int messageId) {
		createNotification(messageId, false);
	}

	private void createNotification(int messageId, boolean cancelNotification) {
		String message = getResources().getString(messageId);
		String title = getResources().getString(R.string.notification_title);

		Notification notification = new Notification(android.R.drawable.stat_notify_sync, title + " - " + message,
				System.currentTimeMillis());

		Intent i = new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction(
				Intent.ACTION_SYNC);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

		if (cancelNotification) {
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
		}

		notification.setLatestEventInfo(this, title, message, pi);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
}
