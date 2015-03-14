package com.boardgamegeek.ui;

import android.app.Activity;
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
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.sorter.LocationsSorter;
import com.boardgamegeek.sorter.LocationsSorterFactory;
import com.boardgamegeek.ui.model.Location;
import com.boardgamegeek.util.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class LocationsFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String STATE_SELECTED_NAME = "selectedName";
	private static final String STATE_SORT_TYPE = "sortType";
	private static final int TOKEN = 0;
	private LocationsAdapter mAdapter;
	private String mSelectedName;
	private LocationsSorter mSorter;

	public interface Callbacks {
		public boolean onLocationSelected(String name);

		public void onLocationCountChanged(int count);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onLocationSelected(String name) {
			return true;
		}

		@Override
		public void onLocationCountChanged(int count) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}
		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mSelectedName = savedInstanceState.getString(STATE_SELECTED_NAME);
		}
	}

	@DebugLog
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = LocationsSorterFactory.TYPE_DEFAULT;
		if (savedInstanceState != null) {
			sortType = savedInstanceState.getInt(STATE_SORT_TYPE);
		}
		mSorter = LocationsSorterFactory.create(sortType, getActivity());

		setEmptyText(getString(R.string.empty_locations));
		requery();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!TextUtils.isEmpty(mSelectedName)) {
			outState.putString(STATE_SELECTED_NAME, mSelectedName);
		}
		outState.putInt(STATE_SORT_TYPE, mSorter.getType());
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(View v, int position, long id) {
		final String name = (String) v.getTag(R.id.name);
		if (mCallbacks.onLocationSelected(name)) {
			setSelectedLocation(name);
		}
	}

	@DebugLog
	public void requery() {
		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
	}

	public int getSort() {
		return mSorter.getType();
	}

	@DebugLog
	public void setSort(int sort) {
		if (mSorter.getType() != sort) {
			mSorter = LocationsSorterFactory.create(sort, getActivity());
			if (mSorter == null) {
				mSorter = LocationsSorterFactory.create(LocationsSorterFactory.TYPE_DEFAULT, getActivity());
			}
			requery();
		}
	}

	public void setSelectedLocation(String name) {
		mSelectedName = name;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
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
			mCallbacks.onLocationCountChanged(cursor.getCount());
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
				.getQuantityString(R.plurals.plays, location.getPlayCount(), location.getPlayCount()));

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
