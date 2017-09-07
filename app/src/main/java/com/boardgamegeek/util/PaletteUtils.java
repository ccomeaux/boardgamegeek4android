package com.boardgamegeek.util;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
}
