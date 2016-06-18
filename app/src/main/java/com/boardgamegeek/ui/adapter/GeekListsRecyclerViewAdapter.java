package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekListEntry;
import com.boardgamegeek.ui.GeekListActivity;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.util.ActivityUtils;

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
		private final View rootView;
		private int id;
		@BindView(R.id.geeklist_title) TextView title;
		@BindView(R.id.geeklist_creator) TextView creator;
		@BindView(R.id.geeklist_items) TextView numItems;
		@BindView(R.id.geeklist_thumbs) TextView numThumbs;

		public GeekListEntryViewHolder(View itemView) {
			super(itemView);
			rootView = itemView;
			ButterKnife.bind(this, itemView);
		}

		@Override
		public void bind(GeekListEntry geekListEntry) {
			Context context = rootView.getContext();
			id = geekListEntry.getId();
			title.setText(geekListEntry.getTitle());
			creator.setText(context.getString(R.string.by_prefix, geekListEntry.getAuthor()));
			numItems.setText(context.getString(R.string.items_suffix, geekListEntry.getNumberOfItems()));
			numThumbs.setText(context.getString(R.string.thumbs_suffix, geekListEntry.getNumberOfThumbs()));
			rootView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Context context = v.getContext();
					Intent intent = new Intent(context, GeekListActivity.class);
					intent.putExtra(ActivityUtils.KEY_ID, id);
					intent.putExtra(ActivityUtils.KEY_TITLE, title.getText());
					context.startActivity(intent);
				}
			});
		}
	}
}
