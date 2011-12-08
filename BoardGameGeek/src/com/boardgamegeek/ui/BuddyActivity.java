package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddyCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.BuddyGame;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BuddyActivity extends ListActivity implements AsyncQueryListener {
	private final static String TAG = "BuddyActivity";

	private Uri mBuddyUri;
	private NotifyingAsyncQueryHandler mHandler;

	private TextView mFullName;
	private TextView mName;
	private TextView mId;
	private View mProgress;
	private ImageView mAvatarImage;
	private TextView mInfoView;

	private List<BuddyGame> mBuddyGames = new ArrayList<BuddyGame>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buddy);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		mFullName = (TextView) findViewById(R.id.buddy_full_name);
		mName = (TextView) findViewById(R.id.buddy_name);
		mId = (TextView) findViewById(R.id.buddy_id);
		mAvatarImage = (ImageView) findViewById(R.id.buddy_avatar);
		mProgress = findViewById(R.id.buddy_progress);
		mInfoView = (TextView) findViewById(R.id.collection_info);

		final Intent intent = getIntent();
		mBuddyUri = intent.getData();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBuddyUri, BuddiesQuery.PROJECTION);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst()) {
				return;
			}

			mFullName.setText(cursor.getString(BuddiesQuery.FIRSTNAME) + " " + cursor.getString(BuddiesQuery.LASTNAME));
			mName.setText(cursor.getString(BuddiesQuery.NAME));
			mId.setText(cursor.getString(BuddiesQuery.BUDDY_ID));

			if (BggApplication.getInstance().getImageLoad()) {
				final String url = cursor.getString(BuddiesQuery.AVATAR_URL);
				new AvatarTask().execute(url);
			}

			BuddyCollectionTask buddyCollectionTask = new BuddyCollectionTask(this, (String) mName.getText());
			buddyCollectionTask.execute();
		} finally {
			cursor.close();
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	private class AvatarTask extends AsyncTask<String, Void, Drawable> {
		@Override
		protected void onPreExecute() {
			mProgress.setVisibility(View.VISIBLE);
		}

		@Override
		protected Drawable doInBackground(String... params) {
			return ImageCache.getImage(BuddyActivity.this, params[0]);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			mProgress.setVisibility(View.GONE);
			if (result == null) {
				mAvatarImage.setVisibility(View.GONE);
			} else {
				mAvatarImage.setVisibility(View.VISIBLE);
				mAvatarImage.setImageDrawable(result);
			}
		}
	}

	@Override
	protected void onListItemClick(ListView listView, View convertView, int position, long id) {
		super.onListItemClick(listView, convertView, position, id);
		BuddyGamesViewHolder holder = (BuddyGamesViewHolder) convertView.getTag();
		if (holder != null) {
			final Uri gameUri = Games.buildGameUri(Integer.parseInt((String) holder.id.getText()));
			final Intent intent = new Intent(Intent.ACTION_VIEW, gameUri);
			intent.putExtra(BoardgameActivity.KEY_GAME_NAME, holder.name.getText());
			startActivity(intent);
		}
	}

	private class BuddyCollectionTask extends AsyncTask<Void, Void, RemoteBuddyCollectionHandler> {

		private Activity mActivity;
		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteBuddyCollectionHandler mHandler = new RemoteBuddyCollectionHandler();
		private String mUrl;

		public BuddyCollectionTask(Activity activity, String userName) {
			this.mActivity = activity;
			this.mUrl = HttpUtils.constructCollectionUrl(userName, "own");
		}

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(mActivity, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteBuddyCollectionHandler doInBackground(Void... params) {
			try {
				mExecutor.executeGet(mUrl, mHandler);
			} catch (HandlerException e) {
				Log.e(TAG, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteBuddyCollectionHandler result) {
			Log.i(TAG, "Buddy collection size: " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(mActivity, R.string.bgg_down);
			} else if (count == 0) {
				UIUtils.showListMessage(mActivity, mActivity.getResources().getString(R.string.empty_buddy_collection));
			} else {
				mInfoView.setText(String.format(getResources().getString(R.string.msg_collection_info), count));
				mBuddyGames.addAll(result.getResults());
				((ListActivity) mActivity).setListAdapter(new BuddyActivityAdapter(mActivity, mBuddyGames));
			}
		}

	}

	private static class BuddyActivityAdapter extends ArrayAdapter<BuddyGame> {
		private Activity mActivity;
		private List<BuddyGame> mBuddyGames;

		private LayoutInflater mInflater;

		public BuddyActivityAdapter(Activity activity, List<BuddyGame> games) {
			super(activity, R.layout.row_buddygame, games);
			mActivity = activity;
			mInflater = mActivity.getLayoutInflater();
			mBuddyGames = games;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BuddyGamesViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_buddygame, parent, false);
				holder = new BuddyGamesViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (BuddyGamesViewHolder) convertView.getTag();
			}

			BuddyGame game;
			try {
				game = mBuddyGames.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (game != null) {
				holder.id.setText(game.Id);
				holder.name.setText(game.Name);
				holder.year.setText(game.Year);
			}
			return convertView;
		}
	}

	public static class BuddyGamesViewHolder {
		public TextView id;
		public TextView name;
		public TextView year;

		public BuddyGamesViewHolder(View view) {
			id = (TextView) view.findViewById(R.id.gameId);
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
				Buddies.AVATAR_URL, };

		int BUDDY_ID = 0;
		int NAME = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
		int AVATAR_URL = 4;
	}
}
