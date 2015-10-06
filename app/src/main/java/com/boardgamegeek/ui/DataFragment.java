package com.boardgamegeek.ui;

import android.Manifest.permission;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.events.ImportProgressEvent;
import com.boardgamegeek.export.ImporterExporterTask;
import com.boardgamegeek.export.JsonExportTask;
import com.boardgamegeek.export.JsonImportTask;
import com.boardgamegeek.export.Step;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.TaskUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class DataFragment extends Fragment {
	private static final int REQUEST_EXPORT = 0;
	@SuppressWarnings("unused") @InjectView(R.id.backup_location) TextView mFileLocationView;
	@SuppressWarnings("unused") @InjectView(R.id.backup_types) ViewGroup mFileTypes;
	@SuppressWarnings("unused") @InjectView(R.id.progress_container) View mProgressContainer;
	@SuppressWarnings("unused") @InjectView(R.id.progress) ProgressBar mProgressBar;
	@SuppressWarnings("unused") @InjectView(R.id.progress_detail) TextView mProgressDetail;
	private ImporterExporterTask mTask;

	@DebugLog
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_data, container, false);

		ButterKnife.inject(this, root);

		mFileLocationView.setText(FileUtils.getExportPath(false).getPath());

		mTask = new ImporterExporterTask(getActivity(), false);
		for (Step step : mTask.getSteps()) {
			TextView textView = new TextView(getActivity());
			textView.setText(getString(R.string.backup_description, step.getDescription(getActivity()), step.getFileName()));
			mFileTypes.addView(textView);
		}

		return root;
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@DebugLog
	@SuppressWarnings("unused")
	@OnClick(R.id.export_button)
	public void onExportClick(View view) {
		DialogUtils.createConfirmationDialog(getActivity(), R.string.msg_export_confirmation, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (PackageManager.PERMISSION_GRANTED ==
					ContextCompat.checkSelfPermission(getActivity(), permission.WRITE_EXTERNAL_STORAGE)) {
					export();
				} else {
					if (shouldShowRequestPermissionRationale(permission.WRITE_EXTERNAL_STORAGE)) {
						View v = getView();
						if (v != null) {
							Snackbar.make(v, R.string.msg_export_permission_rationale, Snackbar.LENGTH_INDEFINITE).show();
						}
					}
					requestPermissions(new String[] { permission.WRITE_EXTERNAL_STORAGE }, REQUEST_EXPORT);
				}
			}
		}).show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_EXPORT) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				export();
			} else {
				View v = getView();
				if (v != null) {
					Snackbar.make(v, R.string.msg_export_permission_denied, Snackbar.LENGTH_LONG).show();
				}
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@DebugLog
	private void export() {
		initProgressBar();
		TaskUtils.executeAsyncTask(new JsonExportTask(getContext(), false));
	}

	@DebugLog
	@SuppressWarnings("unused")
	@OnClick(R.id.import_button)
	public void onImportClick(View view) {
		DialogUtils.createConfirmationDialog(getActivity(), R.string.msg_import_confirmation, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				initProgressBar();
				TaskUtils.executeAsyncTask(new JsonImportTask(getContext(), false));
			}
		}).show();
	}

	@DebugLog
	@SuppressWarnings("unused")
	public void onEventMainThread(ExportFinishedEvent event) {
		notifyEnd(event.messageId);
	}

	@DebugLog
	@SuppressWarnings("unused")
	public void onEventMainThread(ImportFinishedEvent event) {
		notifyEnd(event.messageId);
	}

	@DebugLog
	@SuppressWarnings("unused")
	public void onEventMainThread(ExportProgressEvent event) {
		if (mProgressBar != null) {
			mProgressBar.setMax(event.totalCount);
			mProgressBar.setProgress(event.currentCount);
		}
		if (mProgressDetail != null && mTask != null && event.stepIndex < mTask.getSteps().size()) {
			String description = mTask.getSteps().get(event.stepIndex).getDescription(getActivity());
			mProgressDetail.setText(description);
		}
	}

	@DebugLog
	@SuppressWarnings("unused")
	public void onEventMainThread(ImportProgressEvent event) {
		if (mProgressBar != null) {
			mProgressBar.setMax(event.totalCount);
			mProgressBar.setProgress(event.currentCount);
		}
	}

	private void initProgressBar() {
		if (mProgressContainer != null) {
			mProgressContainer.setVisibility(View.VISIBLE);
		}
		if (mProgressBar != null) {
			mProgressBar.setMax(1);
			mProgressBar.setProgress(0);
		}
	}

	private void notifyEnd(int messageId) {
		View v = getView();
		if (v != null) {
			Snackbar.make(v, messageId, Snackbar.LENGTH_LONG).show();
		}

		mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		mProgressContainer.setVisibility(View.GONE);
	}
}
