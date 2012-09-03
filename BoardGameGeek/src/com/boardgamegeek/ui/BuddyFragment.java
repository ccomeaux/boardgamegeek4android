package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.UIUtils;

public class BuddyFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private Uri mBuddyUri;

	private TextView mFullName;
	private TextView mName;
	private TextView mId;
	private ImageView mAvatar;

	private ImageFetcher mImageFetcher;

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

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		mImageFetcher.setImageFadeIn(false);
		mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.avatar_size));

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_buddy, null);

		mFullName = (TextView) rootView.findViewById(R.id.buddy_full_name);
		mName = (TextView) rootView.findViewById(R.id.buddy_name);
		mId = (TextView) rootView.findViewById(R.id.buddy_id);
		mAvatar = (ImageView) rootView.findViewById(R.id.buddy_avatar);

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
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

		int id = cursor.getInt(BuddyQuery.BUDDY_ID);
		String firstName = cursor.getString(BuddyQuery.FIRSTNAME).trim();
		String lastName = cursor.getString(BuddyQuery.LASTNAME).trim();
		String name = cursor.getString(BuddyQuery.NAME);

		mFullName.setText(firstName + " " + lastName);
		mName.setText(name);
		mId.setText(String.valueOf(id));

		final String avatarUrl = cursor.getString(BuddyQuery.AVATAR_URL);
		if (!TextUtils.isEmpty(avatarUrl)) {
			mImageFetcher
				.loadAvatarImage(avatarUrl, Buddies.buildAvatarUri(id), mAvatar, R.drawable.person_image_empty);
		}

		cursor.close();
	}

	private interface BuddyQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
			Buddies.AVATAR_URL };

		int BUDDY_ID = 0;
		int NAME = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
		int AVATAR_URL = 4;
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
