package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
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
import com.boardgamegeek.model.ForumResponse;
import com.boardgamegeek.model.Thread;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.PaginatedArrayAdapter;
import com.boardgamegeek.ui.loader.PaginatedData;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

import java.text.NumberFormat;
import java.util.List;

import hugo.weaving.DebugLog;

public class ForumFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<PaginatedData<Thread>> {
	private static final int FORUM_LOADER_ID = 0;

	private ForumAdapter mForumAdapter;
	private int mForumId;
	private String mForumTitle;
	private int mGameId;
	private String mGameName;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mForumId = intent.getIntExtra(ActivityUtils.KEY_FORUM_ID, BggContract.INVALID_ID);
		mForumTitle = intent.getStringExtra(ActivityUtils.KEY_FORUM_TITLE);
		mGameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
	}

	@Override
	@DebugLog
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnScrollListener(this);
	}

	@Override
	@DebugLog
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_forum));
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(FORUM_LOADER_ID, null, this);
	}

	@DebugLog
	public void loadMoreResults() {
		if (isAdded()) {
			Loader<List<Thread>> loader = getLoaderManager().getLoader(FORUM_LOADER_ID);
			if (loader != null) {
				loader.forceLoad();
			}
		}
	}

	@Override
	@DebugLog
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		ThreadRowViewBinder.ViewHolder holder = (ThreadRowViewBinder.ViewHolder) convertView.getTag();
		if (holder != null) {
			Intent intent = new Intent(getActivity(), ThreadActivity.class);
			intent.putExtra(ActivityUtils.KEY_THREAD_ID, holder.threadId);
			intent.putExtra(ActivityUtils.KEY_THREAD_SUBJECT, holder.subject.getText());
			intent.putExtra(ActivityUtils.KEY_FORUM_ID, mForumId);
			intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, mForumTitle);
			intent.putExtra(ActivityUtils.KEY_GAME_ID, mGameId);
			intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
			startActivity(intent);
		}
	}

	@Override
	@DebugLog
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	@DebugLog
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (!isLoading() && loaderHasMoreResults() && visibleItemCount != 0
			&& firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
			saveScrollState();
			loadMoreResults();
		}
	}

	@Override
	@DebugLog
	public Loader<PaginatedData<Thread>> onCreateLoader(int id, Bundle data) {
		return new ForumLoader(getActivity(), mForumId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<PaginatedData<Thread>> loader, PaginatedData<Thread> data) {
		if (getActivity() == null) {
			return;
		}

		if (mForumAdapter == null) {
			mForumAdapter = new ForumAdapter(getActivity(), R.layout.row_forumthread, data);
			setListAdapter(mForumAdapter);
		} else {
			mForumAdapter.update(data);
		}
		restoreScrollState();
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<PaginatedData<Thread>> loader) {
	}

	@DebugLog
	private boolean isLoading() {
		final ForumLoader loader = getLoader();
		return (loader == null) || loader.isLoading();
	}

	@DebugLog
	private boolean loaderHasMoreResults() {
		final ForumLoader loader = getLoader();
		return (loader != null) && loader.hasMoreResults();
	}

	@DebugLog
	private ForumLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<Thread>> loader = getLoaderManager().getLoader(FORUM_LOADER_ID);
			return (ForumLoader) loader;
		}
		return null;
	}

	@DebugLog
	private static class ForumLoader extends PaginatedLoader<Thread> {
		private BggService mService;
		private int mForumId;

		public ForumLoader(Context context, int forumId) {
			super(context);
			mService = Adapter.create();
			mForumId = forumId;
		}

		@Override
		@DebugLog
		public PaginatedData<Thread> loadInBackground() {
			super.loadInBackground();
			ForumData data;
			try {
				int page = getNextPage();
				data = new ForumData(mService.forum(mForumId, page), page);
			} catch (Exception e) {
				data = new ForumData(e);
			}
			return data;
		}
	}

	static class ForumData extends PaginatedData<Thread> {
		public ForumData(ForumResponse response, int page) {
			super(response.threads, response.numberOfThreads(), page, ForumResponse.PAGE_SIZE);
		}

		public ForumData(Exception e) {
			super(e);
		}
	}

	private class ForumAdapter extends PaginatedArrayAdapter<Thread> {
		@DebugLog
		public ForumAdapter(Context context, int resource, PaginatedData<Thread> data) {
			super(context, resource, data);
		}

		@Override
		@DebugLog
		protected boolean isLoaderLoading() {
			return isLoading();
		}

		@Override
		@DebugLog
		protected void bind(View view, Thread item) {
			ThreadRowViewBinder.bindActivityView(view, item);
		}
	}

	private static class ThreadRowViewBinder {
		private static NumberFormat mFormat = NumberFormat.getInstance();
		private static String mAuthorText;
		private static String mLastPostText;
		private static String mCreatedText;

		public static class ViewHolder {
			public int threadId;
			public TextView subject;
			public TextView author;
			public TextView numarticles;
			public TextView lastpostdate;
			public TextView postdate;

			@DebugLog
			public ViewHolder(View view) {
				subject = (TextView) view.findViewById(R.id.thread_title);
				author = (TextView) view.findViewById(R.id.thread_author);
				numarticles = (TextView) view.findViewById(R.id.thread_numarticles);
				lastpostdate = (TextView) view.findViewById(R.id.thread_lastpostdate);
				postdate = (TextView) view.findViewById(R.id.thread_postdate);

				Resources r = view.getResources();
				mAuthorText = r.getString(R.string.forum_thread_author);
				mLastPostText = r.getString(R.string.forum_last_post);
				mCreatedText = r.getString(R.string.forum_thread_created);
			}
		}

		@DebugLog
		public static void bindActivityView(View rootView, Thread thread) {
			ViewHolder tag = (ViewHolder) rootView.getTag();
			final ViewHolder holder;
			if (tag != null) {
				holder = tag;
			} else {
				holder = new ViewHolder(rootView);
				rootView.setTag(holder);
			}

			Resources r = rootView.getResources();

			holder.threadId = thread.id;
			holder.subject.setText(thread.subject);
			holder.author.setText(String.format(mAuthorText, thread.author));
			int replies = thread.numberOfArticles - 1;
			holder.numarticles.setText(r.getQuantityString(R.plurals.forum_thread_replies, replies,
				mFormat.format(replies)));
			holder.lastpostdate.setText(String.format(mLastPostText, DateTimeUtils.formatForumDate(rootView.getContext(), thread.lastPostDate())));
			holder.postdate.setText(String.format(mCreatedText, DateTimeUtils.formatForumDate(rootView.getContext(), thread.postDate())));
		}
	}
}
