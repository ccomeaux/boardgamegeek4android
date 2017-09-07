package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.adapter.BuddyNameAdapter;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.adapter.PlayerNameAdapter;
import com.boardgamegeek.ui.dialog.ColorPickerDialogFragment;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ShowcaseViewWizard;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.boardgamegeek.util.fabric.AddFieldEvent;
import com.github.amlcurran.showcaseview.targets.Target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class LogPlayerActivity extends AppCompatActivity {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_AUTO_POSITION = "AUTO_POSITION";
	public static final String KEY_USED_COLORS = "USED_COLORS";
	public static final String KEY_END_PLAY = "SCORE_SHOWN";
	public static final String KEY_PLAYER = "PLAYER";
	public static final String KEY_FAB_COLOR = "FAB_COLOR";
	public static final String KEY_POSITION = "POSITION";
	public static final String KEY_NEW_PLAYER = "NEW_PLAYER";
	public static final int INVALID_POSITION = -1;

	private static final int HELP_VERSION = 2;
	private static final int TOKEN_COLORS = 1;

	private String gameName;
	private int position;

	@State Player player;
	private Player originalPlayer;

	@BindView(R.id.scroll_container) ScrollView scrollContainer;
	@BindView(R.id.header) TextView headerView;
	@BindView(R.id.two_line_container) View twoLineContainer;
	@BindView(R.id.header2) TextView titleView;
	@BindView(R.id.subheader) TextView subtitleView;
	@BindView(R.id.log_player_username) AutoCompleteTextView usernameView;
	@BindView(R.id.log_player_name) AutoCompleteTextView nameView;
	@BindView(R.id.log_player_team_color) AutoCompleteTextView teamColorView;
	@BindView(R.id.color_view) ImageView colorView;
	@BindView(R.id.log_player_position) EditText positionView;
	@BindView(R.id.log_player_position_button) Button positionButton;
	@BindView(R.id.log_player_score) EditText scoreView;
	@BindView(R.id.log_player_score_button) Button scoreButton;
	@BindView(R.id.log_player_rating) EditText ratingView;
	@BindView(R.id.log_player_new) SwitchCompat newView;
	@BindView(R.id.log_player_win) SwitchCompat winView;
	@BindView(R.id.fab) FloatingActionButton fab;
	@BindView(R.id.fab_buffer) View fabBuffer;
	private ShowcaseViewWizard showcaseWizard;

	private boolean preferToShowTeamColor;
	private boolean preferToShowPosition;
	private boolean preferToShowScore;
	private boolean preferToShowRating;
	private boolean preferToShowNew;
	private boolean preferToShowWin;
	@State boolean userHasShownTeamColor;
	@State boolean userHasShownPosition;
	@State boolean userHasShownScore;
	@State boolean userHasShownRating;
	@State boolean userHasShownNew;
	@State boolean userHasShownWin;
	private int autoPosition;
	private boolean isNewPlayer;
	private ArrayList<String> usedColors;
	private ArrayList<String> colors;

	private final View.OnClickListener actionBarListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.menu_done:
					save();
					break;
				case R.id.menu_cancel:
					cancel();
					break;
			}
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
							colors = new ArrayList<>();
							do {
								colors.add(cursor.getString(0));
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
		ButterKnife.bind(this);

		nameView.setOnItemClickListener(nameClickListener());

		ToolbarUtils.setDoneCancelActionBarView(this, actionBarListener);

		final Intent intent = getIntent();
		int gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		position = intent.getIntExtra(KEY_POSITION, INVALID_POSITION);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		String imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
		autoPosition = intent.getIntExtra(KEY_AUTO_POSITION, Player.SEAT_UNKNOWN);
		String[] usedColors = intent.getStringArrayExtra(KEY_USED_COLORS);
		if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
			userHasShownScore = true;
			scoreView.requestFocus();
		}
		isNewPlayer = intent.getBooleanExtra(KEY_NEW_PLAYER, false);
		PresentationUtils.colorFab(fab, intent.getIntExtra(KEY_FAB_COLOR, ContextCompat.getColor(this, R.color.accent)));

		if (savedInstanceState == null) {
			player = intent.getParcelableExtra(KEY_PLAYER);
			if (player == null) {
				player = new Player();
			}
			if (hasAutoPosition()) {
				player.setSeat(autoPosition);
			}
			originalPlayer = new Player(player);
		} else {
			Icepick.restoreInstanceState(this, savedInstanceState);
		}

		this.usedColors = (usedColors == null) ?
			new ArrayList<String>() :
			new ArrayList<>(Arrays.asList(usedColors));
		this.usedColors.remove(player.color);

		ImageUtils.safelyLoadImage((ImageView) findViewById(R.id.thumbnail), imageUrl);
		bindUi();

		new QueryHandler(getContentResolver()).startQuery(TOKEN_COLORS, null, Games.buildColorsUri(gameId),
			new String[] { GameColors.COLOR }, null, null, null);

		nameView.setAdapter(new PlayerNameAdapter(this));
		usernameView.setAdapter(new BuddyNameAdapter(this));
		teamColorView.setAdapter(new GameColorAdapter(this, gameId, R.layout.autocomplete_color));

		setUpShowcaseViewWizard();
		showcaseWizard.maybeShowHelp();
	}

	@DebugLog
	@Override
	protected void onResume() {
		super.onResume();
		preferToShowTeamColor = PreferencesUtils.showLogPlayerTeamColor(this);
		preferToShowPosition = PreferencesUtils.showLogPlayerPosition(this);
		preferToShowScore = PreferencesUtils.showLogPlayerScore(this);
		preferToShowRating = PreferencesUtils.showLogPlayerRating(this);
		preferToShowNew = PreferencesUtils.showLogPlayerNew(this);
		preferToShowWin = PreferencesUtils.showLogPlayerWin(this);
		setViewVisibility();
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
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
				usernameView.setText((String) view.getTag());
			}
		};
	}

	@DebugLog
	@OnClick(R.id.color_view)
	public void onColorClick() {
		ColorPickerDialogFragment fragment = ColorPickerDialogFragment.newInstance(0,
			ColorUtils.getColorList(), colors, teamColorView.getText().toString(), usedColors, null, 4);

		fragment.setOnColorSelectedListener(new ColorPickerDialogFragment.OnColorSelectedListener() {
			@Override
			public void onColorSelected(String description, int color) {
				teamColorView.setText(description);
			}

		});

		fragment.show(getSupportFragmentManager(), "color_picker");
	}

	@DebugLog
	@OnTextChanged(R.id.log_player_team_color)
	public void afterTextChanged(Editable text) {
		int color = ColorUtils.parseColor(text.toString());
		ColorUtils.setColorViewValue(colorView, color);
	}

	@DebugLog
	@OnClick({ R.id.log_player_position_button, R.id.log_player_score_button })
	public void onNumberToTextClick(Button button) {
		EditText editText = null;
		if (button == positionButton) {
			editText = positionView;
		} else if (button == scoreButton) {
			editText = scoreView;
		}
		if (editText == null) {
			return;
		}
		int type = editText.getInputType();
		if ((type & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
			editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS
				| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			button.setText(R.string.text_to_number);
		} else {
			editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
				| InputType.TYPE_NUMBER_FLAG_SIGNED);
			button.setText(R.string.number_to_text);
		}
		editText.requestFocus();
	}

	@DebugLog
	private void setUpShowcaseViewWizard() {
		showcaseWizard = new ShowcaseViewWizard(this, HelpUtils.HELP_LOGPLAYER_KEY, HELP_VERSION);
		showcaseWizard.addTarget(R.string.help_logplayer, Target.NONE);
	}

	@DebugLog
	private void bindUi() {
		if (hasAutoPosition()) {
			titleView.setText(gameName);
			subtitleView.setText(getString(R.string.title_player) + " #" + autoPosition);
			headerView.setVisibility(View.GONE);
			twoLineContainer.setVisibility(View.VISIBLE);
		} else {
			headerView.setText(gameName);
			headerView.setVisibility(View.VISIBLE);
			twoLineContainer.setVisibility(View.GONE);
		}
		nameView.setTextKeepState(player.name);
		usernameView.setTextKeepState(player.username);
		teamColorView.setTextKeepState(player.color);
		if (player.getStartingPosition() != null) {
			positionView.setTextKeepState(player.getStartingPosition());
		}
		scoreView.setTextKeepState(player.score);
		ratingView.setTextKeepState((player.rating == Player.DEFAULT_RATING) ? "" : String.valueOf(player.rating));
		newView.setChecked(player.New());
		winView.setChecked(player.Win());
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
		enableButton |= hideRow(shouldHideNew(), newView);
		enableButton |= hideRow(shouldHideWin(), winView);
		fabBuffer.setVisibility(enableButton ? View.VISIBLE : View.GONE);
		if (enableButton) {
			fab.show();
		} else {
			fab.hide();
		}
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
		return !preferToShowTeamColor && !userHasShownTeamColor && TextUtils.isEmpty(player.color);
	}

	@DebugLog
	private boolean shouldHidePosition() {
		return !preferToShowPosition && !userHasShownPosition && TextUtils.isEmpty(player.getStartingPosition());
	}

	@DebugLog
	private boolean hasAutoPosition() {
		return autoPosition != Player.SEAT_UNKNOWN;
	}

	@DebugLog
	private boolean shouldHideScore() {
		return !preferToShowScore && !userHasShownScore && TextUtils.isEmpty(player.score);
	}

	@DebugLog
	private boolean shouldHideRating() {
		return !preferToShowRating && !userHasShownRating && !(player.rating > 0);
	}

	@DebugLog
	private boolean shouldHideNew() {
		return !preferToShowNew && !userHasShownNew && !player.New();
	}

	@DebugLog
	private boolean shouldHideWin() {
		return !preferToShowWin && !userHasShownWin && !player.Win();
	}

	@DebugLog
	@OnClick(R.id.fab)
	public void addField() {
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
						userHasShownTeamColor = true;
						viewToFocus = teamColorView;
						viewToScroll = findViewById(R.id.log_player_team_color_container);
					} else if (selection.equals(r.getString(R.string.starting_position))) {
						userHasShownPosition = true;
						viewToFocus = positionView;
						viewToScroll = findViewById(R.id.log_player_position_container);
					} else if (selection.equals(r.getString(R.string.score))) {
						userHasShownScore = true;
						viewToFocus = scoreView;
						viewToScroll = findViewById(R.id.log_player_score_container);
					} else if (selection.equals(r.getString(R.string.rating))) {
						userHasShownRating = true;
						viewToFocus = ratingView;
						viewToScroll = findViewById(R.id.log_player_rating);
					} else if (selection.equals(r.getString(R.string.new_label))) {
						userHasShownNew = true;
						newView.setChecked(true);
						viewToScroll = newView;
						viewToFocus = newView;
					} else if (selection.equals(r.getString(R.string.win))) {
						userHasShownWin = true;
						winView.setChecked(true);
						viewToScroll = winView;
						viewToFocus = winView;
					}
					AddFieldEvent.log("Player", selection);
					setViewVisibility();
					if (viewToFocus != null) {
						viewToFocus.requestFocus();
					}
					if (viewToScroll != null) {
						final View finalView = viewToScroll;
						scrollContainer.post(new Runnable() {
							@Override
							public void run() {
								scrollContainer.smoothScrollTo(0, finalView.getBottom());
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

		CharSequence[] csa = {};
		csa = list.toArray(csa);
		return csa;
	}

	@DebugLog
	private void save() {
		captureForm();
		Intent intent = new Intent();
		intent.putExtra(KEY_PLAYER, player);
		intent.putExtra(KEY_POSITION, position);
		setResult(RESULT_OK, intent);
		finish();
	}

	@DebugLog
	private void cancel() {
		captureForm();
		if (player.equals(originalPlayer)) {
			setResult(RESULT_CANCELED);
			finish();
		} else {
			DialogUtils.createDiscardDialog(this, R.string.player, isNewPlayer).show();
		}
	}

	@DebugLog
	private void captureForm() {
		player.name = nameView.getText().toString().trim();
		player.username = usernameView.getText().toString().trim();
		player.color = teamColorView.getText().toString().trim();
		player.setStartingPosition(positionView.getText().toString().trim());
		player.score = scoreView.getText().toString().trim();
		player.rating = StringUtils.parseDouble(ratingView.getText().toString().trim());
		player.New(newView.isChecked());
		player.Win(winView.isChecked());
	}
}
