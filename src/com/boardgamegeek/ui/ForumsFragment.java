package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
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

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumlistHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumsFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<List<Forum>> {
	private static final String TAG = makeLogTag(ForumsFragment.class);

	public static final String KEY_GAME_NAME = "GAME_NAME";
	
	private static final int FORUMS_LOADER_ID = 0;
	private static final String GENERAL_FORUMS_URL = HttpUtils.BASE_URL_2 + "forumlist?id=1&type=region";

	private String mUrl;
	private int mGameId;
	private String mGameName = "";
	private ForumsAdapter mForumsAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		Uri uri = intent.getData();
		mGameId = Games.getGameId(uri);
		mUrl = (mGameId != BggContract.INVALID_ID) ? HttpUtils.constructForumlistUrl(mGameId) : GENERAL_FORUMS_URL;
		mGameName = intent.getStringExtra(KEY_GAME_NAME);

		setListAdapter(mForumsAdapter);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);

		final ListView listView = getListView();
		listView.setCacheColorHint(Color.WHITE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_forums));
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
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Forum>> forums) {
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		ForumViewHolder holder = (ForumViewHolder) convertView.getTag();
		if (holder != null) {
			Intent forumsIntent = new Intent(getActivity(), ForumActivity.class);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_ID, holder.forumId);
			forumsIntent.putExtra(ForumActivity.KEY_GAME_ID, mGameId);
			forumsIntent.putExtra(ForumActivity.KEY_GAME_NAME, mGameName);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_TITLE, holder.forumTitle.getText());
			forumsIntent.putExtra(ForumActivity.KEY_NUM_THREADS, holder.numThreads.getText());
			this.startActivity(forumsIntent);
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
		private String mUrl;
		private String mErrorMessage;

		public ForumsLoader(Context context, String url) {
			super(context);
			mUrl = url;
			mErrorMessage = "";
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		public List<Forum> loadInBackground() {
			HttpClient httpClient = HttpUtils.createHttpClient(getContext(), true);
			RemoteExecutor executor = new RemoteExecutor(httpClient, null);
			RemoteForumlistHandler handler = new RemoteForumlistHandler();

			LOGI(TAG, "Loading comments from " + mUrl);
			try {
				executor.executeGet(mUrl, handler);

				if (handler.isBggDown()) {
					mErrorMessage = getContext().getString(R.string.bgg_down);
				} else {
					mErrorMessage = "";
				}
			} catch (HandlerException e) {
				LOGE(TAG, "getting forums", e);
				mErrorMessage = e.getMessage();
			}
			return handler.getResults();
		}

		@Override
		public void deliverResult(List<Forum> forums) {
			if (isStarted()) {
				super.deliverResult(forums == null ? null : new ArrayList<Forum>(forums));
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
				holder.numThreads.setText(mResources.getQuantityString(R.plurals.forum_threads, forum.numthreads,
					forum.numthreads));
				if (forum.lastpostdate > 0) {
					holder.lastPost.setText(String.format(mLastPostText, DateUtils.getRelativeTimeSpanString(
						forum.lastpostdate, System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS, 0)));
					holder.lastPost.setVisibility(View.VISIBLE);
				} else {
					holder.lastPost.setVisibility(View.GONE);
				}
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
