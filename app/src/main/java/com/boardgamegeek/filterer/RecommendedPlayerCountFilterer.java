package com.boardgamegeek.filterer;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.StringUtils;

public class RecommendedPlayerCountFilterer extends CollectionFilterer {
	public static final int RECOMMENDED = 1;
	public static final int BEST = 2;
	private int playerCount;
	private int recommendation;

	public RecommendedPlayerCountFilterer(Context context) {
		super(context);
	}

	public RecommendedPlayerCountFilterer(Context context, int playerCount, int recommendation) {
		super(context);
		this.playerCount = playerCount;
		this.recommendation = recommendation;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		playerCount = StringUtils.parseInt(d[0]);
		recommendation = StringUtils.parseInt(d[1]);
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_recommended_player_count;
	}

	@Override
	public String getDisplayText() {
		@StringRes int recommendationResId = recommendation == RecommendedPlayerCountFilterer.BEST ? R.string.best : R.string.recommended;
		return context.getResources().getQuantityString(R.plurals.recommended_player_count_description, playerCount, context.getString(recommendationResId), playerCount);
	}

	@Override
	public String[] getColumns() {
		return new String[] { Games.createRecommendedPlayerCountColumn(String.valueOf(playerCount)) };
	}

	@Override
	public String getSelection() {
		return null;
	}

	@Override
	public String[] getSelectionArgs() {
		return null;
	}

	@Override
	public String getHaving() {
		return Games.createRecommendedPlayerCountColumn(String.valueOf(playerCount)) +
			(recommendation == RecommendedPlayerCountFilterer.BEST ? "=2" : ">0");
	}

	@Override
	public String flatten() {
		return playerCount + DELIMITER + String.valueOf(recommendation);
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public int getRecommendation() {
		return recommendation;
	}
}
