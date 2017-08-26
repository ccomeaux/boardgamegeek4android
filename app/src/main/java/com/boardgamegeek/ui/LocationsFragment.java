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
import com.boardgamegeek.util.fabric.SortEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class LocationsFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TOKEN = 0;
	private LocationsAdapter adapter;
	private String selectedName;
	private LocationsSorter sorter;

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

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(LocationSelectedEvent event) {
		selectedName = event.getLocationName();
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(LocationSortChangedEvent event) {
		setSort(event.getSortType());
	}

	@DebugLog
	public void requery() {
		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
	}

	@DebugLog
	public void setSort(int sortType) {
		if (sorter == null || sorter.getType() != sortType) {
			SortEvent.log("Locations", String.valueOf(sortType));
			sorter = LocationsSorterFactory.create(getActivity(), sortType);
			requery();
		}
	}

	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), Plays.buildLocationsUri(), Location.PROJECTION, null, null, sorter.getOrderByClause());
	}

	@DebugLog
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == TOKEN) {
			if (adapter == null) {
				adapter = new LocationsAdapter(getActivity());
				setListAdapter(adapter);
			}
			adapter.changeCursor(cursor);
			EventBus.getDefault().postSticky(new LocationsCountChangedEvent(cursor.getCount()));
			restoreScrollState();
		} else {
			Timber.d("Query complete, Not Actionable: %s", token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		adapter.changeCursor(null);
	}

	public class LocationsAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private final LayoutInflater inflater;

		@DebugLog
		public LocationsAdapter(Context context) {
			super(context, null, false);
			inflater = LayoutInflater.from(context);
		}

		@DebugLog
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = inflater.inflate(R.layout.row_text_2, parent, false);
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
			UIUtils.setActivatedCompat(view, location.getName().equals(selectedName));
		}

		@DebugLog
		@Override
		public long getHeaderId(int position) {
			return sorter.getHeaderId(getCursor(), position);
		}

		@DebugLog
		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = inflater.inflate(R.layout.row_header, parent, false);
				holder.text = convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(getHeaderText(position));
			return convertView;
		}

		@DebugLog
		private String getHeaderText(int position) {
			return sorter.getHeaderText(getCursor(), position);
		}

		class ViewHolder {
			@BindView(android.R.id.title) TextView name;
			@BindView(android.R.id.text1) TextView quantity;

			public ViewHolder(View view) {
				ButterKnife.bind(this, view);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}
}
