package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Article;
import com.boardgamegeek.model.ThreadResponse;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.widget.BggLoader;
import com.boardgamegeek.ui.widget.Data;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ThreadFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<ThreadFragment.ThreadData> {
	private static final int THREAD_LOADER_ID = 103;

	private ThreadAdapter mThreadAdapter;
	private int mThreadId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mThreadId = intent.getIntExtra(ForumsUtils.KEY_THREAD_ID, BggContract.INVALID_ID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListView listView = getListView();
		listView.setSmoothScrollbarEnabled(false);
		listView.setSelector(android.R.color.transparent);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_thread));
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(THREAD_LOADER_ID, null, this);
	}

	@Override
	public Loader<ThreadData> onCreateLoader(int id, Bundle data) {
		return new ThreadLoader(getActivity(), mThreadId);
	}

	@Override
	public void onLoadFinished(Loader<ThreadData> loader, ThreadData data) {
		if (getActivity() == null) {
			return;
		}

		if (mThreadAdapter == null) {
			mThreadAdapter = new ThreadAdapter(getActivity(), data.list());
			setListAdapter(mThreadAdapter);
		}
		mThreadAdapter.notifyDataSetChanged();

		if (data.hasError()) {
			setEmptyText(data.getErrorMessage());
		} else {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			restoreScrollState();
		}
	}

	@Override
	public void onLoaderReset(Loader<ThreadData> loader) {
	}

	private static class ThreadLoader extends BggLoader<ThreadData> {
		private BggService mService;
		private int mThreadId;

		public ThreadLoader(Context context, int threadId) {
			super(context);
			mService = Adapter.create();
			mThreadId = threadId;
		}

		@Override
		public ThreadData loadInBackground() {
			ThreadData forums;
			try {
				forums = new ThreadData(mService.thread(mThreadId));
			} catch (Exception e) {
				forums = new ThreadData(e);
			}
			return forums;
		}
	}

	static class ThreadData extends Data<Article> {
		private ThreadResponse mResponse;

		public ThreadData(ThreadResponse response) {
			super();
			mResponse = response;
		}

		public ThreadData(Exception e) {
			super(e);
		}

		@Override
		protected List<Article> list() {
			if (mResponse == null || mResponse.articles == null) {
				return new ArrayList<>();
			}
			return mResponse.articles;
		}
	}

	public static class ThreadAdapter extends ArrayAdapter<Article> {
		private LayoutInflater mInflater;

		public ThreadAdapter(Activity activity, List<Article> articles) {
			super(activity, R.layout.row_threadarticle, articles);
			mInflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_threadarticle, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			Article article;
			try {
				article = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (article != null) {
				holder.username.setText(article.username);
				int dateRes = R.string.posted_prefix;
				if (article.getNumberOfEdits() > 0) {
					dateRes = R.string.edited_prefix;
				}
				holder.editdate.setText(getContext().getString(dateRes,
					DateTimeUtils.formatForumDate(getContext(), article.editDate())));
				UIUtils.setTextMaybeHtml(holder.body, article.body);
				Bundle bundle = new Bundle();
				bundle.putString(ForumsUtils.KEY_USER, article.username);
				bundle.putLong(ForumsUtils.KEY_POST_DATE, article.postDate());
				bundle.putLong(ForumsUtils.KEY_EDIT_DATE, article.editDate());
				bundle.putInt(ForumsUtils.KEY_EDIT_COUNT, article.getNumberOfEdits());
				bundle.putString(ForumsUtils.KEY_BODY, article.body);
				bundle.putString(ForumsUtils.KEY_LINK, article.link);
				holder.viewArticle.setTag(bundle);
			}
			return convertView;
		}
	}

	public static class ViewHolder {
		@InjectView(R.id.article_username) TextView username;
		@InjectView(R.id.article_editdate) TextView editdate;
		@InjectView(R.id.article_body) TextView body;
		@InjectView(R.id.article_view) View viewArticle;

		public ViewHolder(View view) {
			ButterKnife.inject(this, view);
		}
	}
}
