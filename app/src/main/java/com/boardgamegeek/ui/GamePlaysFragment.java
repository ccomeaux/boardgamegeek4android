package com.boardgamegeek.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.RefreshableResource;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask;
import com.boardgamegeek.ui.model.Game;
import com.boardgamegeek.ui.model.PlaysByGame;
import com.boardgamegeek.ui.viewmodel.GameViewModel;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.TaskUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GamePlaysFragment extends Fragment implements OnRefreshListener {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final int AGE_IN_DAYS_TO_REFRESH = 1;

	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private String heroImageUrl;
	private boolean arePlayersCustomSorted;
	@ColorInt private int iconColor = Color.TRANSPARENT;
	private boolean isRefreshing;
	@State boolean mightNeedRefreshing = true;
	private GameViewModel viewModel;

	Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.plays_root) View playsRoot;
	@BindView(R.id.plays_label) TextView playsLabel;
	@BindView(R.id.plays_last_play) TextView lastPlayView;
	@BindView(R.id.play_stats_root) View playStatsRoot;
	@BindView(R.id.colors_root) View colorsRoot;
	@BindView(R.id.game_colors_label) TextView colorsLabel;
	@BindView(R.id.sync_timestamp) TimestampView syncTimestampView;
	@BindViews({
		R.id.icon_plays,
		R.id.icon_play_stats,
		R.id.icon_colors
	}) List<ImageView> colorizedIcons;

	public static GamePlaysFragment newInstance(int gameId, String gameName) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);
		GamePlaysFragment fragment = new GamePlaysFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EventBus.getDefault().register(this);
		readBundle(getArguments());
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_game_plays, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		viewModel = ViewModelProviders.of(getActivity()).get(GameViewModel.class);
		viewModel.getGame().observe(this, new Observer<RefreshableResource<Game>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<Game> game) {
				onGameQueryComplete(game.getData());
			}
		});

		viewModel.getPlays().observe(this, new Observer<PlaysByGame>() {
			@Override
			public void onChanged(@Nullable PlaysByGame playsByGame) {
				onPlaysQueryComplete(playsByGame);
			}
		});

		viewModel.getPlayColors().observe(this, new Observer<List<String>>() {
			@Override
			public void onChanged(@Nullable List<String> strings) {
				int count = strings == null ? 0 : strings.size();
				colorsLabel.setText(PresentationUtils.getQuantityText(getContext(), R.plurals.colors_suffix, count, count));
				colorsRoot.setVisibility(View.VISIBLE);
			}
		});
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = bundle.getString(KEY_GAME_NAME);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		EventBus.getDefault().unregister(this);
	}

	@Override
	public void onRefresh() {
		requestRefresh();
	}

	private void requestRefresh() {
		if (!isRefreshing) {
			updateRefreshStatus(true);
			TaskUtils.executeAsyncTask(new SyncPlaysByGameTask(getContext(), gameId));
		} else {
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPlaysByGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			updateRefreshStatus(false);
		}
	}

	protected void updateRefreshStatus(boolean refreshing) {
		this.isRefreshing = refreshing;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}

	private void onGameQueryComplete(Game game) {
		gameName = game.getName();
		imageUrl = game.getImageUrl();
		thumbnailUrl = game.getThumbnailUrl();
		heroImageUrl = game.getHeroImageUrl();
		arePlayersCustomSorted = game.getCustomPlayerSort();
		syncTimestampView.setTimestamp(game.getUpdatedPlays());
		iconColor = game.getIconColor();
		colorize();

		if (mightNeedRefreshing) {
			mightNeedRefreshing = false;
			if (DateTimeUtils.howManyDaysOld(game.getUpdatedPlays()) > AGE_IN_DAYS_TO_REFRESH)
				requestRefresh();
		}
	}

	@DebugLog
	private void onPlaysQueryComplete(PlaysByGame plays) {
		if (plays != null) {
			playsRoot.setVisibility(VISIBLE);

			String description = PresentationUtils.describePlayCount(getActivity(), plays.getPlayCount());
			if (!TextUtils.isEmpty(description)) {
				description = " (" + description + ")";
			}
			playsLabel.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.plays_prefix, plays.getPlayCount(), plays.getPlayCount(), description));

			if (plays.getMaxDate() > 0) {
				lastPlayView.setText(PresentationUtils.getText(getActivity(), R.string.last_played_prefix, PresentationUtils.describePastDaySpan(plays.getMaxDate())));
				lastPlayView.setVisibility(VISIBLE);
			} else {
				lastPlayView.setVisibility(GONE);
			}

			playStatsRoot.setVisibility(plays.getPlayCount() == 0 ? GONE : VISIBLE);
		}
	}

	private void colorize() {
		if (isAdded() &&
			iconColor != Color.TRANSPARENT &&
			colorizedIcons != null) {
			ButterKnife.apply(colorizedIcons, PaletteUtils.getRgbIconSetter(), iconColor);
		}
	}

	@OnClick(R.id.plays_root)
	public void onPlaysClick() {
		GamePlaysActivity.start(getContext(), gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, arePlayersCustomSorted, iconColor);
	}

	@OnClick(R.id.play_stats_root)
	public void onPlayStatsClick() {
		GamePlayStatsActivity.start(getContext(), gameId, gameName, iconColor);
	}

	@OnClick(R.id.colors_root)
	public void onColorsClick() {
		GameColorsActivity.start(getContext(), gameId, gameName, iconColor);
	}
}
