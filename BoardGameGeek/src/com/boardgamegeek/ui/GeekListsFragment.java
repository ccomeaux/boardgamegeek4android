package com.boardgamegeek.ui;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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

	private GeekListsAdapter mGeekListsAdapter;

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
			intent.putExtra(GeekListUtils.KEY_GEEKLIST_ID, holder.id);
			intent.putExtra(GeekListUtils.KEY_GEEKLIST_TITLE, holder.title.getText());
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
			loadMoreResults();
		}
	}

	@Override
	public Loader<PaginatedData<GeekListEntry>> onCreateLoader(int id, Bundle data) {
		return new GeekListsLoader(getActivity());
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
	}

	@Override
	public void onLoaderReset(Loader<PaginatedData<GeekListEntry>> loader) {
	}

	private boolean isLoading() {
		final GeekListsLoader loader = getLoader();
		return (loader != null) ? loader.isLoading() : true;
	}

	private boolean loaderHasMoreResults() {
		final GeekListsLoader loader = getLoader();
		return (loader != null) ? loader.hasMoreResults() : false;
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

		public GeekListsLoader(Context context) {
			super(context);
			mService = Adapter.createWithJson();
		}

		@Override
		public PaginatedData<GeekListEntry> loadInBackground() {
			super.loadInBackground();
			GeekListsData data;
			try {
				int page = getNextPage();
				data = new GeekListsData(mService.geekLists(page, BggService.GEEKLIST_SORT_HOT), page);
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
			holder.numThumbs.setText(context.getString(R.string.thumbs_suffix, geeklist.getNumberOfThumbs()));
		}
	}
}
