package com.boardgamegeek.ui;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.model.GameCollectionItem;
import com.boardgamegeek.ui.model.RefreshableResource;
import com.boardgamegeek.ui.model.Status;
import com.boardgamegeek.ui.viewmodel.GameViewModel;
import com.boardgamegeek.ui.widget.GameCollectionRow;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.PresentationUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class GameCollectionFragment extends Fragment implements OnRefreshListener {
	private static final String KEY_GAME_ID = "GAME_ID";

	private int gameId;
	private boolean isRefreshing;
	@State boolean mightNeedRefreshing = true;

	Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.empty) TextView emptyView;
	@BindView(R.id.collection_container) ViewGroup collectionContainer;
	@BindView(R.id.sync_timestamp) TimestampView syncTimestampView;

	private GameViewModel viewModel;

	public static GameCollectionFragment newInstance(int gameId) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		GameCollectionFragment fragment = new GameCollectionFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		Icepick.restoreInstanceState(this, savedInstanceState);

		viewModel = ViewModelProviders.of(getActivity()).get(GameViewModel.class);
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_game_collection, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		viewModel.getGameCollection().observe(this, new Observer<RefreshableResource<List<GameCollectionItem>>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<List<GameCollectionItem>> items) {
				if (items == null) {
					showError();
				} else {
					updateRefreshStatus(items.getStatus() == Status.REFRESHING);
					if (items.getStatus() == Status.ERROR) {
						showError(items.getMessage());
					}
					updateUi(items.getData());
				}
			}
		});
	}

	private void showError() {
		showError(getString(R.string.empty_game_collection));
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

	private void updateUi(List<GameCollectionItem> items) {
		if (getActivity() == null) return;
		if (items != null) {
			emptyView.setVisibility(View.GONE);
			long oldestSyncTimestamp = Long.MAX_VALUE;
			collectionContainer.removeAllViews();
			for (GameCollectionItem item : items) {
				oldestSyncTimestamp = Math.min(item.getSyncTimestamp(), oldestSyncTimestamp);

				GameCollectionRow row = new GameCollectionRow(getContext());
				row.bind(item.getInternalId(), gameId, item.getGameName(), item.getCollectionId(), item.getYearPublished(), item.getImageUrl());
				row.setThumbnail(item.getThumbnailUrl());
				row.setStatus(item.getStatuses(), item.getNumberOfPlays(), item.getRating(), item.getComment());
				row.setDescription(item.getCollectionName(), item.getCollectionYearPublished());
				row.setComment(item.getComment());
				row.setRating(item.getRating());

				collectionContainer.addView(row);
			}
			syncTimestampView.setTimestamp(oldestSyncTimestamp);
		} else {
			showError();
		}
	}

	private void showError(String message) {
		emptyView.setText(message);
		emptyView.setVisibility(View.VISIBLE);
		collectionContainer.removeAllViews();
		syncTimestampView.setTimestamp(System.currentTimeMillis());
	}

	@Override
	public void onRefresh() {
		if (!isRefreshing) {
			updateRefreshStatus(true);
			viewModel.refreshCollectionItems();
		} else {
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
}
