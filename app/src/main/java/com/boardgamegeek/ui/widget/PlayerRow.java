package com.boardgamegeek.ui.widget;

import android.content.Context;
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
import com.boardgamegeek.util.PresentationUtils;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlayerRow extends LinearLayout {
	private final DecimalFormat ratingFormat = new DecimalFormat("0.0######");

	@BindView(R.id.drag_handle) View dragHandle;
	@BindView(R.id.color_container) View colorContainer;
	@BindView(R.id.color_view) ImageView colorView;
	@BindView(R.id.seat) TextView seatView;
	@BindView(R.id.name_container) View nameContainer;
	@BindView(R.id.name) TextView nameView;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.team_color) TextView teamColorView;
	@BindView(R.id.score) TextView scoreView;
	@BindView(R.id.starting_position) TextView startingPositionView;
	@BindView(R.id.rating) TextView ratingView;
	@BindView(R.id.rating_button) ImageView ratingButton;
	@BindView(R.id.score_button) ImageView scoreButton;
	@BindView(R.id.more) View moreButton;

	private final Typeface nameTypeface;
	private final Typeface usernameTypeface;
	private final Typeface scoreTypeface;
	private final int nameColor;

	public PlayerRow(Context context) {
		this(context, null);
	}

	public PlayerRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.row_player, this);

		setBaselineAligned(false);
		setGravity(Gravity.CENTER_VERTICAL);
		setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.player_row_height));
		setOrientation(LinearLayout.HORIZONTAL);
		int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.material_margin_horizontal);
		int verticalPadding = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

		PresentationUtils.setSelectableBackground(this);

		ButterKnife.bind(this);

		nameTypeface = nameView.getTypeface();
		usernameTypeface = usernameView.getTypeface();
		scoreTypeface = scoreView.getTypeface();
		nameColor = nameView.getTextColors().getDefaultColor();

		ratingButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN);
		scoreButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_under_text), Mode.SRC_IN);
	}

	public void setOnScoreListener(OnClickListener l) {
		PresentationUtils.setSelectableBackgroundBorderless(scoreButton);
		scoreButton.setOnClickListener(l);
	}

	public void setOnRatingListener(OnClickListener l) {
		PresentationUtils.setSelectableBackgroundBorderless(scoreButton);
		ratingButton.setOnClickListener(l);
	}

	public void setOnColorListener(OnClickListener l) {
		PresentationUtils.setSelectableBackgroundBorderless(colorContainer);
		colorContainer.setOnClickListener(l);
	}

	public void setNameListener(OnClickListener l) {
		PresentationUtils.setSelectableBackground(nameContainer);
		nameContainer.setOnClickListener(l);
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
			setText(nameView, getResources().getString(R.string.title_player));
			setText(usernameView, "");
			setText(teamColorView, "");
			setText(scoreView, "");
			setText(ratingView, "");
			ratingButton.setVisibility(GONE);
			scoreButton.setVisibility(View.GONE);
		} else {
			setText(seatView, player.getStartingPosition());
			if (TextUtils.isEmpty(player.name) && TextUtils.isEmpty(player.username)) {
				String name;
				if (player.getSeat() == Player.SEAT_UNKNOWN) {
					name = getResources().getString(R.string.title_player);
				} else {
					name = getResources().getString(R.string.generic_player, player.getSeat());
				}
				setText(nameView, name, nameTypeface, player.New(), player.Win(), true);
				usernameView.setVisibility(View.GONE);
			} else if (TextUtils.isEmpty(player.name)) {
				setText(nameView, player.username, nameTypeface, player.New(), player.Win());
				usernameView.setVisibility(View.GONE);
			} else {
				setText(nameView, player.name, nameTypeface, player.New(), player.Win());
				setText(usernameView, player.username, usernameTypeface, player.New(), player.Win());
			}
			setText(teamColorView, player.color);
			setText(scoreView, player.score, scoreTypeface, false, player.Win());
			setText(ratingView, (player.rating > 0) ? ratingFormat.format(player.rating) : "");
			ratingButton.setVisibility(player.rating > 0 ? VISIBLE : GONE);
			setText(startingPositionView, player.getStartingPosition());

			int color = ColorUtils.parseColor(player.color);
			colorView.setVisibility(View.VISIBLE);
			ColorUtils.setColorViewValue(colorView, color);
			if (player.getSeat() == Player.SEAT_UNKNOWN) {
				seatView.setVisibility(View.GONE);
			} else {
				seatView.setTextColor(ColorUtils.getTextColor(color));
				startingPositionView.setVisibility(View.GONE);
			}
			if (color != ColorUtils.TRANSPARENT) {
				teamColorView.setVisibility(View.GONE);
			}
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
		setText(textView, text, tf, italic, bold, false);
	}

	private void setText(TextView textView, String text, Typeface tf, boolean italic, boolean bold, boolean secondary) {
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
			if (secondary) {
				textView.setTextColor(ContextCompat.getColor(getContext(), R.color.secondary_text));
			} else {
				textView.setTextColor(nameColor);
			}
		}
	}
}
