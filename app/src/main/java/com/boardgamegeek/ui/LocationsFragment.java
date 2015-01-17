package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
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
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.UIUtils;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class LocationsFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String STATE_SELECTED_NAME = "selectedName";
	private static final String STATE_SORT = "sort";
	public static final int SORT_NAME = 0;
	public static final int SORT_QUANTITY = 1;

	private LocationsAdapter mAdapter;
	private String mSelectedName;
	private int mSort = SORT_NAME;

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
			mSort = savedInstanceState.getInt(STATE_SORT);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_locations));
		requery();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!TextUtils.isEmpty(mSelectedName)) {
			outState.putString(STATE_SELECTED_NAME, mSelectedName);
		}
		outState.putInt(STATE_SORT, mSort);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final String name = cursor.getString(LocationsQuery.LOCATION);
		if (mCallbacks.onLocationSelected(name)) {
			setSelectedLocation(name);
		}
	}

	public void requery() {
		getLoaderManager().restartLoader(LocationsQuery._TOKEN, getArguments(), this);
	}

	public int getSort() {
		return mSort;
	}

	public void setSort(int sort) {
		if (sort != SORT_NAME && sort != SORT_QUANTITY) {
			sort = SORT_NAME;
		}
		if (sort != mSort) {
			mSort = sort;
			requery();
		}
	}

	public void setSelectedLocation(String name) {
		mSelectedName = name;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		String sortOrder = null;
		if (mSort == SORT_QUANTITY) {
			sortOrder = Plays.SUM_QUANTITY + " DESC, " + Plays.DEFAULT_SORT;
		}
		return new CursorLoader(getActivity(), Plays.buildLocationsUri(), LocationsQuery.PROJECTION, null, null, sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == LocationsQuery._TOKEN) {
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

		public LocationsAdapter(Context context) {
			super(context, null, false);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_text_2, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			String name = cursor.getString(LocationsQuery.LOCATION);
			if (TextUtils.isEmpty(name)) {
				name = getString(R.string.no_location);
			}
			int quantity = cursor.getInt(LocationsQuery.SUM_QUANTITY);

			holder.name.setText(name);
			holder.quantity.setText(getResources().getQuantityString(R.plurals.plays, quantity, quantity));

			UIUtils.setActivatedCompat(view, name.equals(mSelectedName));
		}

		@Override
		public long getHeaderId(int position) {
			if (position < 0) {
				return 0;
			}
			return getHeaderText(position).hashCode();
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
			holder.text.setText(getHeaderText(position));
			return convertView;
		}

		private String getHeaderText(int position) {
			String headerText = "-";
			int cur = getCursor().getPosition();
			getCursor().moveToPosition(position);
			if (mSort == SORT_NAME) {
				headerText = getCursor().getString(LocationsQuery.LOCATION);
				if (!TextUtils.isEmpty(headerText)) {
					headerText = headerText.substring(0, 1).toUpperCase(Locale.getDefault());
				}
			} else if (mSort == SORT_QUANTITY) {
				int q = getCursor().getInt(LocationsQuery.SUM_QUANTITY);
				String prefix = String.valueOf(q).substring(0, 1);
				String suffix = "";
				if (q > 10000) {
					suffix = "0000+";
				} else if (q > 1000) {
					suffix = "000+";
				} else if (q > 100) {
					suffix = "00+";
				} else if (q > 10) {
					suffix = "0+";
				}
				headerText = prefix + suffix;
			}
			getCursor().moveToPosition(cur);
			return headerText;
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

	private interface LocationsQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { BaseColumns._ID, Plays.LOCATION, Plays.SUM_QUANTITY };

		int LOCATION = 1;
		int SUM_QUANTITY = 2;
	}
}
