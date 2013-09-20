package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCommentsHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.model.Comment;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.url.GameUrlBuilder;

public class CommentsFragment extends SherlockListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<List<Comment>> {
	private static final String TAG = makeLogTag(CommentsFragment.class);
	private static final int COMMENTS_LOADER_ID = 0;
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";
	private static final String STATE_BY_RATING = "by_rating";

	private int mGameId;
	private List<Comment> mComments = new ArrayList<Comment>();
	private CommentsAdapter mCommentsAdapter = new CommentsAdapter();
	private int mListViewStatePosition;
	private int mListViewStateTop;
	private boolean mByRating = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameId = Games.getGameId(intent.getData());

		setListAdapter(mCommentsAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mListViewStatePosition = savedInstanceState.getInt(STATE_POSITION, -1);
			mListViewStateTop = savedInstanceState.getInt(STATE_TOP, 0);
			mByRating = savedInstanceState.getBoolean(STATE_BY_RATING);
		} else {
			mListViewStatePosition = -1;
			mListViewStateTop = 0;
			mByRating = false;
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_comments));
		getLoaderManager().initLoader(COMMENTS_LOADER_ID, null, this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);

		final ListView listView = getListView();
		listView.setOnScrollListener(this);
		listView.setFastScrollEnabled(true);
		listView.setCacheColorHint(Color.WHITE);
		listView.setSelector(android.R.color.transparent);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (isAdded()) {
			View v = getListView().getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			outState.putInt(STATE_POSITION, getListView().getFirstVisiblePosition());
			outState.putInt(STATE_TOP, top);
		}
		outState.putBoolean(STATE_BY_RATING, mByRating);
		super.onSaveInstanceState(outState);
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
			mComments.clear();
			mCommentsAdapter.notifyDataSetChanged();
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
		if (!isLoaderLoading() && loaderHasMoreResults() && visibleItemCount != 0
			&& firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
			loadMoreResults();
		}
	}

	@Override
	public Loader<List<Comment>> onCreateLoader(int id, Bundle data) {
		return new CommentsLoader(getActivity(), mGameId, mByRating);
	}

	@Override
	public void onLoadFinished(Loader<List<Comment>> loader, List<Comment> comments) {
		if (getActivity() == null) {
			return;
		}

		if (comments != null) {
			mComments = comments;
		}
		mCommentsAdapter.notifyDataSetChanged();

		if (mListViewStatePosition != -1 && isAdded()) {
			getListView().setSelectionFromTop(mListViewStatePosition, mListViewStateTop);
			mListViewStatePosition = -1;
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Comment>> loader) {
	}

	private boolean isLoaderLoading() {
		final CommentsLoader loader = getLoader();
		return (loader != null) ? loader.isLoading() : true;
	}

	private boolean loaderHasMoreResults() {
		final CommentsLoader loader = getLoader();
		return (loader != null) ? loader.hasMoreResults() : false;
	}

	private boolean loaderHasError() {
		final CommentsLoader loader = getLoader();
		return (loader != null) ? loader.hasError() : false;
	}

	private String loaderErrorMessage() {
		final CommentsLoader loader = getLoader();
		return (loader != null) ? loader.getErrorMessage() : "";
	}

	private CommentsLoader getLoader() {
		if (isAdded()) {
			Loader<List<Comment>> loader = getLoaderManager().getLoader(COMMENTS_LOADER_ID);
			return (CommentsLoader) loader;
		}
		return null;
	}

	private static class CommentsLoader extends AsyncTaskLoader<List<Comment>> {
		private static final int PAGE_SIZE = 100;
		private int mGameId;
		private List<Comment> mData;
		private int mNextPage;
		private boolean mIsLoading;
		private String mErrorMessage;
		private int mCommentCount;
		private boolean mByRating;

		public CommentsLoader(Context context, int gameId, boolean byRating) {
			super(context);
			init(gameId, byRating);
		}

		private void init(int gameId, boolean byRating) {
			mGameId = gameId;
			mNextPage = 1;
			mIsLoading = true;
			mErrorMessage = "";
			mData = null;
			mCommentCount = 0;
			mByRating = byRating;
		}

		@Override
		public List<Comment> loadInBackground() {
			mIsLoading = true;

			HttpClient httpClient = HttpUtils.createHttpClient(getContext(), true);
			RemoteExecutor executor = new RemoteExecutor(httpClient, getContext());
			RemoteCommentsHandler handler = new RemoteCommentsHandler();

			GameUrlBuilder builder = new GameUrlBuilder(mGameId).useNewApi();
			if (mByRating) {
				builder.ratings(mNextPage);
			} else {
				builder.comments(mNextPage);
			}
			String url = builder.build();
			LOGI(TAG, "Loading comments from " + url);

			executor.safelyExecuteGet(url, handler);
			if (handler.hasError()) {
				mErrorMessage = handler.getErrorMessage();
				mNextPage = 1;
				mCommentCount = 0;
			} else {
				mErrorMessage = "";
				mNextPage++;
				mCommentCount = handler.getCount();
			}
			return handler.getResults();
		}

		@Override
		public void deliverResult(List<Comment> comments) {
			mIsLoading = false;
			if (comments != null) {
				if (mData == null) {
					mData = comments;
				} else {
					mData.addAll(comments);
				}
			}
			if (isStarted()) {
				super.deliverResult(mData == null ? null : new ArrayList<Comment>(mData));
			}
		}

		@Override
		protected void onStartLoading() {
			if (mData != null) {
				deliverResult(null);
			}
			if (takeContentChanged() || mData == null) {
				forceLoad();
			}
		}

		@Override
		protected void onStopLoading() {
			mIsLoading = false;
			cancelLoad();
		}

		@Override
		protected void onReset() {
			super.onReset();
			onStopLoading();
			mData = null;
		}

		public boolean isLoading() {
			return mIsLoading;
		}

		public boolean hasMoreResults() {
			return (mNextPage - 1) * PAGE_SIZE < mCommentCount;
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}
	}

	private class CommentsAdapter extends BaseAdapter {
		private static final int VIEW_TYPE_COMMENT = 0;
		private static final int VIEW_TYPE_LOADING = 1;

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItemViewType(position) == VIEW_TYPE_COMMENT;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public int getCount() {
			return mComments.size()
				+ (((isLoaderLoading() && mComments.size() == 0) || loaderHasMoreResults() || loaderHasError()) ? 1 : 0);
		}

		@Override
		public int getItemViewType(int position) {
			return (position >= mComments.size()) ? VIEW_TYPE_LOADING : VIEW_TYPE_COMMENT;
		}

		@Override
		public Object getItem(int position) {
			return (getItemViewType(position) == VIEW_TYPE_COMMENT) ? mComments.get(position) : null;
		}

		@Override
		public long getItemId(int position) {
			return (getItemViewType(position) == VIEW_TYPE_COMMENT) ? mComments.get(position).Username.hashCode() : -1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (getItemViewType(position) == VIEW_TYPE_LOADING) {
				if (convertView == null) {
					convertView = getLayoutInflater(null).inflate(R.layout.row_status, parent, false);
				}

				if (loaderHasError()) {
					convertView.findViewById(android.R.id.progress).setVisibility(View.GONE);
					((TextView) convertView.findViewById(android.R.id.text1)).setText(loaderErrorMessage());
				} else {
					convertView.findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
					((TextView) convertView.findViewById(android.R.id.text1)).setText(R.string.loading);
				}

				return convertView;

			} else {
				Comment comment = (Comment) getItem(position);
				if (convertView == null) {
					convertView = getLayoutInflater(null).inflate(R.layout.row_comment, parent, false);
				}

				CommentRowViewBinder.bindActivityView(convertView, comment);
				return convertView;
			}
		}
	}

	private static class CommentRowViewBinder {
		private static final int backgroundColors[] = { Color.WHITE, 0xffff0000, 0xffff3366, 0xffff6699, 0xffff66cc,
			0xffcc99ff, 0xff9999ff, 0xff99ffff, 0xff66ff99, 0xff33cc99, 0xff00cc00 };

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

			holder.username.setText(comment.Username);
			holder.rating.setText(new DecimalFormat("#0.00").format(StringUtils.parseDouble(comment.Rating, 0.0)));
			final int rating = (int) StringUtils.parseDouble(comment.Rating, 0.0);
			holder.rating.setBackgroundColor(backgroundColors[rating]);
			holder.comment.setText(comment.Value);
		}
	}
}
