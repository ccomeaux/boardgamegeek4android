package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

public class BuddiesFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(BuddiesFragment.class);
	private static final String STATE_SELECTED_ID = "selectedId";

	private BuddiesAdapter mAdapter;
	private int mSelectedBuddyId;

	public interface Callbacks {
		public boolean onBuddySelected(int buddyId, String name, String fullName);

		public void onBuddyCountChanged(int count);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onBuddySelected(int buddyId, String name, String fullName) {
			return true;
		}

		@Override
		public void onBuddyCountChanged(int count) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		if (savedInstanceState != null) {
			mSelectedBuddyId = savedInstanceState.getInt(STATE_SELECTED_ID);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (PreferencesUtils.getSyncBuddies(getActivity())) {
			setEmptyText(getString(R.string.empty_buddies));
		} else {
			setEmptyText(getString(R.string.empty_buddies_sync_off));
		}
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
	public void onListItemClick(View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int buddyId = cursor.getInt(BuddiesQuery.BUDDY_ID);
		final String name = cursor.getString(BuddiesQuery.NAME);
		final String fullName = BuddyUtils.buildFullName(cursor, BuddiesQuery.FIRSTNAME, BuddiesQuery.LASTNAME);
		if (mCallbacks.onBuddySelected(buddyId, name, fullName)) {
			setSelectedBuddyId(buddyId);
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
			+ "!=?", new String[] { Authenticator.getUserId(getActivity()) }, null);
		loader.setUpdateThrottle(2000);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == BuddiesQuery._TOKEN) {
			if (mAdapter == null) {
				mAdapter = new BuddiesAdapter(getActivity());
				setListAdapter(mAdapter);
			}
			mAdapter.changeCursor(cursor);
			mCallbacks.onBuddyCountChanged(cursor.getCount());
			restoreScrollState();
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	public class BuddiesAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private LayoutInflater mInflater;

		public BuddiesAdapter(Context context) {
			super(context, null, false);
			mInflater = LayoutInflater.from(context);
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
			String avatarUrl = cursor.getString(BuddiesQuery.AVATAR_URL);

			UIUtils.setActivatedCompat(view, buddyId == mSelectedBuddyId);

			holder.fullname.setText(buildFullName(firstName, lastName, name).trim());
			holder.name.setText(buildName(firstName, lastName, name).trim());
			loadThumbnail(avatarUrl, holder.avatar, R.drawable.person_image_empty);
		}

		@Override
		public long getHeaderId(int position) {
			if (position < 0) {
				return 0;
			}
			return getHeaderText(position).charAt(0);
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = mInflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(R.id.separator);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(getHeaderText(position));
			return convertView;
		}

		private String getHeaderText(int position) {
			String missingLetter = "-";
			int cur = getCursor().getPosition();
			getCursor().moveToPosition(position);
			String name = getCursor().getString(BuddiesQuery.LASTNAME);
			getCursor().moveToPosition(cur);
			String targetLetter = TextUtils.isEmpty(name) ? missingLetter : name.substring(0, 1).toUpperCase(
				Locale.getDefault());
			return targetLetter;
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

		class ViewHolder {
			TextView fullname;
			TextView name;
			ImageView avatar;
			TextView separator;

			public ViewHolder(View view) {
				fullname = (TextView) view.findViewById(R.id.list_fullname);
				name = (TextView) view.findViewById(R.id.list_name);
				avatar = (ImageView) view.findViewById(R.id.list_avatar);
				separator = (TextView) view.findViewById(R.id.separator);
			}
		}

		class HeaderViewHolder {
			TextView text;
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