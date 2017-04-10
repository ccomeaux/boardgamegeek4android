package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ResolverUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SaveViewDialogFragment extends DialogFragment {
	private static final String KEY_NAME = "title_id";
	private static final String KEY_DESCRIPTION = "color_count";

	private Context context;
	private OnViewSavedListener listener;
	private String name;
	private String description;

	private Unbinder unbinder;
	@BindView(R.id.name) EditText nameView;
	@BindView(R.id.default_view) CheckBox defaultView;
	@BindView(R.id.description) TextView descriptionView;

	public static SaveViewDialogFragment newInstance(Context context, String name, String description) {
		SaveViewDialogFragment dialogFragment = new SaveViewDialogFragment();
		dialogFragment.context = context;
		dialogFragment.setArguments(name, description);
		return dialogFragment;
	}

	public void setArguments(String name, String description) {
		Bundle bundle = new Bundle();
		bundle.putString(KEY_NAME, name);
		bundle.putString(KEY_DESCRIPTION, description);
		setArguments(bundle);
	}

	public interface OnViewSavedListener {
		void onInsertRequested(String name, boolean isDefault);

		void onUpdateRequested(String name, boolean isDefault, long viewId);
	}

	public void setOnViewSavedListener(OnViewSavedListener listener) {
		this.listener = listener;
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View layout = LayoutInflater.from(context).inflate(R.layout.dialog_save_view, null);
		unbinder = ButterKnife.bind(this, layout);

		if (getArguments() != null) {
			name = getArguments().getString(KEY_NAME);
			description = getArguments().getString(KEY_DESCRIPTION);
		}

		tryBindUi();

		AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert)
			.setTitle(R.string.title_save_view)
			.setView(layout)
			.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String name = nameView.getText().toString().trim();
					final boolean isDefault = defaultView.isChecked();

					final long viewId = findViewId(name);
					if (viewId > 0) {
						new AlertDialog.Builder(context)
							.setTitle(R.string.title_collection_view_name_in_use)
							.setMessage(R.string.msg_collection_view_name_in_use)
							.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (listener != null) listener.onUpdateRequested(name, isDefault, viewId);
								}
							})
							.setNegativeButton(R.string.create, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (listener != null) listener.onInsertRequested(name, isDefault);
								}
							})
							.create()
							.show();
					} else {
						if (listener != null) listener.onInsertRequested(name, isDefault);
					}
				}
			})
			.setNegativeButton(R.string.cancel, null)
			.setCancelable(true);
		final AlertDialog dialog = builder.create();
		nameView.requestFocus();
		Window window = dialog.getWindow();
		if (window != null) {
			window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE | LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				enableSaveButton(dialog, nameView);
			}
		});
		return dialog;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private void tryBindUi() {
		if (isAdded()) {
			PresentationUtils.setAndSelectExistingText(nameView, name);
			long viewDefaultId = PreferencesUtils.getViewDefaultId(context);
			defaultView.setChecked(viewDefaultId != -1 && findViewId(name) == viewDefaultId);
			descriptionView.setText(description);
		}
	}

	private long findViewId(String name) {
		if (TextUtils.isEmpty(name)) return BggContract.INVALID_ID;
		return ResolverUtils.queryLong(context.getContentResolver(),
			CollectionViews.CONTENT_URI,
			CollectionViews._ID, 0,
			CollectionViews.NAME + "=?",
			new String[] { name });
	}

	private static void enableSaveButton(final AlertDialog dialog, final EditText nameView) {
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(nameView.getText().toString().trim().length() > 0);
		nameView.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
					nameView.getText().toString().trim().length() > 0);
			}
		});
	}
}
