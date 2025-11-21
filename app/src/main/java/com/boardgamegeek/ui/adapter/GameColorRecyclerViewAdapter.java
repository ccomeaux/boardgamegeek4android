package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.BaseColumns;
import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.databinding.RowColorBinding;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class GameColorRecyclerViewAdapter extends RecyclerView.Adapter<GameColorRecyclerViewAdapter.ViewHolder> {
	public static final String[] PROJECTION = new String[] { BaseColumns._ID, GameColors.COLOR };
	private Cursor cursor;
	@LayoutRes private final int layoutId;
	private final SparseBooleanArray selectedItems;
	private final Callback callback;

	public interface Callback {
		void onItemClick(int position);

		boolean onItemLongPress(int position);
	}

	public GameColorRecyclerViewAdapter(Cursor cursor, @LayoutRes int layoutId, Callback callback) {
		this.cursor = cursor;
		this.layoutId = layoutId;
		this.callback = callback;
		selectedItems = new SparseBooleanArray();
		setHasStableIds(true);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		RowColorBinding binding = RowColorBinding.inflate(LayoutInflater.from(context), parent, false);
		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		if (cursor.moveToPosition(position)) {
			holder.bind(position);
		}
	}

	@Override
	public int getItemCount() {
		return cursor != null ? cursor.getCount() : 0;
	}

	@Override
	public long getItemId(int position) {
		if (cursor != null) {
			if (cursor.moveToPosition(position)) {
				return cursor.getLong(0);
			} else {
				return RecyclerView.NO_ID;
			}
		} else {
			return RecyclerView.NO_ID;
		}
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		private final RowColorBinding binding;

		public ViewHolder(RowColorBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		public void bind(final int position) {
			String colorName = getColorName(position);

			binding.colorName.setText(colorName);
			int color = ColorUtils.parseColor(colorName);
			if (color != Color.TRANSPARENT) {
				ColorUtils.setColorViewValue(binding.colorView, color);
			} else {
				binding.colorView.setImageDrawable(null);
			}
			itemView.setActivated(selectedItems.get(position, false));

			itemView.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					return callback != null && callback.onItemLongPress(position);
				}
			});

			itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (callback != null) {
						callback.onItemClick(position);
					}
				}
			});
		}
	}

	public String getColorName(int position) {
		if (cursor != null) {
			cursor.moveToPosition(position);
			return cursor.getString(1);
		} else {
			return "";
		}
	}

	public void changeCursor(Cursor cursor) {
		Cursor old = swapCursor(cursor);
		if (old != null) {
			old.close();
		}
	}

	public Cursor swapCursor(Cursor newCursor) {
		if (newCursor == cursor) {
			return null;
		}
		Cursor oldCursor = cursor;
		cursor = newCursor;
		if (newCursor != null) {
			notifyDataSetChanged();
		} else {
			notifyItemRangeRemoved(0, oldCursor.getCount());
		}
		return oldCursor;
	}

	public void toggleSelection(int position) {
		if (selectedItems.get(position, false)) {
			selectedItems.delete(position);
		} else {
			selectedItems.put(position, true);
		}

		notifyItemChanged(position);
	}

	public void clearSelections() {
		selectedItems.clear();
		notifyDataSetChanged();
	}

	public int getSelectedItemCount() {
		return selectedItems.size();
	}

	public List<Integer> getSelectedItems() {
		List<Integer> items = new ArrayList<>(selectedItems.size());
		for (int i = 0; i < selectedItems.size(); i++) {
			items.add(selectedItems.keyAt(i));
		}
		return items;
	}
}