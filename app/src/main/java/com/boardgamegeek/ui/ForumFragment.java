package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
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
import com.boardgamegeek.util.PresentationUtils;
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

	class ForumAdapter extends PaginatedArrayAdapter<Thread> {
		@DebugLog
		public ForumAdapter(Context context, PaginatedData<Thread> data) {
			super(context, R.layout.row_forum_thread, data);
		}

		@Override
		@DebugLog
		protected void bind(View view, Thread item) {
			final ViewHolder holder = getViewHolder(view, forumId, forumTitle, gameId, gameName);
			holder.bind(item);
		}

		class ViewHolder {
			private final View rootView;
			private final int forumId;
			private final String forumTitle;
			private final int gameId;
			private final String gameName;
			private int threadId;
			@BindView(R.id.subject) TextView subjectView;
			@BindView(R.id.author) TextView authorView;
			@BindView(R.id.number_of_articles) TextView numberOfArticlesView;
			@BindView(R.id.last_post_date) TextView lastPostDateView;
			@BindView(R.id.post_date) TextView postDateView;

			@DebugLog
			public ViewHolder(View view, int forumId, String forumTitle, int gameId, String gameName) {
				rootView = view;
				this.forumId = forumId;
				this.forumTitle = forumTitle;
				this.gameId = gameId;
				this.gameName = gameName;
				ButterKnife.bind(this, view);
			}

			public void bind(Thread thread) {
				final Context context = rootView.getContext();
				threadId = thread.id;
				subjectView.setText(thread.subject);
				authorView.setText(context.getString(R.string.forum_thread_author, thread.author));
				int replies = thread.numberOfArticles - 1;
				numberOfArticlesView.setText(context.getResources().getQuantityString(R.plurals.forum_thread_replies, replies, replies));
				PresentationUtils.formatDate(context, lastPostDateView, thread.lastPostDate(), R.string.forum_last_post);
				PresentationUtils.formatDate(context, postDateView, thread.postDate(), R.string.forum_thread_created);
				rootView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(context, ThreadActivity.class);
						intent.putExtra(ActivityUtils.KEY_THREAD_ID, threadId);
						intent.putExtra(ActivityUtils.KEY_THREAD_SUBJECT, subjectView.getText());
						intent.putExtra(ActivityUtils.KEY_FORUM_ID, forumId);
						intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, forumTitle);
						intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
						intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
						context.startActivity(intent);
					}
				});
			}
		}

		@NonNull
		private ViewHolder getViewHolder(View rootView, int forumId, String forumTitle, int gameId, String gameName) {
			ViewHolder tag = (ViewHolder) rootView.getTag();
			if (tag != null) {
				return tag;
			} else {
				final ViewHolder holder = new ViewHolder(rootView, forumId, forumTitle, gameId, gameName);
				rootView.setTag(holder);
				return holder;
			}
		}
	}
}
