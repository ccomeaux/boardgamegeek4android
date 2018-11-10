package com.boardgamegeek.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.entities.LocationEntity;
import com.boardgamegeek.entities.PlayEntity;
import com.boardgamegeek.entities.PlayerColorEntity;
import com.boardgamegeek.entities.PlayerEntity;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.viewmodel.PlaysSummaryViewModel;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class PlaysSummaryFragment extends Fragment implements OnRefreshListener, OnSharedPreferenceChangeListener {
	private boolean isRefreshing;

	private Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.card_sync) View syncCard;
	@BindView(R.id.card_plays) View playsCard;
	@BindView(R.id.plays_subtitle_in_progress) TextView playsInProgressSubtitle;
	@BindView(R.id.plays_in_progress_container) LinearLayout playsInProgressContainer;
	@BindView(R.id.plays_subtitle_recent) TextView recentPlaysSubtitle;
	@BindView(R.id.plays_container) LinearLayout recentPlaysContainer;
	@BindView(R.id.more_plays_button) Button morePlaysButton;
	@BindView(R.id.card_players) View playersCard;
	@BindView(R.id.players_container) LinearLayout playersContainer;
	@BindView(R.id.card_locations) View locationsCard;
	@BindView(R.id.locations_container) LinearLayout locationsContainer;
	@BindView(R.id.more_players_button) Button morePlayersButton;
	@BindView(R.id.more_locations_button) Button moreLocationsButton;
	@BindView(R.id.card_colors) View colorsCard;
	@BindView(R.id.color_container) LinearLayout colorContainer;
	@BindView(R.id.h_index) TextView hIndexView;
	@BindView(R.id.sync_status) TextView syncStatusView;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_plays_summary, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		hIndexView.setText(PresentationUtils.getText(getActivity(), R.string.game_h_index_prefix, PreferencesUtils.getGameHIndex(getActivity())));

		PlaysSummaryViewModel viewModel = ViewModelProviders.of(this).get(PlaysSummaryViewModel.class);
		viewModel.getPlaysInProgress().observe(this, new Observer<List<PlayEntity>>() {
			@Override
			public void onChanged(List<PlayEntity> playEntities) {
				onPlaysInProgressQueryComplete(playEntities);
			}
		});
		viewModel.getPlaysNotInProgress().observe(this, new Observer<List<PlayEntity>>() {
			@Override
			public void onChanged(List<PlayEntity> playEntities) {
				onPlaysQueryComplete(playEntities);
			}
		});
		viewModel.getPlayCount().observe(this, new Observer<Integer>() {
			@Override
			public void onChanged(Integer integer) {
				onPlayCountQueryComplete(integer);
			}
		});
		viewModel.getPlayers().observe(this, new Observer<List<PlayerEntity>>() {
			@Override
			public void onChanged(List<PlayerEntity> playerEntities) {
				onPlayersQueryComplete(playerEntities);
			}
		});
		viewModel.getLocations().observe(this, new Observer<List<LocationEntity>>() {
			@Override
			public void onChanged(List<LocationEntity> locationEntities) {
				onLocationsQueryComplete(locationEntities);
			}
		});
		viewModel.getColors().observe(this, new Observer<List<PlayerColorEntity>>() {
			@Override
			public void onChanged(List<PlayerColorEntity> playerColorEntities) {
				onColorsQueryComplete(playerColorEntities);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		bindStatusMessage();
		bindSyncCard();
		SyncPrefs.getPrefs(getContext()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		SyncPrefs.getPrefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		bindStatusMessage();
	}

	private void bindStatusMessage() {
		long oldestDate = SyncPrefs.getPlaysOldestTimestamp(getContext());
		long newestDate = SyncPrefs.getPlaysNewestTimestamp(getContext());
		if (oldestDate == Long.MAX_VALUE && newestDate == 0L) {
			syncStatusView.setText(R.string.plays_sync_status_none);
		} else if (oldestDate == 0L) {
			syncStatusView.setText(String.format(getString(R.string.plays_sync_status_new),
				DateUtils.formatDateTime(getContext(), newestDate, DateUtils.FORMAT_SHOW_DATE)));
		} else if (newestDate == 0L) {
			syncStatusView.setText(String.format(getString(R.string.plays_sync_status_old),
				DateUtils.formatDateTime(getContext(), oldestDate, DateUtils.FORMAT_SHOW_DATE)));
		} else {
			syncStatusView.setText(String.format(getString(R.string.plays_sync_status_range),
				DateUtils.formatDateTime(getContext(), oldestDate, DateUtils.FORMAT_SHOW_DATE),
				DateUtils.formatDateTime(getContext(), newestDate, DateUtils.FORMAT_SHOW_DATE)));
		}
	}

	private void bindSyncCard() {
		syncCard.setVisibility(PreferencesUtils.getSyncPlays(getContext()) ||
			PreferencesUtils.getSyncPlaysTimestamp(getContext()) > 0 ?
			View.GONE :
			View.VISIBLE);
	}

	private void onPlaysInProgressQueryComplete(List<PlayEntity> plays) {
		int numberOfPlaysInProgress = plays == null ? 0 : plays.size();
		final int visibility = numberOfPlaysInProgress == 0 ? View.GONE : View.VISIBLE;
		playsInProgressSubtitle.setVisibility(visibility);
		playsInProgressContainer.setVisibility(visibility);
		recentPlaysSubtitle.setVisibility(visibility);

		playsInProgressContainer.removeAllViews();
		if (numberOfPlaysInProgress > 0) {
			for (PlayEntity play : plays) {
				addPlayToContainer(play, playsInProgressContainer);
			}
			playsCard.setVisibility(View.VISIBLE);
		}
	}

	private void onPlaysQueryComplete(List<PlayEntity> plays) {
		recentPlaysContainer.removeAllViews();
		if (plays != null && plays.size() > 0) {
			for (PlayEntity play : plays) {
				addPlayToContainer(play, recentPlaysContainer);
			}
			playsCard.setVisibility(View.VISIBLE);
			recentPlaysContainer.setVisibility(View.VISIBLE);
		}
	}

	private void addPlayToContainer(final PlayEntity play, LinearLayout container) {
		View view = createRow(container, play.getGameName(), PresentationUtils.describePlayDetails(getActivity(), play.getDate(), play.getLocation(), play.getQuantity(), play.getLength(), play.getPlayerCount()));
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayActivity.start(getContext(),
					play.getInternalId(),
					play.getGameId(),
					play.getGameName(),
					play.getThumbnailUrl(),
					play.getImageUrl(),
					play.getHeroImageUrl());
			}
		});
	}

	private void onPlayCountQueryComplete(Integer playCount) {
		morePlaysButton.setVisibility(View.VISIBLE);
		morePlaysButton.setText(R.string.more);
		int morePlaysCount = playCount - 5;
		if (morePlaysCount > 0) {
			morePlaysButton.setText(String.format(getString(R.string.more_suffix), morePlaysCount));
		}
	}

	private void onPlayersQueryComplete(List<PlayerEntity> players) {
		playersContainer.removeAllViews();
		playersCard.setVisibility(players.size() == 0 ? View.GONE : View.VISIBLE);
		for (final PlayerEntity player : players) {
			View view = createRowWithPlayCount(playersContainer, player.getDescription(), player.getPlayCount());
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					BuddyActivity.start(getContext(),
						player.getUsername(),
						player.getName());
				}
			});

		}
		morePlayersButton.setVisibility(players.size() == 0 ? View.GONE : View.VISIBLE);
	}

	private void onLocationsQueryComplete(List<LocationEntity> locations) {
		locationsContainer.removeAllViews();
		locationsCard.setVisibility(locations.size() == 0 ? View.GONE : View.VISIBLE);
		for (final LocationEntity location : locations) {
			View view = createRowWithPlayCount(locationsContainer, location.getName(), location.getPlayCount());
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					LocationActivity.start(getContext(), location.getName());
				}
			});
		}
		moreLocationsButton.setVisibility(locations.size() == 0 ? View.GONE : View.VISIBLE);
	}

	private View createRow(ViewGroup container, String title, String text) {
		View view = LayoutInflater.from(getContext()).inflate(R.layout.row_play_summary, container, false);
		container.addView(view);
		((TextView) view.findViewById(R.id.line1)).setText(title);
		((TextView) view.findViewById(R.id.line2)).setText(text);
		return view;
	}

	private View createRowWithPlayCount(LinearLayout container, String title, int playCount) {
		return createRow(container, title, getResources().getQuantityString(R.plurals.plays_suffix, playCount, playCount));
	}

	private void onColorsQueryComplete(List<PlayerColorEntity> colors) {
		colorContainer.removeAllViews();
		colorsCard.setVisibility(colors.size() == 0 ? View.GONE : View.VISIBLE);
		for (PlayerColorEntity color : colors) {
			ImageView view = createViewToBeColored();
			ColorUtils.setColorViewValue(view, color.getRgb());
			colorContainer.addView(view);
		}
	}

	private ImageView createViewToBeColored() {
		ImageView view = new ImageView(getActivity());
		int size = getResources().getDimensionPixelSize(R.dimen.color_circle_diameter_small);
		int margin = getResources().getDimensionPixelSize(R.dimen.color_circle_diameter_small_margin);
		LayoutParams lp = new LayoutParams(size, size);
		lp.setMargins(margin, margin, margin, margin);
		view.setLayoutParams(lp);
		return view;
	}

	@OnClick(R.id.sync)
	public void onSyncClick() {
		PreferencesUtils.setSyncPlays(getContext());
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS);
		PreferencesUtils.setSyncPlaysTimestamp(getContext());
		bindSyncCard();
	}

	@OnClick(R.id.sync_cancel)
	public void onSyncCancelClick() {
		PreferencesUtils.setSyncPlaysTimestamp(getContext());
		bindSyncCard();
	}

	@OnClick(R.id.more_plays_button)
	public void onPlaysClick() {
		startActivity(new Intent(getActivity(), PlaysActivity.class));
	}

	@OnClick(R.id.more_players_button)
	public void onPlayersClick() {
		startActivity(new Intent(getActivity(), PlayersActivity.class));
	}

	@OnClick(R.id.more_locations_button)
	public void onLocationsClick() {
		startActivity(new Intent(getActivity(), LocationsActivity.class));
	}

	@OnClick(R.id.edit_colors_button)
	public void onColorsClick() {
		PlayerColorsActivity.start(getContext(), AccountUtils.getUsername(getActivity()), null);
	}

	@OnClick(R.id.more_play_stats_button)
	public void onStatsClick() {
		startActivity(new Intent(getActivity(), PlayStatsActivity.class));
	}

	@Override
	public void onRefresh() {
		if (!isRefreshing) {
			SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS);
		} else {
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(@NonNull SyncEvent event) {
		if (((event.getType() & SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) == SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) ||
			((event.getType() & SyncService.FLAG_SYNC_PLAYS_UPLOAD) == SyncService.FLAG_SYNC_PLAYS_UPLOAD)) {
			updateRefreshStatus(true);
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(SyncCompleteEvent event) {
		updateRefreshStatus(false);
	}

	private void updateRefreshStatus(boolean value) {
		this.isRefreshing = value;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}
}
