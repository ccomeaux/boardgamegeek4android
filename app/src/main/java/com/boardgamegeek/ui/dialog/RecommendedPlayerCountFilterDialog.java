package com.boardgamegeek.ui.dialog;


import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.RecommendedPlayerCountFilterer;
import com.boardgamegeek.util.MathUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecommendedPlayerCountFilterDialog implements CollectionFilterDialog {
	private Context context;
	@BindView(R.id.best) RadioButton bestButton;
	@BindView(R.id.recommended) RadioButton recommendedButton;
	@BindView(R.id.range_bar) SeekBar seekBar;
	@BindView(R.id.description) TextView descriptionView;

	@Override
	public void createDialog(Context context, OnFilterChangedListener listener, CollectionFilterer filter) {
		this.context = context;
		View layout = LayoutInflater.from(context).inflate(R.layout.dialog_collection_filter_recommended_player_count, null);

		ButterKnife.bind(this, layout);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateDescription(seekBar.getProgress() + 1);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		initializeUi(filter);

		AlertDialog alertDialog = createAlertDialog(context, listener, layout);
		alertDialog.show();
	}

	private void initializeUi(CollectionFilterer filter) {
		RecommendedPlayerCountFilterer rpcFilter = (RecommendedPlayerCountFilterer) filter;
		int playerCount = 4;
		int recommendation = 1;
		if (rpcFilter != null) {
			playerCount = MathUtils.constrain(rpcFilter.getPlayerCount(), 1, seekBar.getMax() + 1);
			recommendation = rpcFilter.getRecommendation();
		}

		if (recommendation == RecommendedPlayerCountFilterer.BEST) {
			bestButton.toggle();
		} else {
			recommendedButton.toggle();
		}
		seekBar.setProgress(playerCount - 1);
		updateDescription(playerCount);
	}

	private void updateDescription(int playerCount) {
		descriptionView.setText(context.getResources().getQuantityString(R.plurals.players_suffix, playerCount, playerCount));
	}

	private AlertDialog createAlertDialog(final Context context, final OnFilterChangedListener listener, View layout) {
		return new Builder(context, R.style.Theme_bgglight_Dialog_Alert)
			.setTitle(R.string.menu_recommended_player_count)
			.setPositiveButton(R.string.set, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) {
						int playerCount = seekBar.getProgress() + 1;
						int recommendation = bestButton.isChecked() ? RecommendedPlayerCountFilterer.BEST : RecommendedPlayerCountFilterer.RECOMMENDED;
						listener.addFilter(new RecommendedPlayerCountFilterer(context, playerCount, recommendation));
					}
				}
			})
			.setNegativeButton(R.string.clear, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) listener.removeFilter(new RecommendedPlayerCountFilterer(context).getType());
				}
			})
			.setView(layout)
			.create();
	}

	@Override
	public int getType(Context context) {
		return new RecommendedPlayerCountFilterer(context).getType();
	}
}
