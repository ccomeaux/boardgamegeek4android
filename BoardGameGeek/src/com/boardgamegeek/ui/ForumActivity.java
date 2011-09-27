package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumActivity extends ListActivity {
	private final String TAG = "ForumActivity";

	public static final String KEY_FORUM_ID = "FORUM_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_FORUM_TITLE = "FORUM_NAME";
	public static final String KEY_NUM_THREADS = "NUM_THREADS";
	public static final String KEY_THREADS = "THREADS";
	
	public static final int PAGE_SIZE = 50;

	private static final String KEY_CURRENT_PAGE = "CURRENT_PAGE";
	private static final String KEY_PAGE_COUNT = "PAGE_COUNT";
	private static final String KEY_LAST_DISPLAYED_PAGE = "LAST_DISPLAYED_PAGE";
	private static final String KEY_THREAD_COUNT = "THREAD_COUNT";

	private ForumsUtils.ForumAdapter mAdapter;
	private int mCurrentPage;
	private int mPageCount;

	private int mLastDisplayedPage;
	private int mThreadCount;

	private List<ForumThread> mAllForumThreads = new ArrayList<ForumThread>();
	private List<ForumThread> mCurrentForumThreads = new ArrayList<ForumThread>();

	private String mForumId;
	private String mGameName;
	private String mThumbnailUrl;
	private String mForumName;

	private TextView mInfoView;
	
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
			mCurrentPage = 1;
			mLastDisplayedPage = 1;
			mPageCount = -1;
			mThreadCount = 0;
		} else {
			mForumId = savedInstanceState.getString(KEY_FORUM_ID);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mForumName = savedInstanceState.getString(KEY_FORUM_TITLE);
			mAllForumThreads = savedInstanceState.getParcelableArrayList(KEY_THREADS);
			mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE);
			mLastDisplayedPage = savedInstanceState.getInt(KEY_LAST_DISPLAYED_PAGE);
			mPageCount = savedInstanceState.getInt(KEY_PAGE_COUNT);
			mThreadCount = savedInstanceState.getInt(KEY_THREAD_COUNT);
		}
		
		mInfoView = (TextView) findViewById(R.id.threads_counter);

		UIUtils.setTitle(this, mForumName);
		if (TextUtils.isEmpty(mGameName)) {
			findViewById(R.id.game_thumbnail).setClickable(false);
			findViewById(R.id.forum_game_header).setVisibility(View.GONE);
			findViewById(R.id.forum_header_divider).setVisibility(View.GONE);
		} else {
			findViewById(R.id.forum_game_header).setVisibility(View.VISIBLE);
			findViewById(R.id.forum_header_divider).setVisibility(View.VISIBLE);
			UIUtils.setGameHeader(this, mGameName, mThumbnailUrl);
		}

		if (mAllForumThreads == null || mAllForumThreads.size() == 0) {
			ForumsUtils.ForumTask forumTask = new ForumsUtils.ForumTask(this, TAG);
			forumTask.execute();
		} else {
			updateDisplay();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_FORUM_ID, mForumId);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putString(KEY_FORUM_TITLE, mForumName);
		outState.putParcelableArrayList(KEY_THREADS, (ArrayList<? extends Parcelable>) mAllForumThreads);
		outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
		outState.putInt(KEY_LAST_DISPLAYED_PAGE, mLastDisplayedPage);
		outState.putInt(KEY_PAGE_COUNT, mPageCount);
		outState.putInt(KEY_THREAD_COUNT, mThreadCount);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	@Override
	protected void onListItemClick(ListView listView, View convertView, int position, long id) {
		ForumsUtils.ForumViewHolder holder = (ForumsUtils.ForumViewHolder) convertView.getTag();
		if (holder != null) {
			Intent forumsIntent = new Intent(this, ThreadActivity.class);
			forumsIntent.putExtra(ThreadActivity.KEY_THREAD_ID, holder.threadId);
			forumsIntent.putExtra(ThreadActivity.KEY_GAME_NAME, mGameName);
			forumsIntent.putExtra(ThreadActivity.KEY_THUMBNAIL_URL, mThumbnailUrl);
			forumsIntent.putExtra(ThreadActivity.KEY_THREAD_SUBJECT, holder.subject.getText());
			this.startActivity(forumsIntent);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.paging, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.menu_back);
		mi.setEnabled(mCurrentPage > 1);

		mi = menu.findItem(R.id.menu_forward);
		mi.setEnabled(mCurrentPage < mPageCount);

		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCurrentForumThreads.clear();
		switch (item.getItemId()) {
			case R.id.menu_back:
				mCurrentPage--;
				setInfoText();
				setCurrentForumsPage();
				return true;
			case R.id.menu_forward:
				mAdapter.notifyDataSetChanged();
				mCurrentPage++;
				setInfoText();
				if (mCurrentPage > mLastDisplayedPage) {
					mLastDisplayedPage++;
					ForumsUtils.ForumTask forumTask = new ForumsUtils.ForumTask(this, TAG);
					forumTask.execute();
				} else {
					setCurrentForumsPage();
				}
				return true;
		}
		return false;
	}
	
	public void updateDisplay() {
		setInfoText();
		setCurrentForumsPage();
	}
	
	public void setInfoText() {
		mInfoView.setVisibility(View.VISIBLE);
		mInfoView.setText(getPageStart() + " - " + getPageEnd() + " of " + mThreadCount + " threads");
	}
	
	public void setCurrentForumsPage() {
		for (int i = getPageStart(); i <= getPageEnd(); i++) {
			mCurrentForumThreads.add(mAllForumThreads.get(i - 1));
		}
		Log.i(TAG, "Displaying from " + getPageStart() + " to " + getPageEnd());
		if (mAdapter == null) {
			mAdapter = new ForumsUtils.ForumAdapter(this, mCurrentForumThreads);
			setListAdapter(mAdapter);
		} else {
			mAdapter.notifyDataSetChanged();
			setSelection(0);
		}
	}
	
	private int getPageEnd() {
		return Math.min(mCurrentPage * ForumActivity.PAGE_SIZE, mThreadCount);
	}

	private int getPageStart() {
		return (mCurrentPage - 1) * ForumActivity.PAGE_SIZE + 1;
	}
	
	public String getForumId() {
		return mForumId;
	}

	public int getCurrentPage() {
		return mCurrentPage;
	}
	
	public int getPageCount() {
		return mPageCount;
	}

	public void setmPageCount(int mPageCount) {
		this.mPageCount = mPageCount;
	}
	
	public int getThreadCount() {
		return mThreadCount;
	}

	public void setThreadCount(int mThreadCount) {
		this.mThreadCount = mThreadCount;
	}

	public void addMoreForums(List<ForumThread> results) {
		Log.i(TAG, "Adding more forums");
		mAllForumThreads.addAll(results);
	}
}
