package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CollectionSorterFactory {
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_COLLECTION_NAME = 1;
	public static final int TYPE_GEEK_RATING = 2;
	public static final int TYPE_PLAY_COUNT_ASC = 3;
	public static final int TYPE_PLAY_COUNT_DESC = 4;
	public static final int TYPE_YEAR_PUBLISHED_ASC = 5;
	public static final int TYPE_YEAR_PUBLISHED_DESC = 6;
	public static final int TYPE_PLAY_TIME_ASC = 7;
	public static final int TYPE_PLAY_TIME_DESC = 8;
	public static final int TYPE_AGE_ASC = 9;
	public static final int TYPE_AGE_DESC = 10;
	public static final int TYPE_AVERAGE_WEIGHT_ASC = 11;
	public static final int TYPE_AVERAGE_WEIGHT_DESC = 12;
	public static final int TYPE_WISHLIST_PRIORITY = 13;
	public static final int TYPE_LAST_VIEWED = 14;
	public static final int TYPE_MY_RATING = 15;
	public static final int TYPE_RANK = 16;
	public static final int TYPE_AVERAGE_RATING = 17;
	public static final int TYPE_ACQUISITION_DATE = 18;
	public static final int TYPE_DEFAULT = TYPE_COLLECTION_NAME;

	private final Context context;
	private List<CollectionSorter> sorters;

	public CollectionSorterFactory(@NonNull Context context) {
		this.context = context;
		sorters = new ArrayList<>();
		sorters.add(new CollectionNameSorter(context));
		sorters.add(new GeekRatingSorter(context));
		sorters.add(new YearPublishedAscendingSorter(context));
		sorters.add(new YearPublishedDescendingSorter(context));
		sorters.add(new PlayTimeAscendingSorter(context));
		sorters.add(new PlayTimeDescendingSorter(context));
		sorters.add(new SuggestedAgeAscendingSorter(context));
		sorters.add(new SuggestedAgeDescendingSorter(context));
		sorters.add(new AverageWeightAscendingSorter(context));
		sorters.add(new AverageWeightDescendingSorter(context));
		sorters.add(new PlayCountAscendingSorter(context));
		sorters.add(new PlayCountDescendingSorter(context));
		sorters.add(new WishlistPrioritySorter(context));
		sorters.add(new LastViewedSorter(context));
		sorters.add(new MyRatingSorter(context));
		sorters.add(new RankSorter(context));
		sorters.add(new AverageRatingSorter(context));
		sorters.add(new AcquisitionDateSorter(context));
	}

	public CollectionSorter create(int type) {
		for (CollectionSorter sorter : sorters) {
			if (sorter.getType() == type) {
				return sorter;
			}
		}
		return null;
	}
}
