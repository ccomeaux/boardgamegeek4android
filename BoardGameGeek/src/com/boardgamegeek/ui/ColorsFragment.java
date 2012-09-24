package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.UIUtils;

public class ColorsFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	public static final int MENU_COLOR_DELETE = Menu.FIRST;
	private static final String TAG = makeLogTag(ColorsFragment.class);
	private int mGameId;
	private CursorAdapter mAdapter;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		setEmptyText(getString(R.string.empty_colors));
		setListShown(false);

		Uri colorsUri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		mGameId = Games.getGameId(colorsUri);

		getLoaderManager().restartLoader(ColorsQuery._TOKEN, getArguments(), this);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return;
		}
		final String color = cursor.getString(ColorsQuery.COLOR);

		menu.setHeaderTitle(color);
		menu.add(0, MENU_COLOR_DELETE, 0, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return false;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return false;
		}
		final String color = cursor.getString(ColorsQuery.COLOR);

		switch (item.getItemId()) {
			case MENU_COLOR_DELETE: {
				getActivity().getContentResolver().delete(Games.buildColorsUri(mGameId, color), null, null);
				// mHandler.startQuery(mGameColorUri, Query.PROJECTION);
				return true;
			}
		}
		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		// Uri gameUri = UIUtils.fragmentArgumentsToIntent(data).getData();
		// if (gameUri == null) {
		// return null;
		// }
		CursorLoader loader = new CursorLoader(getActivity(), Games.buildColorsUri(mGameId), ColorsQuery.PROJECTION,
			null, null, null);
		loader.setUpdateThrottle(2000);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new ColorAdapter(getActivity());
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

	private class ColorAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public ColorAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.color.setText(cursor.getString(ColorsQuery.COLOR));
		}
	}

	static class ViewHolder {
		TextView color;

		public ViewHolder(View view) {
			color = (TextView) view.findViewById(android.R.id.text1);
		}
	}

	private interface ColorsQuery {
		int _TOKEN = 0x20;
		String[] PROJECTION = { BaseColumns._ID, GameColors.COLOR, };
		int COLOR = 1;
	}
}
