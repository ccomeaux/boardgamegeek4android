package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

import com.boardgamegeek.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PrivateInfoDialogFragment extends DialogFragment {
	public interface PrivateInfoDialogListener {
		void onFinishEditDialog(String comment);
	}

	private ViewGroup root;
	private PrivateInfoDialogListener listener;

	@SuppressWarnings("unused") @Bind(R.id.comment) EditText commentView;
	private String existingComment;

	@NonNull
	public static PrivateInfoDialogFragment newInstance(@Nullable ViewGroup root, PrivateInfoDialogListener listener) {
		PrivateInfoDialogFragment fragment = new PrivateInfoDialogFragment();
		fragment.initialize(root, listener);
		return fragment;
	}

	private void initialize(@Nullable ViewGroup root, PrivateInfoDialogListener listener) {
		this.root = root;
		this.listener = listener;
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		View rootView = layoutInflater.inflate(R.layout.dialog_private_info, root, false);
		ButterKnife.bind(this, rootView);

		setAndSelectExistingText();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.title_private_info);
		builder.setView(rootView)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) {
						listener.onFinishEditDialog(commentView.getText().toString().trim());
					}
				}
			});

		final AlertDialog dialog = builder.create();
		int inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
		commentView.setInputType(commentView.getInputType() | inputType);
		requestFocus(dialog);
		return dialog;
	}

	public void setComment(String comment) {
		this.existingComment = comment;
	}

	private void setAndSelectExistingText() {
		if (commentView != null && !TextUtils.isEmpty(existingComment)) {
			commentView.setText(existingComment);
			commentView.setSelection(0, existingComment.length());
		}
	}

	private void requestFocus(@NonNull AlertDialog dialog) {
		commentView.requestFocus();
		dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}
}
