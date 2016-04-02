package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
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
import com.boardgamegeek.util.PresentationUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class HotnessFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<HotnessFragment.HotnessData>, MultiChoiceModeListener {
	private static final int LOADER_ID = 1;

	private BoardGameAdapter adapter;
	private LinkedHashSet<Integer> selectedPositions = new LinkedHashSet<>();
	private MenuItem logPlayMenuItem;
	private MenuItem logPlayQuickMenuItem;
	private MenuItem bggLinkMenuItem;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEmptyText(getString(R.string.empty_hotness));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);

		final ListView listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(this);
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

		if (adapter == null) {
			adapter = new BoardGameAdapter(getActivity(), data.list());
			setListAdapter(adapter);
		}
		adapter.notifyDataSetChanged();

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
		HotGame game = adapter.getItem(position);
		ActivityUtils.launchGame(getActivity(), game.id, game.name);
	}

	private static class HotnessLoader extends BggLoader<HotnessData> {
		private BggService bggService;

		public HotnessLoader(Context context) {
			super(context);
			bggService = Adapter.create();
		}

		@Override
		public HotnessData loadInBackground() {
			HotnessData games;
			try {
				games = new HotnessData(bggService.getHotness(BggService.HOTNESS_TYPE_BOARDGAME));
			} catch (Exception e) {
				games = new HotnessData(e);
			}
			return games;
		}
	}

	static class HotnessData extends Data<HotGame> {
		private HotnessResponse response;

		public HotnessData(HotnessResponse response) {
			this.response = response;
		}

		public HotnessData(Exception e) {
			super(e);
		}

		@Override
		public List<HotGame> list() {
			if (response == null || response.games == null) {
				return new ArrayList<>();
			}
			return response.games;
		}
	}

	private class BoardGameAdapter extends ArrayAdapter<HotGame> {
		private LayoutInflater inflater;

		public BoardGameAdapter(Activity activity, List<HotGame> games) {
			super(activity, R.layout.row_hotness, games);
			inflater = activity.getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.row_hotness, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			HotGame game = getItem(position);
			if (game != null) {
				holder.name.setText(game.name);
				holder.year.setText(PresentationUtils.describeYear(getActivity(), game.yearPublished));
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
		logPlayMenuItem = menu.findItem(R.id.menu_log_play);
		logPlayQuickMenuItem = menu.findItem(R.id.menu_log_play_quick);
		bggLinkMenuItem = menu.findItem(R.id.menu_link);
		selectedPositions.clear();
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
			selectedPositions.add(position);
		} else {
			selectedPositions.remove(position);
		}

		int count = selectedPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));

		logPlayMenuItem.setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		logPlayQuickMenuItem.setVisible(PreferencesUtils.showQuickLogPlay(getActivity()));
		bggLinkMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (selectedPositions == null || !selectedPositions.iterator().hasNext()) {
			return false;
		}
		HotGame game = adapter.getItem(selectedPositions.iterator().next());
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), game.id, game.name, game.thumbnailUrl, game.thumbnailUrl, false);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, selectedPositions.size());
				Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
				for (int position : selectedPositions) {
					HotGame g = adapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), g.id, g.name);
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				if (selectedPositions.size() == 1) {
					ActivityUtils.shareGame(getActivity(), game.id, game.name);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(selectedPositions.size());
					for (int position : selectedPositions) {
						HotGame g = adapter.getItem(position);
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
