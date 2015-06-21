package com.boardgamegeek.util;

import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;

import com.squareup.picasso.Transformation;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Used to get Picasso and Palette to play well with each other.
 */
public final class PaletteTransformation implements Transformation {
	private static final PaletteTransformation INSTANCE = new PaletteTransformation();
	private static final Map<Bitmap, Palette> CACHE = new WeakHashMap<>();

	private PaletteTransformation() {
	}

	public static PaletteTransformation instance() {
		return INSTANCE;
	}

	public static Palette getPalette(Bitmap bitmap) {
		return CACHE.get(bitmap);
	}

	@Override
	public Bitmap transform(Bitmap source) {
		Palette palette = Palette.from(source).generate();
		CACHE.put(source, palette);
		return source;
	}

	@Override
	public String key() {
		return ""; // Stable key for all requests. An unfortunate requirement.
	}
}
