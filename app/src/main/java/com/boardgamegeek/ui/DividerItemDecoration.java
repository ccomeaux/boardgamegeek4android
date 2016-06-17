package com.boardgamegeek.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.boardgamegeek.R;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
	private Drawable dividerDrawable;

	public DividerItemDecoration(Context context) {
		dividerDrawable = ContextCompat.getDrawable(context, R.drawable.vertical_divider);
	}

	@Override
	public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
		int left = parent.getPaddingLeft();
		int right = parent.getWidth() - parent.getPaddingRight();

		int childCount = parent.getChildCount();
		for (int i = 0; i < childCount - 1; i++) {
			View child = parent.getChildAt(i);

			RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

			int top = child.getBottom() + params.bottomMargin;
			int bottom = top + dividerDrawable.getIntrinsicHeight();

			dividerDrawable.setBounds(left, top, right, bottom);
			dividerDrawable.draw(canvas);
		}
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);

		if (parent.getChildAdapterPosition(view) == 0) {
			return;
		}

		outRect.top = dividerDrawable.getIntrinsicHeight();
	}
}
