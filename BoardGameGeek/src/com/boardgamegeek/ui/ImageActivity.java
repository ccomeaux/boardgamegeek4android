package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.squareup.picasso.Picasso;

public class ImageActivity extends ActionBarActivity {
	public static final String KEY_IMAGE_URL = "IMAGE_URL";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_image);
		ImageView imageView = (ImageView) findViewById(R.id.image);

		String imageUrl = getIntent().getStringExtra(KEY_IMAGE_URL);

		Picasso.with(this).load(imageUrl).placeholder(R.drawable.progress).error(R.drawable.thumbnail_image_empty)
			.fit().centerInside().into(imageView);
	}
}