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

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Game.Comment;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.PaginatedArrayAdapter;
import com.boardgamegeek.ui.widget.PaginatedData;
import com.boardgamegeek.ui.widget.PaginatedLoader;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.UIUtils;

public class CommentsFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<PaginatedData<Comment>> {
	private static final int COMMENTS_LOADER_ID = 0;
	private static final String STATE_BY_RATING = "by_rating";

	private CommentsAdapter mCommentsAdapter;
	private int mGameId;
	private boolean mByRating = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameId = Games.getGameId(intent.getData());

		if (savedInstanceState != null) {
			mByRating = savedInstanceState.getBoolean(STATE_BY_RATING);
		}
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
		outState.putBoolean(STATE_BY_RATING, mByRating);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.game_comments, menu);
		if (mByRating) {
			menu.findItem(R.id.menu_comments_by_rating).setChecked(true);
		} else {
			menu.findItem(R.id.menu_comments_by_user).setChecked(true);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if ((id == R.id.menu_comments_by_user && mByRating) || (id == R.id.menu_comments_by_rating && !mByRating)) {
			item.setChecked(true);
			mByRating = !mByRating;
			if (mCommentsAdapter != null) {
				mCommentsAdapter.clear();
			}
			getLoaderManager().restartLoader(COMMENTS_LOADER_ID, null, this);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
		return new CommentsLoader(getActivity(), mGameId, mByRating);
	}

	@Override
	public void onLoadFinished(Loader<PaginatedData<Comment>> loader, PaginatedData<Comment> data) {
		if (getActivity() == null) {
			return;
		}

		if (mCommentsAdapter == null) {
			mCommentsAdapter = new CommentsAdapter(getActivity(), R.layout.row_comment, data);
			setListAdapter(mCommentsAdapter);
		} else {
			mCommentsAdapter.update(data);
		}
	}

	@Override
	public void onLoaderReset(Loader<PaginatedData<Comment>> loader) {
	}

	private boolean isLoading() {
		final CommentsLoader loader = getLoader();
		return (loader != null) ? loader.isLoading() : true;
	}

	private boolean loaderHasMoreResults() {
		final CommentsLoader loader = getLoader();
		return (loader != null) ? loader.hasMoreResults() : false;
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
			super(response.games.get(0).comments.comments, response.games.get(0).comments.totalitems, page,
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
			ColorUtils.setTextViewBackground(holder.rating, ColorUtils.getRatingColor(comment.getRating()));
			holder.comment.setText(comment.value);
		}
	}
}
