package com.boardgamegeek.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.ui.HomeActivity;

public class SyncService extends IntentService {
	private final static String TAG = "SyncService";

	public static final String EXTRA_STATUS_RECEIVER = "com.boardgamegeek.extra.STATUS_RECEIVER";

	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_COMPLETE = 2;
	public static final int STATUS_ERROR = 3;

	private static final String BASE_URL = "http://boardgamegeek.com/xmlapi2/";
	private static final int NOTIFICATION_ID = 1;

	private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;
	private static final int TIMEOUT_SECS = 20;
	private static final int BUFFER_SIZE = 8192;
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	private static boolean mUseGzip = true;

	private ResultReceiver mResultReceiver;
	private NotificationManager mNotificationManager;
	private HttpClient mHttpClient;
	private ContentResolver mContentResolver;
	private String mUsername;

	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mHttpClient = createHttpClient(this);
		mContentResolver = getContentResolver();
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
				new String[] { Buddies.BUDDY_NAME },
				null, null, null);
			RemoteBuddyUserHandler handler = new RemoteBuddyUserHandler();
			while (cursor.moveToNext()) {
				final String url = URLEncoder.encode(cursor.getString(0));
				remoteExecutor.executeGet(BASE_URL + "user?name=" + url, handler);
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

	private static HttpClient createHttpClient(Context context) {
		final HttpParams params = createHttpParams(context);
		final DefaultHttpClient client = new DefaultHttpClient(params);
		if (mUseGzip) {
			addGzipInterceptors(client);
		}
		return client;
	}

	private static HttpParams createHttpParams(Context context) {
		final HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_SECS * SECOND_IN_MILLIS);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT_SECS * SECOND_IN_MILLIS);
		HttpConnectionParams.setSocketBufferSize(params, BUFFER_SIZE);
		if (mUseGzip) {
			HttpProtocolParams.setUserAgent(params, buildUserAgent(context));
		}
		return params;
	}

	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " (" + info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	private static void addGzipInterceptors(DefaultHttpClient client) {
		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});
	}

	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}
}
