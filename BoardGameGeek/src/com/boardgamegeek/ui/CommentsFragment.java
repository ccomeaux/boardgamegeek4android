package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
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
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCommentsHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Comment;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class CommentsFragment extends SherlockListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<List<Comment>> {
	private static final String TAG = makeLogTag(CommentsFragment.class);
	private static final int COMMENTS_LOADER_ID = 0;
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";

	private int mGameId;
	private List<Comment> mComments = new ArrayList<Comment>();
	private CommentsAdapter mCommentsAdapter = new CommentsAdapter();
	private int mListViewStatePosition;
	private int mListViewStateTop;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameId = Games.getGameId(intent.getData());

		setListAdapter(mCommentsAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mListViewStatePosition = savedInstanceState.getInt(STATE_POSITION, -1);
			mListViewStateTop = savedInstanceState.getInt(STATE_TOP, 0);
		} else {
			mListViewStatePosition = -1;
			mListViewStateTop = 0;
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
		super.onSaveInstanceState(outState);
	}

	public void refresh(boolean forceRefresh) {
		if (isLoaderLoading() && !forceRefresh) {
			return;
		}

		// clear current items
		mComments.clear();
		mCommentsAdapter.notifyDataSetInvalidated();

		final CommentsLoader loader = getLoader();
		if (loader != null) {
			loader.init(mGameId);
		}

		loadMoreResults();
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
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// Simple implementation of the infinite scrolling UI pattern; loads more comments as the user scrolls to the
		// end of the list.
		if (!isLoaderLoading() && loaderHasMoreResults() && visibleItemCount != 0
			&& firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
			loadMoreResults();
		}
	}

	@Override
	public Loader<List<Comment>> onCreateLoader(int id, Bundle data) {
		return new CommentsLoader(getActivity(), mGameId);
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
		private List<Comment> mCommentList;
		private int mNextPage;
		private boolean mIsLoading;
		private String mErrorMessage;
		private int mCommentCount;

		public CommentsLoader(Context context, int gameId) {
			super(context);
			init(gameId);
		}

		private void init(int gameId) {
			mGameId = gameId;
			mNextPage = 1;
			mIsLoading = true;
			mErrorMessage = "";
			mCommentList = null;
			mCommentCount = 0;
		}

		@Override
		public List<Comment> loadInBackground() {
			mIsLoading = true;

			HttpClient httpClient = HttpUtils.createHttpClient(getContext(), true);
			RemoteExecutor executor = new RemoteExecutor(httpClient, null);
			RemoteCommentsHandler handler = new RemoteCommentsHandler();

			String url = HttpUtils.constructCommentsUrl(mGameId, mNextPage);
			LOGI(TAG, "Loading comments from " + url);
			try {
				executor.executeGet(url, handler);

				if (handler.isBggDown()) {
					handleError(getContext().getString(R.string.bgg_down));
				} else {
					mNextPage++;
					mErrorMessage = "";
					mCommentCount = handler.getCount();
				}
			} catch (HandlerException e) {
				LOGE(TAG, "getting comments", e);
				handleError(e.getMessage());
			}
			return handler.getResults();
		}

		private void handleError(String message) {
			mErrorMessage = message;
			mNextPage = 1;
			mCommentCount = 0;
		}

		@Override
		public void deliverResult(List<Comment> comments) {
			mIsLoading = false;
			if (comments != null) {
				if (mCommentList == null) {
					mCommentList = comments;
				} else {
					mCommentList.addAll(comments);
				}
			}
			if (isStarted()) {
				// Need to return new ArrayList for some reason or onLoadFinished() is not called
				super.deliverResult(mCommentList == null ? null : new ArrayList<Comment>(mCommentList));
			}
		}

		@Override
		protected void onStartLoading() {
			if (mCommentList != null) {
				// If we already have results and are starting up, deliver what we already have.
				deliverResult(null);
			} else {
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
			mCommentList = null;
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
