package com.boardgamegeek.util;

import java.util.HashMap;
import java.util.Locale;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ColorUtils {
	public static final int TRANSPARENT = 0;
	public static final int BLACK = 0xFF000000;
	public static final int DKGRAY = 0xFF444444;
	public static final int GRAY = 0xFF888888;
	public static final int LTGRAY = 0xFFCCCCCC;
	public static final int WHITE = 0xFFFFFFFF;
	public static final int RED = 0xFFFF0000;
	public static final int GREEN = 0xFF008000; // dark green really
	public static final int BLUE = 0xFF0000FF;
	public static final int YELLOW = 0xFFFFFF00;
	public static final int CYAN = 0xFF00FFFF;
	public static final int MAGENTA = 0xFFFF00FF;
	public static final int PURPLE = 0xFF800080;
	public static final int ORANGE = 0xFFFFA500;
	public static final int BROWN = 0xFFA52A2A;
	public static final int NATURAL = 0xFFE9C2A6;
	public static final int TAN = 0xFFDB9370;
	public static final int IVORY = 0xFFFFFFF0;
	public static final int ROSE = 0xFFFF007F;
	public static final int PINK = 0xFFCD919E;
	public static final int TEAL = 0xFF008080;
	public static final int AQUA = 0xFF66CCCC;
	public static final int BRONZE = 0xFF8C7853;
	public static final int SILVER = 0xFFC0C0C0;
	public static final int GOLD = 0xFFFFD700;

	public static int parseColor(String colorString) {
		if (TextUtils.isEmpty(colorString)) {
			return TRANSPARENT;
		}
		if (colorString.charAt(0) == '#') {
			// Use a long to avoid rollovers on #ffXXXXXX
			long color = Long.parseLong(colorString.substring(1), 16);
			if (colorString.length() == 7) {
				// Set the alpha value
				color |= 0x00000000ff000000;
			} else if (colorString.length() != 9) {
				return TRANSPARENT;
			}
			return (int) color;
		} else {
			String key = colorString.toLowerCase(Locale.US);
			Integer color = sColorNameMap.get(key);
			if (color != null) {
				return color;
			}
		}
		return TRANSPARENT;
	}

	private static final HashMap<String, Integer> sColorNameMap;

	static {
		sColorNameMap = new HashMap<String, Integer>();
		sColorNameMap.put("black", BLACK);
		sColorNameMap.put("darkgray", DKGRAY);
		sColorNameMap.put("gray", GRAY);
		sColorNameMap.put("lightgray", LTGRAY);
		sColorNameMap.put("white", WHITE);
		sColorNameMap.put("red", RED);
		sColorNameMap.put("green", GREEN);
		sColorNameMap.put("blue", BLUE);
		sColorNameMap.put("yellow", YELLOW);
		sColorNameMap.put("cyan", CYAN);
		sColorNameMap.put("magenta", MAGENTA);
		sColorNameMap.put("purple", PURPLE);
		sColorNameMap.put("orange", ORANGE);
		sColorNameMap.put("brown", BROWN);
		sColorNameMap.put("natural", NATURAL);
		sColorNameMap.put("tan", TAN);
		sColorNameMap.put("ivory", IVORY);
		sColorNameMap.put("rose", ROSE);
		sColorNameMap.put("pink", PINK);
		sColorNameMap.put("teal", TEAL);
		sColorNameMap.put("aqua", AQUA);
		sColorNameMap.put("bronze", BRONZE);
		sColorNameMap.put("silver", SILVER);
		sColorNameMap.put("gold", GOLD);
	}

	// Modified from Roman Nurik's DashClock https://code.google.com/p/dashclock/
	public static void setColorViewValue(View view, int color) {
		if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;

			if (color == ColorUtils.TRANSPARENT) {
				imageView.setImageDrawable(null);
				return;
			}

			Resources res = imageView.getContext().getResources();

			Drawable currentDrawable = imageView.getDrawable();
			GradientDrawable colorChoiceDrawable;
			if (currentDrawable != null && currentDrawable instanceof GradientDrawable) {
				// Reuse drawable
				colorChoiceDrawable = (GradientDrawable) currentDrawable;
			} else {
				colorChoiceDrawable = new GradientDrawable();
				colorChoiceDrawable.setShape(GradientDrawable.OVAL);
			}

			// Set stroke to dark version of color
			int darkenedColor = Color.rgb(Color.red(color) * 192 / 256, Color.green(color) * 192 / 256,
				Color.blue(color) * 192 / 256);

			colorChoiceDrawable.setColor(color);
			colorChoiceDrawable
				.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics()),
					darkenedColor);

			Drawable drawable = colorChoiceDrawable;

			imageView.setImageDrawable(drawable);

		} else if (view instanceof TextView) {
			if (color != ColorUtils.TRANSPARENT) {
				((TextView) view).setTextColor(color);
			}
		}
	}

}
