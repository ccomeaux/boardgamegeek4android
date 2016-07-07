package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.boardgamegeek.R;

public class ScoreGraphView extends View {
	private Paint barPaint;
	private Paint scorePaint;

	private double lowScore;
	private double averageScore;
	private double averageWinScore;
	private double highScore;
	private float scoreRadius;
	private int lowScoreColor;
	private int averageScoreColor;
	private int averageWinScoreColor;
	private int highScoreColor;

	public ScoreGraphView(Context context) {
		super(context);
		init(context);
	}

	public ScoreGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		barPaint.setColor(Color.BLACK);
		barPaint.setStrokeWidth(1);

		scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		scorePaint.setStrokeWidth(1);

		scoreRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());

		lowScoreColor = ContextCompat.getColor(getContext(), R.color.score_low);
		averageScoreColor = ContextCompat.getColor(getContext(), R.color.score_average);
		averageWinScoreColor = ContextCompat.getColor(getContext(), R.color.score_average_win);
		highScoreColor = ContextCompat.getColor(getContext(), R.color.score_high);
	}

	public void setLowScore(double lowScore) {
		this.lowScore = lowScore;
	}

	public void setAverageScore(double averageScore) {
		this.averageScore = averageScore;
	}

	public void setAverageWinScore(double averageWinScore) {
		this.averageWinScore = averageWinScore;
	}

	public void setHighScore(double highScore) {
		this.highScore = highScore;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		final float y = canvas.getHeight() / 2;
		final float left = scoreRadius;
		final float right = canvas.getWidth() - scoreRadius;

		canvas.drawLine(left, y, right, y, barPaint);

		drawScore(canvas, y, left, right, lowScore, lowScoreColor);
		drawScore(canvas, y, left, right, averageScore, averageScoreColor);
		drawScore(canvas, y, left, right, averageWinScore, averageWinScoreColor);
		drawScore(canvas, y, left, right, highScore, highScoreColor);
	}

	private void drawScore(Canvas canvas, float y, float left, float right, double score, @ColorInt int color) {
		scorePaint.setColor(color);
		float x = (float) ((score - lowScore) / (highScore - lowScore) * (right - left) + left);
		canvas.drawCircle(x, y, scoreRadius, scorePaint);
	}
}
