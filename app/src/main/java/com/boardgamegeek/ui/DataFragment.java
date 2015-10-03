package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.export.JsonExportTask;
import com.boardgamegeek.export.JsonImportTask;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.TaskUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class DataFragment extends Fragment {
	@SuppressWarnings("unused") @InjectView(R.id.backup_location) TextView mFileLocationView;

	@DebugLog
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_data, container, false);

		ButterKnife.inject(this, root);

		mFileLocationView.setText(FileUtils.getExportPath(false).getPath());

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
				TaskUtils.executeAsyncTask(new JsonExportTask(getContext(), false));
			}
		}).show();
	}

	@DebugLog
	@SuppressWarnings("unused")
	@OnClick(R.id.import_button)
	public void onImportClick(View view) {
		DialogUtils.createConfirmationDialog(getActivity(), R.string.msg_import_confirmation, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				TaskUtils.executeAsyncTask(new JsonImportTask(getContext(), false));
			}
		}).show();
	}

	@DebugLog
	@SuppressWarnings("unused")
	public void onEventMainThread(ExportFinishedEvent event) {
		View v = getView();
		if (v != null) {
			Snackbar.make(v, R.string.msg_export_finished, Snackbar.LENGTH_LONG).show();
		}
	}

	@DebugLog
	@SuppressWarnings("unused")
	public void onEventMainThread(ImportFinishedEvent event) {
		View v = getView();
		if (v != null) {
			Snackbar.make(v, R.string.msg_import_finished, Snackbar.LENGTH_LONG).show();
		}
	}
}
