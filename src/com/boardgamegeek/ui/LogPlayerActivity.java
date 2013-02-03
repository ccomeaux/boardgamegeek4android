package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

public class LogPlayerActivity extends SherlockFragmentActivity implements OnItemClickListener {
	private static final String KEY_PLAYER = "PLAYER";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_TEAM_COLOR_SHOWN = "TEAM_COLOR_SHOWN";
	private static final String KEY_POSITION_SHOWN = "POSITION_SHOWN";
	private static final String KEY_SCORE_SHOWN = "SCORE_SHOWN";
	private static final String KEY_RATING_SHOWN = "RATING_SHOWN";
	private static final String KEY_NEW_SHOWN = "NEW_SHOWN";
	private static final String KEY_WIN_SHOWN = "WIN_SHOWN";

	private int mGameId;
	private String mGameName;

	private UsernameAdapter mUsernameAdapter;
	private ColorAdapter mColorAdapter;
	private Player mPlayer;
	private Player mOriginalPlayer;

	private EditText mName;
	private AutoCompleteTextView mUsername;
	private AutoCompleteTextView mTeamColor;
	private EditText mStartingPosition;
	private EditText mScore;
	private EditText mRating;
	private CheckBox mNew;
	private CheckBox mWin;

	private boolean mTeamColorShown;
	private boolean mPositionShown;
	private boolean mScoreShown;
	private boolean mRatingShown;
	private boolean mNewShown;
	private boolean mWinShown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		setContentView(R.layout.activity_logplayer);
		setUiVariables();

		final Intent intent = getIntent();
		mGameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(KEY_GAME_NAME);
		if (!TextUtils.isEmpty(mGameName)) {
			getSupportActionBar().setSubtitle(mGameName);
		}

		if (savedInstanceState == null) {
			mPlayer = new Player(intent);
			mOriginalPlayer = new Player(mPlayer);
		} else {
			mTeamColorShown = savedInstanceState.getBoolean(KEY_TEAM_COLOR_SHOWN);
			mPositionShown = savedInstanceState.getBoolean(KEY_POSITION_SHOWN);
			mScoreShown = savedInstanceState.getBoolean(KEY_SCORE_SHOWN);
			mRatingShown = savedInstanceState.getBoolean(KEY_RATING_SHOWN);
			mNewShown = savedInstanceState.getBoolean(KEY_NEW_SHOWN);
			mWinShown = savedInstanceState.getBoolean(KEY_WIN_SHOWN);

			mPlayer = savedInstanceState.getParcelable(KEY_PLAYER);
		}

		bindUi();

		mUsernameAdapter = new UsernameAdapter(this);
		mUsername.setAdapter(mUsernameAdapter);

		mColorAdapter = new ColorAdapter(this);
		mTeamColor.setAdapter(mColorAdapter);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_PLAYER, mPlayer);
		outState.putBoolean(KEY_TEAM_COLOR_SHOWN, mTeamColorShown);
		outState.putBoolean(KEY_POSITION_SHOWN, mPositionShown);
		outState.putBoolean(KEY_SCORE_SHOWN, mScoreShown);
		outState.putBoolean(KEY_RATING_SHOWN, mRatingShown);
		outState.putBoolean(KEY_NEW_SHOWN, mNewShown);
		outState.putBoolean(KEY_WIN_SHOWN, mWinShown);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.logplayer, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		hideAddFieldMenuItem(menu.findItem(R.id.menu_add_field));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onBackPressed() {
		save();
	}

	private void setUiVariables() {
		mUsername = (AutoCompleteTextView) findViewById(R.id.log_player_username);
		mName = (EditText) findViewById(R.id.log_player_name);
		mTeamColor = (AutoCompleteTextView) findViewById(R.id.log_player_team_color);
		mStartingPosition = (EditText) findViewById(R.id.log_player_position);
		mScore = (EditText) findViewById(R.id.log_player_score);
		mRating = (EditText) findViewById(R.id.log_player_rating);
		mNew = (CheckBox) findViewById(R.id.log_player_new);
		mWin = (CheckBox) findViewById(R.id.log_player_win);

		mUsername.setOnItemClickListener(this);
	}

	private void bindUi() {
		mName.setText(mPlayer.Name);
		mUsername.setText(mPlayer.Username);
		mTeamColor.setText(mPlayer.TeamColor);
		mStartingPosition.setText(mPlayer.StartingPosition);
		mScore.setText(mPlayer.Score);
		mRating.setText(String.valueOf(mPlayer.Rating));
		mNew.setChecked(mPlayer.New);
		mWin.setChecked(mPlayer.Win);
		hideFields();
	}

	private void hideFields() {
		findViewById(R.id.log_player_team_color_label).setVisibility(hideTeamColor() ? View.GONE : View.VISIBLE);
		mTeamColor.setVisibility(hideTeamColor() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_player_position_label).setVisibility(hidePosition() ? View.GONE : View.VISIBLE);
		mStartingPosition.setVisibility(hidePosition() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_player_score_label).setVisibility(hideScore() ? View.GONE : View.VISIBLE);
		mScore.setVisibility(hideScore() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_player_rating_label).setVisibility(hideRating() ? View.GONE : View.VISIBLE);
		mRating.setVisibility(hideRating() ? View.GONE : View.VISIBLE);
		mNew.setVisibility(hideNew() ? View.GONE : View.VISIBLE);
		mWin.setVisibility(hideWin() ? View.GONE : View.VISIBLE);
	}

	public void hideAddFieldMenuItem(MenuItem mi) {
		mi.setVisible(hideTeamColor() || hidePosition() || hideScore() || hideRating() || hideNew() || hideWin());
	}

	private boolean hideTeamColor() {
		return !PreferencesUtils.showLogPlayerTeamColor(this) && !mTeamColorShown
			&& TextUtils.isEmpty(mPlayer.TeamColor);
	}

	private boolean hidePosition() {
		return !PreferencesUtils.showLogPlayerPosition(this) && !mPositionShown
			&& TextUtils.isEmpty(mPlayer.StartingPosition);
	}

	private boolean hideScore() {
		return !PreferencesUtils.showLogPlayerScore(this) && !mScoreShown && TextUtils.isEmpty(mPlayer.Score);
	}

	private boolean hideRating() {
		return !PreferencesUtils.showLogPlayerRating(this) && !mRatingShown && !(mPlayer.Rating > 0);
	}

	private boolean hideNew() {
		return !PreferencesUtils.showLogPlayerNew(this) && !mNewShown && !mPlayer.New;
	}

	private boolean hideWin() {
		return !PreferencesUtils.showLogPlayerWin(this) && !mWinShown && !mPlayer.Win;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_save:
				save();
				return true;
			case R.id.menu_cancel:
				cancel();
				return true;
			case R.id.menu_add_field:
				final CharSequence[] array = createAddFieldArray();
				if (array == null || array.length == 0) {
					return false;
				}
				final MenuItem mi = item;
				new AlertDialog.Builder(this).setTitle(R.string.add_field)
					.setItems(array, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Resources r = getResources();

							String selection = array[which].toString();
							if (selection == r.getString(R.string.team_color)) {
								mTeamColorShown = true;
								findViewById(R.id.log_player_team_color_label).setVisibility(View.VISIBLE);
								mTeamColor.setVisibility(View.VISIBLE);
							} else if (selection == r.getString(R.string.starting_position)) {
								mPositionShown = true;
								findViewById(R.id.log_player_position_label).setVisibility(View.VISIBLE);
								mStartingPosition.setVisibility(View.VISIBLE);
							} else if (selection == r.getString(R.string.score)) {
								mScoreShown = true;
								findViewById(R.id.log_player_score_label).setVisibility(View.VISIBLE);
								mScore.setVisibility(View.VISIBLE);
							} else if (selection == r.getString(R.string.rating)) {
								mRatingShown = true;
								findViewById(R.id.log_player_rating_label).setVisibility(View.VISIBLE);
								mRating.setVisibility(View.VISIBLE);
							} else if (selection == r.getString(R.string.new_label)) {
								mNewShown = true;
								mNew.setVisibility(View.VISIBLE);
								mNew.setChecked(true);
							} else if (selection == r.getString(R.string.win)) {
								mWinShown = true;
								mWin.setVisibility(View.VISIBLE);
								mWin.setChecked(true);
							}
							hideAddFieldMenuItem(mi);
						}
					}).show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<CharSequence>();

		if (hideTeamColor()) {
			list.add(r.getString(R.string.team_color));
		}
		if (hidePosition()) {
			list.add(r.getString(R.string.starting_position));
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

	private void save() {
		captureForm();
		setResult(RESULT_OK, mPlayer.toIntent());
		finish();
	}

	private void cancel() {
		captureForm();
		if (mPlayer.equals(mOriginalPlayer)) {
			setResult(RESULT_CANCELED);
			finish();
		} else {
			ActivityUtils.createCancelDialog(this).show();
		}
	}

	private void captureForm() {
		mPlayer.Name = mName.getText().toString().trim();
		mPlayer.Username = mUsername.getText().toString().trim();
		mPlayer.TeamColor = mTeamColor.getText().toString().trim();
		mPlayer.StartingPosition = mStartingPosition.getText().toString().trim();
		mPlayer.Score = mScore.getText().toString().trim();
		mPlayer.Rating = StringUtils.parseDouble(mRating.getText().toString().trim());
		mPlayer.New = mNew.isChecked();
		mPlayer.Win = mWin.isChecked();
	}

	private class UsernameAdapter extends CursorAdapter {
		public UsernameAdapter(Context context) {
			super(context, null, false);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(R.layout.dropdown_logplayer_buddy, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			String userName = cursor.getString(BuddiesQuery.NAME);
			String firstName = cursor.getString(BuddiesQuery.FIRST_NAME);
			String lastName = cursor.getString(BuddiesQuery.LAST_NAME);
			String nickname = cursor.getString(BuddiesQuery.PLAY_NICKNAME);
			String fullName = (firstName + " " + lastName).trim();

			((TextView) view.findViewById(R.id.buddy_username)).setText(userName);
			((TextView) view.findViewById(R.id.buddy_description)).setText(fullName);
			((TextView) view.findViewById(R.id.buddy_nickname)).setText(nickname);
			view.setTag(TextUtils.isEmpty(nickname) ? firstName : nickname);
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(BuddiesQuery.NAME);
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String selection = null;
			String[] selectionArgs = null;
			if (!TextUtils.isEmpty(constraint)) {
				selection = Buddies.BUDDY_NAME + " LIKE ? OR " + Buddies.BUDDY_FIRSTNAME + " LIKE ? OR "
					+ Buddies.BUDDY_LASTNAME + " LIKE ? OR " + Buddies.PLAY_NICKNAME + " LIKE ?";
				String selectionArg = constraint + "%";
				selectionArgs = new String[] { selectionArg, selectionArg, selectionArg, selectionArg };
			}
			return getContentResolver().query(Buddies.CONTENT_URI, BuddiesQuery.PROJECTION, selection, selectionArgs,
				Buddies.NAME_SORT);
		}
	}

	private class ColorAdapter extends CursorAdapter {
		public ColorAdapter(Context context) {
			super(context, null, false);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(R.layout.autocomplete_item, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView textView = (TextView) view.findViewById(R.id.autocomplete_item);
			textView.setText(cursor.getString(ColorsQuery.COLOR));
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(ColorsQuery.COLOR);
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String selection = null;
			String[] selectionArgs = null;
			if (!TextUtils.isEmpty(constraint)) {
				selection = GameColors.COLOR + " LIKE ?";
				selectionArgs = new String[] { constraint + "%" };
			}
			return getContentResolver().query(Games.buildColorsUri(mGameId), ColorsQuery.PROJECTION, selection,
				selectionArgs, null);
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
			Buddies.PLAY_NICKNAME };
		int NAME = 1;
		int FIRST_NAME = 2;
		int LAST_NAME = 3;
		int PLAY_NICKNAME = 4;
	}

	private interface ColorsQuery {
		String[] PROJECTION = { GameColors._ID, GameColors.COLOR };
		int COLOR = 1;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mName.setText((String) view.getTag());
	}
}
