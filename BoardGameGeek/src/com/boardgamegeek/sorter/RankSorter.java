package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.util.SparseArray;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class RankSorter extends CollectionSorter {
	private String mDefaultHeaderText;
	private String mDefaultText;
	private static SparseArray<String> mRanks = buildRanks();

	public RankSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Games.GAME_RANK, false);
		mDescriptionId = R.string.menu_collection_sort_rank;
		mDefaultHeaderText = context.getResources().getString(R.string.unranked);
		mDefaultText = context.getResources().getString(R.string.text_not_available);
	}

	private static SparseArray<String> buildRanks() {
		SparseArray<String> ranks = new SparseArray<String>();
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

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_RANK;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Games.GAME_RANK };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		int rank = getInt(cursor, Games.GAME_RANK, Integer.MAX_VALUE);
		for (int i = 0; i < mRanks.size(); i++) {
			int key = mRanks.keyAt(i);
			if (rank < key) {
				return mRanks.get(key);
			}
		}
		return mDefaultHeaderText;
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		int rank = getInt(cursor, Games.GAME_RANK, Integer.MAX_VALUE);
		if (rank == Integer.MAX_VALUE) {
			return mDefaultText;
		}
		return String.valueOf(rank);
	}
}
