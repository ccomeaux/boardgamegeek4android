package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class ForumFragment extends BggListFragment implements OnScrollListener,
	LoaderManager.LoaderCallbacks<PaginatedData<Thread>> {
	private static final int LOADER_ID = 0;

	private ForumAdapter adapter;
	private int forumId;
	private String forumTitle;
	private int gameId;
	private String gameName;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		forumId = intent.getIntExtra(ActivityUtils.KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = intent.getStringExtra(ActivityUtils.KEY_FORUM_TITLE);
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
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
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	protected boolean padTop() {
		return true;
	}

	@Override
	protected boolean dividerShown() {
		return true;
	}

	@DebugLog
	private void loadMoreResults() {
		if (isAdded()) {
			Loader<List<Thread>> loader = getLoaderManager().getLoader(LOADER_ID);
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
			intent.putExtra(ActivityUtils.KEY_THREAD_SUBJECT, holder.subjectView.getText());
			intent.putExtra(ActivityUtils.KEY_FORUM_ID, forumId);
			intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, forumTitle);
			intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
			intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
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
		return new ForumLoader(getActivity(), forumId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<PaginatedData<Thread>> loader, PaginatedData<Thread> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new ForumAdapter(getActivity(), data);
			setListAdapter(adapter);
		} else {
			adapter.update(data);
		}
		initializeTimeBasedUi();
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
	@Nullable
	private ForumLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<Thread>> loader = getLoaderManager().getLoader(LOADER_ID);
			return (ForumLoader) loader;
		}
		return null;
	}

	@DebugLog
	private static class ForumLoader extends PaginatedLoader<Thread> {
		private final BggService bggService;
		private final int forumId;

		public ForumLoader(Context context, int forumId) {
			super(context);
			bggService = Adapter.createForXml();
			this.forumId = forumId;
		}

		@DebugLog
		@Override
		protected PaginatedData<Thread> fetchPage(int pageNumber) {
			ForumData data;
			try {
				data = new ForumData(bggService.forum(forumId, pageNumber).execute().body(), pageNumber);
			} catch (Exception e) {
				data = new ForumData(e);
			}
			return data;
		}
	}

	@Override
	@DebugLog
	protected void updateTimeBasedUi() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	static class ForumData extends PaginatedData<Thread> {
		public ForumData(ForumResponse response, int page) {
			super(response.getThreads(), response.numberOfThreads(), page, ForumResponse.PAGE_SIZE);
		}

		public ForumData(Exception e) {
			super(e);
		}
	}

	private class ForumAdapter extends PaginatedArrayAdapter<Thread> {
		@DebugLog
		public ForumAdapter(Context context, PaginatedData<Thread> data) {
			super(context, R.layout.row_forum_thread, data);
		}

		@Override
		@DebugLog
		protected void bind(View view, Thread item) {
			ThreadRowViewBinder.bindActivityView(view, item);
		}
	}

	static class ThreadRowViewBinder {
		@SuppressWarnings("unused")
		public static class ViewHolder {
			public int threadId;
			@BindView(R.id.subject) TextView subjectView;
			@BindView(R.id.author) TextView authorView;
			@BindView(R.id.number_of_articles) TextView numberOfArticlesView;
			@BindView(R.id.last_post_date) TextView lastPostDateView;
			@BindView(R.id.post_date) TextView postDateView;

			@DebugLog
			public ViewHolder(View view) {
				ButterKnife.bind(this, view);
			}
		}

		@DebugLog
		public static void bindActivityView(View rootView, Thread thread) {
			final ViewHolder holder = getViewHolder(rootView);
			final Context context = rootView.getContext();

			holder.threadId = thread.id;
			holder.subjectView.setText(thread.subject);
			holder.authorView.setText(context.getString(R.string.forum_thread_author, thread.author));
			int replies = thread.numberOfArticles - 1;
			holder.numberOfArticlesView.setText(context.getResources().getQuantityString(R.plurals.forum_thread_replies, replies, replies));
			formatDate(context, holder.lastPostDateView, thread.lastPostDate(), R.string.forum_last_post);
			formatDate(context, holder.postDateView, thread.postDate(), R.string.forum_thread_created);
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

		private static void formatDate(Context context, TextView textView, long date, @StringRes int stringResId) {
			CharSequence formattedDate = DateTimeUtils.formatForumDate(context, date);
			textView.setText(context.getString(stringResId, formattedDate));
		}
	}
}
