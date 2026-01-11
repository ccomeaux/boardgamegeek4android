package com.boardgamegeek.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.databinding.RowGeeklistBinding;
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.ui.GeekListActivity;
import com.boardgamegeek.ui.model.PaginatedData;

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
		private final RowGeeklistBinding binding;

		public GeekListEntryViewHolder(View itemView) {
			super(itemView);
			binding = RowGeeklistBinding.bind(itemView);
		}

		@Override
		public void bind(final GeekListEntry geekListEntry) {
			binding.title.setText(geekListEntry.getTitle());
			binding.creator.setText(geekListEntry.getAuthor());
			binding.numberOfItems.setText(String.valueOf(geekListEntry.getNumberOfItems()));
			binding.numberOfThumbs.setText(String.valueOf(geekListEntry.getNumberOfThumbs()));
			itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					GeekListActivity.start(v.getContext(), geekListEntry.getId(), geekListEntry.getTitle());
				}
			});
		}
	}
}
