package com.boardgamegeek.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BoardgameActivity extends TabActivity implements AsyncQueryListener {
	// private final static String TAG = "BoardgameActivity";

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;

	private int mId;
	private String mName;

	private TextView mNameView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_boardgame);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		final Intent intent = getIntent();
		mBoardgameUri = intent.getData();

		setUiVariables();
		setupMainTab();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBoardgameUri, BoardgameQuery.PROJECTION);
	}

	private void setUiVariables() {
		mNameView = (TextView) findViewById(R.id.game_name);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst())
			{
				return;
			}
			
			mId = cursor.getInt(BoardgameQuery.GAME_ID);
			mName = cursor.getString(BoardgameQuery.GAME_NAME);

			mNameView.setText(mName);
						
			// final String url = cursor.getString(BuddiesQuery.AVATAR_URL);
			// new BuddyAvatarTask().execute(url);

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

	private void setupMainTab() {
		final TabHost host = getTabHost();
		
		final Intent intent = new Intent(this, GameInfoActivityTab.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(mBoardgameUri);
		intent.addCategory(Intent.CATEGORY_TAB);

		host.addTab(host.newTabSpec("main")
			.setIndicator(buildIndicator(R.string.main_tab_title))
			.setContent(intent));
	}
	
	private View buildIndicator(int textRes) {
		final TextView indicator = (TextView) getLayoutInflater().inflate(R.layout.tab_indicator,
				getTabWidget(), false);
		indicator.setText(textRes);
		return indicator;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.boardgame, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// only allow logging a play once the game is populated
		MenuItem mi = menu.findItem(R.id.log_play);
		mi.setEnabled(!TextUtils.isEmpty(mName));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.log_play:
				logPlay(false);
				return true;
			case R.id.log_play_quick:
				logPlay(true);
				return true;
		}
		return false;
	}

	private void logPlay(boolean quick) {
		Intent intent = new Intent(this, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
		intent.putExtra("GAME_ID", mId);
		intent.putExtra("GAME_NAME", mName);
		startActivity(intent);
	}

	private interface BoardgameQuery {
		String[] PROJECTION = {
			Games._ID,
			Games.GAME_NAME,
			Games.GAME_ID,
		};

		// int ID = 0;
		int GAME_NAME = 1;
		int GAME_ID = 2;
	}
}
