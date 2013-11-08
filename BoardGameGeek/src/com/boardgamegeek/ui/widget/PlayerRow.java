package com.boardgamegeek.ui.widget;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Typeface;
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

public class PlayerRow extends LinearLayout {
	private DecimalFormat mFormat = new DecimalFormat("0.0######");
	private Typeface mNameTypeface;
	private int mLightTextColor;
	private int mDefaultTextColor;
	private boolean mAutoSort;

	private TextView mName;
	private TextView mUsername;
	private View mColorSwatchContainer;
	private View mColorSwatch;
	private TextView mTeamColor;
	private TextView mScore;
	private TextView mStartingPosition;
	private TextView mRating;
	private ImageView mDeleteButton;

	public PlayerRow(Context context) {
		this(context, null);
	}

	public PlayerRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.row_player, this);
		initializeUi();
	}

	private void initializeUi() {
		mName = (TextView) findViewById(R.id.name);
		mUsername = (TextView) findViewById(R.id.username);
		mColorSwatchContainer = findViewById(R.id.color_swatch_container);
		mColorSwatch = findViewById(R.id.color_swatch);
		mTeamColor = (TextView) findViewById(R.id.team_color);
		mScore = (TextView) findViewById(R.id.score);
		mRating = (TextView) findViewById(R.id.rating);
		mStartingPosition = (TextView) findViewById(R.id.starting_position);

		mNameTypeface = mName.getTypeface();
		mDefaultTextColor = mScore.getTextColors().getDefaultColor();
		mLightTextColor = getResources().getColor(R.color.light_text);

		mDeleteButton = (ImageView) findViewById(R.id.log_player_delete);
	}

	public void setOnDeleteListener(OnClickListener l) {
		mDeleteButton.setVisibility(View.VISIBLE);
		mDeleteButton.setFocusable(false); // necessary to allow the row to receive click events
		mDeleteButton.setOnClickListener(l);
	}

	public void setAutoSort(boolean value) {
		mAutoSort = value;
	}

	public void setPlayer(Player player) {
		if (player == null) {
			setText(mName, "");
			setText(mUsername, "");
			setText(mTeamColor, "");
			setText(mScore, "");
			setText(mRating, "");
			setText(mStartingPosition, "");
		} else {
			setText(mUsername, player.Username);
			if (TextUtils.isEmpty(player.Name)) {
				mName.setVisibility(View.GONE);
			} else {
				mName.setVisibility(View.VISIBLE);
				mName.setText(player.Name);
				if (player.New && player.Win) {
					mName.setTypeface(mNameTypeface, Typeface.BOLD_ITALIC);
				} else if (player.New) {
					mName.setTypeface(mNameTypeface, Typeface.ITALIC);
				} else if (player.Win) {
					mName.setTypeface(mNameTypeface, Typeface.BOLD);
				} else {
					mName.setTypeface(mNameTypeface, Typeface.NORMAL);
				}
			}

			int color = ColorUtils.parseColor(player.TeamColor);
			if (color != ColorUtils.TRANSPARENT) {
				mColorSwatch.setBackgroundColor(color);
				mColorSwatchContainer.setVisibility(View.VISIBLE);
				mTeamColor.setVisibility(View.GONE);
			} else {
				mColorSwatchContainer.setVisibility(View.INVISIBLE);
				setText(mTeamColor, player.TeamColor);
			}

			setText(mScore, player.Score);

			setText(mStartingPosition, (player.getSeat() == Player.SEAT_UNKNOWN) ? player.getStartingPosition() : "#"
				+ player.getSeat());
			mStartingPosition.setTextColor(mAutoSort ? mLightTextColor : mDefaultTextColor);

			setText(mRating, (player.Rating > 0) ? mFormat.format(player.Rating) : "");
		}
	}

	private void setText(TextView textView, String text) {
		textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		textView.setText(text);
	}
}
