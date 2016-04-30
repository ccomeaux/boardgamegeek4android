package com.boardgamegeek.ui.dialog;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class NumberPadDialogFragment extends DialogFragment {
	private static final String KEY_TITLE = "TITLE";
	private static final String KEY_OUTPUT = "OUTPUT";
	private static final String KEY_COLOR = "COLOR";
	private static final int MAX_LENGTH = 10;

	@Bind(R.id.title) TextView titleView;
	@Bind(R.id.output) TextView outputView;
	@Bind(R.id.num_delete) View deleteView;
	private OnClickListener clickListener;
	private double minValue = 0.0;
	private double maxValue = Double.MAX_VALUE;
	private int maxMantissa = MAX_LENGTH;

	public interface OnClickListener {
		void onDoneClick(String output);
	}

	public static NumberPadDialogFragment newInstance(String title, String output) {
		return newInstance(title, output, null);
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

	public void setMinValue(double value) {
		minValue = value;
	}

	public void setMaxValue(double value) {
		maxValue = value;
	}

	public void setMaxMantissa(int value) {
		maxMantissa = value;
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
		ButterKnife.bind(this, view);

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
		String output = outputView.getText().toString() + ((TextView) v).getText();
		if (isWithinLength(output) && isWithinRange(output)) {
			maybeBuzz(v);
			outputView.setText(output);
			enableDelete();
		}
	}

	@OnClick(R.id.num_done)
	void onDoneClick(@SuppressWarnings("UnusedParameters") View v) {
		if (clickListener != null) {
			clickListener.onDoneClick(outputView.getText().toString());
		}
		dismiss();
	}

	@OnClick(R.id.num_delete)
	void onDeleteClick(View v) {
		final CharSequence text = outputView.getText();
		if (text.length() > 0) {
			String output = text.subSequence(0, text.length() - 1).toString();
			if (isWithinLength(output) && isWithinRange(output)) {
				maybeBuzz(v);
				outputView.setText(output);
				enableDelete();
			}
		}
	}

	private void maybeBuzz(View v) {
		if (PreferencesUtils.getHapticFeedback(getContext())) {
			v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		}
	}

	@SuppressWarnings("SameReturnValue")
	@OnLongClick(R.id.num_delete)
	boolean onDeleteLongClick() {
		outputView.setText("");
		enableDelete();
		return true;
	}

	private boolean isWithinLength(String text) {
		return TextUtils.isEmpty(text) || text.length() <= MAX_LENGTH && getMantissaLength(text) <= maxMantissa;
	}

	private int getMantissaLength(String text) {
		if (!text.contains(".")) {
			return 0;
		}
		String[] parts = text.split("\\.");
		if (parts.length > 1) {
			return parts[1].length();
		}
		return 0;
	}

	private boolean isWithinRange(String text) {
		if (TextUtils.isEmpty(text) || ".".equals(text)) {
			return true;
		}
		if (hasTwoDecimalPoints(text)) {
			return false;
		}
		double value = parseDouble(text);
		return value >= minValue && value <= maxValue;
	}

	private boolean hasTwoDecimalPoints(String text) {
		if (text == null) {
			return false;
		}
		int decimalIndex = text.indexOf('.');
		if (decimalIndex >= 0) {
			return text.indexOf('.', decimalIndex + 1) >= 0;
		}
		return false;
	}

	private double parseDouble(String text) {
		if (TextUtils.isEmpty(text) || ".".equals(text)) {
			return 0.0;
		}
		if (text.endsWith(".")) {
			return Double.parseDouble(text + "0");
		}
		if (text.startsWith(".")) {
			return Double.parseDouble("0" + text);
		}
		return Double.parseDouble(text);
	}

	private void enableDelete() {
		deleteView.setEnabled(outputView.length() > 0);
	}
}
