package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.SelectionBuilder;

public class FavoriteFilterer extends CollectionFilterer {
	private static final String FAVORITE = "1";
	private static final String NOT_FAVORITE = "0";
	private boolean isFavorite;

	public FavoriteFilterer(Context context) {
		super(context);
	}

	public FavoriteFilterer(@NonNull Context context, boolean isFavorite) {
		super(context);
		this.isFavorite = isFavorite;
	}

	@Override
	public void setData(@NonNull String data) {
		isFavorite = data.equals(FAVORITE);
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_favorite;
	}

	@Override
	public String getDisplayText() {
		return context.getString(isFavorite ? R.string.favorites : R.string.not_favorites);
	}

	@Override
	public String getSelection() {
		return isFavorite ?
			Collection.STARRED + "=?" :
			SelectionBuilder.whereZeroOrNull(Collection.STARRED);
	}

	@Override
	public String[] getSelectionArgs() {
		return isFavorite ?
			new String[] { "1" } :
			null;
	}

	@Override
	public String flatten() {
		return isFavorite ? FAVORITE : NOT_FAVORITE;
	}

	public boolean isFavorite() {
		return isFavorite;
	}
}
