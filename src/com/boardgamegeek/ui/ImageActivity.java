package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.UIUtils;

public class ImageActivity extends Activity {
	// private final static String TAG = "ImageActivity";

	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private ImageView mImageView;
	private String mImageUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		mImageView = (ImageView) findViewById(R.id.image);

		final Intent intent = getIntent();
		mImageUrl = intent.getStringExtra(KEY_IMAGE_URL);

		Drawable d = ImageCache.getDrawableFromCache(intent.getStringExtra(KEY_THUMBNAIL_URL));
		if (d != null) {
			mImageView.setVisibility(View.VISIBLE);
			mImageView.setImageDrawable(d);
		}
		
		String gameName = intent.getStringExtra(KEY_GAME_NAME);
		((TextView) findViewById(R.id.game_name)).setText(gameName);

		new ImageTask().execute(mImageUrl);
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	private class ImageTask extends AsyncTask<String, Void, Drawable> {
		private boolean mOomError = false;

		@Override
		protected void onPreExecute() {
			findViewById(R.id.image_progress).setVisibility(View.VISIBLE);
		}

		@Override
		protected Drawable doInBackground(String... params) {
			try {
				return ImageCache.getImage(ImageActivity.this, params[0], true);
			} catch (OutOfMemoryError e) {
				mOomError = true;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Drawable result) {
			findViewById(R.id.image_progress).setVisibility(View.GONE);
			if (result != null) {
				mImageView.setVisibility(View.VISIBLE);
				mImageView.setImageDrawable(result);
			} else if (mOomError) {
				findViewById(R.id.image_error).setVisibility(View.VISIBLE);
			}
		}
	}
}
