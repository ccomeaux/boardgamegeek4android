package com.boardgamegeek.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.LocationSelectedEvent;
import com.boardgamegeek.events.LocationSortChangedEvent;
import com.boardgamegeek.events.LocationsCountChangedEvent;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.sorter.LocationsSorter;
import com.boardgamegeek.sorter.LocationsSorterFactory;
import com.boardgamegeek.ui.model.Location;
import com.boardgamegeek.util.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class LocationsFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TOKEN = 0;
	private LocationsAdapter mAdapter;
	private String mSelectedName;
	private LocationsSorter mSorter;

	@DebugLog
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_locations));
		setSort(LocationsSorterFactory.TYPE_DEFAULT);
	}

	@DebugLog
	@Override
	public void onListItemClick(View v, int position, long id) {
		final String name = (String) v.getTag(R.id.name);
		EventBus.getDefault().postSticky(new LocationSelectedEvent(name));
	}

	@DebugLog
	public void onEvent(LocationSelectedEvent event) {
		mSelectedName = event.getLocationName();
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@DebugLog
	public void onEvent(LocationSortChangedEvent event) {
		setSort(event.getSortType());
	}

	@DebugLog
	public void requery() {
		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
	}

	@DebugLog
	public void setSort(int sort) {
		if (mSorter == null || mSorter.getType() != sort) {
			mSorter = LocationsSorterFactory.create(getActivity(), sort);
			if (mSorter == null) {
				mSorter = LocationsSorterFactory.create(getActivity(), LocationsSorterFactory.TYPE_DEFAULT);
			}
			requery();
		}
	}

	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), Plays.buildLocationsUri(), Location.PROJECTION, null, null, mSorter.getOrderByClause());
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
				mAdapter = new LocationsAdapter(getActivity());
				setListAdapter(mAdapter);
			}
			mAdapter.changeCursor(cursor);
			EventBus.getDefault().postSticky(new LocationsCountChangedEvent(cursor.getCount()));
			restoreScrollState();
		} else {
			Timber.d("Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	public class LocationsAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private LayoutInflater mInflater;

		@DebugLog
		public LocationsAdapter(Context context) {
			super(context, null, false);
			mInflater = LayoutInflater.from(context);
		}

		@DebugLog
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_text_2, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@DebugLog
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			Location location = Location.fromCursor(cursor);

			if (TextUtils.isEmpty(location.getName())) {
				holder.name.setText(R.string.no_location);
			} else {
				holder.name.setText(location.getName());
			}
			holder.quantity.setText(getResources()
				.getQuantityString(R.plurals.plays_suffix, location.getPlayCount(), location.getPlayCount()));

			view.setTag(R.id.name, location.getName());
			UIUtils.setActivatedCompat(view, location.getName().equals(mSelectedName));
		}

		@DebugLog
		@Override
		public long getHeaderId(int position) {
			return mSorter.getHeaderId(getCursor(), position);
		}

		@DebugLog
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
			holder.text.setText(getHeaderText(position));
			return convertView;
		}

		@DebugLog
		private String getHeaderText(int position) {
			return mSorter.getHeaderText(getCursor(), position);
		}

		class ViewHolder {
			@InjectView(android.R.id.title) TextView name;
			@InjectView(android.R.id.text1) TextView quantity;

			public ViewHolder(View view) {
				ButterKnife.inject(this, view);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}
}
