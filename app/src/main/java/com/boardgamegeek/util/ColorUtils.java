package com.boardgamegeek.util;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;

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
	public static final int ORANGE = 0xFFE59400;
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
			Integer color = sColorNameMap.get(formatKey(colorString));
			if (color != null) {
				return color;
			}
		}
		return TRANSPARENT;
	}

	public static int getRatingColor(double rating) {
		int baseRating = clamp((int) rating, 0, 10);
		return blendColors(BACKGROUND_COLORS[baseRating], BACKGROUND_COLORS[baseRating + 1], baseRating + 1 - rating);
	}

	private static int clamp(int number, int low, int high) {
		if (number < low) {
			return low;
		}
		if (number > high) {
			return high;
		}
		return number;
	}

	private static int blendColors(int color1, int color2, double ratio) {
		double ir = 1.0 - ratio;

		int a = (int) (Color.alpha(color1) * ratio + Color.alpha(color2) * ir);
		int r = (int) (Color.red(color1) * ratio + Color.red(color2) * ir);
		int g = (int) (Color.green(color1) * ratio + Color.green(color2) * ir);
		int b = (int) (Color.blue(color1) * ratio + Color.blue(color2) * ir);

		return Color.argb(a, r, g, b);
	}

	public static final int BACKGROUND_COLORS[] = { 0x00ffffff, 0xffff0000, 0xffff3366, 0xffff6699, 0xffff66cc,
		0xffcc99ff, 0xff9999ff, 0xff99ffff, 0xff66ff99, 0xff33cc99, 0xff00cc00, 0x00ffffff };

	private static final HashMap<String, Integer> sColorNameMap;
	private static final List<Pair<String, Integer>> sColorNameList;

	static {
		sColorNameList = new ArrayList<>();
		sColorNameList.add(new Pair<>("Red", RED));
		sColorNameList.add(new Pair<>("Yellow", YELLOW));
		sColorNameList.add(new Pair<>("Blue", BLUE));
		sColorNameList.add(new Pair<>("Green", GREEN));
		sColorNameList.add(new Pair<>("Purple", PURPLE));
		sColorNameList.add(new Pair<>("Orange", ORANGE));
		sColorNameList.add(new Pair<>("White", WHITE));
		sColorNameList.add(new Pair<>("Black", BLACK));
		sColorNameList.add(new Pair<>("Natural", NATURAL));
		sColorNameList.add(new Pair<>("Brown", BROWN));
		sColorNameList.add(new Pair<>("Tan", TAN));
		sColorNameList.add(new Pair<>("Gray", GRAY));
		sColorNameList.add(new Pair<>("Gold", GOLD));
		sColorNameList.add(new Pair<>("Silver", SILVER));
		sColorNameList.add(new Pair<>("Bronze", BRONZE));
		sColorNameList.add(new Pair<>("Ivory", IVORY));
		sColorNameList.add(new Pair<>("Rose", ROSE));
		sColorNameList.add(new Pair<>("Pink", PINK));
		sColorNameList.add(new Pair<>("Teal", TEAL));
		// sColorNameList.add(new Pair<String, Integer>("Aqua", AQUA));
		// sColorNameList.add(new Pair<String, Integer>("Cyan", CYAN));
		// sColorNameList.add(new Pair<String, Integer>("Magenta", MAGENTA));
		// sColorNameList.add(new Pair<String, Integer>("Light Gray", LTGRAY));
		// sColorNameList.add(new Pair<String, Integer>("Dark Gray", DKGRAY));

		sColorNameMap = new HashMap<>();
		for (Pair<String, Integer> pair : sColorNameList) {
			sColorNameMap.put(formatKey(pair.first), pair.second);
		}
	}

	private static String formatKey(String name) {
		return name.toLowerCase(Locale.US);
	}

	public static List<Pair<String, Integer>> getColorList() {
		return sColorNameList;
	}

	@SuppressWarnings("deprecation")
	public static void setTextViewBackground(TextView view, int color) {
		Resources r = view.getResources();

		Drawable currentDrawable = view.getBackground();
		GradientDrawable backgroundDrawable;
		if (currentDrawable != null && currentDrawable instanceof GradientDrawable) {
			// Reuse drawable
			backgroundDrawable = (GradientDrawable) currentDrawable;
		} else {
			backgroundDrawable = new GradientDrawable();
		}

		int darkenedColor = darkenColor(color);

		backgroundDrawable.setColor(color);
		backgroundDrawable.setStroke(
			(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics()), darkenedColor);

		view.setBackgroundDrawable(backgroundDrawable);
	}

	// Modified from Roman Nurik's DashClock https://code.google.com/p/dashclock/
	public static void setColorViewValue(View view, int color) {
		if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;

			Resources r = imageView.getResources();

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
			int darkenedColor = darkenColor(color);

			colorChoiceDrawable.setColor(color);
			colorChoiceDrawable.setStroke(
				(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics()), darkenedColor);

			imageView.setImageDrawable(colorChoiceDrawable);
		} else if (view instanceof TextView) {
			if (color != ColorUtils.TRANSPARENT) {
				((TextView) view).setTextColor(color);
			}
		}
	}

	private static int darkenColor(int color) {
		if (color == TRANSPARENT) {
			return Color.argb(127, 127, 127, 127);
		}
		return Color.rgb(Color.red(color) * 192 / 256, Color.green(color) * 192 / 256, Color.blue(color) * 192 / 256);
	}

	/**
	 * Calculate whether a color is light or dark, based on a commonly known brightness formula.
	 *
	 * @see {@literal http://en.wikipedia.org/wiki/HSV_color_space%23Lightness}
	 */
	public static boolean isColorDark(int color) {
		return ((30 * Color.red(color) + 59 * Color.green(color) + 11 * Color.blue(color)) / 100) <= 130;
	}

	public static Palette.Swatch getInverseSwatch(Palette palette) {
		Palette.Swatch swatch = palette.getLightMutedSwatch();
		if (swatch != null) {
			return swatch;
		}

		swatch = palette.getMutedSwatch();
		if (swatch != null) {
			return swatch;
		}

		return palette.getSwatches().get(0);
	}

	public static Palette.Swatch getIconSwatch(Palette palette) {
		Palette.Swatch swatch = palette.getDarkVibrantSwatch();
		if (swatch != null) {
			return swatch;
		}

		swatch = palette.getVibrantSwatch();
		if (swatch != null) {
			return swatch;
		}

		return palette.getSwatches().get(0);
	}

	public static final ButterKnife.Setter<TextView, Palette.Swatch> colorTextViewSetter =
		new ButterKnife.Setter<TextView, Palette.Swatch>() {
			@Override
			public void set(TextView view, Palette.Swatch value, int index) {
				if (view != null && value != null) {
					view.setTextColor(value.getBodyTextColor());
					for (Drawable d : view.getCompoundDrawables()) {
						if (d != null) {
							d.setColorFilter(value.getTitleTextColor(), PorterDuff.Mode.SRC_ATOP);
						}
					}
				}
			}
		};

	public static final ButterKnife.Setter<ImageView, Palette.Swatch> colorIconSetter =
		new ButterKnife.Setter<ImageView, Palette.Swatch>() {
			@Override
			public void set(ImageView view, Palette.Swatch value, int index) {
				if (view != null && value != null) {
					view.setColorFilter(value.getRgb());
				}
			}
		};
}
