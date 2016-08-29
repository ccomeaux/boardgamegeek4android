package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.util.ColorUtils;

import java.text.DecimalFormat;

public class PlayerRow extends LinearLayout {
	private final DecimalFormat ratingFormat = new DecimalFormat("0.0######");

	private View dragHandle;
	private ImageView colorView;
	private TextView seatView;
	private TextView nameView;
	private TextView usernameView;
	private TextView teamColorView;
	private TextView scoreView;
	private TextView startingPositionView;
	private TextView ratingView;
	private ImageView deleteButton;
	private ImageView scoreButton;

	private Typeface nameTypeface;
	private Typeface usernameTypeface;
	private Typeface scoreTypeface;

	private boolean hasScoreListener;

	public PlayerRow(Context context) {
		this(context, null);
	}

	public PlayerRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.row_player, this);
		initializeUi();
	}

	private void initializeUi() {
		dragHandle = findViewById(R.id.drag_handle);
		colorView = (ImageView) findViewById(R.id.color_view);
		seatView = (TextView) findViewById(R.id.seat);
		nameView = (TextView) findViewById(R.id.name);
		usernameView = (TextView) findViewById(R.id.username);
		teamColorView = (TextView) findViewById(R.id.team_color);
		scoreView = (TextView) findViewById(R.id.score);
		ratingView = (TextView) findViewById(R.id.rating);
		startingPositionView = (TextView) findViewById(R.id.starting_position);
		scoreButton = (ImageView) findViewById(R.id.score_button);

		nameTypeface = nameView.getTypeface();
		usernameTypeface = usernameView.getTypeface();
		scoreTypeface = scoreView.getTypeface();

		scoreButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN);

		deleteButton = (ImageView) findViewById(R.id.log_player_delete);
	}

	public void setOnDeleteListener(OnClickListener l) {
		deleteButton.setVisibility(View.VISIBLE);
		deleteButton.setFocusable(false); // necessary to allow the row to receive click events
		deleteButton.setOnClickListener(l);
	}

	public void setOnScoreListener(OnClickListener l) {
		hasScoreListener = true;
		scoreButton.setVisibility(View.VISIBLE);
		scoreButton.setFocusable(false); // necessary to allow the row to receive click events
		scoreButton.setOnClickListener(l);
	}

	public void setAutoSort(boolean value) {
		dragHandle.setVisibility(value ? View.VISIBLE : View.GONE);
	}

	public void setPlayer(Player player) {
		if (player == null) {
			colorView.setVisibility(View.GONE);
			setText(seatView, "");
			setText(nameView, "");
			setText(usernameView, "");
			setText(teamColorView, "");
			setText(scoreView, "");
			setText(ratingView, "");
			scoreButton.setVisibility(View.GONE);
		} else {
			setText(seatView, player.getStartingPosition());
			if (TextUtils.isEmpty(player.name)) {
				setText(nameView, player.username, nameTypeface, player.New(), player.Win());
				usernameView.setVisibility(View.GONE);
			} else {
				setText(nameView, player.name, nameTypeface, player.New(), player.Win());
				setText(usernameView, player.username, usernameTypeface, player.New(), player.Win());
			}
			setText(teamColorView, player.color);
			setText(scoreView, player.score, scoreTypeface, false, player.Win());
			setText(ratingView, (player.rating > 0) ? ratingFormat.format(player.rating) : "");
			setText(startingPositionView, player.getStartingPosition());

			int color = ColorUtils.parseColor(player.color);
			colorView.setVisibility(View.VISIBLE);
			ColorUtils.setColorViewValue(colorView, color);
			if (player.getSeat() == Player.SEAT_UNKNOWN) {
				seatView.setVisibility(View.GONE);
			} else {
				if (color != ColorUtils.TRANSPARENT && ColorUtils.isColorDark(color)) {
					seatView.setTextColor(Color.WHITE);
				} else {
					seatView.setTextColor(Color.BLACK);
				}
				startingPositionView.setVisibility(View.GONE);
			}
			if (color != ColorUtils.TRANSPARENT) {
				teamColorView.setVisibility(View.GONE);
			}

			scoreButton.setVisibility(hasScoreListener ? View.VISIBLE : View.GONE);
		}
	}

	private void setText(TextView textView, String text) {
		textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		textView.setText(text);
	}

	private void setText(TextView textView, String text, Typeface tf, boolean italic, boolean bold) {
		setText(textView, text);
		if (!TextUtils.isEmpty(text)) {
			if (italic && bold) {
				textView.setTypeface(tf, Typeface.BOLD_ITALIC);
			} else if (italic) {
				textView.setTypeface(tf, Typeface.ITALIC);
			} else if (bold) {
				textView.setTypeface(tf, Typeface.BOLD);
			} else {
				textView.setTypeface(tf, Typeface.NORMAL);
			}
		}
	}
}
