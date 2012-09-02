package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.UIUtils;

public class BuddiesFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(BuddiesFragment.class);

	private static final String STATE_SELECTED_ID = "selectedId";

	private CursorAdapter mAdapter;
	private int mSelectedBuddyId;
	private int mBuddyQueryToken;

	public interface Callbacks {
		/** Return true to select (activate) the session in the list, false otherwise. */
		public boolean onBuddySelected(int buddyId);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onBuddySelected(int sessionId) {
			return true;
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mSelectedBuddyId = savedInstanceState.getInt(STATE_SELECTED_ID);
		}
		reloadFromArguments(getArguments());
	}

	protected void reloadFromArguments(Bundle arguments) {
		setListAdapter(null);

		mAdapter = new BuddiesAdapter(getActivity());
		setListAdapter(mAdapter);

		mBuddyQueryToken = BuddiesQuery._TOKEN;
		getLoaderManager().restartLoader(mBuddyQueryToken, arguments, this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.empty_buddies));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
		activity.getContentResolver().registerContentObserver(BggContract.Buddies.CONTENT_URI, true, mObserver);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
		getActivity().getContentResolver().unregisterContentObserver(mObserver);
	}

	/** {@inheritDoc} */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int buddyId = cursor.getInt(BuddiesQuery.BUDDY_ID);
		if (mCallbacks.onBuddySelected(buddyId)) {
			mSelectedBuddyId = buddyId;
			mAdapter.notifyDataSetChanged();
		}
	}

	public void setSelectedSessionId(int id) {
		mSelectedBuddyId = id;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private final ContentObserver mObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() == null) {
				return;
			}

			Loader<Cursor> loader = getLoaderManager().getLoader(mBuddyQueryToken);
			if (loader != null) {
				loader.forceLoad();
			}
		}
	};

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		final Intent intent = SimpleSinglePaneActivity.fragmentArgumentsToIntent(data);
		final Uri buddiesUri = intent.getData();
		return new CursorLoader(getActivity(), buddiesUri, BuddiesQuery.PROJECTION, Buddies.BUDDY_ID + "!=?",
			new String[] { "0" }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == BuddiesQuery._TOKEN) {
			mAdapter.changeCursor(cursor);
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
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

			UIUtils.setActivatedCompat(view, buddyId == mSelectedBuddyId);

			String firstName = cursor.getString(BuddiesQuery.FIRSTNAME);
			String lastName = cursor.getString(BuddiesQuery.LASTNAME);
			String name = cursor.getString(BuddiesQuery.NAME);
			holder.fullname.setText(buildFullName(firstName, lastName, name).trim());
			holder.name.setText(buildName(firstName, lastName, name).trim());
			holder.avatarUrl = Buddies.buildAvatarUri(buddyId);

			// Drawable thumbnail = ImageUtils.getDrawable(BuddiesActivity.this, holder.avatarUrl);
			// if (thumbnail == null) {
			// holder.avatar.setVisibility(View.GONE);
			// } else {
			// holder.avatarUrl = null;
			// holder.avatar.setImageDrawable(thumbnail);
			// holder.avatar.setVisibility(View.VISIBLE);
			// }
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
		Uri avatarUrl;

		public ViewHolder(View view) {
			fullname = (TextView) view.findViewById(R.id.list_fullname);
			name = (TextView) view.findViewById(R.id.list_name);
			avatar = (BezelImageView) view.findViewById(R.id.list_avatar);
		}
	}

	private interface BuddiesQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID, Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME,
			Buddies.BUDDY_LASTNAME };

		// int _ID = 0;
		int BUDDY_ID = 1;
		int NAME = 2;
		int FIRSTNAME = 3;
		int LASTNAME = 4;
	}
}
