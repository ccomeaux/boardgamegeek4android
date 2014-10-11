package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.GeekListUtils;
import com.boardgamegeek.util.UIUtils;

public class GeekListItemFragment extends SherlockFragment {
	private String mTitle;
	private String mImageUrl;
	private long mPostedDate;
	private long mEditedDate;
	private String mBody;

	@InjectView(R.id.title) TextView mTitleView;
	@InjectView(R.id.image) ImageView mImageView;
	@InjectView(R.id.posted_date) TextView mPostedDateView;
	@InjectView(R.id.edited_date) TextView mEditedDateView;
	@InjectView(R.id.body) WebView mBodyView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mTitle = intent.getStringExtra(GeekListUtils.KEY_TITLE);
		int imageId = intent.getIntExtra(GeekListUtils.KEY_IMAGE_ID, BggContract.INVALID_ID);
		if (imageId != BggContract.INVALID_ID) {
			mImageUrl = ActivityUtils.createImagePath(imageId);
		}
		mPostedDate = intent.getLongExtra(GeekListUtils.KEY_POSTED_DATE, 0);
		mEditedDate = intent.getLongExtra(GeekListUtils.KEY_EDITED_DATE, 0);
		mBody = intent.getStringExtra(GeekListUtils.KEY_BODY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_item, container, false);
		ButterKnife.inject(this, rootView);

		mTitleView.setText(mTitle);
		ActivityUtils.safelyLoadImage(mImageView, mImageUrl);
		mPostedDateView.setText(getString(R.string.posted_prefix,
			DateTimeUtils.formatForumDate(getActivity(), mPostedDate)));
		mEditedDateView.setText(getString(R.string.edited_prefix,
			DateTimeUtils.formatForumDate(getActivity(), mEditedDate)));
		String content = GeekListUtils.convertBoardGameGeekXmlText(mBody);
		UIUtils.setWebViewText(mBodyView, content);

		return rootView;
	}
}
