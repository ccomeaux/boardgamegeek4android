package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteForumlistHandler;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumlistActivity extends ListActivity {
	private final String TAG = "ForumlistActivity";

	public static final String KEY_FORUMLIST_ID = "FORUMLIST_ID";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_FORUMS = "FORUMS";

//	private ArrayAdapter mAdapter;
	private List<Forum> mForums = new ArrayList<Forum>();

	private int mForumlistId;
	private String mThumbnailUrl;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_forumlist);
		findViewById(R.id.game_thumbnail).setClickable(false);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mForumlistId = intent.getExtras().getInt(KEY_FORUMLIST_ID);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
		} else {
			mForumlistId = savedInstanceState.getInt(KEY_FORUMLIST_ID);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mForums = savedInstanceState.getParcelableArrayList(KEY_FORUMS);
		}

		UIUtils.setTitle(this);
		UIUtils u = new UIUtils(this);
		u.setGameName(mGameName);
		u.setThumbnail(mThumbnailUrl);

		if (mForums == null || mForums.size() == 0) {
			AsyncTask<Void, Void, RemoteForumlistHandler> task =
				new ForumsUtils.ForumlistTask(this, mForums, HttpUtils.constructForumlistUrl(mForumlistId), mGameName, TAG);
			task.execute();
		} else {
			setListAdapter(new ForumsUtils.ForumlistAdapter(this, mForums));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_FORUMLIST_ID, mForumlistId);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putParcelableArrayList(KEY_FORUMS, (ArrayList<? extends Parcelable>) mForums);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	@Override
	protected void onListItemClick(ListView listView, View convertView, int position, long id) {
		ForumsUtils.ForumlistViewHolder holder = (ForumsUtils.ForumlistViewHolder) convertView.getTag();
		if (holder != null) {
			Intent forumsIntent = new Intent(this, ForumActivity.class);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_ID, holder.forumId);
			forumsIntent.putExtra(ForumActivity.KEY_GAME_NAME, mGameName);
			forumsIntent.putExtra(ForumActivity.KEY_THUMBNAIL_URL, mThumbnailUrl);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_TITLE, holder.forumTitle.getText());
			forumsIntent.putExtra(ForumActivity.KEY_NUM_THREADS, holder.numThreads.getText());
			this.startActivity(forumsIntent);
		}
	}
}
