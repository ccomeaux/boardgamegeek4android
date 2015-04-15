package com.boardgamegeek.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import com.boardgamegeek.events.BuddiesCountChangedEvent;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.ui.model.Buddy;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class BuddiesFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TOKEN = 0;
	private static final String SORT_COLUMN = Buddies.BUDDY_LASTNAME;
	private BuddiesAdapter mAdapter;
	private int mSelectedBuddyId;

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}


	@DebugLog
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (PreferencesUtils.getSyncBuddies(getActivity())) {
			setEmptyText(getString(R.string.empty_buddies));
		} else {
			setEmptyText(getString(R.string.empty_buddies_sync_off));
		}
		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
	}

	@DebugLog
	@Override
	public void onListItemClick(View v, int position, long id) {
		final int buddyId = (int) v.getTag(R.id.id);
		final String name = String.valueOf(v.getTag(R.id.name));
		final String fullName = String.valueOf(v.getTag(R.id.full_name));
		EventBus.getDefault().postSticky(new BuddySelectedEvent(buddyId, name, fullName));
	}

	@DebugLog
	public void onEvent(BuddySelectedEvent event) {
		mSelectedBuddyId = event.buddyId;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		Uri buddiesUri = UIUtils.fragmentArgumentsToIntent(data).getData();
		if (buddiesUri == null) {
			buddiesUri = Buddies.CONTENT_URI;
		}

		CursorLoader loader = new CursorLoader(getActivity(), buddiesUri,
			Buddy.PROJECTION,
			Buddies.BUDDY_ID + "!=? AND " + Buddies.BUDDY_FLAG + "=1",
			new String[] { Authenticator.getUserId(getActivity()) },
			null);
		loader.setUpdateThrottle(2000);
		return loader;
	}

	@DebugLog
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == TOKEN) {
			if (mAdapter == null) {
				mAdapter = new BuddiesAdapter(getActivity());
				setListAdapter(mAdapter);
			}
			mAdapter.changeCursor(cursor);
			EventBus.getDefault().postSticky(new BuddiesCountChangedEvent(cursor.getCount()));
			restoreScrollState();
		} else {
			Timber.d("Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@DebugLog
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

			Buddy buddy = Buddy.fromCursor(cursor);

			UIUtils.setActivatedCompat(view, buddy.getId() == mSelectedBuddyId);

			loadThumbnail(buddy.getAvatarUrl(), holder.avatar, R.drawable.person_image_empty);

			if (TextUtils.isEmpty(buddy.getFullName())) {
				holder.fullName.setText(buddy.getUserName());
				holder.name.setVisibility(View.GONE);
			} else {
				holder.fullName.setText(buddy.getFullName());
				holder.name.setVisibility(View.VISIBLE);
				holder.name.setText(buddy.getUserName());
			}

			view.setTag(R.id.id, buddy.getId());
			view.setTag(R.id.name, buddy.getUserName());
			view.setTag(R.id.full_name, buddy.getFullName());
		}

		@Override
		public long getHeaderId(int position) {
			if (position < 0) {
				return 0;
			}
			return CursorUtils.getFirstCharacter(getCursor(), position, SORT_COLUMN, "-").charAt(0);
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = mInflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(CursorUtils.getFirstCharacter(getCursor(), position, SORT_COLUMN, "-"));
			return convertView;
		}

		class ViewHolder {
			@InjectView(R.id.list_fullname) TextView fullName;
			@InjectView(R.id.list_name) TextView name;
			@InjectView(R.id.list_avatar) ImageView avatar;

			public ViewHolder(View view) {
				ButterKnife.inject(this, view);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}
}