package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

import com.boardgamegeek.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class EditTextDialogFragment extends DialogFragment {
	public interface EditTextDialogListener {
		void onFinishEditDialog(String inputText);
	}

	private static final String KEY_TITLE_ID = "title_id";
	@StringRes private int titleResId;
	@SuppressWarnings("unused") @InjectView(R.id.edit_text) EditText editText;
	private EditTextDialogListener listener;
	private String existingText;

	public static EditTextDialogFragment newInstance(@StringRes int titleResId, EditTextDialogListener listener) {
		EditTextDialogFragment fragment = new EditTextDialogFragment();
		fragment.initialize(titleResId, listener);
		return fragment;
	}

	private void initialize(@StringRes int titleResId, EditTextDialogListener listener) {
		this.titleResId = titleResId;
		this.listener = listener;
		setArguments(this.titleResId);
	}

	public void setArguments(@StringRes int titleResId) {
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_TITLE_ID, titleResId);
		setArguments(bundle);
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		View rootView = layoutInflater.inflate(R.layout.dialog_edit_text, null);
		ButterKnife.inject(this, rootView);

		if (getArguments() != null) {
			titleResId = getArguments().getInt(KEY_TITLE_ID);
		}

		setAndSelectExistingText();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if (titleResId > 0) {
			builder.setTitle(titleResId);
		}
		builder.setView(rootView)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) {
						listener.onFinishEditDialog(editText.getText().toString().trim());
					}
				}
			});

		final AlertDialog dialog = builder.create();
		editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		requestFocus(dialog);
		return dialog;
	}

	public void setText(String text) {
		this.existingText = text;
	}

	private void setAndSelectExistingText() {
		if (editText != null && !TextUtils.isEmpty(existingText)) {
			editText.setText(existingText);
			editText.setSelection(0, existingText.length());
		}
	}

	private void requestFocus(AlertDialog dialog) {
		editText.requestFocus();
		dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}
}
