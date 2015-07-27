package com.boardgamegeek.ui.dialog;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.StringUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class ScoreDialogFragment extends DialogFragment {
	private static final String KEY_TITLE = "TITLE";
	private static final String KEY_SCORE = "SCORE";
	private static final String KEY_COLOR = "COLOR";

	@InjectView(R.id.player_name) TextView mPlayerName;
	@InjectView(R.id.number_readout) TextView mReadout;
	@InjectView(R.id.num_delete) View mDelete;
	private OnClickListener mListener;
	private int mMaxLength = 10;

	public interface OnClickListener {
		void onDoneClick(String score);
	}

	public static ScoreDialogFragment newInstance(String title, String score, String color) {
		final ScoreDialogFragment fragment = new ScoreDialogFragment();
		Bundle args = new Bundle();
		args.putString(KEY_TITLE, title);
		if (StringUtils.isNumeric(score)) {
			args.putString(KEY_SCORE, score);
		}
		int c = ColorUtils.parseColor(color);
		if (c != Color.TRANSPARENT) {
			args.putInt(KEY_COLOR, c);
		}
		fragment.setArguments(args);
		return fragment;
	}

	public void setOnDoneClickListener(OnClickListener listener) {
		mListener = listener;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, 0);
	}

	@Override
	public void onResume() {
		super.onResume();
		Window window = getDialog().getWindow();
		DisplayMetrics dm = getResources().getDisplayMetrics();
		int width = Math.min(
			getActivity().getResources().getDimensionPixelSize(R.dimen.dialog_width),
			dm.widthPixels * 3 / 4);
		int height = window.getAttributes().height;
		window.setLayout(width, height);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_score, container, false);
		ButterKnife.inject(this, view);

		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey(KEY_TITLE)) {
				mPlayerName.setText(args.getString(KEY_TITLE));
			}
			if (args.containsKey(KEY_SCORE)) {
				mReadout.setText(args.getString(KEY_SCORE));
				enableDelete();
			}
			if (args.containsKey(KEY_COLOR)) {
				int color = args.getInt(KEY_COLOR);
				mPlayerName.setBackgroundColor(color);
				if (color != ColorUtils.TRANSPARENT && ColorUtils.isColorDark(color)) {
					mPlayerName.setTextColor(Color.WHITE);
				} else {
					mPlayerName.setTextColor(Color.BLACK);
				}
			}
		}

		return view;
	}

	@OnClick({
		R.id.num_0,
		R.id.num_1,
		R.id.num_2,
		R.id.num_3,
		R.id.num_4,
		R.id.num_5,
		R.id.num_6,
		R.id.num_7,
		R.id.num_8,
		R.id.num_9,
		R.id.num_decimal
	})
	void onNumPadClick(View v) {
		final CharSequence text = mReadout.getText();
		if (text.length() < mMaxLength) {
			v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			mReadout.setText(text.toString() + ((TextView) v).getText());
			enableDelete();
		}
	}

	@OnClick(R.id.num_done)
	void onDoneClick(View v) {
		if (mListener != null) {
			mListener.onDoneClick(mReadout.getText().toString());
		}
		dismiss();
	}

	@OnClick(R.id.num_delete)
	void onDeleteClick(View v) {
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		CharSequence score = mReadout.getText();
		if (score.length() > 0) {
			mReadout.setText(score.subSequence(0, score.length() - 1));
			enableDelete();
		}
	}

	@OnLongClick(R.id.num_delete)
	boolean onDeleteLongClick() {
		mReadout.setText("");
		enableDelete();
		return true;
	}

	private void enableDelete() {
		mDelete.setEnabled(mReadout.length() > 0);
	}
}
