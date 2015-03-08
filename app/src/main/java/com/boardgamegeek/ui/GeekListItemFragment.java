package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.GeekListUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.ScrimUtil;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.VersionUtils;
import com.boardgamegeek.util.XmlConverter;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;

public class GeekListItemFragment extends Fragment implements ImageUtils.Callback {
	private String mOrder;
	private String mTitle;
	private String mType;
	private int mImageId;
	private String mUsername;
	private int mThumbs;
	private long mPostedDate;
	private long mEditedDate;
	private String mBody;

	private ViewGroup mRootView;
	@InjectView(R.id.hero_container) View mHeroContainer;
	@InjectView(R.id.header_container) View mHeaderContainer;
	@InjectView(R.id.order) TextView mOrderView;
	@InjectView(R.id.title) TextView mTitleView;
	@InjectView(R.id.type) TextView mTypeView;
	@InjectView(R.id.image) ImageView mImageView;
	@InjectView(R.id.author_container) View mAuthorContainer;
	@InjectView(R.id.username) TextView mUsernameView;
	@InjectView(R.id.thumbs) TextView mThumbsView;
	@InjectView(R.id.posted_date) TextView mPostedDateView;
	@InjectView(R.id.edited_date) TextView mEditedDateView;
	@InjectView(R.id.body) WebView mBodyView;
	@InjectViews({
		R.id.username,
		R.id.thumbs,
		R.id.posted_date,
		R.id.edited_date
	}) List<TextView> mColorizedTextViews;

	private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
		= new ViewTreeObserver.OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			ImageUtils.resizeImagePerAspectRatio(mImageView, mRootView.getHeight() / 3, mHeroContainer);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mOrder = intent.getStringExtra(GeekListUtils.KEY_ORDER);
		mTitle = intent.getStringExtra(GeekListUtils.KEY_NAME);
		mType = intent.getStringExtra(GeekListUtils.KEY_TYPE);
		mImageId = intent.getIntExtra(GeekListUtils.KEY_IMAGE_ID, BggContract.INVALID_ID);
		mUsername = intent.getStringExtra(GeekListUtils.KEY_USERNAME);
		mThumbs = intent.getIntExtra(GeekListUtils.KEY_THUMBS, 0);
		mPostedDate = intent.getLongExtra(GeekListUtils.KEY_POSTED_DATE, 0);
		mEditedDate = intent.getLongExtra(GeekListUtils.KEY_EDITED_DATE, 0);
		mBody = intent.getStringExtra(GeekListUtils.KEY_BODY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_item, container, false);
		ButterKnife.inject(this, mRootView);

		ScrimUtil.applyDefaultScrim(mHeaderContainer);
		ViewTreeObserver vto = mRootView.getViewTreeObserver();
		if (vto.isAlive()) {
			vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
		}

		mOrderView.setText(mOrder);
		mTitleView.setText(mTitle);
		mTypeView.setText(mType);
		ImageUtils.safelyLoadImage(mImageView, mImageId, this);
		mUsernameView.setText(mUsername);
		mThumbsView.setText(getString(R.string.thumbs_suffix, mThumbs));
		mPostedDateView.setText(getString(R.string.posted_prefix,
			DateTimeUtils.formatForumDate(getActivity(), mPostedDate)));
		mEditedDateView.setText(getString(R.string.edited_prefix,
			DateTimeUtils.formatForumDate(getActivity(), mEditedDate)));
		String content = new XmlConverter().toHtml(mBody);
		UIUtils.setWebViewText(mBodyView, content);

		return mRootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ButterKnife.reset(this);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mRootView == null) {
			return;
		}

		ViewTreeObserver vto = mRootView.getViewTreeObserver();
		if (vto.isAlive()) {
			if (VersionUtils.hasJellyBean()) {
				vto.removeOnGlobalLayoutListener(mGlobalLayoutListener);
			} else {
				//noinspection deprecation
				vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
			}
		}
	}

	@Override
	public void onPaletteGenerated(Palette palette) {
		Palette.Swatch swatch = PaletteUtils.getInverseSwatch(palette);
		mAuthorContainer.setBackgroundColor(swatch.getRgb());
		ButterKnife.apply(mColorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
	}
}
