package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumlistActivity extends ListActivity {
	private static final String TAG = "ForumlistActivity";
	private static final String GENERAL_FORUMLIST_LINK = "http://boardgamegeek.com/xmlapi2/forumlist?id=1&type=region";

	public static final String KEY_FORUMLIST_ID = "FORUMLIST_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_FORUMS = "FORUMS";

	private List<Forum> mForums = new ArrayList<Forum>();

	private int mForumlistId;
	private int mGameId;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_forumlist);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mForumlistId = intent.getExtras().getInt(KEY_FORUMLIST_ID);
			mGameId = intent.getExtras().getInt(KEY_GAME_ID);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
		} else {
			mForumlistId = savedInstanceState.getInt(KEY_FORUMLIST_ID);
			mGameId = savedInstanceState.getInt(KEY_GAME_ID);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mForums = savedInstanceState.getParcelableArrayList(KEY_FORUMS);
		}

		if (TextUtils.isEmpty(mGameName)) {
			UIUtils.setTitle(this, R.string.title_generalforumlist);
			findViewById(R.id.forumlist_game_header).setVisibility(View.GONE);
			findViewById(R.id.forumlist_header_divider).setVisibility(View.GONE);
		} else {
			UIUtils.setTitle(this);
			findViewById(R.id.forumlist_game_header).setVisibility(View.VISIBLE);
			findViewById(R.id.forumlist_header_divider).setVisibility(View.VISIBLE);
			UIUtils.setGameHeader(this, mGameName, mGameId);
		}

		if (mForums == null || mForums.size() == 0) {
			String url = TextUtils.isEmpty(mGameName) ? GENERAL_FORUMLIST_LINK : HttpUtils
				.constructForumlistUrl(mForumlistId);
			ForumsUtils.ForumlistTask task = new ForumsUtils.ForumlistTask(this, mForums, url, mGameName, TAG);

			task.execute();
		} else {
			setListAdapter(new ForumsUtils.ForumlistAdapter(this, mForums));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_FORUMLIST_ID, mForumlistId);
		outState.putInt(KEY_GAME_ID, mGameId);
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
			forumsIntent.putExtra(ForumActivity.KEY_GAME_ID, mGameId);
			forumsIntent.putExtra(ForumActivity.KEY_GAME_NAME, mGameName);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_TITLE, holder.forumTitle.getText());
			forumsIntent.putExtra(ForumActivity.KEY_NUM_THREADS, holder.numThreads.getText());
			this.startActivity(forumsIntent);
		}
	}
}
