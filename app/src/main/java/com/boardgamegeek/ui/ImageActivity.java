package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PaletteTransformation;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class ImageActivity extends AppCompatActivity {
	private static final String KEY_IMAGE_URL = "IMAGE_URL";

	@BindView(R.id.image) ImageView imageView;
	@BindView(R.id.progress) View progressBar;

	public static void start(Context context, String imageUrl) {
		if (TextUtils.isEmpty(imageUrl)) {
			Timber.w("Missing the required image URL.");
			return;
		}
		Intent intent = new Intent(context, ImageActivity.class);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_image);
		ButterKnife.bind(this);

		final String imageUrl = getIntent().getStringExtra(KEY_IMAGE_URL);
		if (TextUtils.isEmpty(imageUrl)) {
			Timber.w("Received an empty imageUrl");
			finish();
			return;
		}

		if (savedInstanceState == null) {
			String imageId = Uri.parse(imageUrl).getLastPathSegment();
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Image")
				.putContentId(imageId));
		}

		Picasso.with(this)
			.load(HttpUtils.ensureScheme(imageUrl))
			.error(R.drawable.thumbnail_image_empty)
			.fit()
			.centerInside()
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