package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayerActivity extends Activity {
	// private static final String TAG = "LogPlayerActivity";

	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private String mGameName;
	private String mThumbnailUrl;

	private Player mPlayer;

	private EditText mName;
	private EditText mUsername;
	private EditText mTeamColor;
	private EditText mStartingPosition;
	private EditText mScore;
	private EditText mRating;
	private CheckBox mNew;
	private CheckBox mWin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplayer);
		setUiVariables();
		UIUtils.setTitle(this);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);

			mPlayer = new Player();

			UIUtils u = new UIUtils(this);
			u.setGameName(mGameName);
			u.setThumbnail(mThumbnailUrl);
		}
	}

	private void setUiVariables() {
		mName = (EditText) findViewById(R.id.logPlayerName);
		mUsername = (EditText) findViewById(R.id.logPlayerUsername);
		mTeamColor = (EditText) findViewById(R.id.logPlayerTeamColor);
		mStartingPosition = (EditText) findViewById(R.id.logPlayerStartingPosition);
		mScore = (EditText) findViewById(R.id.logPlayerScore);
		mRating = (EditText) findViewById(R.id.logPlayerRating);
		mNew = (CheckBox) findViewById(R.id.logPlayerNew);
		mWin = (CheckBox) findViewById(R.id.logPlayerWin);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.save:
				save();
				return true;
			case R.id.cancel:
				finish();
				return true;
		}
		return false;
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	public void onSaveClick(View v) {
		save();
	}

	public void onCancelClick(View v) {
		// TODO: implement Are You Sure dialog
		setResult(RESULT_CANCELED);
		finish();
	}

	private void save() {
		captureForm();
		setResult(RESULT_OK, mPlayer.toIntent());
		finish();
	}

	private void captureForm() {
		mPlayer.Name = mName.getText().toString();
		mPlayer.Username = mUsername.getText().toString();
		mPlayer.TeamColor = mTeamColor.getText().toString();
		mPlayer.StartingPosition = mStartingPosition.getText().toString();
		mPlayer.Score = mScore.getText().toString();
		mPlayer.Rating = StringUtils.parseDouble(mRating.getText().toString());
		mPlayer.New = mNew.isChecked();
		mPlayer.Win = mWin.isChecked();
	}
}
