package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.UIUtils;

public class BuddiesFragment extends SherlockListFragment implements AbsListView.OnScrollListener,
	LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(BuddiesFragment.class);
	private static final String STATE_SELECTED_ID = "selectedId";

	private CursorAdapter mAdapter;
	private ImageFetcher mImageFetcher;
	private int mSelectedBuddyId;

	public interface Callbacks {
		public boolean onBuddySelected(int buddyId, String buddyName);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onBuddySelected(int buddyId, String buddyName) {
			return true;
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		mImageFetcher.setLoadingImage(R.drawable.person_image_empty);
		mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.thumbnail_list_size));

		if (savedInstanceState != null) {
			mSelectedBuddyId = savedInstanceState.getInt(STATE_SELECTED_ID);
		}

		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.empty_buddies));
		setListShown(false);

		getLoaderManager().restartLoader(BuddiesQuery._TOKEN, getArguments(), this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mSelectedBuddyId > 0) {
			outState.putInt(STATE_SELECTED_ID, mSelectedBuddyId);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.setPauseWork(false);
		mImageFetcher.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int buddyId = cursor.getInt(BuddiesQuery.BUDDY_ID);
		final String buddyName = cursor.getString(BuddiesQuery.NAME);
		if (mCallbacks.onBuddySelected(buddyId, buddyName)) {
			setSelectedBuddyId(buddyId);
		}
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView listView, int scrollState) {
		// Pause disk cache access to ensure smoother scrolling
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
			|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			mImageFetcher.setPauseWork(true);
		} else {
			mImageFetcher.setPauseWork(false);
		}
	}

	public void setSelectedBuddyId(int id) {
		mSelectedBuddyId = id;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		Uri buddiesUri = UIUtils.fragmentArgumentsToIntent(data).getData();
		if (buddiesUri == null) {
			buddiesUri = Buddies.CONTENT_URI;
		}
		CursorLoader loader = new CursorLoader(getActivity(), buddiesUri, BuddiesQuery.PROJECTION, Buddies.BUDDY_ID
			+ "!=?", new String[] { "0" }, null);
		loader.setUpdateThrottle(2000);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new BuddiesAdapter(getActivity());
			setListAdapter(mAdapter);
		}

		int token = loader.getId();
		if (token == BuddiesQuery._TOKEN) {
			mAdapter.changeCursor(cursor);
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	private class BuddiesAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public BuddiesAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_buddy, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			int buddyId = cursor.getInt(BuddiesQuery.BUDDY_ID);
			String firstName = cursor.getString(BuddiesQuery.FIRSTNAME);
			String lastName = cursor.getString(BuddiesQuery.LASTNAME);
			String name = cursor.getString(BuddiesQuery.NAME);
			String url = cursor.getString(BuddiesQuery.AVATAR_URL);

			UIUtils.setActivatedCompat(view, buddyId == mSelectedBuddyId);

			holder.fullname.setText(buildFullName(firstName, lastName, name).trim());
			holder.name.setText(buildName(firstName, lastName, name).trim());
			holder.avatar.setImageResource(R.drawable.person_image_empty);
			mImageFetcher.loadAvatarImage(url, Buddies.buildAvatarUri(buddyId), holder.avatar);
		}

		private String buildFullName(String firstName, String lastName, String name) {
			if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
				return name;
			} else if (TextUtils.isEmpty(firstName)) {
				return lastName;
			} else if (TextUtils.isEmpty(lastName)) {
				return firstName;
			} else {
				return firstName + " " + lastName;
			}
		}

		private String buildName(String firstName, String lastName, String name) {
			if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
				return "";
			} else {
				return name;
			}
		}
	}

	static class ViewHolder {
		TextView fullname;
		TextView name;
		BezelImageView avatar;

		public ViewHolder(View view) {
			fullname = (TextView) view.findViewById(R.id.list_fullname);
			name = (TextView) view.findViewById(R.id.list_name);
			avatar = (BezelImageView) view.findViewById(R.id.list_avatar);
		}
	}

	private interface BuddiesQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID, Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME,
			Buddies.BUDDY_LASTNAME, Buddies.AVATAR_URL };

		int BUDDY_ID = 1;
		int NAME = 2;
		int FIRSTNAME = 3;
		int LASTNAME = 4;
		int AVATAR_URL = 5;
	}
}
