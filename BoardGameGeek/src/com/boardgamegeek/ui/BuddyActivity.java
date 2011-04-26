package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BuddyActivity extends Activity implements AsyncQueryListener {
	// private final static String TAG = "BuddyActivity";

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
			if (!cursor.moveToFirst()) {
				return;
			}

			mFullName.setText(cursor.getString(BuddiesQuery.FIRSTNAME) + " "
					+ cursor.getString(BuddiesQuery.LASTNAME));
			mName.setText(cursor.getString(BuddiesQuery.NAME));
			mId.setText(cursor.getString(BuddiesQuery.BUDDY_ID));

			if (BggApplication.getInstance().getImageLoad()) {
				final String url = cursor.getString(BuddiesQuery.AVATAR_URL);
				new AvatarTask().execute(url);
			}

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

	private class AvatarTask extends AsyncTask<String, Void, Drawable> {

		@Override
		protected void onPreExecute() {
			findViewById(R.id.buddy_progress).setVisibility(View.VISIBLE);
		}

		@Override
		protected Drawable doInBackground(String... params) {
			return ImageCache.getImage(BuddyActivity.this, params[0]);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			findViewById(R.id.buddy_progress).setVisibility(View.GONE);
			if (result == null) {
				mAvatarImage.setVisibility(View.GONE);
			} else {
				mAvatarImage.setVisibility(View.VISIBLE);
				mAvatarImage.setImageDrawable(result);
			}
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { Buddies.BUDDY_ID, Buddies.BUDDY_NAME,
				Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
				Buddies.AVATAR_URL, };

		int BUDDY_ID = 0;
		int NAME = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
		int AVATAR_URL = 4;
	}
}
