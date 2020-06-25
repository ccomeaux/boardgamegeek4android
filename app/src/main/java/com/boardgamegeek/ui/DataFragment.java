package com.boardgamegeek.ui;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.events.ImportProgressEvent;
import com.boardgamegeek.export.CollectionViewExportTask;
import com.boardgamegeek.export.CollectionViewImportTask;
import com.boardgamegeek.export.Constants;
import com.boardgamegeek.export.GameExportTask;
import com.boardgamegeek.export.GameImportTask;
import com.boardgamegeek.export.JsonExportTask;
import com.boardgamegeek.export.JsonImportTask;
import com.boardgamegeek.export.UserExportTask;
import com.boardgamegeek.export.UserImportTask;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.ui.widget.DataStepRow;
import com.boardgamegeek.ui.widget.DataStepRow.Listener;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.FileUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class DataFragment extends Fragment implements Listener {
	private static final int REQUEST_EXPORT = 1000;
	private static final int REQUEST_IMPORT = 2000;
	private static final int REQUEST_PERMISSIONS = 3000;
	private static final String ANSWERS_EVENT_NAME = "DataManagement";
	private static final String ANSWERS_ATTRIBUTE_KEY_ACTION = "Action";

	private FirebaseAnalytics firebaseAnalytics;

	private Unbinder unbinder;
	@BindView(R.id.backup_types) ViewGroup fileTypesView;
	private String currentType;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());
	}

	@DebugLog
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_data, container, false);

		unbinder = ButterKnife.bind(this, root);
		createDataRow(Constants.TYPE_COLLECTION_VIEWS, R.string.backup_type_collection_view, R.string.backup_description_collection_view);
		createDataRow(Constants.TYPE_GAMES, R.string.backup_type_game, R.string.backup_description_game);
		createDataRow(Constants.TYPE_USERS, R.string.backup_type_user, R.string.backup_description_user);

		return root;
	}

	private void createDataRow(String type, @StringRes int typeResId, @StringRes int descriptionResId) {
		DataStepRow row = new DataStepRow(requireContext());
		row.setListener(this);
		row.bind(type, typeResId, descriptionResId);
		row.setTag(type);
		fileTypesView.addView(row);
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

	@Nullable
	private JsonExportTask getExportTask(String type, Uri uri) {
		if (TextUtils.isEmpty(type)) return null;
		switch (type) {
			case Constants.TYPE_COLLECTION_VIEWS:
				return new CollectionViewExportTask(getContext(), uri);
			case Constants.TYPE_GAMES:
				return new GameExportTask(getContext(), uri);
			case Constants.TYPE_USERS:
				return new UserExportTask(getContext(), uri);
		}
		return null;
	}

	@Nullable
	private JsonImportTask getImportTask(String type, Uri uri) {
		if (TextUtils.isEmpty(type)) return null;
		switch (type) {
			case Constants.TYPE_COLLECTION_VIEWS:
				return new CollectionViewImportTask(getContext(), uri);
			case Constants.TYPE_GAMES:
				return new GameImportTask(getContext(), uri);
			case Constants.TYPE_USERS:
				return new UserImportTask(getContext(), uri);
		}
		return null;
	}

	@Override
	public void onExportClicked(final String type) {
		if (FileUtils.shouldUseDefaultFolders()) {
			DialogUtils.createConfirmationDialog(getActivity(),
				R.string.msg_export_confirmation,
				(dialog, which) -> {
					if (PackageManager.PERMISSION_GRANTED ==
						ContextCompat.checkSelfPermission(requireContext(), permission.WRITE_EXTERNAL_STORAGE)) {
						performExport(type, null);
					} else {
						if (shouldShowRequestPermissionRationale(permission.WRITE_EXTERNAL_STORAGE)) {
							showSnackbar(R.string.msg_export_permission_rationale);
						}
						requestPermissions(new String[] { permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSIONS);
					}
				}).show();
		} else {
			currentType = type;
			Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("text/json");
			intent.putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(type));
			startActivityForResult(intent, REQUEST_EXPORT);
		}
	}

	@Override
	public void onImportClicked(final String type) {
		if (FileUtils.shouldUseDefaultFolders()) {
			DialogUtils.createConfirmationDialog(getActivity(),
				R.string.msg_import_confirmation,
				(dialog, which) -> performImport(type, null)).show();
		} else {
			currentType = type;
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("text/json");
			intent.putExtra(Intent.EXTRA_TITLE, FileUtils.getExportFileName(type));
			startActivityForResult(intent, REQUEST_IMPORT);
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK || !isAdded() || data == null) return;

		Uri uri = data.getData();
		if (uri == null) return;

		try {
			int modeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			requireContext().getContentResolver().takePersistableUriPermission(uri, modeFlags);
		} catch (SecurityException e) {
			Timber.e(e, "Could not persist URI permissions for '%s'.", uri.toString());
		}

		if (requestCode == REQUEST_EXPORT) {
			performExport(currentType, uri);
		} else if (requestCode == REQUEST_IMPORT) {
			performImport(currentType, uri);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_PERMISSIONS) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				performExport(currentType, null);
			} else {
				showSnackbar(R.string.msg_export_permission_denied);
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@DebugLog
	private void performExport(String type, Uri uri) {
		JsonExportTask task = getExportTask(type, uri);
		if (task == null) {
			Timber.i("No task found for %s", type);
			return;
		}
		DataStepRow row = findRow(type);
		if (row != null) row.initProgressBar();
		//noinspection unchecked
		TaskUtils.<Void>executeAsyncTask(task);
		logAction("Export");
	}

	@DebugLog
	private void performImport(String type, Uri uri) {
		JsonImportTask task = getImportTask(type, uri);
		if (task == null) {
			Timber.i("No task found for %s", type);
			return;
		}
		DataStepRow row = findRow(type);
		if (row != null) row.initProgressBar();
		//noinspection unchecked
		TaskUtils.<Void>executeAsyncTask(task);
		logAction("Import");
	}

	private void logAction(String action) {
		Bundle bundle = new Bundle();
		bundle.putString(ANSWERS_ATTRIBUTE_KEY_ACTION, action);
		firebaseAnalytics.logEvent(ANSWERS_EVENT_NAME, bundle);
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ExportFinishedEvent event) {
		DataStepRow row = findRow(event.getType());
		if (row != null) row.hideProgressBar();
		notifyEnd(event.getErrorMessage(), R.string.msg_export_success, R.string.msg_export_failed);
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ImportFinishedEvent event) {
		DataStepRow row = findRow(event.getType());
		if (row != null) row.hideProgressBar();
		notifyEnd(event.getErrorMessage(), R.string.msg_import_success, R.string.msg_import_failed);
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ExportProgressEvent event) {
		DataStepRow row = findRow(event.getType());
		if (row != null) row.updateProgressBar(event.getTotalCount(), event.getCurrentCount());
	}

	@DebugLog
	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ImportProgressEvent event) {
		DataStepRow row = findRow(event.getType());
		if (row != null) row.updateProgressBar(event.getTotalCount(), event.getCurrentCount());
	}

	private DataStepRow findRow(String type) {
		for (int i = 0; i < fileTypesView.getChildCount(); i++) {
			View view = fileTypesView.getChildAt(i);
			if (view != null) {
				Object tag = view.getTag();
				if (tag.equals(type)) {
					return (DataStepRow) view;
				}
			}
		}
		return null;
	}

	private void notifyEnd(String errorMessage, @StringRes int successResId, @StringRes int failureResId) {
		String message = TextUtils.isEmpty(errorMessage) ?
			getString(successResId) :
			getString(failureResId) + " - " + errorMessage;
		showSnackbar(message);
	}

	private void showSnackbar(@StringRes int messageResId) {
		View view = getView();
		if (view != null) {
			Snackbar.make(view, messageResId, Snackbar.LENGTH_LONG).show();
		}
	}

	private void showSnackbar(String message) {
		View view = getView();
		if (view != null) {
			Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
		}
	}
}
