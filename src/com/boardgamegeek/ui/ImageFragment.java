package com.boardgamegeek.ui;

import android.content.Intent;
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
	private String mImageUrl;
	private ImageFetcher mImageFetcher;
	private ImageView mImageView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mImageUrl = intent.getStringExtra(ImageActivity.KEY_IMAGE_URL);

		if (TextUtils.isEmpty(mImageUrl)) {
			return;
		}

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_image, null);

		mImageView = (ImageView) rootView.findViewById(R.id.image);
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
}
