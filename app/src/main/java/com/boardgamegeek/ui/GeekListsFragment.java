package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.ui.adapter.GeekListsRecyclerViewAdapter;
import com.boardgamegeek.ui.decoration.VerticalDividerItemDecoration;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.ui.model.GeekLists;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.util.AnimationUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class GeekListsFragment extends Fragment implements LoaderManager.LoaderCallbacks<PaginatedData<GeekListEntry>> {
	private static final int LOADER_ID = 0;
	private static final int VISIBLE_THRESHOLD = 3;
	private static final int SORT_TYPE_INVALID = -1;
	private static final int SORT_TYPE_HOT = 0;
	private static final int SORT_TYPE_RECENT = 1;
	private static final int SORT_TYPE_ACTIVE = 2;
	@State int sortType = 0;
	private GeekListsRecyclerViewAdapter adapter;

	Unbinder unbinder;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		setHasOptionsMenu(true);
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_forum, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@DebugLog
	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@DebugLog
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	@DebugLog
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

	@DebugLog
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

	@DebugLog
	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new VerticalDividerItemDecoration(getActivity()));

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				final GeekListsLoader loader = getLoader();
				if (loader != null && !loader.isLoading() && loader.hasMoreResults()) {
					int totalItemCount = layoutManager.getItemCount();
					int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
					if (lastVisibleItemPosition + VISIBLE_THRESHOLD >= totalItemCount) {
						loadMoreResults();
					}
				}
			}
		});
	}

	private GeekListsLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<GeekListEntry>> loader = getLoaderManager().getLoader(LOADER_ID);
			return (GeekListsLoader) loader;
		}
		return null;
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
	public Loader<PaginatedData<GeekListEntry>> onCreateLoader(int id, Bundle data) {
		return new GeekListsLoader(getActivity(), sortType);
	}

	@Override
	public void onLoadFinished(Loader<PaginatedData<GeekListEntry>> loader, PaginatedData<GeekListEntry> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new GeekListsRecyclerViewAdapter(getActivity(), data);
			recyclerView.setAdapter(adapter);
		} else {
			adapter.update(data);
		}

		if (adapter.getItemCount() == 0) {
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else {
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		}
		AnimationUtils.fadeOut(progressView);
	}

	@Override
	public void onLoaderReset(Loader<PaginatedData<GeekListEntry>> loader) {
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
		protected PaginatedData<GeekListEntry> fetchPage(int pageNumber) {
			GeekLists data;
			try {
				String sort = BggService.GEEK_LIST_SORT_HOT;
				switch (sortType) {
					case SORT_TYPE_RECENT:
						sort = BggService.GEEK_LIST_SORT_RECENT;
						break;
					case SORT_TYPE_ACTIVE:
						sort = BggService.GEEK_LIST_SORT_ACTIVE;
						break;
				}
				data = new GeekLists(bggService.geekLists(pageNumber, sort).execute().body(), pageNumber);
			} catch (Exception e) {
				data = new GeekLists(e);
			}
			return data;
		}
	}
}
