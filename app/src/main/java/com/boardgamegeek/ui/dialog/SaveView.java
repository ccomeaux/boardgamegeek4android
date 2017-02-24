package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
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

public class SaveView {
	private final Context context;
	private OnViewSavedListener listener;

	private final View layout;
	@BindView(R.id.name) EditText nameView;
	@BindView(R.id.default_view) CheckBox defaultView;
	@BindView(R.id.description) TextView descriptionView;

	public SaveView(Context context) {
		this.context = context;
		layout = LayoutInflater.from(context).inflate(R.layout.dialog_save_view, null);
		ButterKnife.bind(this, layout);
	}

	public interface OnViewSavedListener {
		void onInsertRequested(String name, boolean isDefault);

		void onUpdateRequested(String name, boolean isDefault, long viewId);
	}

	public void setOnViewSavedListener(OnViewSavedListener listener) {
		this.listener = listener;
	}

	public void createDialog(String name, String description) {
		PresentationUtils.setAndSelectExistingText(nameView, name);
		defaultView.setChecked(findViewId(name) == PreferencesUtils.getViewDefaultId(context));
		descriptionView.setText(description);

		AlertDialog.Builder builder = new AlertDialog.Builder(context)
			.setTitle(R.string.title_save_view)
			.setView(layout)
			.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String name = nameView.getText().toString().trim();
					final boolean isDefault = defaultView.isChecked();

					final long viewId = findViewId(name);
					if (viewId > 0) {
						new AlertDialog.Builder(context).setTitle(R.string.title_collection_view_name_in_use)
							.setMessage(R.string.msg_collection_view_name_in_use)
							.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (listener != null) {
										listener.onUpdateRequested(name, isDefault, viewId);
									}
								}
							})
							.setNegativeButton(R.string.create, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (listener != null) {
										listener.onInsertRequested(name, isDefault);
									}
								}
							})
							.create()
							.show();
					} else {
						if (listener != null) {
							listener.onInsertRequested(name, isDefault);
						}
					}
				}
			}).setNegativeButton(R.string.cancel, null).setCancelable(true);
		final AlertDialog dialog = builder.create();
		Window window = dialog.getWindow();
		if (window != null) window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				enableSaveButton(dialog, nameView);
			}
		});
		dialog.show();
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
