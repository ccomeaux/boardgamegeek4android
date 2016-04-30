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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class DataFragment extends Fragment {
	private static final int REQUEST_EXPORT = 0;
	private Unbinder unbinder;
	@BindView(R.id.backup_location) TextView fileLocationView;
	@BindView(R.id.backup_types) ViewGroup fileTypesView;
	@BindView(R.id.progress_container) View progressContainer;
	@BindView(R.id.progress) ProgressBar progressBar;
	@BindView(R.id.progress_detail) TextView progressDetailView;
	private ImporterExporterTask task;

	@DebugLog
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_data, container, false);

		unbinder = ButterKnife.bind(this, root);

		fileLocationView.setText(FileUtils.getExportPath(false).getPath());

		task = new ImporterExporterTask(getActivity(), false);
		for (Step step : task.getSteps()) {
			TextView textView = new TextView(getActivity());
			textView.setText(getString(R.string.backup_description, step.getDescription(getActivity()), step.getFileName()));
			fileTypesView.addView(textView);
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

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@DebugLog
	@OnClick(R.id.export_button)
	public void onExportClick() {
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
	@OnClick(R.id.import_button)
	public void onImportClick() {
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
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ExportFinishedEvent event) {
		notifyEnd(event.getMessageId());
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe
	public void onEvent(ImportFinishedEvent event) {
		notifyEnd(event.getMessageId());
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ExportProgressEvent event) {
		if (progressBar != null) {
			progressBar.setMax(event.getTotalCount());
			progressBar.setProgress(event.getCurrentCount());
		}
		if (progressDetailView != null && task != null && event.getStepIndex() < task.getSteps().size()) {
			String description = task.getSteps().get(event.getStepIndex()).getDescription(getActivity());
			progressDetailView.setText(description);
		}
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(ImportProgressEvent event) {
		if (progressBar != null) {
			progressBar.setMax(event.getTotalCount());
			progressBar.setProgress(event.getCurrentCount());
		}
	}

	private void initProgressBar() {
		if (progressContainer != null) {
			progressContainer.setVisibility(View.VISIBLE);
		}
		if (progressBar != null) {
			progressBar.setMax(1);
			progressBar.setProgress(0);
		}
	}

	private void notifyEnd(int messageId) {
		View v = getView();
		if (v != null) {
			Snackbar.make(v, messageId, Snackbar.LENGTH_LONG).show();
		}

		progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		progressContainer.setVisibility(View.GONE);
	}
}
