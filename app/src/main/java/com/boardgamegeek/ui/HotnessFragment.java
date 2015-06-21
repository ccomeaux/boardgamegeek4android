package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.HotGame;
import com.boardgamegeek.model.HotnessResponse;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.Data;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class HotnessFragment extends BggListFragment implements
	LoaderManager.LoaderCallbacks<HotnessFragment.HotnessData>, MultiChoiceModeListener {
	private static final int LOADER_ID = 1;

	private BoardGameAdapter mAdapter;
	private LinkedHashSet<Integer> mSelectedPositions = new LinkedHashSet<>();
	private MenuItem mLogPlayMenuItem;
	private MenuItem mLogPlayQuickMenuItem;
	private MenuItem mBggLinkMenuItem;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getString(R.string.empty_hotness));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);
		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
	}

	@Override
	protected boolean padTop() {
		return true;
	}

	@Override
	public Loader<HotnessData> onCreateLoader(int id, Bundle data) {
		return new HotnessLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<HotnessData> loader, HotnessData data) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new BoardGameAdapter(getActivity(), data.list());
			setListAdapter(mAdapter);
		}
		mAdapter.notifyDataSetChanged();

		if (data.hasError()) {
			setEmptyText(data.getErrorMessage());
		} else {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			restoreScrollState();
		}
	}

	@Override
	public void onLoaderReset(Loader<HotnessData> loader) {
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		HotGame game = mAdapter.getItem(position);
		ActivityUtils.launchGame(getActivity(), game.id, game.name);
	}

	private static class HotnessLoader extends BggLoader<HotnessData> {
		private BggService mService;

		public HotnessLoader(Context context) {
			super(context);
			mService = Adapter.create();
		}

		@Override
		public HotnessData loadInBackground() {
			HotnessData games;
			try {
				games = new HotnessData(mService.getHotness(BggService.HOTNESS_TYPE_BOARDGAME));
			} catch (Exception e) {
				games = new HotnessData(e);
			}
			return games;
		}
	}

	static class HotnessData extends Data<HotGame> {
		private HotnessResponse mResponse;

		public HotnessData(HotnessResponse response) {
			mResponse = response;
		}

		public HotnessData(Exception e) {
			super(e);
		}

		@Override
		public List<HotGame> list() {
			if (mResponse == null || mResponse.games == null) {
				return new ArrayList<>();
			}
			return mResponse.games;
		}
	}

	private class BoardGameAdapter extends ArrayAdapter<HotGame> {
		private LayoutInflater mInflater;

		public BoardGameAdapter(Activity activity, List<HotGame> games) {
			super(activity, R.layout.row_hotness, games);
			mInflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_hotness, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			HotGame game = getItem(position);
			if (game != null) {
				holder.name.setText(game.name);
				String yearText;
				if (game.yearPublished > 0) {
					yearText = getString(R.string.year_positive, game.yearPublished);
				} else if (game.yearPublished == 0) {
					yearText = getString(R.string.year_zero, game.yearPublished);
				} else {
					yearText = getString(R.string.year_negative, -game.yearPublished);
				}
				holder.year.setText(yearText);
				holder.rank.setText(String.valueOf(game.rank));
				loadThumbnail(game.thumbnailUrl, holder.thumbnail);
			}

			return convertView;
		}
	}

	private static class ViewHolder {
		TextView name;
		TextView year;
		TextView rank;
		ImageView thumbnail;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			rank = (TextView) view.findViewById(R.id.rank);
			thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.game_context, menu);
		mLogPlayMenuItem = menu.findItem(R.id.menu_log_play);
		mLogPlayQuickMenuItem = menu.findItem(R.id.menu_log_play_quick);
		mBggLinkMenuItem = menu.findItem(R.id.menu_link);
		mSelectedPositions.clear();
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
			mSelectedPositions.add(position);
		} else {
			mSelectedPositions.remove(position);
		}

		int count = mSelectedPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));

		mLogPlayMenuItem.setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		mLogPlayQuickMenuItem.setVisible(PreferencesUtils.showQuickLogPlay(getActivity()));
		mBggLinkMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (mSelectedPositions == null || !mSelectedPositions.iterator().hasNext()) {
			return false;
		}
		HotGame game = mAdapter.getItem(mSelectedPositions.iterator().next());
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), game.id, game.name, game.thumbnailUrl, game.thumbnailUrl, false);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, mSelectedPositions.size());
				Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
				for (int position : mSelectedPositions) {
					HotGame g = mAdapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), g.id, g.name);
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				if (mSelectedPositions.size() == 1) {
					ActivityUtils.shareGame(getActivity(), game.id, game.name);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(mSelectedPositions.size());
					for (int position : mSelectedPositions) {
						HotGame g = mAdapter.getItem(position);
						games.add(new Pair<>(g.id, g.name));
					}
					ActivityUtils.shareGames(getActivity(), games);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), game.id);
				return true;
		}
		return false;
	}
}
