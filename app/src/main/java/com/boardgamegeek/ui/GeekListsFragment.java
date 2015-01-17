package com.boardgamegeek.ui;

import java.util.List;

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
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.model.GeekListsResponse;
import com.boardgamegeek.ui.widget.PaginatedArrayAdapter;
import com.boardgamegeek.ui.widget.PaginatedData;
import com.boardgamegeek.ui.widget.PaginatedLoader;
import com.boardgamegeek.util.GeekListUtils;

public class GeekListsFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<PaginatedData<GeekListEntry>> {
	private static final int GEEKLISTS_LOADER_ID = 0;
	private static final String STATE_SORT = "SORT";
	private static final int SORT_INVALID = -1;
	private static final int SORT_HOT = 0;
	private static final int SORT_RECENT = 1;
	private static final int SORT_ACTIVE = 2;
	private int mSort = 0;

	private GeekListsAdapter mGeekListsAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		if (savedInstanceState != null) {
			mSort = savedInstanceState.getInt(STATE_SORT);
		}
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
		getLoaderManager().initLoader(GEEKLISTS_LOADER_ID, null, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SORT, mSort);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.geeklists, menu);
		switch (mSort) {
			case SORT_RECENT:
				menu.findItem(R.id.menu_sort_geeklists_recent).setChecked(true);
				break;
			case SORT_ACTIVE:
				menu.findItem(R.id.menu_sort_geeklists_active).setChecked(true);
				break;
			case SORT_HOT:
			default:
				menu.findItem(R.id.menu_sort_geeklists_hot).setChecked(true);
				break;
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int sort = SORT_INVALID;
		int id = item.getItemId();
		switch (id) {
			case R.id.menu_sort_geeklists_recent:
				if (mSort != SORT_RECENT) {
					sort = SORT_RECENT;
				}
				break;
			case R.id.menu_sort_geeklists_active:
				if (mSort != SORT_ACTIVE) {
					sort = SORT_ACTIVE;
				}
				break;
			case R.id.menu_sort_geeklists_hot:
				if (mSort != SORT_HOT) {
					sort = SORT_HOT;
				}
				break;
		}
		if (sort != SORT_INVALID) {
			mSort = sort;
			item.setChecked(true);
			if (mGeekListsAdapter != null) {
				mGeekListsAdapter.clear();
			}
			getLoaderManager().restartLoader(GEEKLISTS_LOADER_ID, null, this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void loadMoreResults() {
		if (isAdded()) {
			Loader<List<GeekListEntry>> loader = getLoaderManager().getLoader(GEEKLISTS_LOADER_ID);
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
			intent.putExtra(GeekListUtils.KEY_ID, holder.id);
			intent.putExtra(GeekListUtils.KEY_TITLE, holder.title.getText());
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
		return new GeekListsLoader(getActivity(), mSort);
	}

	@Override
	public void onLoadFinished(Loader<PaginatedData<GeekListEntry>> loader, PaginatedData<GeekListEntry> data) {
		if (getActivity() == null) {
			return;
		}

		if (mGeekListsAdapter == null) {
			mGeekListsAdapter = new GeekListsAdapter(getActivity(), R.layout.row_geeklist, data);
			setListAdapter(mGeekListsAdapter);
		} else {
			mGeekListsAdapter.update(data);
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
			Loader<PaginatedData<GeekListEntry>> loader = getLoaderManager().getLoader(GEEKLISTS_LOADER_ID);
			return (GeekListsLoader) loader;
		}
		return null;
	}

	private static class GeekListsLoader extends PaginatedLoader<GeekListEntry> {
		private BggService mService;
		private int mSort;

		public GeekListsLoader(Context context, int sort) {
			super(context);
			mService = Adapter.createWithJson();
			mSort = sort;
		}

		@Override
		public PaginatedData<GeekListEntry> loadInBackground() {
			super.loadInBackground();
			GeekListsData data;
			try {
				int page = getNextPage();
				String sort = BggService.GEEKLIST_SORT_HOT;
				switch (mSort) {
					case SORT_RECENT:
						sort = BggService.GEEKLIST_SORT_RECENT;
						break;
					case SORT_ACTIVE:
						sort = BggService.GEEKLIST_SORT_ACTIVE;
						break;
				}
				data = new GeekListsData(mService.geekLists(page, sort), page);
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
		public GeekListsAdapter(Context context, int resource, PaginatedData<GeekListEntry> data) {
			super(context, resource, data);
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
			@InjectView(R.id.geeklist_title) TextView title;
			@InjectView(R.id.geeklist_creator) TextView creator;
			@InjectView(R.id.geeklist_items) TextView numItems;
			@InjectView(R.id.geeklist_thumbs) TextView numThumbs;

			public ViewHolder(View view) {
				ButterKnife.inject(this, view);
			}
		}

		public static void bindActivityView(View rootView, GeekListEntry geeklist) {
			ViewHolder tag = (ViewHolder) rootView.getTag();
			final ViewHolder holder;
			if (tag != null) {
				holder = tag;
			} else {
				holder = new ViewHolder(rootView);
				rootView.setTag(holder);
			}

			Context context = rootView.getContext();
			holder.id = geeklist.getId();
			holder.title.setText(geeklist.getTitle());
			holder.creator.setText(context.getString(R.string.by_prefix, geeklist.getAuthor()));
			holder.numItems.setText(context.getString(R.string.items_suffix, geeklist.getNumberOfItems()));
			holder.numThumbs.setText(context.getString(R.string.thumbs_suffix, geeklist.getNumberOfThumbs()));
		}
	}
}
