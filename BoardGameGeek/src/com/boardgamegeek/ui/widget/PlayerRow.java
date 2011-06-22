package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;

public class PlayerRow extends LinearLayout {
	private Player mPlayer;

	private Button mEditButton;
	private Button mDeleteButton;
	private TextView mName;

	private OnClickListener mEditClickListener;
	private OnClickListener mDeleteClickListener;

	public PlayerRow(Context context) {
		this(context, null);
	}

	public PlayerRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		setOrientation(LinearLayout.HORIZONTAL);
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		setGravity(Gravity.CENTER_VERTICAL);

		// float smallTextSize =
		// getResources().getDimension(R.dimen.text_size_small);
		float mediumTextSize = getResources().getDimension(R.dimen.text_size_medium);
		LayoutParams simpleWrap = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		mEditButton = new Button(context);
		mEditButton.setText(R.string.edit);
		mEditButton.setLayoutParams(simpleWrap);
		mEditButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mEditClickListener != null) {
					mEditClickListener.onClick(PlayerRow.this);
				}
			}
		});

		mDeleteButton = new Button(context);
		mDeleteButton.setText(R.string.delete);
		mDeleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mDeleteClickListener != null) {
					mDeleteClickListener.onClick(PlayerRow.this);
				}
			}
		});

		mName = new TextView(context);
		mName.setTextSize(TypedValue.COMPLEX_UNIT_PX, mediumTextSize);
		mName.setGravity(Gravity.CENTER_VERTICAL);
		mName.setLayoutParams(simpleWrap);

		addView(mName);
		addView(mEditButton);
		addView(mDeleteButton, simpleWrap);
	}

	public void setOnEditListener(OnClickListener l) {
		mEditClickListener = l;
	}

	public void setOnDeleteListener(OnClickListener l) {
		mDeleteClickListener = l;
	}

	public void setPlayer(Player player) {
		mPlayer = player;
		bindUi();
	}

	public Player getPlayer() {
		return mPlayer;
	}

	private void bindUi() {
		if (mPlayer == null) {
			mName.setText("");
		} else {
			mName.setText(mPlayer.Name);
		}
	}
}
