package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

public class Knob  {
	private Bitmap mImg; // the image of the knob
	private int mCoordX = 0; // the x coordinate at the canvas
	private int mCoordY = 0; // the y coordinate at the canvas
	private int mId; // gives every knob his own id
 
	public Knob(Context context, int drawable) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		mImg = BitmapFactory.decodeResource(context.getResources(), drawable); 
	}
		
	public Knob(Context context, int drawable, Point point) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		mImg = BitmapFactory.decodeResource(context.getResources(), drawable);
		mCoordX = point.x;
		mCoordY = point.y;
	}
				
	void setX(int newValue) {
		mCoordX = newValue;
	}
		
	public int getX() {
		return mCoordX;
	}

	void setY(int newValue) {
		mCoordY = newValue;
	}
		
	public int getY() {
		return mCoordY;
	}
	
	public void setId(int id) {
		this.mId = id;
	}
	
	public int getId() {
		return mId;
	}
		
	public Bitmap getBitmap() {
		return mImg;
	}		
}