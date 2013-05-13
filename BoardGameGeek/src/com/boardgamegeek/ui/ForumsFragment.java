package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumsHandler;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumsFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<List<Forum>> {
	private static final String TAG = makeLogTag(ForumsFragment.class);
	private static final int FORUMS_LOADER_ID = 0;
	private static final String GENERAL_FORUMS_URL = HttpUtils.BASE_URL_2 + "forumlist?id=1&type=region";

	private String mUrl;
	private int mGameId;
	private String mGameName;
	private ForumsAdapter mForumsAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		Uri uri = intent.getData();
		mGameId = Games.getGameId(uri);
		mUrl = (mGameId != BggContract.INVALID_ID) ? HttpUtils.constructForumlistUrl(mGameId) : GENERAL_FORUMS_URL;
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);

		setListAdapter(mForumsAdapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_forums));
	}

	@Override
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().restartLoader(FORUMS_LOADER_ID, null, this);
	}

	@Override
	public Loader<List<Forum>> onCreateLoader(int id, Bundle data) {
		return new ForumsLoader(getActivity(), mUrl);
	}

	@Override
	public void onLoadFinished(Loader<List<Forum>> loader, List<Forum> forums) {
		if (getActivity() == null) {
			return;
		}

		mForumsAdapter = new ForumsAdapter(getActivity(), forums);
		setListAdapter(mForumsAdapter);

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
	public void onLoaderReset(Loader<List<Forum>> forums) {
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		ForumViewHolder holder = (ForumViewHolder) convertView.getTag();
		if (holder != null) {
			Intent intent = new Intent(getActivity(), ForumActivity.class);
			intent.putExtra(ForumsUtils.KEY_FORUM_ID, holder.forumId);
			intent.putExtra(ForumsUtils.KEY_FORUM_TITLE, holder.forumTitle.getText());
			intent.putExtra(ForumsUtils.KEY_GAME_ID, mGameId);
			intent.putExtra(ForumsUtils.KEY_GAME_NAME, mGameName);
			startActivity(intent);
		}
	}

	private boolean loaderHasError() {
		ForumsLoader loader = getLoader();
		return (loader != null) ? loader.hasError() : false;
	}

	private String loaderErrorMessage() {
		ForumsLoader loader = getLoader();
		return (loader != null) ? loader.getErrorMessage() : "";
	}

	private ForumsLoader getLoader() {
		if (isAdded()) {
			Loader<List<Forum>> loader = getLoaderManager().getLoader(FORUMS_LOADER_ID);
			return (ForumsLoader) loader;
		}
		return null;
	}

	private static class ForumsLoader extends AsyncTaskLoader<List<Forum>> {
		private List<Forum> mData;
		private String mUrl;
		private String mErrorMessage;

		public ForumsLoader(Context context, String url) {
			super(context);
			mUrl = url;
			mErrorMessage = "";
		}

		@Override
		public List<Forum> loadInBackground() {
			HttpClient httpClient = HttpUtils.createHttpClient(getContext(), true);
			RemoteExecutor executor = new RemoteExecutor(httpClient, getContext());
			RemoteForumsHandler handler = new RemoteForumsHandler();

			LOGI(TAG, "Loading forums from " + mUrl);
			executor.safelyExecuteGet(mUrl, handler);
			mErrorMessage = handler.getErrorMessage();
			return handler.getResults();
		}

		@Override
		public void deliverResult(List<Forum> forums) {
			if (isReset()) {
				return;
			}

			mData = forums;
			if (isStarted()) {
				super.deliverResult(forums == null ? null : new ArrayList<Forum>(forums));
			}
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
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset() {
			super.onReset();
			onStopLoading();
			mData = null;
		}

		public boolean hasError() {
			return !TextUtils.isEmpty(mErrorMessage);
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}
	}

	public static class ForumsAdapter extends ArrayAdapter<Forum> {
		private LayoutInflater mInflater;
		private Resources mResources;
		private String mLastPostText;
		private NumberFormat mFormat = NumberFormat.getInstance();

		public ForumsAdapter(Activity activity, List<Forum> forums) {
			super(activity, R.layout.row_forum, forums);
			mInflater = activity.getLayoutInflater();
			mResources = activity.getResources();
			mLastPostText = mResources.getString(R.string.forum_last_post);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ForumViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_forum, parent, false);
				holder = new ForumViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ForumViewHolder) convertView.getTag();
			}

			Forum forum;
			try {
				forum = getItem(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (forum != null) {
				holder.forumId = forum.id;
				holder.forumTitle.setText(forum.title);
				holder.numThreads.setText(mResources.getQuantityString(R.plurals.forum_threads, forum.numberOfThreads,
					mFormat.format(forum.numberOfThreads)));
				holder.lastPost.setText(String.format(mLastPostText,
					DateUtils.getRelativeTimeSpanString(forum.lastPostDate)));
				holder.lastPost.setVisibility((forum.lastPostDate > 0) ? View.VISIBLE : View.GONE);
			}
			return convertView;
		}
	}

	public static class ForumViewHolder {
		public String forumId;
		public TextView forumTitle;
		public TextView numThreads;
		public TextView lastPost;

		public ForumViewHolder(View view) {
			forumTitle = (TextView) view.findViewById(R.id.forum_title);
			numThreads = (TextView) view.findViewById(R.id.numthreads);
			lastPost = (TextView) view.findViewById(R.id.lastpost);
		}
	}
}
