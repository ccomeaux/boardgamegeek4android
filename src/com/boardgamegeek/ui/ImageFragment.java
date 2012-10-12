package com.boardgamegeek.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.UIUtils;

public class ImageFragment extends SherlockFragment {

	private Uri mGameUri;
	private String mImageUrl;
	private ImageFetcher mImageFetcher;
	private ImageView mImageView;

	// private View mProgressView;
	// private View mErrorView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameUri = intent.getData();
		mImageUrl = intent.getStringExtra(ImageActivity.KEY_IMAGE_URL);

		if (mGameUri == null || TextUtils.isEmpty(mImageUrl)) {
			return;
		}

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		// mImageFetcher.setImageFadeIn(false);
		// mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.avatar_size));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_image, null);

		mImageView = (ImageView) rootView.findViewById(R.id.image);
		// mProgressView = rootView.findViewById(R.id.image_progress);
		// mErrorView = rootView.findViewById(R.id.image_error);

		mImageFetcher.loadImage(mImageUrl, mImageView);

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.flushCache();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
	}

	// private class ImageTask extends AsyncTask<String, Void, Drawable> {
	// private boolean mOomError = false;
	//
	// @Override
	// protected void onPreExecute() {
	// mProgressView.setVisibility(View.VISIBLE);
	// }
	//
	// @Override
	// protected Drawable doInBackground(String... params) {
	// try {
	// return ImageUtils.getImage(getActivity(), params[0]);
	// } catch (OutOfMemoryError e) {
	// mOomError = true;
	// }
	// return null;
	// }
	//
	// @Override
	// protected void onPostExecute(Drawable result) {
	// mProgressView.setVisibility(View.GONE);
	// if (result != null) {
	// mImageView.setVisibility(View.VISIBLE);
	// mImageView.setImageDrawable(result);
	// } else if (mOomError) {
	// mErrorView.setVisibility(View.VISIBLE);
	// }
	// }
	// }
}
