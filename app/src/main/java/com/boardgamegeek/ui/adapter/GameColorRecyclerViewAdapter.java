package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.util.ColorUtils;

public class GameColorRecyclerViewAdapter extends RecyclerView.Adapter<GameColorRecyclerViewAdapter.ViewHolder> {
	public static final String[] PROJECTION = new String[] { BaseColumns._ID, GameColors.COLOR };
	private Cursor cursor;
	@LayoutRes private final int layoutId;

	public GameColorRecyclerViewAdapter(Cursor cursor, @LayoutRes int layoutId) {
		this.cursor = cursor;
		this.layoutId = layoutId;
		setHasStableIds(true);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		if (cursor.moveToPosition(position)) {
			String colorName = getColorName(position);
			holder.colorName.setText(colorName);
			int color = ColorUtils.parseColor(colorName);
			if (color != ColorUtils.TRANSPARENT) {
				ColorUtils.setColorViewValue(holder.color, color);
			} else {
				holder.color.setImageDrawable(null);
			}
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
		final TextView colorName;
		final ImageView color;

		public ViewHolder(View view) {
			super(view);
			colorName = (TextView) view.findViewById(R.id.color_name);
			color = (ImageView) view.findViewById(R.id.color_view);
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
}