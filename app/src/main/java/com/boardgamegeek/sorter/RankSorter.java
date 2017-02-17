package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.SparseArray;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class RankSorter extends CollectionSorter {
	@NonNull private final String defaultHeaderText;
	@NonNull private final String defaultText;
	private static final SparseArray<String> RANKS = buildRanks();

	public RankSorter(@NonNull Context context) {
		super(context);
		defaultHeaderText = context.getResources().getString(R.string.unranked);
		defaultText = context.getResources().getString(R.string.text_not_available);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_rank;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_rank;
	}

	@Override
	protected String getSortColumn() {
		return Games.GAME_RANK;
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		int rank = getInt(cursor, Games.GAME_RANK, Integer.MAX_VALUE);
		for (int i = 0; i < RANKS.size(); i++) {
			int key = RANKS.keyAt(i);
			if (rank <= key) {
				return RANKS.get(key);
			}
		}
		return defaultHeaderText;
	}

	@NonNull
	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		int rank = getInt(cursor, Games.GAME_RANK, Integer.MAX_VALUE);
		if (rank == Integer.MAX_VALUE) {
			return defaultText;
		}
		return String.valueOf(rank);
	}

	@NonNull
	private static SparseArray<String> buildRanks() {
		SparseArray<String> ranks = new SparseArray<>();
		ranks.put(100, "1 - 100");
		ranks.put(250, "101 - 250");
		ranks.put(500, "251 - 500");
		ranks.put(1000, "501 - 1000");
		ranks.put(2500, "1001 - 2500");
		ranks.put(5000, "2501 - 5000");
		ranks.put(10000, "5001 - 10000");
		ranks.put(Integer.MAX_VALUE, "10001+");
		return ranks;
	}
}
