package com.boardgamegeek.ui.dialog;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.IntExtensionsKt;
import com.boardgamegeek.R;
import com.boardgamegeek.StringExtensionsKt;
import com.boardgamegeek.entities.GameEntity;
import com.boardgamegeek.entities.GameRankEntity;
import com.boardgamegeek.entities.RefreshableResource;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.ui.viewmodel.GameViewModel;
import com.boardgamegeek.ui.widget.GameRankRow;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PresentationUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class RanksFragment extends DialogFragment {
	private Unbinder unbinder;
	@BindView(R.id.unRankedView) TextView unRankedView;
	@BindView(R.id.subtypesView) ViewGroup subtypesView;
	@BindView(R.id.familiesView) ViewGroup familiesView;
	@BindView(R.id.standardDeviationView) TextView standardDeviationView;
	@BindView(R.id.votesView) TextView votesView;

	public static void launch(Fragment fragment) {
		final RanksFragment dialog = new RanksFragment();
		dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog);
		DialogUtils.showAndSurvive(fragment, dialog);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialog_game_ranks, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getDialog().setTitle(R.string.title_ranks_ratings);

		GameViewModel viewModel = ViewModelProviders.of(getActivity()).get(GameViewModel.class);

		viewModel.getGame().observe(this, new Observer<RefreshableResource<GameEntity>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<GameEntity> gameRefreshableResource) {
				int voteCount = gameRefreshableResource.getData().getNumberOfRatings();
				double standardDeviation = gameRefreshableResource.getData().getStandardDeviation();
				votesView.setText(PresentationUtils.getQuantityText(getContext(), R.plurals.votes_suffix, voteCount, voteCount));
				if (voteCount == 0) {
					standardDeviationView.setVisibility(View.GONE);
				} else {
					standardDeviationView.setText(PresentationUtils.getText(getContext(), R.string.standard_deviation_prefix, standardDeviation));
					standardDeviationView.setVisibility(View.VISIBLE);
				}
			}
		});

		viewModel.getRanks().observe(this, new Observer<List<GameRankEntity>>() {
			@Override
			public void onChanged(@Nullable List<GameRankEntity> gameRankEntities) {
				subtypesView.removeAllViews();
				familiesView.removeAllViews();

				boolean hasRankedSubtype = false;
				CharSequence unRankedSubtype = getString(R.string.game);

				if (gameRankEntities != null || gameRankEntities.size() > 0) {
					for (GameRankEntity rank : gameRankEntities) {
						if (IntExtensionsKt.isRankValid(rank.getValue())) {
							GameRankRow row = new GameRankRow(getContext(), rank.isFamilyType());
							row.setRank(rank.getValue());
							row.setName(StringExtensionsKt.asRankDescription(rank.getName(), getContext(), rank.getType()));
							row.setRatingView(rank.getBayesAverage());
							switch (rank.getType()) {
								case BggService.RANK_TYPE_SUBTYPE:
									subtypesView.addView(row);
									subtypesView.setVisibility(View.VISIBLE);
									unRankedView.setVisibility(View.GONE);
									hasRankedSubtype = true;
									break;
								case BggService.RANK_TYPE_FAMILY:
									familiesView.addView(row);
									familiesView.setVisibility(View.VISIBLE);
									break;
								default:
									Timber.i("Invalid rank type: %s", rank.getType());
									break;
							}
						} else if (rank.isSubType()) {
							unRankedSubtype = StringExtensionsKt.asRankDescription(rank.getName(), getContext(), rank.getType());
						}
					}
				}
				if (!hasRankedSubtype && !TextUtils.isEmpty(unRankedSubtype)) {
					unRankedView.setText(PresentationUtils.getText(getContext(), R.string.unranked_prefix, unRankedSubtype));
					unRankedView.setVisibility(View.VISIBLE);
				}
			}
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}
}
