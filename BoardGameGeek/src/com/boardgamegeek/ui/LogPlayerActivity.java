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
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AutoCompleteAdapter;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayerActivity extends SherlockFragmentActivity implements OnItemClickListener {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_CANCEL_ON_BACK = "CANCEL_ON_BACK";
	public static final String KEY_AUTO_POSITION = "AUTO_POSITION";
	public static final String KEY_END_PLAY = "SCORE_SHOWN";
	public static final String KEY_PLAYER = "PLAYER";
	private static final String KEY_TEAM_COLOR_SHOWN = "TEAM_COLOR_SHOWN";
	private static final String KEY_POSITION_SHOWN = "POSITION_SHOWN";
	private static final String KEY_SCORE_SHOWN = "SCORE_SHOWN";
	private static final String KEY_RATING_SHOWN = "RATING_SHOWN";
	private static final String KEY_NEW_SHOWN = "NEW_SHOWN";
	private static final String KEY_WIN_SHOWN = "WIN_SHOWN";

	private static final int HELP_VERSION = 1;

	private int mGameId;
	private String mGameName;

	private UsernameAdapter mUsernameAdapter;
	private Player mPlayer;
	private Player mOriginalPlayer;

	private AutoCompleteTextView mUsername;
	private AutoCompleteTextView mName;
	private AutoCompleteTextView mTeamColor;
	private EditText mPosition;
	private Button mPositionButton;
	private EditText mScore;
	private Button mScoreButton;
	private EditText mRating;
	private CheckBox mNew;
	private CheckBox mWin;

	private boolean mTeamColorShown;
	private boolean mPositionShown;
	private boolean mScoreShown;
	private boolean mRatingShown;
	private boolean mNewShown;
	private boolean mWinShown;
	private boolean mCancelOnBack;
	private int mAutoPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		setContentView(R.layout.activity_logplayer);
		setUiVariables();

		final Intent intent = getIntent();
		mGameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(KEY_GAME_NAME);
		mCancelOnBack = intent.getBooleanExtra(KEY_CANCEL_ON_BACK, false);
		mAutoPosition = intent.getIntExtra(KEY_AUTO_POSITION, Player.SEAT_UNKNOWN);
		if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
			mScoreShown = true;
			mScore.requestFocus();
		}

		if (hasAutoPosition()) {
			setTitle(getTitle() + " #" + mAutoPosition);
		}

		if (!TextUtils.isEmpty(mGameName)) {
			getSupportActionBar().setSubtitle(mGameName);
		}

		if (savedInstanceState == null) {
			mPlayer = intent.getParcelableExtra(KEY_PLAYER);
			if (mPlayer == null) {
				mPlayer = new Player();
			}
			if (hasAutoPosition()) {
				mPlayer.setSeat(mAutoPosition);
			}
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
		mName.setAdapter(new AutoCompleteAdapter(this, PlayPlayers.NAME, Plays.buildPlayersByNameWithoutUsernameUri(),
			PlayPlayers.NAME));
		mTeamColor.setAdapter(new AutoCompleteAdapter(this, GameColors.COLOR, Games.buildColorsUri(mGameId)));

		UIUtils.showHelpDialog(this, HelpUtils.HELP_LOGPLAYER_KEY, HELP_VERSION, R.string.help_logplayer);
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
		if (mCancelOnBack) {
			cancel();
		} else {
			save();
		}
	}

	private void setUiVariables() {
		mUsername = (AutoCompleteTextView) findViewById(R.id.log_player_username);
		mName = (AutoCompleteTextView) findViewById(R.id.log_player_name);
		mTeamColor = (AutoCompleteTextView) findViewById(R.id.log_player_team_color);
		mPosition = (EditText) findViewById(R.id.log_player_position);
		mPositionButton = (Button) findViewById(R.id.log_player_position_button);
		mScore = (EditText) findViewById(R.id.log_player_score);
		mScoreButton = (Button) findViewById(R.id.log_player_score_button);
		mRating = (EditText) findViewById(R.id.log_player_rating);
		mNew = (CheckBox) findViewById(R.id.log_player_new);
		mWin = (CheckBox) findViewById(R.id.log_player_win);

		mPositionButton.setOnClickListener(numberToTextClick());
		mScoreButton.setOnClickListener(numberToTextClick());
		mUsername.setOnItemClickListener(this);
	}

	private OnClickListener numberToTextClick() {
		return new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText editText = null;
				if (v == mPositionButton) {
					editText = mPosition;
				} else if (v == mScoreButton) {
					editText = mScore;
				}
				if (editText == null) {
					return;
				}
				int type = editText.getInputType();
				if ((type & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
					editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS
						| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				} else {
					editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
						| InputType.TYPE_NUMBER_FLAG_SIGNED);
				}
				editText.requestFocus();
			}
		};
	}

	private void bindUi() {
		mName.setText(mPlayer.name);
		mUsername.setText(mPlayer.username);
		mTeamColor.setText(mPlayer.color);
		mPosition.setText(mPlayer.getStartingPosition());
		mScore.setText(mPlayer.score);
		mRating.setText((mPlayer.rating == Player.DEFAULT_RATING) ? "" : String.valueOf(mPlayer.rating));
		mNew.setChecked(mPlayer.New());
		mWin.setChecked(mPlayer.Win());
		hideFields();
	}

	private void hideFields() {
		findViewById(R.id.log_player_team_color_label).setVisibility(shouldHideTeamColor() ? View.GONE : View.VISIBLE);
		mTeamColor.setVisibility(shouldHideTeamColor() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_player_position_container).setVisibility(
			hasAutoPosition() || shouldHidePosition() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_player_score_container).setVisibility(shouldHideScore() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_player_rating_label).setVisibility(shouldHideRating() ? View.GONE : View.VISIBLE);
		mRating.setVisibility(shouldHideRating() ? View.GONE : View.VISIBLE);
		mNew.setVisibility(shouldHideNew() ? View.GONE : View.VISIBLE);
		mWin.setVisibility(shouldHideWin() ? View.GONE : View.VISIBLE);
	}

	public void hideAddFieldMenuItem(MenuItem mi) {
		mi.setVisible(shouldHideTeamColor() || (shouldHidePosition() && !hasAutoPosition()) || shouldHideScore()
			|| shouldHideRating() || shouldHideNew() || shouldHideWin());
	}

	private boolean shouldHideTeamColor() {
		return !PreferencesUtils.showLogPlayerTeamColor(this) && !mTeamColorShown && TextUtils.isEmpty(mPlayer.color);
	}

	private boolean shouldHidePosition() {
		return !PreferencesUtils.showLogPlayerPosition(this) && !mPositionShown
			&& TextUtils.isEmpty(mPlayer.getStartingPosition());
	}

	private boolean hasAutoPosition() {
		return mAutoPosition != Player.SEAT_UNKNOWN;
	}

	private boolean shouldHideScore() {
		return !PreferencesUtils.showLogPlayerScore(this) && !mScoreShown && TextUtils.isEmpty(mPlayer.score);
	}

	private boolean shouldHideRating() {
		return !PreferencesUtils.showLogPlayerRating(this) && !mRatingShown && !(mPlayer.rating > 0);
	}

	private boolean shouldHideNew() {
		return !PreferencesUtils.showLogPlayerNew(this) && !mNewShown && !mPlayer.New();
	}

	private boolean shouldHideWin() {
		return !PreferencesUtils.showLogPlayerWin(this) && !mWinShown && !mPlayer.Win();
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
							View viewToFocus = null;

							String selection = array[which].toString();
							if (selection == r.getString(R.string.team_color)) {
								mTeamColorShown = true;
								viewToFocus = mTeamColor;
							} else if (selection == r.getString(R.string.starting_position)) {
								mPositionShown = true;
								viewToFocus = mPosition;
							} else if (selection == r.getString(R.string.score)) {
								mScoreShown = true;
								viewToFocus = mScore;
							} else if (selection == r.getString(R.string.rating)) {
								mRatingShown = true;
								viewToFocus = mRating;
							} else if (selection == r.getString(R.string.new_label)) {
								mNewShown = true;
								mNew.setChecked(true);
							} else if (selection == r.getString(R.string.win)) {
								mWinShown = true;
								mWin.setChecked(true);
							}
							hideAddFieldMenuItem(mi);
							hideFields();
							if (viewToFocus != null) {
								viewToFocus.requestFocus();
							}
						}
					}).show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<CharSequence>();

		if (shouldHideTeamColor()) {
			list.add(r.getString(R.string.team_color));
		}
		if (!hasAutoPosition() && shouldHidePosition()) {
			list.add(r.getString(R.string.starting_position));
		}
		if (shouldHideScore()) {
			list.add(r.getString(R.string.score));
		}
		if (shouldHideRating()) {
			list.add(r.getString(R.string.rating));
		}
		if (shouldHideNew()) {
			list.add(r.getString(R.string.new_label));
		}
		if (shouldHideWin()) {
			list.add(r.getString(R.string.win));
		}

		CharSequence[] csa = {};
		csa = list.toArray(csa);
		return csa;
	}

	private void save() {
		captureForm();
		Intent intent = new Intent();
		intent.putExtra(KEY_PLAYER, mPlayer);
		setResult(RESULT_OK, intent);
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
		mPlayer.name = mName.getText().toString().trim();
		mPlayer.username = mUsername.getText().toString().trim();
		mPlayer.color = mTeamColor.getText().toString().trim();
		mPlayer.setStartingPosition(mPosition.getText().toString().trim());
		mPlayer.score = mScore.getText().toString().trim();
		mPlayer.rating = StringUtils.parseDouble(mRating.getText().toString().trim());
		mPlayer.New(mNew.isChecked());
		mPlayer.Win(mWin.isChecked());
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

	private interface BuddiesQuery {
		String[] PROJECTION = { Buddies._ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
			Buddies.PLAY_NICKNAME };
		int NAME = 1;
		int FIRST_NAME = 2;
		int LAST_NAME = 3;
		int PLAY_NICKNAME = 4;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mName.setText((String) view.getTag());
	}
}
