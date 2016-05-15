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
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class ThreadFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<SafeResponse<ThreadResponse>> {
	private static final int LOADER_ID = 103;
	private ThreadAdapter adapter;
	private int threadId;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		threadId = intent.getIntExtra(ActivityUtils.KEY_THREAD_ID, BggContract.INVALID_ID);
	}

	@Override
	@DebugLog
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListView listView = getListView();
		listView.setSmoothScrollbarEnabled(false);
		listView.setSelector(android.R.color.transparent);
	}

	@Override
	@DebugLog
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_thread));
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	@DebugLog
	public Loader<SafeResponse<ThreadResponse>> onCreateLoader(int id, Bundle data) {
		return new ThreadLoader(getActivity(), threadId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<SafeResponse<ThreadResponse>> loader, SafeResponse<ThreadResponse> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new ThreadAdapter(getActivity(),
				(data == null || data.getBody() == null) ?
					new ArrayList<Article>(0) :
					data.getBody().getArticles());
			setListAdapter(adapter);
		}
		initializeTimeBasedUi();

		if (data == null) {
			setEmptyText(getString(R.string.empty_thread));
		} else if (data.hasError()) {
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
	@DebugLog
	public void onLoaderReset(Loader<SafeResponse<ThreadResponse>> loader) {
	}

	@Override
	@DebugLog
	protected void updateTimeBasedUi() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private static class ThreadLoader extends BggLoader<SafeResponse<ThreadResponse>> {
		private final BggService bggService;
		private final int threadId;

		public ThreadLoader(Context context, int threadId) {
			super(context);
			bggService = Adapter.createForXml();
			this.threadId = threadId;
		}

		@Override
		public SafeResponse<ThreadResponse> loadInBackground() {
			return new SafeResponse<>(bggService.thread(threadId));
		}
	}

	static class ThreadAdapter extends ArrayAdapter<Article> {
		private final LayoutInflater inflater;

		@DebugLog
		public ThreadAdapter(Activity activity, List<Article> articles) {
			super(activity, R.layout.row_thread_article, articles);
			inflater = activity.getLayoutInflater();
		}

		@Override
		@DebugLog
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.row_thread_article, parent, false);
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
				holder.usernameView.setText(article.username);
				int dateRes = R.string.posted_prefix;
				if (article.getNumberOfEdits() > 0) {
					dateRes = R.string.edited_prefix;
				}
				holder.editDateView.setText(getContext().getString(dateRes, DateTimeUtils.formatForumDate(getContext(), article.editDate())));
				UIUtils.setTextMaybeHtml(holder.bodyView, article.body);
				Bundle bundle = new Bundle();
				bundle.putString(ActivityUtils.KEY_USER, article.username);
				bundle.putLong(ActivityUtils.KEY_POST_DATE, article.postDate());
				bundle.putLong(ActivityUtils.KEY_EDIT_DATE, article.editDate());
				bundle.putInt(ActivityUtils.KEY_EDIT_COUNT, article.getNumberOfEdits());
				bundle.putString(ActivityUtils.KEY_BODY, article.body);
				bundle.putString(ActivityUtils.KEY_LINK, article.link);
				holder.viewButton.setTag(bundle);
			}
			return convertView;
		}
	}

	@SuppressWarnings("unused")
	public static class ViewHolder {
		@BindView(R.id.username) TextView usernameView;
		@BindView(R.id.edit_date) TextView editDateView;
		@BindView(R.id.body) TextView bodyView;
		@BindView(R.id.view_button) View viewButton;

		@DebugLog
		public ViewHolder(View view) {
			ButterKnife.bind(this, view);
		}
	}
}
