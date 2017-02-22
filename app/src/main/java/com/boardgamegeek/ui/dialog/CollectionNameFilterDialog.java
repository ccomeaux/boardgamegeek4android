package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.CollectionNameFilter;
import com.boardgamegeek.interfaces.CollectionView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CollectionNameFilterDialog implements CollectionFilterDialog {
	@BindView(R.id.name) EditText filterTextView;
	@BindView(R.id.starts_with) CheckBox startWithCheckBox;

	@Override
	public void createDialog(final Context context, final CollectionView view, CollectionFilterer filter) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View layout = inflater.inflate(R.layout.dialog_collection_filter_name, null);
		ButterKnife.bind(this, layout);
		initializeUi(filter);
		AlertDialog alertDialog = createAlertDialog(context, view, layout);
		requestFocus(alertDialog);
		alertDialog.show();
	}

	private AlertDialog createAlertDialog(final Context context, final CollectionView view, View layout) {
		return new Builder(context)
			.setTitle(R.string.menu_collection_name)
			.setPositiveButton(R.string.set, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.addFilter(new CollectionNameFilter(context, filterTextView.getText(), startWithCheckBox.isChecked()));
				}
			})
			.setNegativeButton(R.string.clear, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.removeFilter(new CollectionNameFilter(context).getType());
				}
			})
			.setView(layout)
			.create();
	}

	private void initializeUi(CollectionFilterer filter) {
		CollectionNameFilter collectionNameFilter = (CollectionNameFilter) filter;
		if (collectionNameFilter != null) {
			filterTextView.setText(collectionNameFilter.getFilterText());
			filterTextView.setSelection(0, filterTextView.getText().length());

			startWithCheckBox.setChecked(collectionNameFilter.startsWith());
		}
	}

	private void requestFocus(AlertDialog alertDialog) {
		filterTextView.requestFocus();
		Window window = alertDialog.getWindow();
		if (window != null) window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}

	@Override
	public int getType(Context context) {
		return new CollectionNameFilter(context).getType();
	}
}
