package com.boardgamegeek.ui;

import java.text.NumberFormat;
import java.util.List;

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
import com.boardgamegeek.io.ForumService;
import com.boardgamegeek.io.ForumService.ForumResponse;
import com.boardgamegeek.io.ForumService.Thread;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.widget.PaginatedArrayAdapter;
import com.boardgamegeek.ui.widget.PaginatedData;
import com.boardgamegeek.ui.widget.PaginatedLoader;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<PaginatedData<Thread>> {
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
			Loader<List<Thread>> loader = getLoaderManager().getLoader(FORUM_LOADER_ID);
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
		if (!isLoading() && loaderHasMoreResults() && visibleItemCount != 0
			&& firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
			loadMoreResults();
		}
	}

	@Override
	public Loader<PaginatedData<Thread>> onCreateLoader(int id, Bundle data) {
		return new ForumLoader(getActivity(), mForumId);
	}

	@Override
	public void onLoadFinished(Loader<PaginatedData<Thread>> loader, PaginatedData<Thread> data) {
		if (getActivity() == null) {
			return;
		}

		saveScrollState();
		if (mForumAdapter == null) {
			mForumAdapter = new ForumAdapter(getActivity(), R.layout.row_forumthread, data);
			setListAdapter(mForumAdapter);
		} else {
			mForumAdapter.update(data);
		}
		restoreScrollState();
	}

	@Override
	public void onLoaderReset(Loader<PaginatedData<Thread>> loader) {
	}

	private boolean isLoading() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.isLoading() : true;
	}

	private boolean loaderHasMoreResults() {
		final ForumLoader loader = getLoader();
		return (loader != null) ? loader.hasMoreResults() : false;
	}

	private ForumLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<Thread>> loader = getLoaderManager().getLoader(FORUM_LOADER_ID);
			return (ForumLoader) loader;
		}
		return null;
	}

	private static class ForumLoader extends PaginatedLoader<Thread> {
		private ForumService mService;
		private int mForumId;

		public ForumLoader(Context context, int forumId) {
			super(context);
			mService = Adapter.get().create(ForumService.class);
			mForumId = forumId;
		}

		@Override
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
			super(response.threads, response.numberOfThreads, page, ForumService.ForumResponse.PAGE_SIZE);
		}

		public ForumData(Exception e) {
			super(e);
		}
	}

	private class ForumAdapter extends PaginatedArrayAdapter<Thread> {
		public ForumAdapter(Context context, int resource, PaginatedData<Thread> data) {
			super(context, resource, data);
		}

		@Override
		protected boolean isLoaderLoading() {
			return isLoading();
		}

		@Override
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
			holder.lastpostdate.setText(String.format(mLastPostText,
				DateTimeUtils.formatForumDate(rootView.getContext(), thread.lastPostDate())));
			holder.postdate.setText(String.format(mCreatedText,
				DateTimeUtils.formatForumDate(rootView.getContext(), thread.postDate())));
		}
	}
}
