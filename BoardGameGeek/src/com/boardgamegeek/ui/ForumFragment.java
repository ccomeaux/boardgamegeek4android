package com.boardgamegeek.ui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumParser;
import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.ForumFragment.ForumData;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<ForumData> {
	private static final int FORUM_LOADER_ID = 0;

	private ForumAdapter mForumAdapter;
	private int mForumId;
	private String mForumTitle;
	private int mGameId;
	private String mGameName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mForumId = intent.getIntExtra(ForumsUtils.KEY_FORUM_ID, BggContract.INVALID_ID);
		mForumTitle = intent.getStringExtra(ForumsUtils.KEY_FORUM_TITLE);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnScrollListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_forum));
	}

	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(FORUM_LOADER_ID, null, this);
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
	public Loader<ForumData> onCreateLoader(int id, Bundle data) {
		return new ForumLoader(getActivity(), mForumId);
	}

	@Override
	public void onLoadFinished(Loader<ForumData> loader, ForumData data) {
		if (getActivity() == null) {
			return;
		}

		saveScrollState();
		if (mForumAdapter == null) {
			mForumAdapter = new ForumAdapter(data.getData(), data.getErrorMessage(), data.getTotalCount(),
				data.getCurrentPage());
			setListAdapter(mForumAdapter);
		} else {
			mForumAdapter.clear();
			mForumAdapter.addAll(data.getData());
			mForumAdapter.setCurrentPage(data.getCurrentPage());
		}
		restoreScrollState();
	}

	@Override
	public void onLoaderReset(Loader<ForumData> loader) {
	}

	private boolean isLoaderLoading() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.isLoading() : true;
	}

	private boolean loaderHasMoreResults() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.hasMoreResults() : false;
	}

	private ForumLoader getLoader() {
		if (isAdded()) {
			Loader<ForumData> loader = getLoaderManager().getLoader(FORUM_LOADER_ID);
			return (ForumLoader) loader;
		}
		return null;
	}

	static class ForumData {
		private List<ForumThread> mThreads;
		private String mErrorMessage;
		private int mTotalCount;
		private int mCurrentPage;

		ForumData(List<ForumThread> threads, int totalCount, int page) {
			mThreads = threads;
			mErrorMessage = "";
			mTotalCount = totalCount;
			mCurrentPage = page;
		}

		ForumData(String errorMessage) {
			mThreads = new ArrayList<ForumThread>();
			mErrorMessage = errorMessage;
			mTotalCount = 0;
			mCurrentPage = 0;
		}

		ForumData(ForumData data) {
			this.mThreads = new ArrayList<ForumThread>(data.mThreads);
			this.mErrorMessage = data.mErrorMessage;
			this.mTotalCount = data.mTotalCount;
			this.mCurrentPage = data.mCurrentPage;
		}

		public void addAll(List<ForumThread> threads) {
			mThreads.addAll(threads);
			mCurrentPage++;
		}

		public List<ForumThread> getData() {
			return mThreads;
		}

		public int getTotalCount() {
			return mTotalCount;
		}

		public int getCurrentPage() {
			return mCurrentPage;
		}

		public int getNextPage() {
			return mCurrentPage + 1;
		}

		private int getPageSize() {
			return 50;
		}

		public boolean hasMoreResults() {
			return mCurrentPage * getPageSize() < mTotalCount;
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}
	}

	private static class ForumLoader extends AsyncTaskLoader<ForumData> {
		private int mForumId;
		private ForumData mData;
		private boolean mIsLoading;

		public ForumLoader(Context context, int forumId) {
			super(context);
			mForumId = forumId;
			mIsLoading = true;
			mData = null;
		}

		@Override
		protected void onStartLoading() {
			if (mData != null) {
				deliverResult(mData);
			}
			if (takeContentChanged() || mData == null) {
				forceLoad();
			}
		}

		@Override
		public ForumData loadInBackground() {
			mIsLoading = true;

			RemoteExecutor executor = new RemoteExecutor(getContext());
			int page = mData == null ? 1 : mData.getNextPage();
			RemoteForumParser parser = new RemoteForumParser(mForumId, page);
			executor.safelyExecuteGet(parser);
			ForumData data = null;
			if (parser.hasError()) {
				data = new ForumData(parser.getErrorMessage());
			} else {
				data = new ForumData(parser.getResults(), parser.getCount(), page);
			}
			return data;
		}

		@Override
		public void deliverResult(ForumData data) {
			mIsLoading = false;
			if (data != null) {
				if (mData == null) {
					mData = data;
				} else if (data.getCurrentPage() == mData.getNextPage()) {
					mData.addAll(data.getData());
				}
			}
			if (isStarted()) {
				super.deliverResult(new ForumData(mData));
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
			if (mData == null) {
				return true;
			}
			return mData.hasMoreResults();
		}

		// public boolean hasError() {
		// if (mData == null) {
		// return false;
		// }
		// return mData.hasError();
		// }
		//
		// public String getErrorMessage() {
		// if (mData == null) {
		// return "";
		// }
		// return mData.getErrorMessage();
		// }
	}

	private class ForumAdapter extends ArrayAdapter<ForumThread> {
		private static final int VIEW_TYPE_THREAD = 0;
		private static final int VIEW_TYPE_LOADING = 1;
		private static final int PAGE_SIZE = 50;
		private String mErrorMessage;
		private int mTotalCount;
		private int mCurrentPage;

		public ForumAdapter(List<ForumThread> objects, String errorMessage, int count, int page) {
			super(getActivity(), 0, objects);
			mErrorMessage = errorMessage;
			mTotalCount = count;
			mCurrentPage = page;
		}

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
			int count = super.getCount()
				+ (((isLoaderLoading() && super.getCount() == 0) || hasMoreResults() || hasError()) ? 1 : 0);
			return count;
		}

		@Override
		public int getItemViewType(int position) {
			return (position >= super.getCount()) ? VIEW_TYPE_LOADING : VIEW_TYPE_THREAD;
		}

		@Override
		public ForumThread getItem(int position) {
			return (getItemViewType(position) == VIEW_TYPE_THREAD) ? super.getItem(position) : null;
		}

		@Override
		public long getItemId(int position) {
			return (getItemViewType(position) == VIEW_TYPE_THREAD) ? super.getItemId(position) : -1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (getItemViewType(position) == VIEW_TYPE_LOADING) {
				if (convertView == null) {
					convertView = getLayoutInflater(null).inflate(R.layout.row_status, parent, false);
				}

				if (hasError()) {
					convertView.findViewById(android.R.id.progress).setVisibility(View.GONE);
					((TextView) convertView.findViewById(android.R.id.text1)).setText(mErrorMessage);
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

		public void setCurrentPage(int page) {
			mCurrentPage = page;
		}

		private boolean hasMoreResults() {
			return mCurrentPage * PAGE_SIZE < mTotalCount;
		}

		private boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}
	}

	private static class ThreadRowViewBinder {
		private static NumberFormat mFormat = NumberFormat.getInstance();

		public static class ViewHolder {
			public int threadId;
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
				DateTimeUtils.formatForumDate(rootView.getContext(), thread.lastPostDate)));
			holder.postdate.setText(String.format(mCreatedText,
				DateTimeUtils.formatForumDate(rootView.getContext(), thread.postDate)));
		}
	}
}
