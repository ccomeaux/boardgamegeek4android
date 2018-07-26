package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.ui.GeekListActivity;
import com.boardgamegeek.ui.model.PaginatedData;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GeekListsRecyclerViewAdapter extends PaginatedRecyclerViewAdapter<GeekListEntry> {
	public GeekListsRecyclerViewAdapter(Context context, PaginatedData<GeekListEntry> data) {
		super(context, R.layout.row_geeklist, data);
	}

	@NonNull
	@Override
	protected PaginatedItemViewHolder getViewHolder(View itemView) {
		return new GeekListEntryViewHolder(itemView);
	}

	public class GeekListEntryViewHolder extends PaginatedItemViewHolder {
		@BindView(R.id.title) TextView title;
		@BindView(R.id.creator) TextView creator;
		@BindView(R.id.number_of_items) TextView numberOfItems;
		@BindView(R.id.number_of_thumbs) TextView numberOfThumbs;

		public GeekListEntryViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@Override
		public void bind(final GeekListEntry geekListEntry) {
			title.setText(geekListEntry.getTitle());
			creator.setText(geekListEntry.getAuthor());
			numberOfItems.setText(String.valueOf(geekListEntry.getNumberOfItems()));
			numberOfThumbs.setText(String.valueOf(geekListEntry.getNumberOfThumbs()));
			itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					GeekListActivity.start(v.getContext(), geekListEntry.getId(), geekListEntry.getTitle());
				}
			});
		}
	}
}
