package com.boardgamegeek.ui;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class BuddyActivity extends Activity implements AsyncQueryListener {
	private final static String TAG = "BuddyActivity";

	private static HttpClient sHttpClient;

	private Uri mBuddyUri;
	private NotifyingAsyncQueryHandler mHandler;

	private TextView mFullName;
	private ImageView mAvatarImage;
	private TextView mName;
	private TextView mId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buddy);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		mFullName = (TextView) findViewById(R.id.buddy_full_name);
		mAvatarImage = (ImageView) findViewById(R.id.buddy_avatar);
		mName = (TextView) findViewById(R.id.buddy_name);
		mId = (TextView) findViewById(R.id.buddy_id);

		final Intent intent = getIntent();
		mBuddyUri = intent.getData();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBuddyUri, BuddiesQuery.PROJECTION);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst())
				return;

			mFullName.setText(cursor.getString(BuddiesQuery.FIRSTNAME) + " "
					+ cursor.getString(BuddiesQuery.LASTNAME));
			mName.setText(cursor.getString(BuddiesQuery.NAME));
			mId.setText(cursor.getString(BuddiesQuery.BUDDY_ID));

			final String url = cursor.getString(BuddiesQuery.AVATAR_URL);
			new BuddyAvatarTask().execute(url);

		} finally {
			cursor.close();
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	private static synchronized HttpClient getHttpClient(Context context) {
		if (sHttpClient == null) {
			sHttpClient = HttpUtils.createHttpClient(context, true);
		}
		return sHttpClient;
	}

	private class BuddyAvatarTask extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(String... params) {
			final String url = params[0];

			try {
				final Context context = BuddyActivity.this;
				final HttpClient client = getHttpClient(context);
				final HttpGet get = new HttpGet(url);
				final HttpResponse response = client.execute(get);
				final HttpEntity entity = response.getEntity();

				final int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK || entity == null) {
					Log.w(TAG, "Didn't find avatar");
				}

				final byte[] imageData = EntityUtils.toByteArray(entity);
				return BitmapFactory.decodeByteArray(imageData, 0,
						imageData.length);

			} catch (Exception e) {
				Log.e(TAG, "Problem loading buddy avatar", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result == null) {
				mAvatarImage.setVisibility(View.GONE);
			} else {
				mAvatarImage.setVisibility(View.VISIBLE);
				mAvatarImage.setImageBitmap(result);
			}
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = {
			Buddies.BUDDY_ID,
			Buddies.BUDDY_NAME,
			Buddies.BUDDY_FIRSTNAME,
			Buddies.BUDDY_LASTNAME,
			Buddies.AVATAR_URL,
		};

		int BUDDY_ID = 0;
		int NAME = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
		int AVATAR_URL = 4;
	}
}
