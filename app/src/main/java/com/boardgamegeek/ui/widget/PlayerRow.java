package com.boardgamegeek.ui.widget;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Color;
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

	private View mDragHandle;
	private ImageView mColorView;
	private TextView mSeat;
	private TextView mName;
	private TextView mUsername;
	private TextView mTeamColor;
	private TextView mScore;
	private TextView mStartingPosition;
	private TextView mRating;
	private ImageView mDeleteButton;

	private Typeface mNameTypeface;
	private Typeface mUsernameTypeface;
	private Typeface mScoreTypeface;

	public PlayerRow(Context context) {
		this(context, null);
	}

	public PlayerRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.row_player, this);
		initializeUi();
	}

	private void initializeUi() {
		mDragHandle = findViewById(R.id.drag_handle);
		mColorView = (ImageView) findViewById(R.id.color_view);
		mSeat = (TextView) findViewById(R.id.seat);
		mName = (TextView) findViewById(R.id.name);
		mUsername = (TextView) findViewById(R.id.username);
		mTeamColor = (TextView) findViewById(R.id.team_color);
		mScore = (TextView) findViewById(R.id.score);
		mRating = (TextView) findViewById(R.id.rating);
		mStartingPosition = (TextView) findViewById(R.id.starting_position);

		mNameTypeface = mName.getTypeface();
		mUsernameTypeface = mUsername.getTypeface();
		mScoreTypeface = mScore.getTypeface();

		mDeleteButton = (ImageView) findViewById(R.id.log_player_delete);
	}

	public void setOnDeleteListener(OnClickListener l) {
		mDeleteButton.setVisibility(View.VISIBLE);
		mDeleteButton.setFocusable(false); // necessary to allow the row to receive click events
		mDeleteButton.setOnClickListener(l);
	}

	public void setAutoSort(boolean value) {
		mDragHandle.setVisibility(value ? View.VISIBLE : View.GONE);
	}

	public void setPlayer(Player player) {
		if (player == null) {
			mColorView.setVisibility(View.GONE);
			setText(mSeat, "");
			setText(mName, "");
			setText(mUsername, "");
			setText(mTeamColor, "");
			setText(mScore, "");
			setText(mRating, "");
		} else {
			int color = ColorUtils.parseColor(player.color);

			setText(mSeat, player.getStartingPosition());
			setText(mName, player.name, mNameTypeface, player.New(), player.Win());
			setText(mUsername, player.username, mUsernameTypeface, player.New(), false);
			setText(mTeamColor, player.color);
			setText(mScore, player.score, mScoreTypeface, false, player.Win());
			setText(mRating, (player.rating > 0) ? mFormat.format(player.rating) : "");
			setText(mStartingPosition, player.getStartingPosition());

			mColorView.setVisibility(View.VISIBLE);
			ColorUtils.setColorViewValue(mColorView, color);
			if (player.getSeat() == Player.SEAT_UNKNOWN) {
				mSeat.setVisibility(View.GONE);
			} else {
				if (color != ColorUtils.TRANSPARENT && ColorUtils.isColorDark(color)) {
					mSeat.setTextColor(Color.WHITE);
				} else {
					mSeat.setTextColor(Color.BLACK);
				}
				mStartingPosition.setVisibility(View.GONE);
			}
			if (color != ColorUtils.TRANSPARENT) {
				mTeamColor.setVisibility(View.GONE);
			}
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
