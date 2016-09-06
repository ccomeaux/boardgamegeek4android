package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;

import com.boardgamegeek.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class UpdateBuddyNicknameDialogFragment extends DialogFragment {
	public interface UpdateBuddyNicknameDialogListener {
		void onFinishEditDialog(String newNickname, boolean shouldUpdatePlays);
	}

	private static final String KEY_TITLE_ID = "title_id";
	@StringRes private int titleResId;
	private ViewGroup root;
	private UpdateBuddyNicknameDialogListener listener;

	private Unbinder unbinder;
	@BindView(R.id.edit_nickname) EditText editText;
	@BindView(R.id.change_plays) CheckBox changePlays;
	private String nickname;

	@NonNull
	public static UpdateBuddyNicknameDialogFragment newInstance(
		@StringRes int titleResId,
		@Nullable ViewGroup root,
		UpdateBuddyNicknameDialogListener listener) {

		UpdateBuddyNicknameDialogFragment fragment = new UpdateBuddyNicknameDialogFragment();
		fragment.initialize(titleResId, root, listener);
		return fragment;
	}

	private void initialize(
		@StringRes int titleResId,
		@Nullable ViewGroup root,
		UpdateBuddyNicknameDialogListener listener) {

		this.titleResId = titleResId;
		this.root = root;
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
		View rootView = layoutInflater.inflate(R.layout.dialog_edit_nickname, root, false);
		unbinder = ButterKnife.bind(this, rootView);

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
						listener.onFinishEditDialog(editText.getText().toString().trim(), changePlays.isChecked());
					}
				}
			});

		final AlertDialog dialog = builder.create();
		int inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS;
		editText.setInputType(editText.getInputType() | inputType);
		requestFocus(dialog);
		return dialog;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	private void setAndSelectExistingText() {
		if (editText != null && !TextUtils.isEmpty(nickname)) {
			editText.setText(nickname);
			editText.setSelection(0, nickname.length());
		}
	}

	private void requestFocus(@NonNull AlertDialog dialog) {
		editText.requestFocus();
		dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}
}
