package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.UIUtils;

public class ThreadActivity extends SimpleSinglePaneActivity {
	private static final int HELP_VERSION = 1;

	private int mThreadId;
	private String mThreadSubject;
	private int mForumId;
	private String mForumTitle;
	private int mGameId;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mThreadId = intent.getIntExtra(ForumsUtils.KEY_THREAD_ID, BggContract.INVALID_ID);
		mThreadSubject = intent.getStringExtra(ForumsUtils.KEY_THREAD_SUBJECT);
		mForumId = intent.getIntExtra(ForumsUtils.KEY_FORUM_ID, BggContract.INVALID_ID);
		mForumTitle = intent.getStringExtra(ForumsUtils.KEY_FORUM_TITLE);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);

		final ActionBar actionBar = getSupportActionBar();
		if (TextUtils.isEmpty(mGameName)) {
			actionBar.setTitle(mForumTitle);
			actionBar.setSubtitle(mThreadSubject);
		} else {
			actionBar.setTitle(mThreadSubject + " - " + mForumTitle);
			actionBar.setSubtitle(mGameName);
		}

		UIUtils.showHelpDialog(this, HelpUtils.HELP_THREAD_KEY, HELP_VERSION, R.string.help_thread);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ThreadFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, ForumActivity.class);
				intent.putExtra(ForumsUtils.KEY_FORUM_ID, mForumId);
				intent.putExtra(ForumsUtils.KEY_FORUM_TITLE, mForumTitle);
				intent.putExtra(ForumsUtils.KEY_GAME_ID, mGameId);
				intent.putExtra(ForumsUtils.KEY_GAME_NAME, mGameName);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "thread/" + mThreadId);
				return true;
			case R.id.menu_share:
				String description = String.format(getString(R.string.share_thread_text), mThreadSubject, mForumTitle,
					mGameName);
				String link = ActivityUtils.createBggUri("thread/" + mThreadId).toString();
				ActivityUtils.share(this, getString(R.string.share_thread_subject), description + "\n\n" + link,
					R.string.title_share_game);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onButtonClick(View v) {
		Intent intent = new Intent(this, ArticleActivity.class);
		Bundle b = (Bundle) v.getTag();
		b.putInt(ForumsUtils.KEY_THREAD_ID, mThreadId);
		b.putString(ForumsUtils.KEY_THREAD_SUBJECT, mThreadSubject);
		b.putInt(ForumsUtils.KEY_FORUM_ID, mForumId);
		b.putString(ForumsUtils.KEY_FORUM_TITLE, mForumTitle);
		b.putInt(ForumsUtils.KEY_GAME_ID, mGameId);
		b.putString(ForumsUtils.KEY_GAME_NAME, mGameName);
		intent.putExtras(b);
		startActivity(intent);
	}
}
