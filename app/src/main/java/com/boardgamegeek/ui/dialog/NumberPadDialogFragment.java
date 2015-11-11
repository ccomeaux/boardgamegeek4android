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

public class NumberPadDialogFragment extends DialogFragment {
	private static final String KEY_TITLE = "TITLE";
	private static final String KEY_OUTPUT = "OUTPUT";
	private static final String KEY_COLOR = "COLOR";

	@SuppressWarnings("unused") @InjectView(R.id.title) TextView titleView;
	@SuppressWarnings("unused") @InjectView(R.id.output) TextView outputView;
	@SuppressWarnings("unused") @InjectView(R.id.num_delete) View deleteView;
	private OnClickListener clickListener;
	private int maxLength = 10;

	public interface OnClickListener {
		void onDoneClick(String output);
	}

	public static NumberPadDialogFragment newInstance(String title, String output, String colorDescription) {
		final NumberPadDialogFragment fragment = new NumberPadDialogFragment();
		Bundle args = new Bundle();
		args.putString(KEY_TITLE, title);
		if (StringUtils.isNumeric(output)) {
			args.putString(KEY_OUTPUT, output);
		}
		int c = ColorUtils.parseColor(colorDescription);
		if (c != Color.TRANSPARENT) {
			args.putInt(KEY_COLOR, c);
		}
		fragment.setArguments(args);
		return fragment;
	}

	public void setOnDoneClickListener(OnClickListener listener) {
		clickListener = listener;
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
		View view = inflater.inflate(R.layout.dialog_number_pad, container, false);
		ButterKnife.inject(this, view);

		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey(KEY_TITLE)) {
				titleView.setText(args.getString(KEY_TITLE));
			}
			if (args.containsKey(KEY_OUTPUT)) {
				outputView.setText(args.getString(KEY_OUTPUT));
				enableDelete();
			}
			if (args.containsKey(KEY_COLOR)) {
				int color = args.getInt(KEY_COLOR);
				titleView.setBackgroundColor(color);
				if (color != ColorUtils.TRANSPARENT && ColorUtils.isColorDark(color)) {
					titleView.setTextColor(Color.WHITE);
				} else {
					titleView.setTextColor(Color.BLACK);
				}
			}
		}

		return view;
	}

	@SuppressWarnings("unused")
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
		final CharSequence text = outputView.getText();
		if (text.length() < maxLength) {
			v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			outputView.setText(text.toString() + ((TextView) v).getText());
			enableDelete();
		}
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.num_done)
	void onDoneClick(@SuppressWarnings("UnusedParameters") View v) {
		if (clickListener != null) {
			clickListener.onDoneClick(outputView.getText().toString());
		}
		dismiss();
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.num_delete)
	void onDeleteClick(View v) {
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		CharSequence output = outputView.getText();
		if (output.length() > 0) {
			outputView.setText(output.subSequence(0, output.length() - 1));
			enableDelete();
		}
	}

	@SuppressWarnings({ "unused", "SameReturnValue" })
	@OnLongClick(R.id.num_delete)
	boolean onDeleteLongClick() {
		outputView.setText("");
		enableDelete();
		return true;
	}

	private void enableDelete() {
		deleteView.setEnabled(outputView.length() > 0);
	}
}
