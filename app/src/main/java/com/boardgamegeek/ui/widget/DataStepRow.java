package com.boardgamegeek.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.FileUtils;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

@SuppressLint("ViewConstructor")
public class DataStepRow extends LinearLayout {
	@BindView(R.id.description) TextView descriptionView;
	@BindView(R.id.file_name) TextView fileNameView;
	@BindView(R.id.progress) ProgressBar progressBar;
	@BindView(R.id.export_button) Button exportButton;
	@BindView(R.id.import_button) Button importButton;
	@BindDimen(R.dimen.padding_half) int verticalPadding;
	@BindDimen(R.dimen.view_row_height) int minimumHeight;

	private final Unbinder unbinder;
	private String type;
	private Listener listener;

	public interface Listener {
		void onExportClicked(String tag);

		void onImportClicked(String type);
	}

	public DataStepRow(Context context) {
		super(context);

		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		setGravity(Gravity.CENTER_VERTICAL);

		LayoutInflater.from(getContext()).inflate(R.layout.widget_data_step_row, this, true);
		unbinder = ButterKnife.bind(this);

		setOrientation(VERTICAL);
		setPadding(0, verticalPadding, 0, verticalPadding);
		setMinimumHeight(minimumHeight);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}


	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (unbinder != null) unbinder.unbind();
	}

	public void bind(String type, @StringRes int descriptionResId) {
		this.type = type;
		descriptionView.setText(descriptionResId);
		if (FileUtils.shouldUseDefaultFolders()) {
			fileNameView.setText(FileUtils.getExportFile(type).toString());
			fileNameView.setVisibility(VISIBLE);
		} else {
			fileNameView.setVisibility(GONE);
		}
	}

	public void initProgressBar() {
		if (progressBar != null) {
			progressBar.setIndeterminate(false);
			AnimationUtils.fadeIn(progressBar);
		}
		if (importButton != null) importButton.setEnabled(false);
		if (exportButton != null) exportButton.setEnabled(false);
	}

	public void updateProgressBar(int max, int progress) {
		if (progressBar != null) {
			if (max < 0) {
				progressBar.setIndeterminate(true);
			} else {
				progressBar.setIndeterminate(false);
				progressBar.setMax(max);
				progressBar.setProgress(progress);
			}
		}
	}

	public void hideProgressBar() {
		AnimationUtils.fadeOutToInvisible(progressBar);
		if (importButton != null) importButton.setEnabled(true);
		if (exportButton != null) exportButton.setEnabled(true);
	}

	@OnClick(R.id.export_button)
	void onExportClick() {
		if (listener != null) listener.onExportClicked(type);
	}

	@OnClick(R.id.import_button)
	void onImportClick() {
		if (listener != null) listener.onImportClicked(type);
	}
}
