package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.SparseArray;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.HttpUtils;

public class SyncService extends IntentService {
	private static final String TAG = makeLogTag(SyncService.class);

	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_COMPLETE = 2;
	public static final int STATUS_ERROR = 3;
	public static final String EXTRA_STATUS_RECEIVER = "com.boardgamegeek.extra.STATUS_RECEIVER";

	public static final String KEY_SYNC_TYPE = "KEY_SYNC_TYPE";
	public static final String KEY_SYNC_ID = "KEY_SYNC_ID";
	public static final String KEY_SYNC_SUPPRESS_NOTIFICATIONS = "KEY_SYNC_SUPPRESS_NOTIFICATIONS";
	public static final int SYNC_TYPE_ALL = 0;
	public static final int SYNC_TYPE_COLLECTION = 1;
	public static final int SYNC_TYPE_PLAYS = 2;
	public static final int SYNC_TYPE_BUDDIES = 3;
	public static final int SYNC_TYPE_GAME = 4;
	public static final int SYNC_TYPE_GAME_PLAYS = 5;
	public static final int SYNC_TYPE_PLAYS_UPLOAD = 6;
	public static final int SYNC_TYPE_DESIGNER = 10;
	public static final int SYNC_TYPE_ARTIST = 11;
	public static final int SYNC_TYPE_PUBLISHER = 12;

	private static final int NOTIFICATION_ID = 1;
	private static boolean mUseGzip = true;

	private AccountManager mAccountManager;
	private NotificationManager mNotificationManager;
	private HttpClient mHttpClient;
	private SparseArray<List<SyncTask>> mTaskList = new SparseArray<List<SyncTask>>();
	private ResultReceiver mResultReceiver;
	private RemoteExecutor mRemoteExecutor;
	private boolean mSuppressNotifications;

	public static void start(Context context, DetachableResultReceiver receiver, int type) {
		context.startService(new Intent(Intent.ACTION_SYNC, null, context, SyncService.class).putExtra(
			SyncService.EXTRA_STATUS_RECEIVER, receiver).putExtra(SyncService.KEY_SYNC_TYPE, type));
	}

	public static void start(Context context, DetachableResultReceiver receiver, int type, int id) {
		context.startService(new Intent(Intent.ACTION_SYNC, null, context, SyncService.class)
			.putExtra(SyncService.EXTRA_STATUS_RECEIVER, receiver).putExtra(SyncService.KEY_SYNC_TYPE, type)
			.putExtra(KEY_SYNC_ID, id).putExtra(KEY_SYNC_SUPPRESS_NOTIFICATIONS, true));
	}

	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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
			List<SyncTask> list = new ArrayList<SyncTask>(2);
			list.add(new SyncPlays());
			list.add(new SyncPlaysUpload());
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
		LOGD(TAG, "onHandleIntent(intent=" + intent + ")");

		if (!Intent.ACTION_SYNC.equals(intent.getAction())) {
			LOGW(TAG, "Invalid intent action: " + intent.getAction());
			return;
		}

		mAccountManager = AccountManager.get(getApplicationContext());
		Account account = Authenticator.getAccount(mAccountManager);
		if (account == null) {
			sendError(getResources().getString(R.string.pref_warning_username));
			return;
		}

		mHttpClient = HttpUtils.createHttpClient(this, account.name, mAccountManager.getPassword(account),
			Long.parseLong(mAccountManager.getUserData(account, Authenticator.KEY_PASSWORD_EXPIRY)), mUseGzip);

		mResultReceiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
		mSuppressNotifications = intent.getBooleanExtra(KEY_SYNC_SUPPRESS_NOTIFICATIONS, false);
		int syncType = intent.getIntExtra(KEY_SYNC_TYPE, SYNC_TYPE_ALL);
		int syncId = intent.getIntExtra(KEY_SYNC_ID, BggContract.INVALID_ID);

		List<SyncTask> tasks = mTaskList.get(syncType);
		if (tasks == null) {
			if (syncId != BggContract.INVALID_ID) {
				switch (syncType) {
					case SYNC_TYPE_GAME:
						tasks = createTask(new SyncGame(syncId));
						break;
					case SYNC_TYPE_GAME_PLAYS:
						tasks = createTask(new SyncGamePlays(syncId));
						break;
					case SYNC_TYPE_DESIGNER:
						tasks = createTask(new SyncDesigner(syncId));
						break;
					case SYNC_TYPE_ARTIST:
						tasks = createTask(new SyncArtist(syncId));
						break;
					case SYNC_TYPE_PUBLISHER:
						tasks = createTask(new SyncPublisher(syncId));
						break;
				}
			} else {
				switch (syncType) {
					case SYNC_TYPE_PLAYS_UPLOAD:
						tasks = createTask(new SyncPlaysUpload());
						break;
				}
			}
		}
		if (tasks == null) {
			syncType = SYNC_TYPE_ALL;
			tasks = mTaskList.get(SYNC_TYPE_ALL);
		}

		mRemoteExecutor = new RemoteExecutor(mHttpClient, this);

		final long startTime = System.currentTimeMillis();
		if (syncType == SYNC_TYPE_ALL) {
			BggApplication.getInstance().putSyncTimestamp(startTime);
		}
		signalStart();

		try {
			for (SyncTask task : tasks) {
				createNotification(task.getNotification());
				task.execute(mRemoteExecutor, this);
				if (task.isBggDown()) {
					String message = getResources().getString(R.string.notification_bgg_down);
					LOGD(TAG, message);
					sendError(message);
					break;
				}
			}

			LOGD(TAG, "Sync took " + (System.currentTimeMillis() - startTime) + "ms with GZIP "
				+ (mUseGzip ? "on" : "off"));
		} catch (Exception e) {
			LOGE(TAG, "Failed during sync type " + syncType, e);
			sendError(e.toString());
		} finally {
			BggApplication.getInstance().putSyncTimestamp(0);
			signalEnd();
			mResultReceiver = null;
		}
	}

	private List<SyncTask> createTask(SyncTask task) {
		List<SyncTask> taskList = new ArrayList<SyncTask>(1);
		taskList.add(task);
		return taskList;
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
		if (mSuppressNotifications || messageId == SyncTask.NO_NOTIFICATION) {
			return;
		}

		String message = getResources().getString(messageId);
		String title = getResources().getString(R.string.notification_title);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

		Intent intent = new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction(
			Intent.ACTION_SYNC);
		builder.setContentTitle(title).setContentText(message).setTicker(title + " - " + message)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0)).setAutoCancel(cancelNotification);

		mNotificationManager.notify(NOTIFICATION_ID, builder.build());
	}
}
