package com.boardgamegeek.ui;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.export.CollectionViewStep;
import com.boardgamegeek.export.GameStep;
import com.boardgamegeek.export.ImporterExporterTask;
import com.boardgamegeek.export.JsonExportTask;
import com.boardgamegeek.export.JsonImportTask;
import com.boardgamegeek.export.Step;
import com.boardgamegeek.export.UserStep;
import com.boardgamegeek.ui.widget.DataStepRow;
import com.boardgamegeek.ui.widget.DataStepRow.Listener;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.TaskUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class DataFragment extends Fragment implements Listener {
	private static final int REQUEST_TASK_FIRST = 100;
	private static final int REQUEST_IMPORT_OFFSET = 10;
	private static final String ANSWERS_EVENT_NAME = "DataManagement";
	private static final String ANSWERS_ATTRIBUTE_KEY_ACTION = "Action";
	private Unbinder unbinder;
	@BindView(R.id.backup_types) ViewGroup fileTypesView;
	@BindView(R.id.progress_container) View progressContainer;
	@BindView(R.id.progress) ProgressBar progressBar;
	private final Map<Integer, Step> requestCodeSteps = new ArrayMap<>();
	private Step currentStep;

	@DebugLog
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_data, container, false);

		unbinder = ButterKnife.bind(this, root);

		List<Step> steps = new ArrayList<>();
		steps.add(new CollectionViewStep());
		steps.add(new GameStep());
		steps.add(new UserStep());
		for (int i = 0; i < steps.size(); i++) {
			int requestCode = REQUEST_TASK_FIRST + i;
			Step step = steps.get(i);
			requestCodeSteps.put(requestCode, step);
			DataStepRow row = new DataStepRow(getContext());
			row.setListener(this);
			row.bind(step, requestCode);
			fileTypesView.addView(row);
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

	@Override
	public void onExportClicked(final int requestCode) {
		if (requestCodeSteps.containsKey(requestCode)) {
			currentStep = requestCodeSteps.get(requestCode);

			if (ImporterExporterTask.shouldUseDefaultFolders()) {
				DialogUtils.createConfirmationDialog(getActivity(),
					R.string.msg_export_confirmation,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (PackageManager.PERMISSION_GRANTED ==
								ContextCompat.checkSelfPermission(getActivity(), permission.WRITE_EXTERNAL_STORAGE)) {
								export(currentStep, null);
							} else {
								if (shouldShowRequestPermissionRationale(permission.WRITE_EXTERNAL_STORAGE)) {
									showSnackbar(R.string.msg_export_permission_rationale);
								}
								requestPermissions(new String[] { permission.WRITE_EXTERNAL_STORAGE }, requestCode);
							}
						}
					}).show();
			} else {
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("text/json");
				intent.putExtra(Intent.EXTRA_TITLE, currentStep.getFileName());
				startActivityForResult(intent, requestCode);
			}
		}
	}

	@Override
	public void onImportClicked(int requestCode) {
		if (requestCodeSteps.containsKey(requestCode)) {
			currentStep = requestCodeSteps.get(requestCode);

			if (ImporterExporterTask.shouldUseDefaultFolders()) {
				DialogUtils.createConfirmationDialog(getActivity(),
					R.string.msg_import_confirmation,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							performImport(currentStep, null);
						}
					}).show();
			} else {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("text/json");
				intent.putExtra(Intent.EXTRA_TITLE, currentStep.getFileName());
				startActivityForResult(intent, requestCode + REQUEST_IMPORT_OFFSET);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK || !isAdded() || data == null) return;

		if (requestCodeSteps.containsKey(requestCode) ||
			requestCodeSteps.containsKey(requestCode - REQUEST_IMPORT_OFFSET)) {
			Uri uri = data.getData();

			if (uri == null) return;

			try {
				int modeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				getContext().getContentResolver().takePersistableUriPermission(uri, modeFlags);
			} catch (SecurityException e) {
				Timber.e(e, "Could not persist URI permissions for '%s'.", uri.toString());
			}

			if (requestCodeSteps.containsKey(requestCode)) {
				export(requestCodeSteps.get(requestCode), uri);
			} else {
				performImport(requestCodeSteps.get(requestCode - REQUEST_IMPORT_OFFSET), uri);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCodeSteps.containsKey(requestCode)) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				export(currentStep, null);
			} else {
				showSnackbar(R.string.msg_export_permission_denied);
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@DebugLog
	private void export(Step step, Uri uri) {
		initProgressBar();
		TaskUtils.executeAsyncTask(new JsonExportTask(getContext(), step, uri));
		Answers.getInstance().logCustom(new CustomEvent(ANSWERS_EVENT_NAME).putCustomAttribute(ANSWERS_ATTRIBUTE_KEY_ACTION, "Export"));
	}

	@DebugLog
	private void performImport(Step step, Uri uri) {
		initProgressBar();
		TaskUtils.executeAsyncTask(new JsonImportTask(getContext(), step, uri));
		Answers.getInstance().logCustom(new CustomEvent(ANSWERS_EVENT_NAME).putCustomAttribute(ANSWERS_ATTRIBUTE_KEY_ACTION, "Import"));
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ExportFinishedEvent event) {
		notifyEnd(event.getErrorMessage(), R.string.msg_export_success, R.string.msg_export_failed);
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ImportFinishedEvent event) {
		notifyEnd(event.getErrorMessage(), R.string.msg_import_success, R.string.msg_import_failed);
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ExportProgressEvent event) {
		if (progressBar != null) {
			if (event.getTotalCount() < 0) {
				progressBar.setIndeterminate(true);
			} else {
				progressBar.setIndeterminate(false);
				progressBar.setMax(event.getTotalCount());
				progressBar.setProgress(event.getCurrentCount());
			}
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

	private void notifyEnd(String errorMessage, @StringRes int successResId, @StringRes int failureResId) {
		View view = getView();
		if (view != null) {
			if (TextUtils.isEmpty(errorMessage)) {
				Snackbar.make(view, successResId, Snackbar.LENGTH_LONG).show();
			} else {
				Snackbar.make(view, getString(failureResId) + "\n" + errorMessage, Snackbar.LENGTH_LONG).show();
			}
		}
		progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		progressContainer.setVisibility(View.GONE);
	}

	private void showSnackbar(@StringRes int messageResId) {
		View view = getView();
		if (view != null) {
			Snackbar.make(view, messageResId, Snackbar.LENGTH_LONG).show();
		}
	}
}
