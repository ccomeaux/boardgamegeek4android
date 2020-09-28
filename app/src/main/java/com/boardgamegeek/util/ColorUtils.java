package com.boardgamegeek.util;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.collection.ArrayMap;
import androidx.core.view.ViewCompat;

/**
 * Static methods for modifying and applying colors to views.
 */
public class ColorUtils {
	private static final int BLACK = 0xFF000000;
	// private static final int DKGRAY = 0xFF444444;
	private static final int GRAY = 0xFF888888;
	// private static final int LTGRAY = 0xFFCCCCCC;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int RED = 0xFFFF0000;
	private static final int GREEN = 0xFF008000; // dark green really
	private static final int BLUE = 0xFF0000FF;
	private static final int YELLOW = 0xFFFFFF00;
	// private static final int CYAN = 0xFF00FFFF;
	// private static final int MAGENTA = 0xFFFF00FF;
	private static final int PURPLE = 0xFF800080;
	private static final int ORANGE = 0xFFE59400;
	private static final int BROWN = 0xFFA52A2A;
	private static final int NATURAL = 0xFFE9C2A6;
	private static final int TAN = 0xFFDB9370;
	private static final int IVORY = 0xFFFFFFF0;
	private static final int ROSE = 0xFFFF007F;
	private static final int PINK = 0xFFCD919E;
	private static final int TEAL = 0xFF008080;
	// private static final int AQUA = 0xFF66CCCC;
	private static final int BRONZE = 0xFF8C7853;
	private static final int SILVER = 0xFFC0C0C0;
	private static final int GOLD = 0xFFFFD700;

	private ColorUtils() {
	}

	public static final ArrayMap<String, Integer> colorNameMap;
	private static final ArrayList<Pair<String, Integer>> limitedColorNameList;
	private static final ArrayList<Pair<String, Integer>> colorNameList;

	static {
		limitedColorNameList = new ArrayList<>();
		limitedColorNameList.add(new Pair<>("Red", RED));
		limitedColorNameList.add(new Pair<>("Yellow", YELLOW));
		limitedColorNameList.add(new Pair<>("Blue", BLUE));
		limitedColorNameList.add(new Pair<>("Green", GREEN));
		limitedColorNameList.add(new Pair<>("Purple", PURPLE));
		limitedColorNameList.add(new Pair<>("Orange", ORANGE));
		limitedColorNameList.add(new Pair<>("White", WHITE));
		limitedColorNameList.add(new Pair<>("Black", BLACK));
		limitedColorNameList.add(new Pair<>("Natural", NATURAL));
		limitedColorNameList.add(new Pair<>("Brown", BROWN));

		colorNameList = new ArrayList<>();
		colorNameList.addAll(limitedColorNameList);
		colorNameList.add(new Pair<>("Tan", TAN));
		colorNameList.add(new Pair<>("Gray", GRAY));
		colorNameList.add(new Pair<>("Gold", GOLD));
		colorNameList.add(new Pair<>("Silver", SILVER));
		colorNameList.add(new Pair<>("Bronze", BRONZE));
		colorNameList.add(new Pair<>("Ivory", IVORY));
		colorNameList.add(new Pair<>("Rose", ROSE));
		colorNameList.add(new Pair<>("Pink", PINK));
		colorNameList.add(new Pair<>("Teal", TEAL));
		// colorNameList.add(new Pair<String, Integer>("Aqua", AQUA));
		// colorNameList.add(new Pair<String, Integer>("Cyan", CYAN));
		// colorNameList.add(new Pair<String, Integer>("Magenta", MAGENTA));
		// colorNameList.add(new Pair<String, Integer>("Light Gray", LTGRAY));
		// colorNameList.add(new Pair<String, Integer>("Dark Gray", DKGRAY));

		colorNameMap = new ArrayMap<>();
		for (Pair<String, Integer> pair : colorNameList) {
			colorNameMap.put(formatKey(pair.first), pair.second);
		}
	}

	private static String formatKey(String name) {
		return name.toLowerCase(Locale.US);
	}

	public static List<Pair<String, Integer>> getColorList() {
		//noinspection unchecked
		return (List<Pair<String, Integer>>) colorNameList.clone();
	}

	public static List<Pair<String, Integer>> getLimitedColorList() {
		//noinspection unchecked
		return (List<Pair<String, Integer>>) limitedColorNameList.clone();
	}

	private static int getStrokeWidth(Resources r) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics());
	}

	/**
	 * Set the background of an {@link android.widget.ImageView} to an oval of the specified color, with a darker
	 * version of the color as a border. For a {@link android.widget.TextView}, changes the text color instead. Doesn't
	 * do anything for other views. Modified from Roman Nurik's DashClock (https://code.google.com/p/dashclock/).
	 */
	public static void setColorViewValue(View view, int color) {
		if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;

			Resources r = imageView.getResources();

			Drawable currentDrawable = imageView.getDrawable();
			GradientDrawable colorChoiceDrawable;
			if (currentDrawable instanceof GradientDrawable) {
				// Reuse drawable
				colorChoiceDrawable = (GradientDrawable) currentDrawable;
			} else {
				colorChoiceDrawable = new GradientDrawable();
				colorChoiceDrawable.setShape(GradientDrawable.OVAL);
			}

			// Set stroke to dark version of color
			int darkenedColor = darkenColor(color);

			colorChoiceDrawable.setColor(color);
			final int width = getStrokeWidth(r);
			colorChoiceDrawable.setStroke(width, darkenedColor);

			imageView.setImageDrawable(colorChoiceDrawable);
		} else if (view instanceof TextView) {
			if (color != Color.TRANSPARENT) {
				((TextView) view).setTextColor(color);
			}
		}
	}

	/**
	 * Returns a darker version of the specified color. Returns a translucent gray for transparent colors.
	 */
	private static int darkenColor(int color) {
		if (color == Color.TRANSPARENT) {
			return Color.argb(127, 127, 127, 127);
		}
		return Color.rgb(Color.red(color) * 192 / 256, Color.green(color) * 192 / 256, Color.blue(color) * 192 / 256);
	}
}
