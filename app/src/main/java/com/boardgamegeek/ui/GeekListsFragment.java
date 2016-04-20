package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.model.GeekListsResponse;
import com.boardgamegeek.ui.adapter.PaginatedArrayAdapter;
import com.boardgamegeek.ui.loader.PaginatedData;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.util.ActivityUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;

public class GeekListsFragment extends BggListFragment implements OnScrollListener, LoaderManager.LoaderCallbacks<PaginatedData<GeekListEntry>> {
	private static final int LOADER_ID = 0;
	private static final int SORT_TYPE_INVALID = -1;
	private static final int SORT_TYPE_HOT = 0;
	private static final int SORT_TYPE_RECENT = 1;
	private static final int SORT_TYPE_ACTIVE = 2;
	@State int sortType = 0;
	private GeekListsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnScrollListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_geeklists));
	}

	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.geeklists, menu);
		switch (sortType) {
			case SORT_TYPE_RECENT:
				menu.findItem(R.id.menu_sort_geeklists_recent).setChecked(true);
				break;
			case SORT_TYPE_ACTIVE:
				menu.findItem(R.id.menu_sort_geeklists_active).setChecked(true);
				break;
			case SORT_TYPE_HOT:
			default:
				menu.findItem(R.id.menu_sort_geeklists_hot).setChecked(true);
				break;
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int sort = SORT_TYPE_INVALID;
		int id = item.getItemId();
		switch (id) {
			case R.id.menu_sort_geeklists_recent:
				if (sortType != SORT_TYPE_RECENT) {
					sort = SORT_TYPE_RECENT;
				}
				break;
			case R.id.menu_sort_geeklists_active:
				if (sortType != SORT_TYPE_ACTIVE) {
					sort = SORT_TYPE_ACTIVE;
				}
				break;
			case R.id.menu_sort_geeklists_hot:
				if (sortType != SORT_TYPE_HOT) {
					sort = SORT_TYPE_HOT;
				}
				break;
		}
		if (sort != SORT_TYPE_INVALID) {
			sortType = sort;
			item.setChecked(true);
			if (adapter != null) {
				adapter.clear();
			}
			getLoaderManager().restartLoader(LOADER_ID, null, this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean padTop() {
		return true;
	}

	@Override
	protected boolean dividerShown() {
		return true;
	}

	public void loadMoreResults() {
		if (isAdded()) {
			Loader<List<GeekListEntry>> loader = getLoaderManager().getLoader(LOADER_ID);
			if (loader != null) {
				loader.forceLoad();
			}
		}
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		GeekListRowViewBinder.ViewHolder holder = (GeekListRowViewBinder.ViewHolder) convertView.getTag();
		if (holder != null) {
			Intent intent = new Intent(getActivity(), GeekListActivity.class);
			intent.putExtra(ActivityUtils.KEY_ID, holder.id);
			intent.putExtra(ActivityUtils.KEY_TITLE, holder.title.getText());
			startActivity(intent);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (!isLoading() && loaderHasMoreResults() && visibleItemCount != 0
			&& firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
			saveScrollState();
			loadMoreResults();
		}
	}

	@Override
	public Loader<PaginatedData<GeekListEntry>> onCreateLoader(int id, Bundle data) {
		return new GeekListsLoader(getActivity(), sortType);
	}

	@Override
	public void onLoadFinished(Loader<PaginatedData<GeekListEntry>> loader, PaginatedData<GeekListEntry> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new GeekListsAdapter(getActivity(), data);
			setListAdapter(adapter);
		} else {
			adapter.update(data);
		}
		restoreScrollState();
	}

	@Override
	public void onLoaderReset(Loader<PaginatedData<GeekListEntry>> loader) {
	}

	private boolean isLoading() {
		final GeekListsLoader loader = getLoader();
		return (loader == null) || loader.isLoading();
	}

	private boolean loaderHasMoreResults() {
		final GeekListsLoader loader = getLoader();
		return (loader != null) && loader.hasMoreResults();
	}

	private GeekListsLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<GeekListEntry>> loader = getLoaderManager().getLoader(LOADER_ID);
			return (GeekListsLoader) loader;
		}
		return null;
	}

	private static class GeekListsLoader extends PaginatedLoader<GeekListEntry> {
		private final BggService bggService;
		private final int sortType;

		public GeekListsLoader(Context context, int sortType) {
			super(context);
			bggService = Adapter.createForJson();
			this.sortType = sortType;
		}

		@Override
		public PaginatedData<GeekListEntry> loadInBackground() {
			super.loadInBackground();
			GeekListsData data;
			try {
				int page = getNextPage();
				String sort = BggService.GEEK_LIST_SORT_HOT;
				switch (sortType) {
					case SORT_TYPE_RECENT:
						sort = BggService.GEEK_LIST_SORT_RECENT;
						break;
					case SORT_TYPE_ACTIVE:
						sort = BggService.GEEK_LIST_SORT_ACTIVE;
						break;
				}
				data = new GeekListsData(bggService.geekLists(page, sort).execute().body(), page);
			} catch (Exception e) {
				data = new GeekListsData(e);
			}
			return data;
		}
	}

	static class GeekListsData extends PaginatedData<GeekListEntry> {
		public GeekListsData(GeekListsResponse response, int page) {
			super(response.getGeekListEntries(), response.getTotalCount(), page, GeekListsResponse.PAGE_SIZE);
		}

		public GeekListsData(Exception e) {
			super(e);
		}
	}

	private class GeekListsAdapter extends PaginatedArrayAdapter<GeekListEntry> {
		public GeekListsAdapter(Context context, PaginatedData<GeekListEntry> data) {
			super(context, R.layout.row_geeklist, data);
		}

		@Override
		protected boolean isLoaderLoading() {
			return isLoading();
		}

		@Override
		protected void bind(View view, GeekListEntry item) {
			GeekListRowViewBinder.bindActivityView(view, item);
		}
	}

	public static class GeekListRowViewBinder {
		public static class ViewHolder {
			public int id;
			@SuppressWarnings("unused") @Bind(R.id.geeklist_title) TextView title;
			@SuppressWarnings("unused") @Bind(R.id.geeklist_creator) TextView creator;
			@SuppressWarnings("unused") @Bind(R.id.geeklist_items) TextView numItems;
			@SuppressWarnings("unused") @Bind(R.id.geeklist_thumbs) TextView numThumbs;

			public ViewHolder(View view) {
				ButterKnife.bind(this, view);
			}
		}

		public static void bindActivityView(View rootView, GeekListEntry geekListEntry) {
			ViewHolder tag = (ViewHolder) rootView.getTag();
			final ViewHolder holder;
			if (tag != null) {
				holder = tag;
			} else {
				holder = new ViewHolder(rootView);
				rootView.setTag(holder);
			}

			Context context = rootView.getContext();
			holder.id = geekListEntry.getId();
			holder.title.setText(geekListEntry.getTitle());
			holder.creator.setText(context.getString(R.string.by_prefix, geekListEntry.getAuthor()));
			holder.numItems.setText(context.getString(R.string.items_suffix, geekListEntry.getNumberOfItems()));
			holder.numThumbs.setText(context.getString(R.string.thumbs_suffix, geekListEntry.getNumberOfThumbs()));
		}
	}
}
