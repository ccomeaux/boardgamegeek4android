package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.FavoriteFilterer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavoriteFilterDialog implements CollectionFilterDialog {
	@BindView(R.id.favorite) RadioButton favoriteButton;
	@BindView(R.id.not_favorite) RadioButton notFavoriteButton;

	@Override
	public void createDialog(final Context context, final OnFilterChangedListener listener, CollectionFilterer filter) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View layout = inflater.inflate(R.layout.dialog_collection_filter_favorite, null);
		ButterKnife.bind(this, layout);
		initializeUi(filter);
		AlertDialog alertDialog = createAlertDialog(context, listener, layout);
		alertDialog.show();
	}

	private void initializeUi(CollectionFilterer filter) {
		FavoriteFilterer favoriteFilterer = (FavoriteFilterer) filter;
		if (favoriteFilterer != null) {
			if (favoriteFilterer.isFavorite()) {
				favoriteButton.setChecked(true);
			} else {
				notFavoriteButton.setChecked(true);
			}
		}
	}

	private AlertDialog createAlertDialog(final Context context, final OnFilterChangedListener listener, View layout) {
		return new Builder(context, R.style.Theme_bgglight_Dialog_Alert)
			.setTitle(R.string.menu_favorite)
			.setPositiveButton(R.string.set, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null)
						listener.addFilter(new FavoriteFilterer(context, favoriteButton.isChecked()));
				}
			})
			.setNegativeButton(R.string.clear, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) listener.removeFilter(new FavoriteFilterer(context).getType());
				}
			})
			.setView(layout)
			.create();
	}

	@Override
	public int getType(Context context) {
		return new FavoriteFilterer(context).getType();
	}
}
