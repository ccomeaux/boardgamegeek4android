package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Game.Comment;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.adapter.GameCommentsRecyclerViewAdapter;
import com.boardgamegeek.ui.loader.PaginatedData;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.ui.model.GameComments;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import retrofit2.Call;

public class CommentsFragment extends Fragment implements LoaderManager.LoaderCallbacks<PaginatedData<Comment>> {
	private static final int LOADER_ID = 0;
	private static final int VISIBLE_THRESHOLD = 1;
	private GameCommentsRecyclerViewAdapter adapter;
	private int gameId;
	@State boolean isSortedByRating = false;

	private Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		gameId = Games.getGameId(intent.getData());
		isSortedByRating = intent.getIntExtra(ActivityUtils.KEY_SORT, CommentsActivity.SORT_USER) == CommentsActivity.SORT_RATING;
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_comments, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
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
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(layoutManager);

		// TODO - show divider

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				final CommentsLoader loader = getLoader();
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
	private void loadMoreResults() {
		if (isAdded()) {
			Loader<List<Comment>> loader = getLoaderManager().getLoader(LOADER_ID);
			if (loader != null) {
				loader.forceLoad();
			}
		}
	}

	@DebugLog
	private CommentsLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<Comment>> loader = getLoaderManager().getLoader(LOADER_ID);
			return (CommentsLoader) loader;
		}
		return null;
	}

	@Override
	public Loader<PaginatedData<Comment>> onCreateLoader(int id, Bundle data) {
		return new CommentsLoader(getActivity(), gameId, isSortedByRating);
	}

	@Override
	public void onLoadFinished(Loader<PaginatedData<Comment>> loader, PaginatedData<Comment> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new GameCommentsRecyclerViewAdapter(getActivity(), data);
			recyclerView.setAdapter(adapter);
		} else {
			adapter.update(data);
		}

		AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		AnimationUtils.fadeOut(progressView);
	}

	@Override
	public void onLoaderReset(Loader<PaginatedData<Comment>> loader) {
	}

	private static class CommentsLoader extends PaginatedLoader<Comment> {
		final BggService bggService;
		private final int gameId;
		private final boolean isSortedByRating;

		public CommentsLoader(Context context, int gameId, boolean isSortedByRating) {
			super(context);
			bggService = Adapter.createForXml();
			this.gameId = gameId;
			this.isSortedByRating = isSortedByRating;
		}

		@Override
		protected PaginatedData<Comment> fetchPage(int pageNumber) {
			GameComments data;
			Call<ThingResponse> call;
			if (isSortedByRating) {
				call = bggService.thingWithRatings(gameId, pageNumber);
			} else {
				call = bggService.thingWithComments(gameId, pageNumber);
			}
			try {
				data = new GameComments(call.execute().body().getGames().get(0).comments, pageNumber);
			} catch (Exception e) {
				data = new GameComments(e);
			}
			return data;
		}
	}
}
