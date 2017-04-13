package com.boardgamegeek.ui;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.export.ImporterExporterTask;
import com.boardgamegeek.export.JsonExportTask;
import com.boardgamegeek.export.JsonImportTask;
import com.boardgamegeek.export.Step;
import com.boardgamegeek.ui.widget.DataStepRow;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.TaskUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class DataFragment extends Fragment implements OnSharedPreferenceChangeListener {
	private static final int REQUEST_EXPORT = 0;
	private static final int REQUEST_TASK_FIRST = 100;
	private static final String ANSWERS_EVENT_NAME = "DataManagement";
	private static final String ANSWERS_ATTRIBUTE_KEY_ACTION = "Action";
	private Unbinder unbinder;
	@BindView(R.id.backup_types) ViewGroup fileTypesView;
	@BindView(R.id.progress_container) View progressContainer;
	@BindView(R.id.progress) ProgressBar progressBar;
	@BindView(R.id.progress_detail) TextView progressDetailView;
	private ImporterExporterTask task;
	private final Map<Integer, String> requestCodePreferenceKeys = new ArrayMap<>();

	@DebugLog
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_data, container, false);

		unbinder = ButterKnife.bind(this, root);

		task = new ImporterExporterTask(getActivity());
		for (int i = 0; i < task.getSteps().size(); i++) {
			int requestCode = REQUEST_TASK_FIRST + i;
			Step step = task.getSteps().get(i);
			requestCodePreferenceKeys.put(requestCode, step.getPreferenceKey());
			DataStepRow row = new DataStepRow(this);
			row.bind(step, requestCode);
			row.setTag(step.getPreferenceKey());
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

	@Override
	public void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
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

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK || !isAdded() || data == null) return;

		if (requestCodePreferenceKeys.containsKey(requestCode)) {
			Uri uri = data.getData();

			if (uri == null) return;

			try {
				int modeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				getContext().getContentResolver().takePersistableUriPermission(uri, modeFlags);
			} catch (SecurityException e) {
				Timber.e(e, "Could not persist URI permissions for '%s'.", uri.toString());
			}

			String key = requestCodePreferenceKeys.get(requestCode);
			if (TextUtils.isEmpty(key)) return;

			PreferencesUtils.putUri(getContext(), key, uri);
			updateFileName(key);
		}
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
						View view = getView();
						if (view != null) {
							Snackbar.make(view, R.string.msg_export_permission_rationale, Snackbar.LENGTH_INDEFINITE).show();
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
				View view = getView();
				if (view != null) {
					Snackbar.make(view, R.string.msg_export_permission_denied, Snackbar.LENGTH_LONG).show();
				}
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@DebugLog
	private void export() {
		initProgressBar();
		TaskUtils.executeAsyncTask(new JsonExportTask(getContext()));
		Answers.getInstance().logCustom(new CustomEvent(ANSWERS_EVENT_NAME).putCustomAttribute(ANSWERS_ATTRIBUTE_KEY_ACTION, "Export"));
	}

	@DebugLog
	@OnClick(R.id.import_button)
	public void onImportClick() {
		DialogUtils.createConfirmationDialog(getActivity(), R.string.msg_import_confirmation, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				initProgressBar();
				TaskUtils.executeAsyncTask(new JsonImportTask(getContext()));
				Answers.getInstance().logCustom(new CustomEvent(ANSWERS_EVENT_NAME).putCustomAttribute(ANSWERS_ATTRIBUTE_KEY_ACTION, "Import"));
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
			if (event.getTotalCount() < 0) {
				progressBar.setIndeterminate(true);
			} else {
				progressBar.setIndeterminate(false);
				progressBar.setMax(event.getTotalCount());
				progressBar.setProgress(event.getCurrentCount());
			}
		}
		if (progressDetailView != null &&
			task != null &&
			event.getStepIndex() < task.getSteps().size()) {
			String description = task.getSteps().get(event.getStepIndex()).getDescription(getActivity());
			progressDetailView.setText(description);
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
		if (progressDetailView != null) {
			progressDetailView.setText("");
		}
	}

	private void notifyEnd(int messageId) {
		View view = getView();
		if (view != null) Snackbar.make(view, messageId, Snackbar.LENGTH_LONG).show();
		progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		progressContainer.setVisibility(View.GONE);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updateFileName(key);
	}

	private void updateFileName(String key) {
		if (TextUtils.isEmpty(key)) return;
		for (int i = 0; i < fileTypesView.getChildCount(); i++) {
			View view = fileTypesView.getChildAt(i);
			if (key.equals(view.getTag())) {
				((DataStepRow) view).setFileNameView(PreferencesUtils.getUri(getContext(), key));
				return;
			}
		}
	}
}
