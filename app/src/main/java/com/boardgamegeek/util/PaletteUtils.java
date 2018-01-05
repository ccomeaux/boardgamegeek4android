package com.boardgamegeek.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.support.v7.graphics.Target;
import android.util.Pair;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;

import butterknife.ButterKnife;

/**
 * Helper class for dealing with {@link android.support.v7.graphics.Palette}.
 */
public class PaletteUtils {
	/**
	 * Sets the {@link android.widget.TextView}'s text color filter to the {@link android.support.v7.graphics.Palette.Swatch}'s
	 * base RGB.
	 */
	public static final ButterKnife.Setter<TextView, Palette.Swatch> colorTextViewSetter =
		new ButterKnife.Setter<TextView, Palette.Swatch>() {
			@Override
			public void set(@NonNull TextView view, Palette.Swatch value, int index) {
				if (value != null) {
					view.setTextColor(value.getRgb());
				}
			}
		};

	/**
	 * Sets the {@link android.widget.TextView}'s text color filter to the RGB color.
	 */
	public static final ButterKnife.Setter<TextView, Integer> rgbTextViewSetter =
		new ButterKnife.Setter<TextView, Integer>() {
			@Override
			public void set(@NonNull TextView view, Integer value, int index) {
				if (value != null) {
					view.setTextColor(value);
				}
			}
		};

	public static final ButterKnife.Setter<Button, Palette.Swatch> colorButtonSetter =
		new ButterKnife.Setter<Button, Palette.Swatch>() {
			@Override
			public void set(@NonNull Button view, Palette.Swatch value, int index) {
				if (value != null) {
					view.getBackground().setColorFilter(value.getRgb(), Mode.MULTIPLY);
				}
			}
		};

	/**
	 * Sets the {@link android.widget.ImageView}'s color filter to the RGB color.
	 */
	public static final ButterKnife.Setter<ImageView, Integer> rgbIconSetter =
		new ButterKnife.Setter<ImageView, Integer>() {
			@Override
			public void set(@NonNull ImageView view, Integer value, int index) {
				if (value != null) {
					view.setColorFilter(value);
				}
			}
		};

	/**
	 * Sets the {@link android.widget.TextView}'s text color filter to the {@link android.support.v7.graphics.Palette.Swatch}'s
	 * body text color, and its compound drawables tints as the title text color.
	 */
	public static final ButterKnife.Setter<TextView, Palette.Swatch> colorTextViewOnBackgroundSetter =
		new ButterKnife.Setter<TextView, Palette.Swatch>() {
			@Override
			public void set(@NonNull TextView view, Palette.Swatch value, int index) {
				if (value != null) {
					view.setTextColor(value.getBodyTextColor());
					for (Drawable d : view.getCompoundDrawables()) {
						if (d != null) {
							d.setColorFilter(value.getTitleTextColor(), PorterDuff.Mode.SRC_ATOP);
						}
					}
				}
			}
		};

	private PaletteUtils() {
	}

	/**
	 * Gets a swatch from the palette suitable as a dark background with inverse text on top.
	 */
	public static Palette.Swatch getInverseSwatch(Palette palette, int defaultColor) {
		Palette.Swatch swatch = palette.getLightMutedSwatch();
		if (swatch != null) {
			return swatch;
		}

		swatch = palette.getMutedSwatch();
		if (swatch != null) {
			return swatch;
		}

		return new Palette.Swatch(defaultColor, 0);
	}

	/**
	 * Gets a swatch from the palette suitable for tinting icon images.
	 */
	public static Palette.Swatch getIconSwatch(Palette palette) {
		if (palette == null) return null;

		Palette.Swatch swatch = palette.getDarkVibrantSwatch();
		if (swatch != null) return swatch;

		swatch = palette.getVibrantSwatch();
		if (swatch != null) return swatch;

		if (palette.getSwatches().size() > 0) {
			return palette.getSwatches().get(0);
		}

		return new Palette.Swatch(Color.BLACK, 0);
	}

	/**
	 * Gets a swatch from the palette suitable for light text.
	 */
	public static Palette.Swatch getDarkSwatch(Palette palette) {
		if (palette == null) return null;

		Palette.Swatch swatch = palette.getDarkMutedSwatch();
		if (swatch != null) return swatch;

		swatch = palette.getDarkVibrantSwatch();
		if (swatch != null) return swatch;

		if (palette.getSwatches().size() > 0) {
			return palette.getSwatches().get(0);
		}

		return new Palette.Swatch(Color.BLACK, 0);
	}

	/**
	 * Gets a swatch from the palette best suited for header text.
	 */
	public static Palette.Swatch getHeaderSwatch(Palette palette) {
		Palette.Swatch swatch = palette.getVibrantSwatch();
		if (swatch != null) {
			return swatch;
		}

		swatch = palette.getDarkMutedSwatch();
		if (swatch != null) {
			return swatch;
		}

		if (palette.getSwatches().size() > 0) {
			return palette.getSwatches().get(0);
		}

		return new Palette.Swatch(Color.BLACK, 0);
	}

	final static Target[] winsTargets = new Target[] {
		Target.VIBRANT,
		Target.LIGHT_VIBRANT,
		Target.DARK_VIBRANT,
		Target.LIGHT_MUTED,
		Target.MUTED,
		Target.DARK_MUTED
	};

	final static Target[] winnablePlaysTargets = new Target[] {
		Target.DARK_VIBRANT,
		Target.DARK_MUTED,
		Target.MUTED,
		Target.VIBRANT,
		Target.LIGHT_VIBRANT,
		Target.LIGHT_MUTED
	};

	final static Target[] allPlaysTargets = new Target[] {
		Target.LIGHT_MUTED,
		Target.LIGHT_VIBRANT,
		Target.MUTED,
		Target.VIBRANT,
		Target.DARK_MUTED,
		Target.DARK_VIBRANT
	};

	public static int[] getPlayCountColors(Palette palette, Context context) {
		Pair<Integer, Target> winColor = getColor(palette, winsTargets);
		Pair<Integer, Target> winnablePlaysColor = getColor(palette, winnablePlaysTargets, winColor.second);
		Pair<Integer, Target> allPlaysColor = getColor(palette, allPlaysTargets, winColor.second, winnablePlaysColor.second);

		return new int[] {
			winColor.first == Color.TRANSPARENT ? ContextCompat.getColor(context, R.color.orange) : winColor.first,
			winnablePlaysColor.first == Color.TRANSPARENT ? ContextCompat.getColor(context, R.color.dark_blue) : winnablePlaysColor.first,
			allPlaysColor.first == Color.TRANSPARENT ? ContextCompat.getColor(context, R.color.light_blue) : allPlaysColor.first
		};
	}

	private static Pair<Integer, Target> getColor(Palette palette, Target[] targets, Target... usedTargets) {
		for (Target target : targets) {
			boolean useTarget = true;
			for (Target usedTarget : usedTargets) {
				if (target == usedTarget) {
					useTarget = false;
					break;
				}
			}
			if (useTarget) {
				Swatch swatch = palette.getSwatchForTarget(target);
				if (swatch != null) return Pair.create(swatch.getRgb(), target);
			}
		}
		return Pair.create(Color.TRANSPARENT, null);
	}
}
