package com.boardgamegeek.ui.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.export.ImporterExporterTask;
import com.boardgamegeek.export.Step;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.PreferencesUtils;

import java.io.File;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

@SuppressLint("ViewConstructor")
public class DataStepRow extends RelativeLayout {
	@BindView(R.id.description) TextView descriptionView;
	@BindView(R.id.file_name) TextView fileNameView;
	@BindView(R.id.select_file_button) Button selectFileButton;
	@BindDimen(R.dimen.padding_half) int verticalPadding;
	@BindDimen(R.dimen.view_row_height) int minimumHeight;

	private Fragment fragment;
	private final Unbinder unbinder;
	private String suggestedFileName;
	private int requestCode;

	public DataStepRow(Fragment fragment) {
		super(fragment.getContext());
		this.fragment = fragment;

		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		setGravity(Gravity.CENTER_VERTICAL);

		LayoutInflater.from(getContext()).inflate(R.layout.widget_data_step_row, this, true);
		unbinder = ButterKnife.bind(this);

		setPadding(0, verticalPadding, 0, verticalPadding);
		setMinimumHeight(minimumHeight);

		selectFileButton.setVisibility(ImporterExporterTask.shouldUseDefaultFolders() ? GONE : VISIBLE);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		fragment = null;
		if (unbinder != null) unbinder.unbind();
	}

	public void bind(Step step, int requestCode) {
		this.requestCode = requestCode;
		descriptionView.setText(step.getDescription(getContext()));
		suggestedFileName = step.getFileName();
		setFileNameView(PreferencesUtils.getUri(getContext(), step.getPreferenceKey()));
	}

	public void setFileNameView(Uri uri) {
		if (uri == null) {
			fileNameView.setText(new File(FileUtils.getExportPath(), suggestedFileName).toString());
		} else {
			fileNameView.setText(uri.toString());
		}
	}

	@TargetApi(VERSION_CODES.KITKAT)
	@OnClick(R.id.select_file_button)
	void onSelectFileClick() {
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("text/json");
		intent.putExtra(Intent.EXTRA_TITLE, suggestedFileName);
		fragment.startActivityForResult(intent, requestCode);
	}
}
