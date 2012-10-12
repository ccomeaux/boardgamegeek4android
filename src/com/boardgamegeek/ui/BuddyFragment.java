package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.UIUtils;

public class BuddyFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private Uri mBuddyUri;

	private TextView mFullName;
	private TextView mName;
	private TextView mId;
	private ImageView mAvatar;

	private ImageFetcher mImageFetcher;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mBuddyUri = intent.getData();

		if (mBuddyUri == null) {
			return;
		}

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		mImageFetcher.setImageFadeIn(false);
		mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.avatar_size));

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_buddy, null);

		mFullName = (TextView) rootView.findViewById(R.id.buddy_full_name);
		mName = (TextView) rootView.findViewById(R.id.buddy_name);
		mId = (TextView) rootView.findViewById(R.id.buddy_id);
		mAvatar = (ImageView) rootView.findViewById(R.id.buddy_avatar);

		getLoaderManager().restartLoader(BuddyQuery._TOKEN, null, this);

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == BuddyQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), mBuddyUri, BuddyQuery.PROJECTION, null, null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == BuddyQuery._TOKEN) {
			onBuddyQueryComplete(cursor);
		} else {
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private void onBuddyQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		int id = cursor.getInt(BuddyQuery.BUDDY_ID);
		String firstName = cursor.getString(BuddyQuery.FIRSTNAME);
		String lastName = cursor.getString(BuddyQuery.LASTNAME);
		String name = cursor.getString(BuddyQuery.NAME);

		mFullName.setText(buildFullName(firstName, lastName));
		mName.setText(name);
		mId.setText(String.valueOf(id));

		final String avatarUrl = cursor.getString(BuddyQuery.AVATAR_URL);
		if (!TextUtils.isEmpty(avatarUrl)) {
			mImageFetcher
				.loadAvatarImage(avatarUrl, Buddies.buildAvatarUri(id), mAvatar, R.drawable.person_image_empty);
		}
	}

	private String buildFullName(String firstName, String lastName) {
		if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
			return "";
		} else if (TextUtils.isEmpty(firstName)) {
			return lastName.trim();
		} else if (TextUtils.isEmpty(lastName)) {
			return firstName.trim();
		} else {
			return firstName.trim() + " " + lastName.trim();
		}
	}

	private interface BuddyQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
			Buddies.AVATAR_URL };

		int BUDDY_ID = 0;
		int NAME = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
		int AVATAR_URL = 4;
	}
}
