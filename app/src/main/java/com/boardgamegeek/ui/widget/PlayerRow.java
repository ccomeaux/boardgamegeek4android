package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.util.ColorUtils;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlayerRow extends LinearLayout {
	private final DecimalFormat ratingFormat = new DecimalFormat("0.0######");

	@BindView(R.id.drag_handle) View dragHandle;
	@BindView(R.id.color_container) View colorContainer;
	@BindView(R.id.color_view) ImageView colorView;
	@BindView(R.id.seat) TextView seatView;
	@BindView(R.id.name) TextView nameView;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.team_color) TextView teamColorView;
	@BindView(R.id.score) TextView scoreView;
	@BindView(R.id.starting_position) TextView startingPositionView;
	@BindView(R.id.rating) TextView ratingView;
	@BindView(R.id.score_button) ImageView scoreButton;
	@BindView(R.id.more) View moreButton;

	private final Typeface nameTypeface;
	private final Typeface usernameTypeface;
	private final Typeface scoreTypeface;

	private boolean hasScoreListener;

	public PlayerRow(Context context) {
		this(context, null);
	}

	public PlayerRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.row_player, this);

		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		setBaselineAligned(false);
		setGravity(Gravity.CENTER_VERTICAL);
		setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.player_row_height));
		setOrientation(LinearLayout.HORIZONTAL);
		int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.material_margin_horizontal);
		int verticalPadding = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

		ButterKnife.bind(this);

		nameTypeface = nameView.getTypeface();
		usernameTypeface = usernameView.getTypeface();
		scoreTypeface = scoreView.getTypeface();

		scoreButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN);
	}

	public void setOnScoreListener(OnClickListener l) {
		hasScoreListener = true;
		scoreButton.setVisibility(View.VISIBLE);
		scoreButton.setFocusable(false);
		scoreButton.setOnClickListener(l);
	}

	public void setOnColorListener(OnClickListener l) {
		colorContainer.setFocusable(false);
		colorContainer.setOnClickListener(l);
	}

	public void setOnMoreListener(OnClickListener l) {
		moreButton.setVisibility(View.VISIBLE);
		moreButton.setOnClickListener(l);
	}

	public void setAutoSort(boolean value) {
		dragHandle.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
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

	public View getMoreButton() {
		return moreButton;
	}

	public View getDragHandle() {
		return dragHandle;
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
