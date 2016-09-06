package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.HotGame;
import com.boardgamegeek.model.HotnessResponse;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import retrofit2.Call;

public class HotnessFragment extends Fragment implements LoaderManager.LoaderCallbacks<SafeResponse<HotnessResponse>>, ActionMode.Callback {
	private static final int LOADER_ID = 1;

	private HotGamesAdapter adapter;
	private ActionMode actionMode;
	private Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_hotness, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setHasFixedSize(true);
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
			adapter = new HotGamesAdapter(getActivity(),
				data == null || data.getBody() == null ? new ArrayList<HotGame>() : data.getBody().games,
				new Callback() {
					@Override
					public boolean onItemClick(int position) {
						if (actionMode == null) {
							return false;
						}
						toggleSelection(position);
						return true;
					}

					@Override
					public boolean onItemLongClick(int position) {
						if (actionMode != null) {
							return false;
						}
						actionMode = getActivity().startActionMode(HotnessFragment.this);
						toggleSelection(position);
						return true;
					}

					private void toggleSelection(int position) {
						adapter.toggleSelection(position);
						int count = adapter.getSelectedItemCount();
						if (count == 0) {
							actionMode.finish();
						} else {
							actionMode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));
							actionMode.invalidate();
						}
					}
				});
			recyclerView.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}

		if (data == null) {
			AnimationUtils.fadeOut(recyclerView);
			AnimationUtils.fadeIn(emptyView);
		} else if (data.hasError()) {
			emptyView.setText(data.getErrorMessage());
			AnimationUtils.fadeOut(recyclerView);
			AnimationUtils.fadeIn(emptyView);
		} else {
			AnimationUtils.fadeOut(emptyView);
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		}
		AnimationUtils.fadeOut(progressView);
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

	public interface Callback {
		boolean onItemClick(int position);

		boolean onItemLongClick(int position);
	}

	public class HotGamesAdapter extends RecyclerView.Adapter<HotGamesAdapter.ViewHolder> {
		private final LayoutInflater inflater;
		private final List<HotGame> games;
		private final Callback callback;
		private final SparseBooleanArray selectedItems;

		public HotGamesAdapter(Context context, List<HotGame> games, Callback callback) {
			this.games = games;
			this.callback = callback;
			inflater = LayoutInflater.from(context);
			selectedItems = new SparseBooleanArray();
			setHasStableIds(true);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.row_hotness, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			holder.bind(getItem(position), position);
		}

		@Override
		public int getItemCount() {
			return games == null ? 0 : games.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public HotGame getItem(int position) {
			return games.get(position);
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			private int gameId;
			private String gameName;
			@BindView(R.id.name) TextView name;
			@BindView(R.id.year) TextView year;
			@BindView(R.id.rank) TextView rank;
			@BindView(R.id.thumbnail) ImageView thumbnail;

			public ViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(HotGame game, final int position) {
				if (game == null) return;
				gameId = game.id;
				gameName = game.name;
				name.setText(game.name);
				year.setText(PresentationUtils.describeYear(name.getContext(), game.yearPublished));
				rank.setText(String.valueOf(game.rank));
				ImageUtils.loadThumbnail(game.thumbnailUrl, thumbnail);

				itemView.setActivated(selectedItems.get(position, false));

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean handled = false;
						if (callback != null) {
							handled = callback.onItemClick(position);
						}
						if (!handled) {
							ActivityUtils.launchGame(itemView.getContext(), gameId, gameName);
						}
					}
				});

				itemView.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return callback != null && callback.onItemLongClick(position);
					}
				});
			}
		}

		public void toggleSelection(int position) {
			if (selectedItems.get(position, false)) {
				selectedItems.delete(position);
			} else {
				selectedItems.put(position, true);
			}
			notifyItemChanged(position);
		}

		public void clearSelections() {
			selectedItems.clear();
			notifyDataSetChanged();
		}

		public int getSelectedItemCount() {
			return selectedItems.size();
		}

		public List<Integer> getSelectedItems() {
			List<Integer> items = new ArrayList<>(selectedItems.size());
			for (int i = 0; i < selectedItems.size(); i++) {
				items.add(selectedItems.keyAt(i));
			}
			return items;
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		adapter.clearSelections();
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.game_context, menu);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		int count = adapter.getSelectedItemCount();
		menu.findItem(R.id.menu_log_play).setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(getActivity()));
		menu.findItem(R.id.menu_link).setVisible(count == 1);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		actionMode = null;
		adapter.clearSelections();
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		HotGame game = adapter.getItem(adapter.getSelectedItems().get(0));
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), game.id, game.name, game.thumbnailUrl, game.thumbnailUrl, false);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, adapter.getSelectedItemCount());
				Snackbar.make(containerView, text, Snackbar.LENGTH_SHORT).show();
				for (int position : adapter.getSelectedItems()) {
					HotGame g = adapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), g.id, g.name);
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				if (adapter.getSelectedItemCount() == 1) {
					ActivityUtils.shareGame(getActivity(), game.id, game.name);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(adapter.getSelectedItemCount());
					for (int position : adapter.getSelectedItems()) {
						HotGame g = adapter.getItem(position);
						games.add(Pair.create(g.id, g.name));
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
