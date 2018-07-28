package com.boardgamegeek.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlayerSelectedEvent;
import com.boardgamegeek.events.PlayersCountChangedEvent;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.sorter.PlayersSorter;
import com.boardgamegeek.sorter.PlayersSorterFactory;
import com.boardgamegeek.ui.model.Player;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.fabric.SortEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class PlayersFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String STATE_SORT_TYPE = "sortType";
	private PlayersAdapter adapter;
	private PlayersSorter sorter;
	private Unbinder unbinder;
	@BindView(R.id.empty_container) ViewGroup emptyContainer;
	@BindView(R.id.progress) ContentLoadingProgressBar progressBar;
	@BindView(android.R.id.list) RecyclerView listView;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_players, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		listView.setLayoutManager(new LinearLayoutManager(getContext()));
		listView.setHasFixedSize(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = PlayersSorterFactory.TYPE_DEFAULT;
		if (savedInstanceState != null) {
			sortType = savedInstanceState.getInt(STATE_SORT_TYPE);
		}
		setSort(sortType);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SORT_TYPE, sorter.getType());
	}

	public int getSort() {
		return sorter.getType();
	}

	public void setSort(int sortType) {
		if (sorter == null || sorter.getType() != sortType) {
			SortEvent.log("Players", String.valueOf(sortType));
			sorter = PlayersSorterFactory.create(getContext(), sortType);
			LoaderManager.getInstance(this).restartLoader(0, getArguments(), this);
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getContext(),
			Plays.buildPlayersByUniquePlayerUri(),
			StringUtils.unionArrays(Player.PROJECTION, sorter.getColumns()),
			null,
			null,
			sorter.getOrderByClause());
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;

		List<Player> players = new ArrayList<>();
		if (cursor.moveToFirst()) {
			do {
				players.add(Player.fromCursor(cursor));
			} while (cursor.moveToNext());
		}

		if (adapter == null) {
			adapter = new PlayersAdapter(getContext());
			listView.setAdapter(adapter);
			RecyclerSectionItemDecoration sectionItemDecoration =
				new RecyclerSectionItemDecoration(
					getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
					true,
					adapter);
			listView.addItemDecoration(sectionItemDecoration);
		}

		adapter.changeData(players, sorter);

		EventBus.getDefault().postSticky(new PlayersCountChangedEvent(cursor.getCount()));

		progressBar.hide();
		setListShown(listView.getWindowToken() != null);
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		if (adapter != null) adapter.clear();
	}

	private void setListShown(boolean animate) {
		if (adapter.getItemCount() == 0) {
			AnimationUtils.fadeOut(listView);
			AnimationUtils.fadeIn(emptyContainer);
		} else {
			AnimationUtils.fadeOut(emptyContainer);
			AnimationUtils.fadeIn(listView, animate);
		}
	}

	public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.ViewHolder> implements SectionCallback {
		private final LayoutInflater inflater;
		private final List<Player> players = new ArrayList<>();
		private PlayersSorter sorter;

		@DebugLog
		public PlayersAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void clear() {
			this.players.clear();
			notifyDataSetChanged();
		}

		public void changeData(@NonNull List<Player> players, PlayersSorter sorter) {
			this.players.clear();
			this.players.addAll(players);
			this.sorter = sorter;
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			return players == null ? 0 : players.size();
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder(inflater.inflate(R.layout.row_players_player, parent, false));
		}

		@Override
		public boolean isSection(int position) {
			if (players.size() == 0) return false;
			if (position == 0) return true;
			String thisLetter = sorter.getSectionText(players.get(position));
			String lastLetter = sorter.getSectionText(players.get(position - 1));
			return !thisLetter.equals(lastLetter);
		}

		@NonNull
		@Override
		public CharSequence getSectionHeader(int position) {
			if (players.size() == 0) return "-";
			return sorter.getSectionText(players.get(position));
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			holder.bind(players.get(position));
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			@BindView(android.R.id.title) TextView name;
			@BindView(android.R.id.text1) TextView username;
			@BindView(android.R.id.text2) TextView quantity;

			public ViewHolder(View view) {
				super(view);
				ButterKnife.bind(this, view);
			}

			public void bind(final Player player) {
				if (player == null) return;

				name.setText(player.getName());
				PresentationUtils.setTextOrHide(username, player.getUsername());
				PresentationUtils.setTextOrHide(quantity, sorter.getDisplayText(player));

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						EventBus.getDefault().post(new PlayerSelectedEvent(player.getName(), player.getUsername()));
					}
				});
			}
		}
	}
}
