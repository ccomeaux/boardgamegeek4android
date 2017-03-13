package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class CollectionSorterFactory {
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_DEFAULT = 1; // name
	private final List<CollectionSorter> sorters;

	public CollectionSorterFactory(@NonNull Context context) {
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
		sorters.add(new LastPlayDateSorter(context));
		sorters.add(new WishlistPrioritySorter(context));
		sorters.add(new LastViewedSorter(context));
		sorters.add(new MyRatingSorter(context));
		sorters.add(new RankSorter(context));
		sorters.add(new AverageRatingSorter(context));
		sorters.add(new AcquisitionDateSorter(context));
		sorters.add(new AcquiredFromSorter(context));
		sorters.add(new PricePaidSorter(context));
		sorters.add(new CurrentValueSorter(context));
	}

	public CollectionSorter create(int type) {
		for (CollectionSorter sorter : sorters) {
			if (sorter.getType() == type) {
				return sorter;
			}
		}
		if (type != TYPE_DEFAULT) {
			Timber.i("Sort type %s not found; attempting to use default", type);
			return create(TYPE_DEFAULT);
		}
		Timber.w("Sort type not found.");
		return null;
	}
}
