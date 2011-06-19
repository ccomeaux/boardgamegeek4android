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
	protected KnobValuesChangedListener mKnobValuesChangedListener;
	private Knob[] mKnobs = new Knob[2]; // array that holds the knobs
	private int mBalID = 0; // variable to know what knob is being dragged
	private Point mPointKnobStart, mPointKnobEnd;
	private boolean mInitialisedSlider;
	private int mStartKnobValue, mEndKnobValue;// value to know the knob
												// position e.g: 0,40,..,100
	private int mSliderWidth, mSliderHeight;// real size of the view that holds
											// the slider
	private Paint mPaintSelected, mPaintNotSelected, mPaintText;
	private Rect mRectangleSelected, mRectangleNotSelected1, mRectangleNotSelected2;
	private int mMinRange = 0, mMaxRange = 100;
	private boolean mSecondThumbEnabled = true;

	private double mMargin;
	private int mRangeDelta;

	private int mLeftBound;
	private int mRightBound;
	private int mDeltaBound;
	private double mRatio;
	private int mknobRadius;

	public DualSliderView(Context context) {
		super(context);
		setFocusable(true);
	}

	public DualSliderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(true);
		mPointKnobStart = new Point();
		mPointKnobEnd = new Point();
		mInitialisedSlider = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// initialise data for knobs, slider
		if (!mInitialisedSlider) {
			mInitialisedSlider = true;
			mSliderWidth = getMeasuredWidth();
			mSliderHeight = getMeasuredHeight();

			mRangeDelta = mMaxRange - mMinRange;
			mMargin = mSliderWidth - mSliderWidth / 12;

			// left and right bound of slider and the difference
			mLeftBound = (int) (mMargin / mRangeDelta);
			mRightBound = (int) mMargin;
			mDeltaBound = mRightBound - mLeftBound;

			Bitmap knobImage = BitmapFactory.decodeResource(getResources(), R.drawable.knob);
			mknobRadius = knobImage.getWidth() / 2;

			/*
			 * The relative ratio which is later used to calculate the value of
			 * the knob using it's position on the X axis
			 */
			mRatio = (double) mDeltaBound / mRangeDelta;

			mPointKnobStart.x = (int) ((mStartKnobValue - mMinRange) * mRatio) + mLeftBound - mknobRadius;
			mPointKnobStart.y = (int) (mSliderHeight / 2.0);
			mPointKnobEnd.x = (int) ((mEndKnobValue - mMinRange) * mRatio) + mLeftBound - mknobRadius;
			mPointKnobEnd.y = (int) (mSliderHeight / 2.0);

			mKnobs[0] = new Knob(getContext(), R.drawable.knob, mPointKnobStart);
			mKnobs[1] = new Knob(getContext(), R.drawable.knob, mPointKnobEnd);
			mKnobs[0].setID(1);
			mKnobs[1].setID(2);
			setStartKnobValue(mStartKnobValue);
			setEndKnobValue(mEndKnobValue);
			knobValuesChanged(true, true, getFirstKnobValue(), getSecondKnobValue());

			mPaintSelected = new Paint();// the paint between knobs
			mPaintSelected.setColor(Color.YELLOW);
			mPaintNotSelected = new Paint();// the paint outside knobs
			mPaintNotSelected.setColor(Color.GRAY);
			mPaintText = new Paint();// the paint for the slider data(the
										// values)
			mPaintText.setColor(Color.WHITE);

			// rectangles that define the line between and outside of knob
			mRectangleSelected = new Rect();
			mRectangleNotSelected1 = new Rect();
			mRectangleNotSelected2 = new Rect();
		}

		for (int i = 0; i <= mRangeDelta; i++) {
			canvas.drawLine((float) (i * mRatio + mLeftBound), (float) (mSliderHeight / 3.0),
					(float) (i * mRatio + mLeftBound), (float) (mSliderHeight / 2.3), mPaintText);
		}

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
			startX = (int) (mMargin / mRangeDelta);
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

	public boolean onTouchEvent(MotionEvent event) {
		int eventaction = event.getAction();
		int X = (int) event.getX();
		int Y = (int) event.getY();

		switch (eventaction) {
			// Touch down to check if the finger is on a knob
			case MotionEvent.ACTION_DOWN:
				mBalID = 0;
				for (Knob knob : mKnobs) {
					// check if inside the bounds of the knob(circle)
					// get the centre of the knob
					int centerX = knob.getX() + knob.getBitmap().getWidth() / 2;
					int centerY = knob.getY() + knob.getBitmap().getHeight() / 2;
					// calculate the radius from the touch to the centre of the
					// knob
					double distance = Math.sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
							* (centerY - Y)));
					if (distance < knob.getBitmap().getWidth() / 2) {
						mBalID = knob.getID();
					}
				}
				break;

			// Touch drag with the knob
			case MotionEvent.ACTION_MOVE:
				int firstKnobValueTmp = 0;
				int secondKnobValueTmp = 0;

				int radiusKnob = mKnobs[0].getBitmap().getWidth() / 2;

				// the first knob should be between the left bound and the
				// second knob
				if (mBalID == 1 && mSecondThumbEnabled) {
					if (X < mLeftBound)
						X = mLeftBound;
					if (X > mRightBound)
						X = mRightBound;
					// if(X >= mKnobs[1].getX()+radiusKnob)
					// X = mKnobs[1].getX()+radiusKnob;
					mKnobs[0].setX(X - radiusKnob);

					int left_knob = mKnobs[0].getX() + radiusKnob;
					secondKnobValueTmp = (int) Math.round((mMaxRange * mRatio - mRightBound + left_knob) / mRatio);

					// if the start value has changed then we pass it to the
					// listener
					if (secondKnobValueTmp != getFirstKnobValue()) {
						setStartKnobValue(secondKnobValueTmp);
						knobValuesChanged(true, false, getFirstKnobValue(), getSecondKnobValue());
					}
				}
				// the second knob should between the first knob and the right
				// bound
				if (mBalID == 2) {
					if (X < mLeftBound)
						X = mLeftBound;
					if (X > mRightBound)
						X = mRightBound;
					// if(mSecondThumbEnabled && X <= mKnobs[0].getX() +
					// radiusKnob)
					// X = mKnobs[0].getX() + radiusKnob;
					// else if(!mSecondThumbEnabled && X < mLeftBound)
					// X = mLeftBound;
					mKnobs[1].setX(X - radiusKnob);

					int right_knob = mKnobs[1].getX() + radiusKnob;
					firstKnobValueTmp = (int) Math.round(((mMaxRange * mRatio - mRightBound + right_knob) / mRatio));

					// if the end value has changed then we pass it to the
					// listener
					if (firstKnobValueTmp != getSecondKnobValue()) {
						setEndKnobValue(firstKnobValueTmp);
						knobValuesChanged(false, true, getFirstKnobValue(), getSecondKnobValue());
					}
				}
				break;

			// Touch drop - actions after knob is released are performed
			case MotionEvent.ACTION_UP:
				break;
		}

		// Redraw the canvas
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

	public boolean isSecondThumbEnabled() {
		return mSecondThumbEnabled;
	}

	public void setSecondThumbEnabled(boolean enable) {
		mSecondThumbEnabled = enable;
		knobValuesChanged(true, true, getFirstKnobValue(), getSecondKnobValue());
		invalidate();
	}

	public int getFirstKnobValue() {
		return mStartKnobValue;
	}

	public void setStartKnobValue(int startKnobValue) {
		this.mStartKnobValue = startKnobValue;
	}

	public int getSecondKnobValue() {
		return mEndKnobValue;
	}

	public void setEndKnobValue(int endKnobValue) {
		this.mEndKnobValue = endKnobValue;
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
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);
		setMeasuredDimension(chosenWidth, chosenHeight);
	}

	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		}
	}

	// in case there is no size specified
	private int getPreferredSize() {
		return 44;
	}
}