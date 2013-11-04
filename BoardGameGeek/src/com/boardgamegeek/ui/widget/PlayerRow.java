package com.boardgamegeek.ui.widget;

import java.text.DecimalFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
	private Player mPlayer;
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
	private View mEditButton;

	private OnClickListener mEditClickListener;
	private OnClickListener mDeleteClickListener;

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
		mDeleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(PlayerRow.this.getContext());
				builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_delete_player)
					.setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (mDeleteClickListener != null) {
								mDeleteClickListener.onClick(PlayerRow.this);
							}
						}
					}).setNegativeButton(R.string.no, null);
				builder.create().show();
			}
		});

		mEditButton = findViewById(R.id.log_player_edit);
		mEditButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mEditClickListener != null) {
					mEditClickListener.onClick(PlayerRow.this);
				}
			}
		});
	}

	public void setOnEditListener(OnClickListener l) {
		setEnabled(true);
		mEditClickListener = l;
	}

	public void setOnDeleteListener(OnClickListener l) {
		mDeleteButton.setVisibility(View.VISIBLE);
		mDeleteClickListener = l;
	}

	public void setPlayer(Player player) {
		mPlayer = player;
		bindUi();
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public void setAutoSort(boolean value) {
		mAutoSort = value;
	}

	private void bindUi() {
		if (mPlayer == null) {
			setText(mName, "");
			setText(mUsername, "");
			setText(mTeamColor, "");
			setText(mScore, "");
			setText(mRating, "");
			setText(mStartingPosition, "");
		} else {
			setText(mUsername, mPlayer.Username);
			if (TextUtils.isEmpty(mPlayer.Name)) {
				mName.setVisibility(View.GONE);
			} else {
				mName.setVisibility(View.VISIBLE);
				mName.setText(mPlayer.Name);
				if (mPlayer.New && mPlayer.Win) {
					mName.setTypeface(mNameTypeface, Typeface.BOLD_ITALIC);
				} else if (mPlayer.New) {
					mName.setTypeface(mNameTypeface, Typeface.ITALIC);
				} else if (mPlayer.Win) {
					mName.setTypeface(mNameTypeface, Typeface.BOLD);
				} else {
					mName.setTypeface(mNameTypeface, Typeface.NORMAL);
				}
			}

			int color = ColorUtils.parseColor(mPlayer.TeamColor);
			if (color != ColorUtils.TRANSPARENT) {
				mColorSwatch.setBackgroundColor(color);
				mColorSwatchContainer.setVisibility(View.VISIBLE);
				mTeamColor.setVisibility(View.GONE);
			} else {
				mColorSwatchContainer.setVisibility(View.INVISIBLE);
				setText(mTeamColor, mPlayer.TeamColor);
			}

			setText(mScore, mPlayer.Score);

			setText(mStartingPosition, (mPlayer.getSeat() == Player.SEAT_UNKNOWN) ? mPlayer.getStartingPosition() : "#"
				+ mPlayer.getSeat());
			mStartingPosition.setTextColor(mAutoSort ? mLightTextColor : mDefaultTextColor);

			setText(mRating, (mPlayer.Rating > 0) ? mFormat.format(mPlayer.Rating) : "");
		}
	}

	private void setText(TextView textView, String text) {
		textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		textView.setText(text);
	}
}
