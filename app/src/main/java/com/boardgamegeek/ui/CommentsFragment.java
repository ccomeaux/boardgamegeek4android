package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Game.Comment;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.adapter.PaginatedArrayAdapter;
import com.boardgamegeek.ui.loader.PaginatedData;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import icepick.Icepick;
import icepick.State;

public class CommentsFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<PaginatedData<Comment>> {
	private static final int COMMENTS_LOADER_ID = 0;
	private CommentsAdapter adapter;
	private int gameId;
	@State boolean isSortedByRating = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		gameId = Games.getGameId(intent.getData());
		isSortedByRating = intent.getIntExtra(ActivityUtils.KEY_SORT, CommentsActivity.SORT_USER) == CommentsActivity.SORT_RATING;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListView listView = getListView();
		listView.setOnScrollListener(this);
		listView.setSelector(android.R.color.transparent);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_comments));
	}

	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(COMMENTS_LOADER_ID, null, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
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
			Loader<List<Comment>> loader = getLoaderManager().getLoader(COMMENTS_LOADER_ID);
			if (loader != null) {
				loader.forceLoad();
			}
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
	public Loader<PaginatedData<Comment>> onCreateLoader(int id, Bundle data) {
		return new CommentsLoader(getActivity(), gameId, isSortedByRating);
	}

	@Override
	public void onLoadFinished(Loader<PaginatedData<Comment>> loader, PaginatedData<Comment> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new CommentsAdapter(getActivity(), R.layout.row_comment, data);
			setListAdapter(adapter);
		} else {
			adapter.update(data);
		}
		restoreScrollState();
	}

	@Override
	public void onLoaderReset(Loader<PaginatedData<Comment>> loader) {
	}

	private boolean isLoading() {
		final CommentsLoader loader = getLoader();
		return (loader == null) || loader.isLoading();
	}

	private boolean loaderHasMoreResults() {
		final CommentsLoader loader = getLoader();
		return (loader != null) && loader.hasMoreResults();
	}

	private CommentsLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<Comment>> loader = getLoaderManager().getLoader(COMMENTS_LOADER_ID);
			return (CommentsLoader) loader;
		}
		return null;
	}

	private static class CommentsLoader extends PaginatedLoader<Comment> {
		BggService mService;
		private int mGameId;
		private boolean mByRating;

		public CommentsLoader(Context context, int gameId, boolean byRating) {
			super(context);
			mService = Adapter.create();
			mGameId = gameId;
			mByRating = byRating;
		}

		@Override
		public PaginatedData<Comment> loadInBackground() {
			super.loadInBackground();
			CommentData data;
			try {
				int page = getNextPage();
				if (mByRating) {
					data = new CommentData(mService.thingWithRatings(mGameId, page), page);
				} else {
					data = new CommentData(mService.thingWithComments(mGameId, page), page);
				}
			} catch (Exception e) {
				data = new CommentData(e);
			}
			return data;
		}
	}

	static class CommentData extends PaginatedData<Comment> {
		public CommentData(ThingResponse response, int page) {
			super(response.getGames().get(0).comments.comments, response.getGames().get(0).comments.totalitems, page,
				ThingResponse.PAGE_SIZE);
		}

		public CommentData(Exception e) {
			super(e);
		}
	}

	private class CommentsAdapter extends PaginatedArrayAdapter<Comment> {
		public CommentsAdapter(Context context, int resource, PaginatedData<Comment> data) {
			super(context, resource, data);
		}

		@Override
		protected boolean isLoaderLoading() {
			return isLoading();
		}

		@Override
		protected void bind(View view, Comment item) {
			CommentRowViewBinder.bindActivityView(view, item);
		}
	}

	private static class CommentRowViewBinder {
		private static class ViewHolder {
			TextView username;
			TextView rating;
			TextView comment;

			public ViewHolder(View view) {
				username = (TextView) view.findViewById(R.id.username);
				rating = (TextView) view.findViewById(R.id.rating);
				comment = (TextView) view.findViewById(R.id.comment);
			}
		}

		private static void bindActivityView(final View rootView, Comment comment) {
			ViewHolder temp = (ViewHolder) rootView.getTag();
			final ViewHolder holder;
			if (temp != null) {
				holder = temp;
			} else {
				holder = new ViewHolder(rootView);
				rootView.setTag(holder);
			}

			holder.username.setText(comment.username);
			holder.rating.setText(comment.getRatingText());
			ColorUtils.setViewBackground(holder.rating, ColorUtils.getRatingColor(comment.getRating()));
			if (TextUtils.isEmpty(comment.value)) {
				holder.comment.setVisibility(View.GONE);
			} else {
				holder.comment.setVisibility(View.VISIBLE);
				holder.comment.setText(comment.value);
			}
		}
	}
}
