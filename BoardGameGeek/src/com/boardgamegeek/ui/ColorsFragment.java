package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

public class ColorsFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
	MultiChoiceModeListener {
	private static final String TAG = makeLogTag(ColorsFragment.class);
	private int mGameId;
	private CursorAdapter mAdapter;
	private LinkedHashSet<Integer> mSelectedColorPositions = new LinkedHashSet<Integer>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_colors));

		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(uri);

		getLoaderManager().restartLoader(ColorsQuery._TOKEN, getArguments(), this);
		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
	}

	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
		com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.game_colors, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_colors_add:
				final EditText editText = new EditText(getActivity());
				editText.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.title_add_color).setView(editText)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String color = editText.getText().toString();
							if (!TextUtils.isEmpty(color)) {
								ContentValues values = new ContentValues();
								values.put(GameColors.COLOR, color);
								getActivity().getContentResolver().insert(Games.buildColorsUri(mGameId), values);
							}
						}
					}).create().show();
				return true;
			case R.id.menu_colors_generate:
				new Task().execute();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), Games.buildColorsUri(mGameId), ColorsQuery.PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.row_color, null,
				new String[] { GameColors.COLOR }, new int[] { R.id.color }, 0);
			setListAdapter(mAdapter);
		}

		int token = loader.getId();
		if (token == ColorsQuery._TOKEN) {
			mAdapter.changeCursor(cursor);
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.colors_context, menu);
		mSelectedColorPositions.clear();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

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

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		mode.finish();
		switch (item.getItemId()) {
			case R.id.menu_delete:
				int count = 0;
				for (int position : mSelectedColorPositions) {
					Cursor cursor = (Cursor) mAdapter.getItem(position);
					String color = cursor.getString(ColorsQuery.COLOR);
					count += getActivity().getContentResolver()
						.delete(Games.buildColorsUri(mGameId, color), null, null);
				}
				Toast.makeText(getActivity(),
					getResources().getQuantityString(R.plurals.msg_colors_deleted, count, count), Toast.LENGTH_SHORT)
					.show();
				return true;
		}
		return false;
	}

	private interface ColorsQuery {
		int _TOKEN = 0x20;
		String[] PROJECTION = { GameColors._ID, GameColors.COLOR, };
		int COLOR = 1;
	}

	protected class Task extends AsyncTask<Void, Void, Integer> {
		@Override
		protected Integer doInBackground(Void... params) {
			Integer count = 0;
			Cursor cursor = null;
			try {
				cursor = getActivity().getContentResolver().query(Plays.buildPlayersUri(),
					new String[] { PlayPlayers.COLOR }, PlayItems.OBJECT_ID + "=?",
					new String[] { String.valueOf(mGameId) }, null);
				if (cursor != null && cursor.moveToFirst()) {
					List<ContentValues> values = new ArrayList<ContentValues>();
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
						count = getActivity().getContentResolver().bulkInsert(Games.buildColorsUri(mGameId),
							values.toArray(array));
					}
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			return count;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result > 0) {
				Toast.makeText(getActivity(), R.string.msg_colors_generated, Toast.LENGTH_SHORT).show();
			}
		}
	}
}
