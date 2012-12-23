package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddyCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.model.BuddyGame;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.CollectionUrlBuilder;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class BuddyCollectionFragment extends SherlockListFragment implements
	LoaderManager.LoaderCallbacks<List<BuddyGame>> {
	private static final String TAG = makeLogTag(BuddyCollectionFragment.class);
	private static final int BUDDY_GAMES_LOADER_ID = 1;

	private BuddyGamesAdapter mGamesAdapter;
	private String mUrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		String mBuddyName = intent.getStringExtra(BuddiesActivity.KEY_BUDDY_NAME);

		if (TextUtils.isEmpty(mBuddyName)) {
			LOGW(TAG, "Missing buddy name.");
			return;
		}

		mUrl = new CollectionUrlBuilder(mBuddyName).status("own").brief().build();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_buddy_collection));
		getLoaderManager().initLoader(BUDDY_GAMES_LOADER_ID, null, this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);

		final ListView listView = getListView();
		listView.setFastScrollEnabled(true);
		listView.setCacheColorHint(Color.WHITE);
		listView.setSelector(android.R.color.transparent);
	}

	@Override
	public void onListItemClick(ListView listView, View convertView, int position, long id) {
		super.onListItemClick(listView, convertView, position, id);
		BuddyGameViewHolder holder = (BuddyGameViewHolder) convertView.getTag();
		if (holder != null) {
			ActivityUtils.launchGame(getActivity(), Integer.parseInt(holder.id), holder.name.getText().toString());
		}
	}

	@Override
	public Loader<List<BuddyGame>> onCreateLoader(int id, Bundle data) {
		return new BuddyGamesLoader(getActivity(), mUrl);
	}

	@Override
	public void onLoadFinished(Loader<List<BuddyGame>> loader, List<BuddyGame> games) {
		if (getActivity() == null) {
			return;
		}

		mGamesAdapter = new BuddyGamesAdapter(getActivity(), games);
		setListAdapter(mGamesAdapter);

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
	public void onLoaderReset(Loader<List<BuddyGame>> loader) {
	}

	private boolean loaderHasError() {
		final BuddyGamesLoader loader = getLoader();
		return (loader != null) ? loader.hasError() : false;
	}

	private String loaderErrorMessage() {
		final BuddyGamesLoader loader = getLoader();
		return (loader != null) ? loader.getErrorMessage() : "";
	}

	private BuddyGamesLoader getLoader() {
		if (isAdded()) {
			Loader<List<BuddyGame>> loader = getLoaderManager().getLoader(BUDDY_GAMES_LOADER_ID);
			return (BuddyGamesLoader) loader;
		}
		return null;
	}

	private static class BuddyGamesLoader extends AsyncTaskLoader<List<BuddyGame>> {
		private String mUrl;
		private String mErrorMessage;

		public BuddyGamesLoader(Context context, String url) {
			super(context);
			mUrl = url;
			mErrorMessage = "";
		}

		@Override
		public List<BuddyGame> loadInBackground() {
			HttpClient httpClient = HttpUtils.createHttpClient(getContext(), true);
			RemoteExecutor executor = new RemoteExecutor(httpClient, null);
			RemoteBuddyCollectionHandler handler = new RemoteBuddyCollectionHandler();

			LOGI(TAG, "Loading buddy collection from " + mUrl);
			executor.safelyExecuteGet(mUrl, handler);
			mErrorMessage = handler.getErrorMessage();

			return handler.getResults();
		}

		@Override
		public void deliverResult(List<BuddyGame> games) {
			if (isStarted()) {
				super.deliverResult(games == null ? null : new ArrayList<BuddyGame>(games));
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

	public static class BuddyGamesAdapter extends ArrayAdapter<BuddyGame> {
		private List<BuddyGame> mBuddyGames;
		private LayoutInflater mInflater;

		public BuddyGamesAdapter(Activity activity, List<BuddyGame> games) {
			super(activity, R.layout.row_collection, games);
			mInflater = activity.getLayoutInflater();
			mBuddyGames = games;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BuddyGameViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_collection, parent, false);
				holder = new BuddyGameViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (BuddyGameViewHolder) convertView.getTag();
			}

			BuddyGame game;
			try {
				game = mBuddyGames.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (game != null) {
				holder.name.setText(game.Name);
				holder.year.setText(game.Year);
				holder.id = game.Id;
			}
			return convertView;
		}
	}

	public static class BuddyGameViewHolder {
		public TextView name;
		public TextView year;
		public String id;

		public BuddyGameViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
		}
	}
}
