package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.LinkedHashSet;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
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
}
