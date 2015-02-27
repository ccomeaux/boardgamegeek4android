package com.boardgamegeek.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PaletteTransformation;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ImageActivity extends ActionBarActivity {
	@InjectView(R.id.image) ImageView imageView;
	@InjectView(R.id.progress) View progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_image);
		ButterKnife.inject(this);

		String imageUrl = getIntent().getStringExtra(ActivityUtils.KEY_IMAGE_URL);

		Picasso.with(this)
			.load(HttpUtils.ensureScheme(imageUrl))
			.error(R.drawable.thumbnail_image_empty)
			.fit().centerInside()
			.transform(PaletteTransformation.instance())
			.into(imageView, new Callback.EmptyCallback() {
				@Override
				public void onSuccess() {
					Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
					Palette palette = PaletteTransformation.getPalette(bitmap);
					imageView.setBackgroundColor(getColor(palette));
					progressBar.setVisibility(View.GONE);
				}
			});
	}

	private int getColor(Palette palette) {
		Palette.Swatch swatch = palette.getDarkMutedSwatch();
		if (swatch == null) {
			swatch = palette.getDarkVibrantSwatch();
		}
		if (swatch == null) {
			swatch = palette.getMutedSwatch();
		}
		if (swatch == null) {
			return Color.BLACK;
		}
		return swatch.getRgb();
	}
}