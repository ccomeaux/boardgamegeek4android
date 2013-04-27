package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumHandler;
import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<List<ForumThread>> {
	private static final String TAG = makeLogTag(ForumFragment.class);
	private static final int FORUM_LOADER_ID = 0;

	private List<ForumThread> mThreads = new ArrayList<ForumThread>();
	private ForumAdapter mForumAdapter = new ForumAdapter();
	private String mForumId;
	private String mForumTitle;
	private int mGameId;
	private String mGameName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mForumId = intent.getStringExtra(ForumsUtils.KEY_FORUM_ID);
		mForumTitle = intent.getStringExtra(ForumsUtils.KEY_FORUM_TITLE);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);

		setListAdapter(mForumAdapter);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnScrollListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(FORUM_LOADER_ID, null, this);
	}

	@Override
	protected int getEmptyStringResoure() {
		return R.string.empty_forum;
	}

	public void refresh(boolean forceRefresh) {
		if (isLoaderLoading() && !forceRefresh) {
			return;
		}

		// clear current items
		mThreads.clear();
		mForumAdapter.notifyDataSetInvalidated();

		final ForumLoader loader = getLoader();
		if (loader != null) {
			loader.init(mForumId);
		}

		loadMoreResults();
	}

	public void loadMoreResults() {
		if (isAdded()) {
			Loader<List<ForumThread>> loader = getLoaderManager().getLoader(FORUM_LOADER_ID);
			if (loader != null) {
				loader.forceLoad();
			}
		}
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		ThreadRowViewBinder.ViewHolder holder = (ThreadRowViewBinder.ViewHolder) convertView.getTag();
		if (holder != null) {
			Intent intent = new Intent(getActivity(), ThreadActivity.class);
			intent.putExtra(ForumsUtils.KEY_THREAD_ID, holder.threadId);
			intent.putExtra(ForumsUtils.KEY_THREAD_SUBJECT, holder.subject.getText());
			intent.putExtra(ForumsUtils.KEY_FORUM_ID, mForumId);
			intent.putExtra(ForumsUtils.KEY_FORUM_TITLE, mForumTitle);
			intent.putExtra(ForumsUtils.KEY_GAME_ID, mGameId);
			intent.putExtra(ForumsUtils.KEY_GAME_NAME, mGameName);
			startActivity(intent);
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
	public Loader<List<ForumThread>> onCreateLoader(int id, Bundle data) {
		return new ForumLoader(getActivity(), mForumId);
	}

	@Override
	public void onLoadFinished(Loader<List<ForumThread>> loader, List<ForumThread> threads) {
		if (getActivity() == null) {
			return;
		}

		if (threads != null) {
			mThreads = threads;
		}
		mForumAdapter.notifyDataSetChanged();
		restoreScrollState();
	}

	@Override
	public void onLoaderReset(Loader<List<ForumThread>> loader) {
	}

	private boolean isLoaderLoading() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.isLoading() : true;
	}

	private boolean loaderHasMoreResults() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.hasMoreResults() : false;
	}

	private boolean loaderHasError() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.hasError() : false;
	}

	private String loaderErrorMessage() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.getErrorMessage() : "";
	}

	private ForumLoader getLoader() {
		if (isAdded()) {
			Loader<List<ForumThread>> loader = getLoaderManager().getLoader(FORUM_LOADER_ID);
			return (ForumLoader) loader;
		}
		return null;
	}

	private static class ForumLoader extends AsyncTaskLoader<List<ForumThread>> {
		private static final int PAGE_SIZE = 50;
		private String mForumId;
		private List<ForumThread> mData;
		private int mNextPage;
		private boolean mIsLoading;
		private String mErrorMessage;
		private int mThreadCount;

		public ForumLoader(Context context, String forumId) {
			super(context);
			init(forumId);
		}

		private void init(String forumId) {
			mForumId = forumId;
			mNextPage = 1;
			mIsLoading = true;
			mErrorMessage = "";
			mData = null;
			mThreadCount = 0;
		}

		@Override
		public List<ForumThread> loadInBackground() {
			mIsLoading = true;

			HttpClient httpClient = HttpUtils.createHttpClient(getContext(), true);
			RemoteExecutor executor = new RemoteExecutor(httpClient, getContext());
			RemoteForumHandler handler = new RemoteForumHandler();

			final String url = HttpUtils.constructForumUrl(mForumId, mNextPage);
			LOGI(TAG, "Loading threads from " + url);
			executor.safelyExecuteGet(url, handler);
			if (handler.hasError()) {
				mErrorMessage = handler.getErrorMessage();
				mNextPage = 1;
				mThreadCount = 0;
			} else {
				mErrorMessage = "";
				mNextPage++;
				mThreadCount = handler.getCount();
			}
			return handler.getResults();
		}

		@Override
		public void deliverResult(List<ForumThread> threads) {
			mIsLoading = false;
			if (threads != null) {
				if (mData == null) {
					mData = threads;
				} else {
					mData.addAll(threads);
				}
			}
			if (isStarted()) {
				super.deliverResult(mData == null ? null : new ArrayList<ForumThread>(mData));
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
			return (mNextPage - 1) * PAGE_SIZE < mThreadCount;
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}
	}

	private class ForumAdapter extends BaseAdapter {
		private static final int VIEW_TYPE_THREAD = 0;
		private static final int VIEW_TYPE_LOADING = 1;

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItemViewType(position) == VIEW_TYPE_THREAD;
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
			return mThreads.size()
				+ (((isLoaderLoading() && mThreads.size() == 0) || loaderHasMoreResults() || loaderHasError()) ? 1 : 0);
		}

		@Override
		public int getItemViewType(int position) {
			return (position >= mThreads.size()) ? VIEW_TYPE_LOADING : VIEW_TYPE_THREAD;
		}

		@Override
		public Object getItem(int position) {
			return (getItemViewType(position) == VIEW_TYPE_THREAD) ? mThreads.get(position) : null;
		}

		@Override
		public long getItemId(int position) {
			return (getItemViewType(position) == VIEW_TYPE_THREAD) ? mThreads.get(position).id.hashCode() : -1;
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
				ForumThread thread = (ForumThread) getItem(position);
				if (convertView == null) {
					convertView = getLayoutInflater(null).inflate(R.layout.row_forumthread, parent, false);
				}

				ThreadRowViewBinder.bindActivityView(convertView, thread);
				return convertView;
			}
		}
	}

	private static class ThreadRowViewBinder {
		private static NumberFormat mFormat = NumberFormat.getInstance();

		public static class ViewHolder {
			public String threadId;
			public TextView subject;
			public TextView author;
			public TextView numarticles;
			public TextView lastpostdate;
			public TextView postdate;

			public ViewHolder(View view) {
				subject = (TextView) view.findViewById(R.id.thread_title);
				author = (TextView) view.findViewById(R.id.thread_author);
				numarticles = (TextView) view.findViewById(R.id.thread_numarticles);
				lastpostdate = (TextView) view.findViewById(R.id.thread_lastpostdate);
				postdate = (TextView) view.findViewById(R.id.thread_postdate);
			}
		}

		public static void bindActivityView(View rootView, ForumThread thread) {
			ViewHolder temp = (ViewHolder) rootView.getTag();
			final ViewHolder holder;
			if (temp != null) {
				holder = temp;
			} else {
				holder = new ViewHolder(rootView);
				rootView.setTag(holder);
			}

			Resources r = rootView.getResources();
			String mAuthorText = r.getString(R.string.forum_thread_author);
			String mLastPostText = r.getString(R.string.forum_last_post);
			String mCreatedText = r.getString(R.string.forum_thread_created);

			holder.threadId = thread.id;
			holder.subject.setText(thread.subject);
			holder.author.setText(String.format(mAuthorText, thread.author));
			int replies = thread.numberOfArticles - 1;
			holder.numarticles.setText(r.getQuantityString(R.plurals.forum_thread_replies, replies,
				mFormat.format(replies)));
			holder.lastpostdate.setText(String.format(mLastPostText,
				DateUtils.getRelativeTimeSpanString(thread.lastPostDate)));
			holder.postdate.setText(String.format(mCreatedText, DateUtils.getRelativeTimeSpanString(thread.postPate)));
		}
	}
}
