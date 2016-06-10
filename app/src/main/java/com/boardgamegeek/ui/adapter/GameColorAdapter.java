package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ColorUtils;

public class GameColorAdapter extends CursorAdapter {
	public static final String[] PROJECTION = new String[] { BaseColumns._ID, GameColors.COLOR };
	private static final String SELECTION = GameColors.COLOR + " LIKE ?";
	private final LayoutInflater inflater;
	private final Uri gameColorsUri;
	@LayoutRes
	private final int layoutId;

	public GameColorAdapter(Context context, int gameId, @LayoutRes int layoutId) {
		super(context, null, false);
		inflater = LayoutInflater.from(context);
		gameColorsUri = createUri(gameId);
		this.layoutId = layoutId;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View row = inflater.inflate(layoutId, parent, false);
		ViewHolder holder = new ViewHolder(row);
		row.setTag(holder);
		return row;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		String colorName = getColorName(cursor);
		holder.colorName.setText(colorName);
		setColor(holder, colorName);
	}

	private void setColor(ViewHolder holder, String colorName) {
		int color = ColorUtils.parseColor(colorName);
		if (color != ColorUtils.TRANSPARENT) {
			ColorUtils.setColorViewValue(holder.color, color);
		} else {
			holder.color.setImageDrawable(null);
		}
	}

	class ViewHolder {
		final TextView colorName;
		final ImageView color;

		public ViewHolder(View view) {
			colorName = (TextView) view.findViewById(R.id.color_name);
			color = (ImageView) view.findViewById(R.id.color_view);
		}
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(1);
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		String selection = null;
		String[] selectionArgs = null;
		if (!TextUtils.isEmpty(constraint)) {
			selection = SELECTION;
			selectionArgs = new String[] { constraint + "%" };
		}
		return mContext.getContentResolver().query(gameColorsUri, PROJECTION, selection, selectionArgs, null);
	}

	public static Uri createUri(int gameId) {
		return Games.buildColorsUri(gameId);
	}

	public static String getColorName(Cursor cursor) {
		if (cursor == null) {
			return "";
		}
		return cursor.getString(1);
	}
}