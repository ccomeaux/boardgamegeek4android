package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.boardgamegeek.R;

public class DualSliderView extends View {
	private static final int MARGIN = 12;
	protected KnobValuesChangedListener mKnobValuesChangedListener;
	private Knob[] mKnobs = new Knob[2]; // array that holds the knobs
	private int mKnobId = 0; // variable to know what knob is being dragged
	private Point mPointKnobStart, mPointKnobEnd;
	private boolean mInitializedSlider;
	private int mStartKnobValue, mEndKnobValue; // value to know the knob position e.g: 0,40,..,100
	private int mSliderWidth, mSliderHeight; // real size of the view that holds the slider
	private Paint mPaintSelected, mPaintNotSelected, mPaintText;
	private Rect mRectangleSelected, mRectangleNotSelected1, mRectangleNotSelected2;
	private int mMinRange = 0;
	private int mMaxRange = 100;
	private double mStep = 1.0;
	private boolean mSecondThumbEnabled = true;

	private double mMargin;
	private int mRangeDelta;
	private int mLineSpacing = 1;

	private int mLeftBound;
	private int mRightBound;
	private int mDeltaBound;
	private double mRatio;
	private int mKnobRadius;

	public DualSliderView(Context context) {
		super(context);
		setFocusable(true);
	}

	public DualSliderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(true);
		mPointKnobStart = new Point();
		mPointKnobEnd = new Point();
		mInitializedSlider = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		init();
		drawTicks(canvas);

		int startX, endX, startY, endY;
		// rectangle between knobs
		if (mSecondThumbEnabled) {
			startX = mKnobs[0].getX() + mKnobs[0].getBitmap().getWidth();
			endX = mKnobs[1].getX();
			startY = (int) (mSliderHeight / 2) + mKnobs[0].getY() / 4;
			endY = (int) (mSliderHeight / 2) + (int) (3.0 * mKnobs[0].getY() / 4);

			if (startX > endX) {
				mRectangleSelected.set(endX, startY, startX, endY);
			} else {
				mRectangleSelected.set(startX, startY, endX, endY);
			}

			// rectangle from left margin to first knob
			startX = (int) mLeftBound;
			endX = mKnobs[0].getX();
			mRectangleNotSelected1.set(startX, startY, endX, endY);

			// rectangle from second knob to right margin
			startX = mKnobs[1].getX() + mKnobs[0].getBitmap().getWidth();
			endX = (int) mMargin;
			mRectangleNotSelected2.set(startX, startY, endX, endY);

			canvas.drawRect(mRectangleNotSelected1, mPaintNotSelected);
			canvas.drawRect(mRectangleNotSelected2, mPaintNotSelected);
			canvas.drawRect(mRectangleSelected, mPaintSelected);
			canvas.drawBitmap(mKnobs[0].getBitmap(), mKnobs[0].getX(), mKnobs[0].getY(), null);
		} else {
			startX = (int) (mMargin / mRangeDelta);
			endX = (int) mMargin;
			startY = (int) (mSliderHeight / 2) + mKnobs[0].getY() / 4;
			endY = (int) (mSliderHeight / 2) + (int) (3.0 * mKnobs[0].getY() / 4);
			mRectangleSelected.set(startX, startY, endX, endY);
			canvas.drawRect(mRectangleSelected, mPaintNotSelected);
		}
		canvas.drawBitmap(mKnobs[1].getBitmap(), mKnobs[1].getX(), mKnobs[1].getY(), null);
	}

	private void init() {
		if (!mInitializedSlider) {
			mInitializedSlider = true;
			mSliderWidth = getMeasuredWidth();
			mSliderHeight = getMeasuredHeight();

			mRangeDelta = mMaxRange - mMinRange;
			mMargin = mSliderWidth - mSliderWidth / MARGIN;

			// left and right bound of slider and the difference
			mLeftBound = (int) mSliderWidth / MARGIN;
			mRightBound = (int) mMargin;
			mDeltaBound = mRightBound - mLeftBound;

			Bitmap knobImage = BitmapFactory.decodeResource(getResources(), R.drawable.knob);
			mKnobRadius = knobImage.getWidth() / 2;

			// The relative ratio which is later used to calculate the value of the knob using it's position on the X axis
			mRatio = (double) mDeltaBound / mRangeDelta;

			mPointKnobStart.x = (int) ((mStartKnobValue - mMinRange) * mRatio) + mLeftBound - mKnobRadius;
			mPointKnobStart.y = (int) (mSliderHeight / 2.0);
			mPointKnobEnd.x = (int) ((mEndKnobValue - mMinRange) * mRatio) + mLeftBound - mKnobRadius;
			mPointKnobEnd.y = (int) (mSliderHeight / 2.0);

			mKnobs[0] = new Knob(getContext(), R.drawable.knob, mPointKnobStart);
			mKnobs[1] = new Knob(getContext(), R.drawable.knob, mPointKnobEnd);
			mKnobs[0].setId(1);
			mKnobs[1].setId(2);
			knobValuesChanged(true, true, getStartKnobValue(), getEndKnobValue());

			mPaintSelected = new Paint(); // the paint between knobs
			mPaintSelected.setColor(Color.YELLOW);
			mPaintNotSelected = new Paint(); // the paint outside knobs
			mPaintNotSelected.setColor(Color.GRAY);
			mPaintText = new Paint(); // the paint for the slider data(the values)
			mPaintText.setColor(Color.WHITE);

			// rectangles that define the line between and outside of knob
			mRectangleSelected = new Rect();
			mRectangleNotSelected1 = new Rect();
			mRectangleNotSelected2 = new Rect();
		}
	}

	private void drawTicks(Canvas canvas) {
		for (int i = 0; i <= mRangeDelta; i += mLineSpacing) {
			canvas.drawLine(
					(float) (i * mRatio + mLeftBound),
					(float) (mSliderHeight / 3.0),
					(float) (i * mRatio + mLeftBound),
					(float) (mSliderHeight / 2.3), mPaintText);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				// Touch the knob
				mKnobId = 0;
				for (Knob knob : mKnobs) {
					// check if inside the bounds of the knob(circle) get the center of the knob
					int centerX = knob.getX() + knob.getBitmap().getWidth() / 2;
					int centerY = knob.getY() + knob.getBitmap().getHeight() / 2;
					// calculate the radius from the touch to the center of the knob
					double distance = Math.sqrt((double) (((centerX - x) * (centerX - x)) + (centerY - y)
							* (centerY - y)));
					if (distance < knob.getBitmap().getWidth() / 2) {
						mKnobId = knob.getId();
					}
				}
				break;

			case MotionEvent.ACTION_MOVE:
				// Drag the knob
				if (mKnobId != 0) {
					x = clamp(x, mLeftBound, mRightBound);
					int newKnobValue = (int) Math.round((mMaxRange * mRatio - mRightBound + x) / mRatio);
					if (mKnobId == 1 && mSecondThumbEnabled) {
						mKnobs[0].setX(x - mKnobRadius);
						if (newKnobValue != getStartKnobValue()) {
							mStartKnobValue = newKnobValue;
							knobValuesChanged(true, false, getStartKnobValue(), getEndKnobValue());
						}
					} else if (mKnobId == 2) {
						mKnobs[1].setX(x - mKnobRadius);
						if (newKnobValue != getEndKnobValue()) {
							mEndKnobValue = newKnobValue;
							knobValuesChanged(false, true, getStartKnobValue(), getEndKnobValue());
						}
					}
				}
				break;
		}

		invalidate();
		return true;
	}

	public void setRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("min must be less than max");
		}
		mMinRange = min;
		mMaxRange = max;
		setStartKnobValue(min);
		setEndKnobValue(max);
	}

	public void setRange(int min, int max, double step) {
		mStep = step;
		setRange((int) (min / step), (int) (max / step));
	}

	public boolean isSecondThumbEnabled() {
		return mSecondThumbEnabled;
	}

	public void setSecondThumbEnabled(boolean enable) {
		mSecondThumbEnabled = enable;
		knobValuesChanged(true, true, getStartKnobValue(), getEndKnobValue());
		invalidate();
	}

	public int getMinKnobValue(){
		return Math.min(getStartKnobValue(), getEndKnobValue());
	}

	public int getMaxKnobValue(){
		return Math.max(getStartKnobValue(), getEndKnobValue());
	}

	public int getStartKnobValue() {
		return (int) (mStartKnobValue * mStep);
	}

	public void setStartKnobValue(int startKnobValue) {
		mStartKnobValue = (int) (startKnobValue / mStep);
	}

	public int getEndKnobValue() {
		return (int) (mEndKnobValue * mStep);
	}

	public void setEndKnobValue(int endKnobValue) {
		mEndKnobValue = (int) (endKnobValue / mStep);
	}

	public void setLineSpacing(int lineSpacing) {
		mLineSpacing = (int) (lineSpacing / mStep);
	}

	public int getLineSpacing() {
		return mLineSpacing;
	}

	/**
	 * Interface which defines the knob values changed listener method
	 */
	public interface KnobValuesChangedListener {
		void onValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd);
	}

	/**
	 * Method applied to the instance of SliderView
	 */
	public void setOnKnobValuesChangedListener(KnobValuesChangedListener l) {
		mKnobValuesChangedListener = l;
	}

	/**
	 * Method used by knob values changed listener
	 */
	private void knobValuesChanged(boolean knobStartChanged, boolean knobEndChanged, int knobStart, int knobEnd) {
		if (mKnobValuesChangedListener != null) {
			mKnobValuesChangedListener.onValuesChanged(knobStartChanged, knobEndChanged, knobStart, knobEnd);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = (int) (57 * getResources().getDisplayMetrics().density);
		setMeasuredDimension(chosenWidth, chosenHeight);
	}

	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return 0;
		}
	}

	private static int clamp(int i, int low, int high) {
		return Math.max(Math.min(i, high), low);
	}
}