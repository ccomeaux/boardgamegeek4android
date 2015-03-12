package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.dialog.ColorPickerDialogFragment;
import com.boardgamegeek.ui.adapter.BuddyNameAdapter;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.adapter.PlayerNameAdapter;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import hugo.weaving.DebugLog;

public class LogPlayerActivity extends ActionBarActivity {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_AUTO_POSITION = "AUTO_POSITION";
	public static final String KEY_USED_COLORS = "USED_COLORS";
	public static final String KEY_END_PLAY = "SCORE_SHOWN";
	public static final String KEY_PLAYER = "PLAYER";
	private static final String KEY_TEAM_COLOR_SHOWN = "TEAM_COLOR_SHOWN";
	private static final String KEY_POSITION_SHOWN = "POSITION_SHOWN";
	private static final String KEY_SCORE_SHOWN = "SCORE_SHOWN";
	private static final String KEY_RATING_SHOWN = "RATING_SHOWN";
	private static final String KEY_NEW_SHOWN = "NEW_SHOWN";
	private static final String KEY_WIN_SHOWN = "WIN_SHOWN";

	private static final int HELP_VERSION = 1;
	private static final int TOKEN_COLORS = 1;

	private String mGameName;

	private Player mPlayer;
	private Player mOriginalPlayer;

	@InjectView(R.id.scroll_container) ObservableScrollView mScrollContainer;
	@InjectView(R.id.header) TextView mHeader;
	@InjectView(R.id.two_line_container) View mTwoLineContainer;
	@InjectView(R.id.header2) TextView mHeader2;
	@InjectView(R.id.subheader) TextView mSubheader;
	@InjectView(R.id.log_player_username) AutoCompleteTextView mUsername;
	@InjectView(R.id.log_player_name) AutoCompleteTextView mName;
	@InjectView(R.id.log_player_team_color) AutoCompleteTextView mTeamColor;
	@InjectView(R.id.color_view) ImageView mColorView;
	@InjectView(R.id.log_player_position) EditText mPosition;
	@InjectView(R.id.log_player_position_button) Button mPositionButton;
	@InjectView(R.id.log_player_score) EditText mScore;
	@InjectView(R.id.log_player_score_button) Button mScoreButton;
	@InjectView(R.id.log_player_rating) EditText mRating;
	@InjectView(R.id.log_player_new) SwitchCompat mNew;
	@InjectView(R.id.log_player_win) SwitchCompat mWin;
	@InjectView(R.id.fab) FloatingActionButton mFab;
	@InjectView(R.id.fab_buffer) View mFabBuffer;

	private boolean mPrefShowTeamColor;
	private boolean mPrefShowPosition;
	private boolean mPrefShowScore;
	private boolean mPrefShowRating;
	private boolean mPrefShowNew;
	private boolean mPrefShowWin;
	private boolean mUserShowTeamColor;
	private boolean mUserShowPosition;
	private boolean mUserShowScore;
	private boolean mUserShowRating;
	private boolean mUserShowNew;
	private boolean mUserShowWin;
	private int mAutoPosition;
	private ArrayList<String> mUsedColors;
	private ArrayList<String> mColors;

	private final View.OnClickListener mActionBarListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			onActionBarItemSelected(v.getId());
		}
	};

	@SuppressLint("HandlerLeak")
	private class QueryHandler extends AsyncQueryHandler {
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@DebugLog
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			if (cursor == null) {
				return;
			}
			if (isFinishing()) {
				cursor.close();
				return;
			}

			switch (token) {
				case TOKEN_COLORS:
					if (cursor.getCount() == 0) {
						cursor.close();
						return;
					}
					try {
						if (cursor.moveToFirst()) {
							mColors = new ArrayList<>();
							do {
								mColors.add(cursor.getString(0));
							} while (cursor.moveToNext());
						}
					} finally {
						cursor.close();
					}
					break;
				default:
					cursor.close();
					break;
			}
		}
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_logplayer);
		ButterKnife.inject(this);
		mFab.attachToScrollView(mScrollContainer);
		mName.setOnItemClickListener(nameClickListener());

		ToolbarUtils.setDoneCancelActionBarView(this, mActionBarListener);

		final Intent intent = getIntent();
		int gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(KEY_GAME_NAME);
		String imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
		mAutoPosition = intent.getIntExtra(KEY_AUTO_POSITION, Player.SEAT_UNKNOWN);
		String[] usedColors = intent.getStringArrayExtra(KEY_USED_COLORS);
		if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
			mUserShowScore = true;
			mScore.requestFocus();
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
			mUserShowTeamColor = savedInstanceState.getBoolean(KEY_TEAM_COLOR_SHOWN);
			mUserShowPosition = savedInstanceState.getBoolean(KEY_POSITION_SHOWN);
			mUserShowScore = savedInstanceState.getBoolean(KEY_SCORE_SHOWN);
			mUserShowRating = savedInstanceState.getBoolean(KEY_RATING_SHOWN);
			mUserShowNew = savedInstanceState.getBoolean(KEY_NEW_SHOWN);
			mUserShowWin = savedInstanceState.getBoolean(KEY_WIN_SHOWN);

			mPlayer = savedInstanceState.getParcelable(KEY_PLAYER);
		}

		mUsedColors = new ArrayList<>(Arrays.asList(usedColors));
		mUsedColors.remove(mPlayer.color);

		ImageUtils.safelyLoadImage((ImageView) findViewById(R.id.thumbnail), imageUrl);
		bindUi();

		new QueryHandler(getContentResolver()).startQuery(TOKEN_COLORS, null, Games.buildColorsUri(gameId),
			new String[] { GameColors.COLOR }, null, null, null);

		mName.setAdapter(new PlayerNameAdapter(this));
		mUsername.setAdapter(new BuddyNameAdapter(this));
		mTeamColor.setAdapter(new GameColorAdapter(this, gameId, R.layout.autocomplete_color));

		HelpUtils.showHelpDialog(this, HelpUtils.HELP_LOGPLAYER_KEY, HELP_VERSION, R.string.help_logplayer);
	}

	@DebugLog
	@Override
	protected void onResume() {
		super.onResume();
		mPrefShowTeamColor = PreferencesUtils.showLogPlayerTeamColor(this);
		mPrefShowPosition = PreferencesUtils.showLogPlayerPosition(this);
		mPrefShowScore = PreferencesUtils.showLogPlayerScore(this);
		mPrefShowRating = PreferencesUtils.showLogPlayerRating(this);
		mPrefShowNew = PreferencesUtils.showLogPlayerNew(this);
		mPrefShowWin = PreferencesUtils.showLogPlayerWin(this);
		setViewVisibility();
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_PLAYER, mPlayer);
		outState.putBoolean(KEY_TEAM_COLOR_SHOWN, mUserShowTeamColor);
		outState.putBoolean(KEY_POSITION_SHOWN, mUserShowPosition);
		outState.putBoolean(KEY_SCORE_SHOWN, mUserShowScore);
		outState.putBoolean(KEY_RATING_SHOWN, mUserShowRating);
		outState.putBoolean(KEY_NEW_SHOWN, mUserShowNew);
		outState.putBoolean(KEY_WIN_SHOWN, mUserShowWin);
	}

	@DebugLog
	@Override
	public void onBackPressed() {
		cancel();
	}

	@DebugLog
	private OnItemClickListener nameClickListener() {
		return new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mUsername.setText((String) view.getTag());
			}
		};
	}

	@DebugLog
	@OnClick(R.id.color_view)
	public void onColorClick(View v) {
		ColorPickerDialogFragment colordashfragment = ColorPickerDialogFragment.newInstance(0,
			ColorUtils.getColorList(), mColors, mTeamColor.getText().toString(), mUsedColors, 4);

		colordashfragment.setOnColorSelectedListener(new ColorPickerDialogFragment.OnColorSelectedListener() {
			@Override
			public void onColorSelected(String description, int color) {
				mTeamColor.setText(description);
			}

		});

		colordashfragment.show(getSupportFragmentManager(), "color_picker");
	}

	@DebugLog
	@OnTextChanged(R.id.log_player_team_color)
	public void afterTextChanged(Editable s) {
		int color = ColorUtils.parseColor(s.toString());
		ColorUtils.setColorViewValue(mColorView, color);
	}

	@DebugLog
	@OnClick({ R.id.log_player_position_button, R.id.log_player_score_button })
	public void onNumberToTextClick(View v) {
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
			((Button) v).setText(R.string.text_to_number);
		} else {
			editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
				| InputType.TYPE_NUMBER_FLAG_SIGNED);
			((Button) v).setText(R.string.number_to_text);
		}
		editText.requestFocus();
	}

	@DebugLog
	private void bindUi() {
		if (hasAutoPosition()) {
			mHeader2.setText(mGameName);
			mSubheader.setText(getString(R.string.title_player) + " #" + mAutoPosition);
			mHeader.setVisibility(View.GONE);
			mTwoLineContainer.setVisibility(View.VISIBLE);
		} else {
			mHeader.setText(mGameName);
			mHeader.setVisibility(View.VISIBLE);
			mTwoLineContainer.setVisibility(View.GONE);
		}
		mName.setTextKeepState(mPlayer.name);
		mUsername.setTextKeepState(mPlayer.username);
		mTeamColor.setTextKeepState(mPlayer.color);
		if (mPlayer.getStartingPosition() != null) {
			mPosition.setTextKeepState(mPlayer.getStartingPosition());
		}
		mScore.setTextKeepState(mPlayer.score);
		mRating.setTextKeepState((mPlayer.rating == Player.DEFAULT_RATING) ? "" : String.valueOf(mPlayer.rating));
		mNew.setChecked(mPlayer.New());
		mWin.setChecked(mPlayer.Win());
	}

	@DebugLog
	private void setViewVisibility() {
		boolean enableButton = hideRow(shouldHideTeamColor(), findViewById(R.id.log_player_team_color_container));
		enableButton |= hideRow(shouldHidePosition(), findViewById(R.id.log_player_position_container));
		if (hasAutoPosition()) {
			hideRow(true, findViewById(R.id.log_player_position_container));
		}
		enableButton |= hideRow(shouldHideScore(), findViewById(R.id.log_player_score_container));
		enableButton |= hideRow(shouldHideRating(), findViewById(R.id.log_player_rating_container));
		enableButton |= hideRow(shouldHideNew(), mNew);
		enableButton |= hideRow(shouldHideWin(), mWin);
		mFab.setVisibility(enableButton ? View.VISIBLE : View.GONE);
		mFabBuffer.setVisibility(enableButton ? View.VISIBLE : View.GONE);
	}

	@DebugLog
	private boolean hideRow(boolean shouldHide, View view) {
		if (shouldHide) {
			view.setVisibility(View.GONE);
			return true;
		}
		view.setVisibility(View.VISIBLE);
		return false;
	}

	@DebugLog
	private boolean shouldHideTeamColor() {
		return !mPrefShowTeamColor && !mUserShowTeamColor && TextUtils.isEmpty(mPlayer.color);
	}

	@DebugLog
	private boolean shouldHidePosition() {
		return !mPrefShowPosition && !mUserShowPosition && TextUtils.isEmpty(mPlayer.getStartingPosition());
	}

	@DebugLog
	private boolean hasAutoPosition() {
		return mAutoPosition != Player.SEAT_UNKNOWN;
	}

	@DebugLog
	private boolean shouldHideScore() {
		return !mPrefShowScore && !mUserShowScore && TextUtils.isEmpty(mPlayer.score);
	}

	@DebugLog
	private boolean shouldHideRating() {
		return !mPrefShowRating && !mUserShowRating && !(mPlayer.rating > 0);
	}

	@DebugLog
	private boolean shouldHideNew() {
		return !mPrefShowNew && !mUserShowNew && !mPlayer.New();
	}

	@DebugLog
	private boolean shouldHideWin() {
		return !mPrefShowWin && !mUserShowWin && !mPlayer.Win();
	}

	@DebugLog
	private boolean onActionBarItemSelected(int itemId) {
		switch (itemId) {
			case R.id.menu_done:
				save();
				return true;
			case R.id.menu_cancel:
				cancel();
				return true;
		}
		return false;
	}

	@DebugLog
	@OnClick(R.id.fab)
	public void addField(View v) {
		final CharSequence[] array = createAddFieldArray();
		if (array == null || array.length == 0) {
			return;
		}
		new AlertDialog.Builder(this).setTitle(R.string.add_field)
			.setItems(array, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Resources r = getResources();
					View viewToFocus = null;
					View viewToScroll = null;

					String selection = array[which].toString();
					if (selection.equals(r.getString(R.string.team_color))) {
						mUserShowTeamColor = true;
						viewToFocus = mTeamColor;
						viewToScroll = findViewById(R.id.log_player_team_color_container);
					} else if (selection.equals(r.getString(R.string.starting_position))) {
						mUserShowPosition = true;
						viewToFocus = mPosition;
						viewToScroll = findViewById(R.id.log_player_position_container);
					} else if (selection.equals(r.getString(R.string.score))) {
						mUserShowScore = true;
						viewToFocus = mScore;
						viewToScroll = findViewById(R.id.log_player_score_container);
					} else if (selection.equals(r.getString(R.string.rating))) {
						mUserShowRating = true;
						viewToFocus = mRating;
						viewToScroll = findViewById(R.id.log_player_rating);
					} else if (selection.equals(r.getString(R.string.new_label))) {
						mUserShowNew = true;
						mNew.setChecked(true);
						viewToScroll = mNew;
						viewToFocus = mNew;
					} else if (selection.equals(r.getString(R.string.win))) {
						mUserShowWin = true;
						mWin.setChecked(true);
						viewToScroll = mWin;
						viewToFocus = mWin;
					}
					setViewVisibility();
					if (viewToFocus != null) {
						viewToFocus.requestFocus();
					}
					if (viewToScroll != null) {
						final View finalView = viewToScroll;
						mScrollContainer.post(new Runnable() {
							@Override
							public void run() {
								mScrollContainer.smoothScrollTo(0, finalView.getBottom());
							}
						});
					}
				}
			}).show();
	}

	@DebugLog
	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<>();

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

		CharSequence[] csa = { };
		csa = list.toArray(csa);
		return csa;
	}

	@DebugLog
	private void save() {
		captureForm();
		Intent intent = new Intent();
		intent.putExtra(KEY_PLAYER, mPlayer);
		setResult(RESULT_OK, intent);
		finish();
	}

	@DebugLog
	private void cancel() {
		captureForm();
		if (mPlayer.equals(mOriginalPlayer)) {
			setResult(RESULT_CANCELED);
			finish();
		} else {
			DialogUtils.createCancelDialog(this).show();
		}
	}

	@DebugLog
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
}
