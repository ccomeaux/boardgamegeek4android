package com.boardgamegeek.ui.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieChartView extends View {

	private final Paint mPaint = new Paint();
	private float mTotal = 0.0f;
	List<Slice> mColors = new ArrayList<Slice>();

	public PieChartView(Context context) {
		this(context, null, 0);
	}

	public PieChartView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PieChartView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mPaint.setAntiAlias(true);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		RectF mOvals = new RectF(0, 0, getWidth(), getHeight());
		float startAngle = 0.0f;
		for (int i = 0; i < mColors.size(); i++) {
			float sweepAngle = (mColors.get(i).Value / mTotal) * 360;
			mPaint.setColor(mColors.get(i).Color);
			canvas.drawArc(mOvals, startAngle - 90, sweepAngle, true, mPaint);
			startAngle += sweepAngle;
		}
	}

	public void addSlice(int value, int color) {
		Slice s = new Slice(value, color);
		mColors.add(s);
		mTotal += value;
	}

	private class Slice {
		int Value;
		int Color;

		Slice(int value, int color) {
			Value = value;
			Color = color;
		}
	}
}
