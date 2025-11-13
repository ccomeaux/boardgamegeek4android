package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.extensions.FloatingActionButtonUtils;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.adapter.BuddyNameAdapter;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.adapter.PlayerNameAdapter;
import com.boardgamegeek.ui.dialog.ColorPickerWithListenerDialogFragment;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShowcaseViewWizard;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.amlcurran.showcaseview.targets.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import icepick.Icepick;
import icepick.State;

import com.boardgamegeek.databinding.ActivityLogplayerBinding;

public class LogPlayerActivity extends AppCompatActivity implements ColorPickerWithListenerDialogFragment.Listener {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
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

	private ActivityLogplayerBinding binding;
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

	private final View.OnClickListener actionBarListener = v -> {
		switch (v.getId()) {
			case R.id.menu_done:
				save();
				break;
			case R.id.menu_cancel:
				cancel();
				break;
		}
	};

	@SuppressLint("HandlerLeak")
	private class QueryHandler extends AsyncQueryHandler {
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			if (cursor == null) {
				return;
			}
			if (isFinishing()) {
				cursor.close();
				return;
			}

			if (token == TOKEN_COLORS) {
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
			} else {
				cursor.close();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityLogplayerBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.logPlayerName.setOnItemClickListener(nameClickListener());
		binding.logPlayerUsername.setOnItemClickListener(userNameClickListener());

		ToolbarUtils.setDoneCancelActionBarView(this, actionBarListener);

		final Intent intent = getIntent();
		int gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		position = intent.getIntExtra(KEY_POSITION, INVALID_POSITION);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		String imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
		String thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL);
		String heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL);
		autoPosition = intent.getIntExtra(KEY_AUTO_POSITION, Player.SEAT_UNKNOWN);
		String[] usedColors = intent.getStringArrayExtra(KEY_USED_COLORS);

		if (thumbnailUrl == null) thumbnailUrl = "";
		if (imageUrl == null) imageUrl = "";
		if (heroImageUrl == null) heroImageUrl = "";

		if (intent.getBooleanExtra(KEY_END_PLAY, false)) {
			userHasShownScore = true;
			binding.logPlayerScore.requestFocus();
		}
		isNewPlayer = intent.getBooleanExtra(KEY_NEW_PLAYER, false);
		FloatingActionButtonUtils.colorize(binding.fab, intent.getIntExtra(KEY_FAB_COLOR, ContextCompat.getColor(this, R.color.accent)));

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
			new ArrayList<>() :
			new ArrayList<>(Arrays.asList(usedColors));
		this.usedColors.remove(player.color);

		ImageUtils.safelyLoadImage(binding.thumbnail, imageUrl, thumbnailUrl, heroImageUrl);
		bindUi();

		new QueryHandler(getContentResolver()).startQuery(TOKEN_COLORS, null, Games.buildColorsUri(gameId),
			new String[] { GameColors.COLOR }, null, null, null);

		binding.logPlayerName.setAdapter(new PlayerNameAdapter(this));
		binding.logPlayerUsername.setAdapter(new BuddyNameAdapter(this));
		binding.logPlayerTeamColor.setAdapter(new GameColorAdapter(this, gameId, R.layout.autocomplete_color));

		// Set up listeners
		binding.colorView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onColorClick();
			}
		});

		binding.logPlayerTeamColor.addTextChangedListener(new android.text.TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				LogPlayerActivity.this.afterTextChanged(s);
			}
		});

		View.OnClickListener numberButtonListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onNumberToTextClick((ImageButton) v);
			}
		};
		binding.logPlayerPositionButton.setOnClickListener(numberButtonListener);
		binding.logPlayerScoreButton.setOnClickListener(numberButtonListener);

		binding.fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addField();
			}
		});

		setUpShowcaseViewWizard();
		showcaseWizard.maybeShowHelp();
	}

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

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onBackPressed() {
		cancel();
	}

	private OnItemClickListener nameClickListener() {
		return (parent, view, position, id) -> binding.logPlayerUsername.setText((String) view.getTag());
	}

	private OnItemClickListener userNameClickListener() {
		return (parent, view, position, id) -> binding.logPlayerName.setText((String) view.getTag());
	}

	public void onColorClick() {
		ColorPickerWithListenerDialogFragment fragment = ColorPickerWithListenerDialogFragment.newInstance(colors, binding.logPlayerTeamColor.getText().toString(), usedColors);
		fragment.show(getSupportFragmentManager(), "color_picker");
	}

	@Override
	public void onColorSelected(@NotNull String description, int color, int requestCode) {
		binding.logPlayerTeamColor.setText(description);
	}

	public void afterTextChanged(Editable text) {
		int color = ColorUtils.parseColor(text.toString());
		ColorUtils.setColorViewValue(binding.colorView, color);
	}

	public void onNumberToTextClick(ImageButton button) {
		EditText editText = null;
		if (button == binding.logPlayerPositionButton) {
			editText = binding.logPlayerPosition;
		} else if (button == binding.logPlayerScoreButton) {
			editText = binding.logPlayerScore;
		}
		if (editText == null) {
			return;
		}

		int type = editText.getInputType();
		if ((type & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
			editText.setInputType(
				InputType.TYPE_CLASS_TEXT |
					InputType.TYPE_TEXT_FLAG_CAP_WORDS |
					InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			button.setImageResource(R.drawable.ic_dialpad);
		} else {
			editText.setInputType(
				InputType.TYPE_CLASS_NUMBER |
					InputType.TYPE_NUMBER_FLAG_DECIMAL);
			if (editText == scoreView) {
				editText.setInputType(editText.getInputType() | InputType.TYPE_NUMBER_FLAG_SIGNED);
			} else {
				editText.setInputType(editText.getInputType() & ~InputType.TYPE_NUMBER_FLAG_SIGNED);
			}
			button.setImageResource(R.drawable.ic_keyboard);
		}
		UIUtils.focusWithKeyboard(editText);
	}

	private void setUpShowcaseViewWizard() {
		showcaseWizard = new ShowcaseViewWizard(this, HelpUtils.HELP_LOGPLAYER_KEY, HELP_VERSION);
		showcaseWizard.addTarget(R.string.help_logplayer, Target.NONE);
	}

	private void bindUi() {
		if (hasAutoPosition()) {
			binding.header2.setText(gameName);
			binding.subheader.setText(getString(R.string.generic_player, autoPosition));
			binding.header.setVisibility(View.GONE);
			binding.twoLineContainer.setVisibility(View.VISIBLE);
		} else {
			binding.header.setText(gameName);
			binding.header.setVisibility(View.VISIBLE);
			binding.twoLineContainer.setVisibility(View.GONE);
		}
		binding.logPlayerName.setTextKeepState(player.name);
		binding.logPlayerUsername.setTextKeepState(player.username);
		binding.logPlayerTeamColor.setTextKeepState(player.color);
		if (player.getStartingPosition() != null) {
			binding.logPlayerPosition.setTextKeepState(player.getStartingPosition());
		}
		binding.logPlayerScore.setTextKeepState(player.score);
		binding.logPlayerRating.setTextKeepState((player.rating == Player.DEFAULT_RATING) ? "" : String.valueOf(player.rating));
		binding.logPlayerNew.setChecked(player.isNew);
		binding.logPlayerWin.setChecked(player.isWin);
	}

	private void setViewVisibility() {
		boolean enableButton = hideRow(shouldHideTeamColor(), binding.logPlayerTeamColorContainer);
		enableButton |= hideRow(shouldHidePosition(), binding.logPlayerPositionContainer);
		if (hasAutoPosition()) {
			hideRow(true, binding.logPlayerPositionContainer);
		}
		enableButton |= hideRow(shouldHideScore(), binding.logPlayerScoreContainer);
		enableButton |= hideRow(shouldHideRating(), binding.logPlayerRatingContainer);
		enableButton |= hideRow(shouldHideNew(), binding.logPlayerNew);
		enableButton |= hideRow(shouldHideWin(), binding.logPlayerWin);
		binding.fabBuffer.setVisibility(enableButton ? View.VISIBLE : View.GONE);
		if (enableButton) {
			binding.fab.show();
		} else {
			binding.fab.hide();
		}
	}

	private boolean hideRow(boolean shouldHide, View view) {
		if (shouldHide) {
			view.setVisibility(View.GONE);
			return true;
		}
		view.setVisibility(View.VISIBLE);
		return false;
	}

	private boolean shouldHideTeamColor() {
		return !preferToShowTeamColor && !userHasShownTeamColor && TextUtils.isEmpty(player.color);
	}

	private boolean shouldHidePosition() {
		return !preferToShowPosition && !userHasShownPosition && TextUtils.isEmpty(player.getStartingPosition());
	}

	private boolean hasAutoPosition() {
		return autoPosition != Player.SEAT_UNKNOWN;
	}

	private boolean shouldHideScore() {
		return !preferToShowScore && !userHasShownScore && TextUtils.isEmpty(player.score);
	}

	private boolean shouldHideRating() {
		return !preferToShowRating && !userHasShownRating && !(player.rating > 0);
	}

	private boolean shouldHideNew() {
		return !preferToShowNew && !userHasShownNew && !player.isNew;
	}

	private boolean shouldHideWin() {
		return !preferToShowWin && !userHasShownWin && !player.isWin;
	}

	public void addField() {
		final CharSequence[] array = createAddFieldArray();
		if (array == null || array.length == 0) {
			return;
		}
		new AlertDialog.Builder(this).setTitle(R.string.add_field)
			.setItems(array, (dialog, which) -> {
				Resources r = getResources();
				View viewToFocus = null;
				View viewToScroll = null;

				String selection = array[which].toString();
				if (selection.equals(r.getString(R.string.team_color))) {
					userHasShownTeamColor = true;
					viewToFocus = binding.logPlayerTeamColor;
					viewToScroll = binding.logPlayerTeamColorContainer;
				} else if (selection.equals(r.getString(R.string.starting_position))) {
					userHasShownPosition = true;
					viewToFocus = binding.logPlayerPosition;
					viewToScroll = binding.logPlayerPositionContainer;
				} else if (selection.equals(r.getString(R.string.score))) {
					userHasShownScore = true;
					viewToFocus = binding.logPlayerScore;
					viewToScroll = binding.logPlayerScoreContainer;
				} else if (selection.equals(r.getString(R.string.rating))) {
					userHasShownRating = true;
					viewToFocus = binding.logPlayerRating;
					viewToScroll = binding.logPlayerRating;
				} else if (selection.equals(r.getString(R.string.new_label))) {
					userHasShownNew = true;
					binding.logPlayerNew.setChecked(true);
					viewToScroll = binding.logPlayerNew;
					viewToFocus = binding.logPlayerNew;
				} else if (selection.equals(r.getString(R.string.win))) {
					userHasShownWin = true;
					binding.logPlayerWin.setChecked(true);
					viewToScroll = binding.logPlayerWin;
					viewToFocus = binding.logPlayerWin;
				}
				setViewVisibility();
				if (viewToFocus != null) {
					viewToFocus.requestFocus();
				}
				if (viewToScroll != null) {
					final View finalView = viewToScroll;
					binding.scrollContainer.post(() -> binding.scrollContainer.smoothScrollTo(0, finalView.getBottom()));
				}
			}).show();
	}

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

	private void save() {
		captureForm();
		Intent intent = new Intent();
		intent.putExtra(KEY_PLAYER, player);
		intent.putExtra(KEY_POSITION, position);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void cancel() {
		captureForm();
		if (player.equals(originalPlayer)) {
			setResult(RESULT_CANCELED);
			finish();
		} else {
			DialogUtils.createDiscardDialog(this, R.string.player, isNewPlayer).show();
		}
	}

	private void captureForm() {
		player.name = binding.logPlayerName.getText().toString().trim();
		player.username = binding.logPlayerUsername.getText().toString().trim();
		player.color = binding.logPlayerTeamColor.getText().toString().trim();
		player.setStartingPosition(binding.logPlayerPosition.getText().toString().trim());
		player.score = binding.logPlayerScore.getText().toString().trim();
		player.rating = StringUtils.parseDouble(binding.logPlayerRating.getText().toString().trim());
		player.isNew = binding.logPlayerNew.isChecked();
		player.isWin = binding.logPlayerWin.isChecked();
	}
}
