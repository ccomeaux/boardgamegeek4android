package com.boardgamegeek.ui;

import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
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
import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.UIUtils;

public class LocationsFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String STATE_SELECTED_NAME = "selectedName";

	private LocationsAdapter mAdapter;
	private String mSelectedName;

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

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_locations));
		getLoaderManager().restartLoader(LocationsQuery._TOKEN, getArguments(), this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!TextUtils.isEmpty(mSelectedName)) {
			outState.putString(STATE_SELECTED_NAME, mSelectedName);
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
		final String name = cursor.getString(LocationsQuery.LOCATION);
		if (mCallbacks.onLocationSelected(name)) {
			setSelectedLocation(name);
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
		return new CursorLoader(getActivity(), Plays.buildLocationsUri(), LocationsQuery.PROJECTION, null, null, null);
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
			View row = mInflater.inflate(R.layout.row_play_location, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			String name = cursor.getString(LocationsQuery.LOCATION);
			UIUtils.setActivatedCompat(view, name.equals(mSelectedName));
			if (TextUtils.isEmpty(name)) {
				name = getString(R.string.no_location);
			}
			holder.name.setText(name);
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
				holder.text = (TextView) convertView.findViewById(android.R.id.title);
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
			String name = getCursor().getString(LocationsQuery.LOCATION);
			getCursor().moveToPosition(cur);
			String targetLetter = TextUtils.isEmpty(name) ? missingLetter : name.substring(0, 1).toUpperCase(
				Locale.getDefault());
			return targetLetter;
		}

		class ViewHolder {
			@InjectView(R.id.name) TextView name;

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

		String[] PROJECTION = { BaseColumns._ID, Plays.LOCATION };

		int LOCATION = 1;
	}
}
