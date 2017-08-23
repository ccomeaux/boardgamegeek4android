package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.ShareEvent;

public class ThreadActivity extends SimpleSinglePaneActivity {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_THREAD_ID = "THREAD_ID";
	private static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";

	private int threadId;
	private String threadSubject;
	private int forumId;
	private String forumTitle;
	private int gameId;
	private String gameName;

	public static void start(Context context, int threadId, String threadSubject, int forumId, String forumTitle, int gameId, String gameName) {
		Intent starter = createIntent(context, threadId, threadSubject, forumId, forumTitle, gameId, gameName);
		context.startActivity(starter);
	}

	public static void startUp(Context context, int threadId, String threadSubject, int forumId, String forumTitle, int gameId, String gameName) {
		Intent starter = createIntent(context, threadId, threadSubject, forumId, forumTitle, gameId, gameName);
		starter.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@NonNull
	private static Intent createIntent(Context context, int threadId, String threadSubject, int forumId, String forumTitle, int gameId, String gameName) {
		Intent starter = new Intent(context, ThreadActivity.class);
		starter.putExtra(KEY_THREAD_ID, threadId);
		starter.putExtra(KEY_THREAD_SUBJECT, threadSubject);
		starter.putExtra(KEY_FORUM_ID, forumId);
		starter.putExtra(KEY_FORUM_TITLE, forumTitle);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		return starter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		threadId = intent.getIntExtra(KEY_THREAD_ID, BggContract.INVALID_ID);
		threadSubject = intent.getStringExtra(KEY_THREAD_SUBJECT);
		forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = intent.getStringExtra(KEY_FORUM_TITLE);
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(KEY_GAME_NAME);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (TextUtils.isEmpty(gameName)) {
				actionBar.setTitle(forumTitle);
				actionBar.setSubtitle(threadSubject);
			} else {
				actionBar.setTitle(threadSubject + " - " + forumTitle);
				actionBar.setSubtitle(gameName);
			}
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Thread")
				.putContentName(threadSubject)
				.putContentId(String.valueOf(threadId)));
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return ThreadFragment.newInstance(threadId, forumId, forumTitle, gameId, gameName);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ForumActivity.startUp(this, forumId, forumTitle, gameId, gameName);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "thread", threadId);
				return true;
			case R.id.menu_share:
				String description = TextUtils.isEmpty(gameName) ?
					String.format(getString(R.string.share_thread_text), threadSubject, forumTitle) :
					String.format(getString(R.string.share_thread_game_text), threadSubject, forumTitle, gameName);
				String link = ActivityUtils.createBggUri("thread", threadId).toString();
				ActivityUtils.share(this, getString(R.string.share_thread_subject), description + "\n\n" + link, R.string.title_share);
				String contentName = TextUtils.isEmpty(gameName) ?
					String.format("%s | %s", forumTitle, threadSubject) :
					String.format("%s | %s | %s", gameName, forumTitle, threadSubject);
				Answers.getInstance().logShare(new ShareEvent()
					.putContentType("Thread")
					.putContentName(contentName)
					.putContentId(String.valueOf(threadId)));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
