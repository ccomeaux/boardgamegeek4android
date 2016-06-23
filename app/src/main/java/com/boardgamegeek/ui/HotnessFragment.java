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
import android.view.View.OnClickListener;
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
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;

public class HotnessFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<SafeResponse<HotnessResponse>>, MultiChoiceModeListener {
	private static final int LOADER_ID = 1;

	private BoardGameAdapter adapter;
	private final LinkedHashSet<Integer> selectedPositions = new LinkedHashSet<>();
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
	public Loader<SafeResponse<HotnessResponse>> onCreateLoader(int id, Bundle data) {
		return new HotnessLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<SafeResponse<HotnessResponse>> loader, SafeResponse<HotnessResponse> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new BoardGameAdapter(getActivity(),
				data == null || data.getBody() == null ?
					new ArrayList<HotGame>() :
					data.getBody().games);
			setListAdapter(adapter);
		}
		adapter.notifyDataSetChanged();

		if (data == null) {
			setEmptyText(getString(R.string.empty_hotness));
		} else if (data.hasError()) {
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
	public void onLoaderReset(Loader<SafeResponse<HotnessResponse>> loader) {
	}

	private static class HotnessLoader extends BggLoader<SafeResponse<HotnessResponse>> {
		private final BggService bggService;

		public HotnessLoader(Context context) {
			super(context);
			bggService = Adapter.createForXml();
		}

		@Override
		public SafeResponse<HotnessResponse> loadInBackground() {
			Call<HotnessResponse> call = bggService.getHotness(BggService.HOTNESS_TYPE_BOARDGAME);
			return new SafeResponse<>(call);
		}
	}

	private class BoardGameAdapter extends ArrayAdapter<HotGame> {
		private final LayoutInflater inflater;

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
			holder.bind(game);
			return convertView;
		}
	}

	public static class ViewHolder {
		private View view;
		private int gameId;
		private String gameName;
		@BindView(R.id.name) TextView name;
		@BindView(R.id.year) TextView year;
		@BindView(R.id.rank) TextView rank;
		@BindView(R.id.thumbnail) ImageView thumbnail;

		public ViewHolder(View view) {
			this.view = view;
			ButterKnife.bind(this, view);
		}

		public void bind(HotGame game) {
			if (game == null) return;
			gameId = game.id;
			gameName = game.name;
			name.setText(game.name);
			year.setText(PresentationUtils.describeYear(name.getContext(), game.yearPublished));
			rank.setText(String.valueOf(game.rank));
			ImageUtils.loadThumbnail(game.thumbnailUrl, thumbnail);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ActivityUtils.launchGame(view.getContext(), gameId, gameName);
				}
			});
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
		if (!selectedPositions.iterator().hasNext()) {
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
