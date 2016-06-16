package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Game.Comment;
import com.boardgamegeek.model.Game.Comments;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.adapter.PaginatedArrayAdapter;
import com.boardgamegeek.ui.loader.PaginatedData;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.Icepick;
import icepick.State;
import retrofit2.Call;

public class CommentsFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<PaginatedData<Comment>> {
	private static final int LOADER_ID = 0;
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
		getLoaderManager().initLoader(LOADER_ID, null, this);
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
			Loader<List<Comment>> loader = getLoaderManager().getLoader(LOADER_ID);
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
			adapter = new CommentsAdapter(getActivity(), data);
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
			Loader<PaginatedData<Comment>> loader = getLoaderManager().getLoader(LOADER_ID);
			return (CommentsLoader) loader;
		}
		return null;
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
		public PaginatedData<Comment> loadInBackground() {
			super.loadInBackground();
			CommentData data;
			int page = getNextPage();
			Call<ThingResponse> call;
			if (isSortedByRating) {
				call = bggService.thingWithRatings(gameId, page);
			} else {
				call = bggService.thingWithComments(gameId, page);
			}
			try {
				data = new CommentData(call.execute().body().getGames().get(0).comments, page);
			} catch (Exception e) {
				data = new CommentData(e);
			}
			return data;
		}
	}

	static class CommentData extends PaginatedData<Comment> {
		public CommentData(Comments comments, int page) {
			super(comments.comments, comments.totalitems, page, ThingResponse.PAGE_SIZE);
		}

		public CommentData(Exception e) {
			super(e);
		}
	}

	private class CommentsAdapter extends PaginatedArrayAdapter<Comment> {
		public CommentsAdapter(Context context, PaginatedData<Comment> data) {
			super(context, R.layout.row_comment, data);
		}

		@Override
		protected boolean isLoaderLoading() {
			return isLoading();
		}

		@Override
		protected void bind(View view, Comment item) {
			final ViewHolder holder = getViewHolder(view);
			holder.bind(item);
		}
	}

	static class ViewHolder {
		@BindView(R.id.username) TextView usernameView;
		@BindView(R.id.rating) TextView ratingView;
		@BindView(R.id.comment) TextView commentView;

		public ViewHolder(View view) {
			ButterKnife.bind(this, view);
		}

		private void bind(Comment comment) {
			usernameView.setText(comment.username);
			ratingView.setText(comment.getRatingText());
			ColorUtils.setViewBackground(ratingView, ColorUtils.getRatingColor(comment.getRating()));
			PresentationUtils.setTextOrHide(commentView, comment.value);
		}
	}

	@NonNull
	private static ViewHolder getViewHolder(View rootView) {
		ViewHolder tag = (ViewHolder) rootView.getTag();
		if (tag != null) {
			return tag;
		} else {
			final ViewHolder holder = new ViewHolder(rootView);
			rootView.setTag(holder);
			return holder;
		}
	}
}
