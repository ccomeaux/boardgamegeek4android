package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumActivity extends ListActivity {
	private final String TAG = "ForumActivity";

	public static final String KEY_FORUM_ID = "FORUM_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_FORUM_TITLE = "FORUM_NAME";
	public static final String KEY_NUM_THREADS = "NUM_THREADS";

	private ForumAdapter mAdapter;
	private List<ForumThread> mForumThreads = new ArrayList<ForumThread>();

	private String mForumId;
	private String mGameName;
	private String mThumbnailUrl;
	private String mForumName;
	private String mNumThreads;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_forum);
		findViewById(R.id.game_thumbnail).setClickable(false);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mForumId = intent.getExtras().getString(KEY_FORUM_ID);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			mForumName = intent.getExtras().getString(KEY_FORUM_TITLE);
			mNumThreads = intent.getExtras().getString(KEY_NUM_THREADS);
		} else {
			mForumId = savedInstanceState.getString(KEY_FORUM_ID);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mForumName = savedInstanceState.getString(KEY_FORUM_TITLE);
			mNumThreads = savedInstanceState.getString(KEY_NUM_THREADS);
		}

		UIUtils.setTitle(this, mForumName);
		UIUtils u = new UIUtils(this);
		u.setGameName(mGameName);
		u.setThumbnail(mThumbnailUrl);

		ForumTask task = new ForumTask();
		task.execute();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_FORUM_ID, mForumId);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putString(KEY_FORUM_TITLE, mForumName);
		outState.putString(KEY_NUM_THREADS, mNumThreads);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	private class ForumTask extends AsyncTask<Void, Void, RemoteForumHandler> {
		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteForumHandler mHandler = new RemoteForumHandler();

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(ForumActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteForumHandler doInBackground(Void... params) {
			final String url = HttpUtils.constructForumUrl(mForumId);
			Log.i(TAG, "Loading forum content from " + url);
			try {
				mExecutor.executeGet(url, mHandler);
			} catch (HandlerException e) {
				Log.e(TAG, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteForumHandler result) {
			Log.i(TAG, "Threads count " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(ForumActivity.this, R.string.bgg_down);
			} else if (count == 0) {
				String message = String.format(getResources().getString(R.string.forum_no_results), mForumName);
				UIUtils.showListMessage(ForumActivity.this, message);
			} else {
				mForumThreads.addAll(result.getResults());
				mAdapter = new ForumAdapter();
				setListAdapter(mAdapter);
			}
		}
	}

	@Override
	protected void onListItemClick(ListView listView, View convertView, int position, long id) {
		ViewHolder holder = (ViewHolder) convertView.getTag();
		if (holder != null) {
			Intent forumsIntent = new Intent(this, ThreadActivity.class);
			forumsIntent.putExtra(ThreadActivity.KEY_THREAD_ID, holder.threadId);
			// forumsIntent.putExtra(ThreadActivity.KEY_THUMBNAIL_URL,
			// mThumbnailUrl);
			forumsIntent.putExtra(ThreadActivity.KEY_THREAD_SUBJECT, holder.subject.getText());
			this.startActivity(forumsIntent);
		}
	}

	private class ForumAdapter extends ArrayAdapter<ForumThread> {
		private LayoutInflater mInflater;

		public ForumAdapter() {
			super(ForumActivity.this, R.layout.row_forumthread, mForumThreads);
			mInflater = getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_forumthread, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ForumThread thread;
			try {
				thread = mForumThreads.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (thread != null) {
				holder.threadId = thread.id;
				holder.subject.setText(thread.subject);
				holder.author.setText(thread.author);
				holder.lastpostdate.setText(thread.lastpostdate);
				holder.postdate.setText(thread.postdate);
			}
			return convertView;
		}
	}

	static class ViewHolder {
		String threadId;
		TextView subject;
		TextView author;
		TextView lastpostdate;
		TextView postdate;

		public ViewHolder(View view) {
			subject = (TextView) view.findViewById(R.id.thread_title);
			author = (TextView) view.findViewById(R.id.thread_author);
			lastpostdate = (TextView) view.findViewById(R.id.thread_lastpostdate);
			postdate = (TextView) view.findViewById(R.id.thread_postdate);
		}
	}
}
