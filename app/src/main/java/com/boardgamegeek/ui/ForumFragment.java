package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Thread;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.ForumRecyclerViewAdapter;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.ui.model.ForumThreads;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class ForumFragment extends Fragment implements LoaderManager.LoaderCallbacks<PaginatedData<Thread>> {
	private static final int LOADER_ID = 0;
	private static final int VISIBLE_THRESHOLD = 3;

	private ForumRecyclerViewAdapter adapter;
	private int forumId;
	private String forumTitle;
	private int gameId;
	private String gameName;

	Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		forumId = intent.getIntExtra(ForumActivity.KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = intent.getStringExtra(ForumActivity.KEY_FORUM_TITLE);
		gameId = intent.getIntExtra(ForumActivity.KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ForumActivity.KEY_GAME_NAME);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_forum, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				final ForumLoader loader = getLoader();
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

	@DebugLog
	@Nullable
	private ForumLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<Thread>> loader = getLoaderManager().getLoader(LOADER_ID);
			return (ForumLoader) loader;
		}
		return null;
	}

	@DebugLog
	private void loadMoreResults() {
		if (isAdded()) {
			Loader<List<Thread>> loader = getLoaderManager().getLoader(LOADER_ID);
			if (loader != null) {
				loader.forceLoad();
			}
		}
	}

	@Override
	@DebugLog
	public Loader<PaginatedData<Thread>> onCreateLoader(int id, Bundle data) {
		return new ForumLoader(getActivity(), forumId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<PaginatedData<Thread>> loader, PaginatedData<Thread> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new ForumRecyclerViewAdapter(getActivity(), data, forumId, forumTitle, gameId, gameName);
			recyclerView.setAdapter(adapter);
		} else {
			adapter.update(data);
		}

		if (adapter.getItemCount() == 0) {
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else {
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		}
		progressView.hide();
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<PaginatedData<Thread>> loader) {
	}

	@DebugLog
	private static class ForumLoader extends PaginatedLoader<Thread> {
		private final BggService bggService;
		private final int forumId;

		public ForumLoader(Context context, int forumId) {
			super(context);
			bggService = Adapter.createForXml();
			this.forumId = forumId;
		}

		@DebugLog
		@Override
		protected PaginatedData<Thread> fetchPage(int pageNumber) {
			ForumThreads data;
			try {
				data = new ForumThreads(bggService.forum(forumId, pageNumber).execute().body(), pageNumber);
			} catch (Exception e) {
				data = new ForumThreads(e);
			}
			return data;
		}
	}
}
