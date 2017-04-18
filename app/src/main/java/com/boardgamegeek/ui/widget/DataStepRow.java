package com.boardgamegeek.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.export.ImporterExporterTask;
import com.boardgamegeek.export.Step;
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
	@BindDimen(R.dimen.padding_half) int verticalPadding;
	@BindDimen(R.dimen.view_row_height) int minimumHeight;

	private final Unbinder unbinder;
	private int requestCode;
	private Listener listener;

	public interface Listener {
		void onExportClicked(int requestCode);

		void onImportClicked(int requestCode);
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

	public void bind(Step step, int requestCode) {
		this.requestCode = requestCode;
		descriptionView.setText(step.getDescription(getContext()));
		if (ImporterExporterTask.shouldUseDefaultFolders()) {
			fileNameView.setText(FileUtils.getExportFile(step.getName()).toString());
			fileNameView.setVisibility(VISIBLE);
		} else {
			fileNameView.setVisibility(GONE);
		}
	}

	public void initProgressBar() {
		if (progressBar != null) {
			progressBar.setMax(1);
			progressBar.setProgress(0);
			AnimationUtils.fadeIn(progressBar);
		}
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
	}

	@OnClick(R.id.export_button)
	void onExportClick() {
		if (listener != null) listener.onExportClicked(requestCode);
	}

	@OnClick(R.id.import_button)
	void onImportClick() {
		if (listener != null) listener.onImportClicked(requestCode);
	}
}
