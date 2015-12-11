package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class ColorsFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<Cursor>, MultiChoiceModeListener {
	private static final int TOKEN = 0x20;
	private int mGameId;
	private GameColorAdapter mAdapter;
	private final LinkedHashSet<Integer> mSelectedColorPositions = new LinkedHashSet<>();
	private AlertDialog mDialog;

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@DebugLog
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		showFab(true);
	}

	@DebugLog
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_colors));

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(uri);

		getLoaderManager().restartLoader(TOKEN, getArguments(), this);
		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
	}

	@DebugLog
	@Override
	protected boolean padTop() {
		return true;
	}

	@DebugLog
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.game_colors, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_colors_generate:
				TaskUtils.executeAsyncTask(new Task());
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), GameColorAdapter.createUri(mGameId), GameColorAdapter.PROJECTION, null, null, null);
	}

	@DebugLog
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new GameColorAdapter(getActivity(), mGameId, R.layout.row_color);
			setListAdapter(mAdapter);
		}

		int token = loader.getId();
		if (token == TOKEN) {
			mAdapter.changeCursor(cursor);
		} else {
			Timber.w("Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@DebugLog
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (mAdapter != null) {
			mAdapter.changeCursor(null);
		}
	}

	@DebugLog
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.colors_context, menu);
		mSelectedColorPositions.clear();
		return true;
	}

	@DebugLog
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@DebugLog
	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@DebugLog
	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if (checked) {
			mSelectedColorPositions.add(position);
		} else {
			mSelectedColorPositions.remove(position);
		}

		int count = mSelectedColorPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_colors_selected, count, count));
	}

	@DebugLog
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		mode.finish();
		switch (item.getItemId()) {
			case R.id.menu_delete:
				int count = 0;
				for (int position : mSelectedColorPositions) {
					String color = mAdapter.getColorName(position);
					count += getActivity().getContentResolver().delete(Games.buildColorsUri(mGameId, color), null, null);
				}
				Snackbar.make(getListContainer(), getResources().getQuantityString(R.plurals.msg_colors_deleted, count, count), Snackbar.LENGTH_SHORT).show();
				return true;
		}
		return false;
	}

	@Override
	protected void onFabClicked(View v) {
		EditTextDialogFragment fragment = EditTextDialogFragment.newInstance(R.string.title_add_color, null, new EditTextDialogListener() {
			@Override
			public void onFinishEditDialog(String inputText) {
				if (!TextUtils.isEmpty(inputText)) {
					ContentValues values = new ContentValues();
					values.put(GameColors.COLOR, inputText);
					getActivity().getContentResolver().insert(Games.buildColorsUri(mGameId), values);
				}
			}
		});
		DialogUtils.showFragment(getActivity(), fragment, "edit_color");
	}

	private class Task extends AsyncTask<Void, Void, Integer> {
		@DebugLog
		@Override
		protected Integer doInBackground(Void... params) {
			Integer count = 0;
			Cursor cursor = null;
			try {
				cursor = getActivity().getContentResolver().query(Plays.buildPlayersByColor(),
					new String[] { PlayPlayers.COLOR }, PlayItems.OBJECT_ID + "=?",
					new String[] { String.valueOf(mGameId) }, null);
				if (cursor != null && cursor.moveToFirst()) {
					List<ContentValues> values = new ArrayList<>();
					do {
						String color = cursor.getString(0);
						if (!TextUtils.isEmpty(color)) {
							ContentValues cv = new ContentValues();
							cv.put(GameColors.COLOR, color);
							values.add(cv);
						}
					} while (cursor.moveToNext());
					if (values.size() > 0) {
						ContentValues[] array = {};
						count = getActivity().getContentResolver().bulkInsert(Games.buildColorsUri(mGameId), values.toArray(array));
					}
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			return count;
		}

		@DebugLog
		@Override
		protected void onPostExecute(Integer result) {
			if (result > 0) {
				Snackbar.make(getListContainer(), R.string.msg_colors_generated, Snackbar.LENGTH_SHORT).show();
			}
		}
	}
}
