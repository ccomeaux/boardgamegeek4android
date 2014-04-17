package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteThreadParser;
import com.boardgamegeek.model.ThreadArticle;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

public class ThreadFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<List<ThreadArticle>> {
	private static final int THREAD_LOADER_ID = 103;

	private ThreadAdapter mThreadAdapter;
	private int mThreadId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mThreadId = intent.getIntExtra(ForumsUtils.KEY_THREAD_ID, BggContract.INVALID_ID);

		setListAdapter(mThreadAdapter);
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
	public Loader<List<ThreadArticle>> onCreateLoader(int id, Bundle data) {
		return new ThreadLoader(getActivity(), mThreadId);
	}

	@Override
	public void onLoadFinished(Loader<List<ThreadArticle>> loader, List<ThreadArticle> articles) {
		if (getActivity() == null) {
			return;
		}

		mThreadAdapter = new ThreadAdapter(getActivity(), articles);
		setListAdapter(mThreadAdapter);

		if (loaderHasError()) {
			setEmptyText(loaderErrorMessage());
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
	public void onLoaderReset(Loader<List<ThreadArticle>> loader) {
	}

	private boolean loaderHasError() {
		final ThreadLoader loader = getLoader();
		return (loader != null) ? loader.hasError() : false;
	}

	private String loaderErrorMessage() {
		final ThreadLoader loader = getLoader();
		return (loader != null) ? loader.getErrorMessage() : "";
	}

	private ThreadLoader getLoader() {
		if (isAdded()) {
			Loader<List<ThreadArticle>> loader = getLoaderManager().getLoader(THREAD_LOADER_ID);
			return (ThreadLoader) loader;
		}
		return null;
	}

	private static class ThreadLoader extends AsyncTaskLoader<List<ThreadArticle>> {
		private int mThreadId;
		private String mErrorMessage;

		public ThreadLoader(Context context, int threadId) {
			super(context);
			mThreadId = threadId;
			mErrorMessage = "";
		}

		@Override
		public List<ThreadArticle> loadInBackground() {
			RemoteExecutor executor = new RemoteExecutor(getContext());
			RemoteThreadParser parser = new RemoteThreadParser(mThreadId);
			executor.safelyExecuteGet(parser);
			mErrorMessage = parser.getErrorMessage();
			return parser.getResults();
		}

		@Override
		public void deliverResult(List<ThreadArticle> articles) {
			if (isStarted()) {
				super.deliverResult(articles == null ? null : new ArrayList<ThreadArticle>(articles));
			}
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset() {
			super.onReset();
			onStopLoading();
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}
	}

	public static class ThreadAdapter extends ArrayAdapter<ThreadArticle> {
		private LayoutInflater mInflater;

		public ThreadAdapter(Activity activity, List<ThreadArticle> articles) {
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

			ThreadArticle article;
			try {
				article = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (article != null) {
				holder.username.setText(article.username);
				holder.editdate.setText(DateTimeUtils.formatForumDate(getContext(), article.editDate));
				UIUtils.setTextMaybeHtml(holder.body, article.body);
				Bundle bundle = new Bundle();
				bundle.putString(ForumsUtils.KEY_USER, article.username);
				bundle.putLong(ForumsUtils.KEY_DATE, article.editDate);
				bundle.putString(ForumsUtils.KEY_BODY, article.body);
				bundle.putString(ForumsUtils.KEY_LINK, article.link);
				holder.viewArticle.setTag(bundle);
			}
			return convertView;
		}
	}

	public static class ViewHolder {
		TextView username;
		TextView editdate;
		TextView body;
		View viewArticle;

		public ViewHolder(View view) {
			username = (TextView) view.findViewById(R.id.article_username);
			editdate = (TextView) view.findViewById(R.id.article_editdate);
			body = (TextView) view.findViewById(R.id.article_body);
			viewArticle = view.findViewById(R.id.article_view);
		}
	}
}
