package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.UIUtils;

public class BuddyFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private Uri mBuddyUri;

	private ViewGroup mRootView;
	private TextView mFullName;
	private TextView mName;
	private TextView mId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mBuddyUri = intent.getData();

		if (mBuddyUri == null) {
			return;
		}

		LoaderManager manager = getLoaderManager();
		manager.restartLoader(BuddyQuery._TOKEN, null, this);

		// mImageFetcher = UIUtils.getImageFetcher(getActivity());
		// mImageFetcher.setImageFadeIn(false);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(R.layout.activity_buddy, null);

		mFullName = (TextView) mRootView.findViewById(R.id.buddy_full_name);
		mName = (TextView) mRootView.findViewById(R.id.buddy_name);
		mId = (TextView) mRootView.findViewById(R.id.buddy_id);

		return mRootView;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == BuddyQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), mBuddyUri, BuddyQuery.PROJECTION, null, null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == BuddyQuery._TOKEN) {
			onBuddyQueryComplete(cursor);
		} else {
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private void onBuddyQueryComplete(Cursor cursor) {
		if (!cursor.moveToFirst()) {
			return;
		}

		mFullName.setText(cursor.getString(BuddyQuery.FIRSTNAME) + " " + cursor.getString(BuddyQuery.LASTNAME));
		mName.setText(cursor.getString(BuddyQuery.NAME));
		mId.setText(cursor.getString(BuddyQuery.BUDDY_ID));
	}

	private interface BuddyQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME, };

		int BUDDY_ID = 0;
		int NAME = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
	}

	// private class BuddyCollectionTask extends AsyncTask<Void, Void, RemoteBuddyCollectionHandler> {
	//
	// private Activity mActivity;
	// private HttpClient mHttpClient;
	// private RemoteExecutor mExecutor;
	// private RemoteBuddyCollectionHandler mHandler = new RemoteBuddyCollectionHandler();
	// private String mUrl;
	//
	// public BuddyCollectionTask(Activity activity, String username) {
	// this.mActivity = activity;
	// this.mUrl = HttpUtils.constructBriefCollectionUrl(username, "own");
	// }
	//
	// @Override
	// protected void onPreExecute() {
	// mHttpClient = HttpUtils.createHttpClient(mActivity, true);
	// mExecutor = new RemoteExecutor(mHttpClient, null);
	// }
	//
	// @Override
	// protected RemoteBuddyCollectionHandler doInBackground(Void... params) {
	// try {
	// mExecutor.executeGet(mUrl, mHandler);
	// } catch (HandlerException e) {
	// LOGE(TAG, "getting buddy", e);
	// }
	// return mHandler;
	// }
	//
	// @Override
	// protected void onPostExecute(RemoteBuddyCollectionHandler result) {
	// LOGI(TAG, "Buddy collection size: " + result.getCount());
	// final int count = result.getCount();
	// if (result.isBggDown()) {
	// UIUtils.showListMessage(mActivity, R.string.bgg_down);
	// } else if (count == 0) {
	// UIUtils.showListMessage(mActivity, mActivity.getResources().getString(R.string.empty_buddy_collection));
	// } else {
	// mInfoView.setText(String.format(getResources().getString(R.string.msg_collection_info), count));
	// mBuddyGames.addAll(result.getResults());
	// ((ListActivity) mActivity).setListAdapter(new BuddyActivityAdapter(mActivity, mBuddyGames));
	// }
	// }
	//
	// }
	//
	// private static class BuddyActivityAdapter extends ArrayAdapter<BuddyGame> {
	// private Activity mActivity;
	// private List<BuddyGame> mBuddyGames;
	//
	// private LayoutInflater mInflater;
	//
	// public BuddyActivityAdapter(Activity activity, List<BuddyGame> games) {
	// super(activity, R.layout.row_buddygame, games);
	// mActivity = activity;
	// mInflater = mActivity.getLayoutInflater();
	// mBuddyGames = games;
	// }
	//
	// @Override
	// public View getView(int position, View convertView, ViewGroup parent) {
	// BuddyGamesViewHolder holder;
	// if (convertView == null) {
	// convertView = mInflater.inflate(R.layout.row_buddygame, parent, false);
	// holder = new BuddyGamesViewHolder(convertView);
	// convertView.setTag(holder);
	// } else {
	// holder = (BuddyGamesViewHolder) convertView.getTag();
	// }
	//
	// BuddyGame game;
	// try {
	// game = mBuddyGames.get(position);
	// } catch (ArrayIndexOutOfBoundsException e) {
	// return convertView;
	// }
	// if (game != null) {
	// holder.id.setText(game.Id);
	// holder.name.setText(game.Name);
	// holder.year.setText(game.Year);
	// }
	// return convertView;
	// }
	// }
	//
	// public static class BuddyGamesViewHolder {
	// public TextView id;
	// public TextView name;
	// public TextView year;
	//
	// public BuddyGamesViewHolder(View view) {
	// id = (TextView) view.findViewById(R.id.gameId);
	// name = (TextView) view.findViewById(R.id.name);
	// year = (TextView) view.findViewById(R.id.year);
	// }
	// }

	// @Override
	// protected void onListItemClick(ListView listView, View convertView, int position, long id) {
	// super.onListItemClick(listView, convertView, position, id);
	// BuddyGamesViewHolder holder = (BuddyGamesViewHolder) convertView.getTag();
	// if (holder != null) {
	// final Uri gameUri = Games.buildGameUri(Integer.parseInt((String) holder.id.getText()));
	// final Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
	// intent.putExtra(BoardgameActivity.KEY_GAME_NAME, holder.name.getText());
	// startActivity(intent);
	// }
	// }
	//
}
