package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayerActivity extends Activity {
	// private static final String TAG = "LogPlayerActivity";

	private static final String KEY_PLAYER = "PLAYER";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_TEAM_COLOR_SHOWN = "TEAM_COLOR_SHOWN";
	private static final String KEY_POSITION_SHOWN = "POSITION_SHOWN";
	private static final String KEY_SCORE_SHOWN = "SCORE_SHOWN";
	private static final String KEY_RATING_SHOWN = "RATING_SHOWN";
	private static final String KEY_NEW_SHOWN = "NEW_SHOWN";
	private static final String KEY_WIN_SHOWN = "WIN_SHOWN";

	private String mGameName;
	private String mThumbnailUrl;

	private UsernameAdapter mAdapter;
	private Player mPlayer;

	private EditText mName;
	private AutoCompleteTextView mUsername;
	private EditText mTeamColor;
	private EditText mStartingPosition;
	private EditText mScore;
	private EditText mRating;
	private CheckBox mNew;
	private CheckBox mWin;
	private AlertDialog mCancelDialog;

	private boolean mTeamColorShown;
	private boolean mPositionShown;
	private boolean mScoreShown;
	private boolean mRatingShown;
	private boolean mNewShown;
	private boolean mWinShown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplayer);
		setUiVariables();
		UIUtils.setTitle(this);
		mCancelDialog = UIUtils.createCancelDialog(this);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);

			mPlayer = new Player(intent);
			bindUi();
		} else {
			mPlayer = savedInstanceState.getParcelable(KEY_PLAYER);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mTeamColorShown = savedInstanceState.getBoolean(KEY_TEAM_COLOR_SHOWN);
			mPositionShown = savedInstanceState.getBoolean(KEY_POSITION_SHOWN);
			mScoreShown = savedInstanceState.getBoolean(KEY_SCORE_SHOWN);
			mRatingShown = savedInstanceState.getBoolean(KEY_RATING_SHOWN);
			mNewShown = savedInstanceState.getBoolean(KEY_NEW_SHOWN);
			mWinShown = savedInstanceState.getBoolean(KEY_WIN_SHOWN);
			bindUi();
		}

		hideFields();
		UIUtils u = new UIUtils(this);
		u.setGameName(mGameName);
		u.setThumbnail(mThumbnailUrl);

		mAdapter = new UsernameAdapter(this);
		mUsername.setAdapter(mAdapter);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_PLAYER, mPlayer);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putBoolean(KEY_TEAM_COLOR_SHOWN, mTeamColorShown);
		outState.putBoolean(KEY_POSITION_SHOWN, mPositionShown);
		outState.putBoolean(KEY_SCORE_SHOWN, mScoreShown);
		outState.putBoolean(KEY_RATING_SHOWN, mRatingShown);
		outState.putBoolean(KEY_NEW_SHOWN, mNewShown);
		outState.putBoolean(KEY_WIN_SHOWN, mWinShown);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.logplay, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.add_field);
		mi.setEnabled(hideTeamColor() || hidePosition() || hideScore() || hideRating() || hideNew() || hideWin());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			if (mUsername.hasFocus()) {
				// sends focus to the next field (user pressed "Next")
				mTeamColor.requestFocus();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private void setUiVariables() {
		mName = (EditText) findViewById(R.id.log_player_name);
		mUsername = (AutoCompleteTextView) findViewById(R.id.log_player_username);
		mTeamColor = (EditText) findViewById(R.id.log_player_team_color);
		mStartingPosition = (EditText) findViewById(R.id.log_player_position);
		mScore = (EditText) findViewById(R.id.log_player_score);
		mRating = (EditText) findViewById(R.id.log_player_rating);
		mNew = (CheckBox) findViewById(R.id.log_player_new);
		mWin = (CheckBox) findViewById(R.id.log_player_win);
	}

	private void hideFields() {
		if (hideTeamColor()) {
			findViewById(R.id.log_player_team_color_row).setVisibility(View.GONE);
		}
		if (hidePosition()) {
			findViewById(R.id.log_player_position_row).setVisibility(View.GONE);
		}
		if (hideScore()) {
			findViewById(R.id.log_player_score_row).setVisibility(View.GONE);
		}
		if (hideRating()) {
			findViewById(R.id.log_player_rating_row).setVisibility(View.GONE);
		}
		if (hideNew()) {
			mNew.setVisibility(View.GONE);
		}
		if (hideWin()) {
			mWin.setVisibility(View.GONE);
		}
	}

	private boolean hideTeamColor() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerTeamColor() && !mTeamColorShown;
	}

	private boolean hidePosition() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerPosition() && !mPositionShown;
	}

	private boolean hideScore() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerScore() && !mScoreShown;
	}

	private boolean hideRating() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerRating() && !mRatingShown;
	}

	private boolean hideNew() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerNew() && !mNewShown;
	}

	private boolean hideWin() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerWin() && !mWinShown;
	}

	private void bindUi() {
		mName.setText(mPlayer.Name);
		mUsername.setText(mPlayer.Username);
		mTeamColor.setText(mPlayer.TeamColor);
		mStartingPosition.setText(mPlayer.StartingPosition);
		mScore.setText(String.valueOf(mPlayer.Score));
		mRating.setText(String.valueOf(mPlayer.Rating));
		mNew.setChecked(mPlayer.New);
		mWin.setChecked(mPlayer.Win);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.save:
				save();
				return true;
			case R.id.cancel:
				cancel();
				return true;
			case R.id.add_field:
				final CharSequence[] array = createAddFieldArray();
				if (array == null || array.length == 0) {
					return false;
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.add_field);
				builder.setItems(array, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Resources r = getResources();

						String selection = array[which].toString();
						if (selection == r.getString(R.string.team_color)) {
							mTeamColorShown = true;
							findViewById(R.id.log_player_team_color_row).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.position)) {
							mPositionShown = true;
							findViewById(R.id.log_player_position_row).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.score)) {
							mScoreShown = true;
							findViewById(R.id.log_player_score_row).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.rating)) {
							mRatingShown = true;
							findViewById(R.id.log_player_rating_row).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.new_label)) {
							mNewShown = true;
							mNew.setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.win)) {
							mWinShown = true;
							mWin.setVisibility(View.VISIBLE);
						}
					}
				});
				builder.show();
				return true;
		}
		return false;
	}

	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<CharSequence>();

		if (hideTeamColor()) {
			list.add(r.getString(R.string.team_color));
		}
		if (hidePosition()) {
			list.add(r.getString(R.string.position));
		}
		if (hideScore()) {
			list.add(r.getString(R.string.score));
		}
		if (hideRating()) {
			list.add(r.getString(R.string.rating));
		}
		if (hideNew()) {
			list.add(r.getString(R.string.new_label));
		}
		if (hideWin()) {
			list.add(r.getString(R.string.win));
		}

		CharSequence[] csa = {};
		csa = list.toArray(csa);
		return csa;
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
		cancel();
	}

	private void save() {
		captureForm();
		setResult(RESULT_OK, mPlayer.toIntent());
		finish();
	}

	private void cancel() {
		mCancelDialog.show();
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

	private class UsernameAdapter extends CursorAdapter {
		public UsernameAdapter(Context context) {
			super(context, null);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(R.layout.list_item, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView nameTextView = (TextView) view.findViewById(R.id.buddy_name);
			nameTextView.setText(cursor.getString(BuddiesQuery.NAME));
			final TextView descriptionTextView = (TextView) view.findViewById(R.id.buddy_description);
			descriptionTextView.setText((cursor.getString(BuddiesQuery.FIRST_NAME) + " " + cursor
					.getString(BuddiesQuery.LAST_NAME)).trim());
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(BuddiesQuery.NAME);
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String selection = null;
			if (!TextUtils.isEmpty(constraint)) {
				selection = Buddies.BUDDY_NAME + " LIKE '" + constraint + "%'";
			}
			return getContentResolver().query(Buddies.CONTENT_URI, BuddiesQuery.PROJECTION, selection, null,
					Buddies.NAME_SORT);
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME };
		// int _ID = 0;
		int NAME = 1;
		int FIRST_NAME = 2;
		int LAST_NAME = 3;
	}
}
