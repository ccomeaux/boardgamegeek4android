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
import com.boardgamegeek.entities.Status;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.model.Game;
import com.boardgamegeek.ui.model.PlaysByGame;
import com.boardgamegeek.ui.viewmodel.GameViewModel;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GamePlaysFragment extends Fragment {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";

	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private String heroImageUrl;
	private boolean arePlayersCustomSorted;
	@ColorInt private int iconColor = Color.TRANSPARENT;
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_game_plays, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				viewModel.refresh();
			}
		});
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		viewModel = ViewModelProviders.of(getActivity()).get(GameViewModel.class);
		viewModel.getGame().observe(this, new Observer<RefreshableResource<Game>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<Game> game) {
				onGameQueryComplete(game.getData());
			}
		});

		viewModel.getPlays().observe(this, new Observer<RefreshableResource<PlaysByGame>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<PlaysByGame> playsByGame) {
				if (playsByGame == null) return;
				updateRefreshStatus(playsByGame.getStatus() == Status.REFRESHING);
				onPlaysQueryComplete(playsByGame.getData());
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
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	protected void updateRefreshStatus(final boolean refreshing) {
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(refreshing);
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
	}

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
