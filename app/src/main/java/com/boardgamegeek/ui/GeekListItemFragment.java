package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.VersionUtils;
import com.boardgamegeek.util.XmlConverter;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;

public class GeekListItemFragment extends Fragment implements ImageUtils.Callback {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	private String mOrder;
	private String mTitle;
	private String mType;
	private int mImageId;
	private String mUsername;
	private int mThumbs;
	private long mPostedDate;
	private long mEditedDate;
	private String mBody;
	private Palette.Swatch mSwatch;

	private ViewGroup mRootView;
	@SuppressWarnings("unused") @InjectView(R.id.hero_container) View mHeroContainer;
	@SuppressWarnings("unused") @InjectView(R.id.header_container) View mHeaderContainer;
	@SuppressWarnings("unused") @InjectView(R.id.order) TextView mOrderView;
	@SuppressWarnings("unused") @InjectView(R.id.title) TextView mTitleView;
	@SuppressWarnings("unused") @InjectView(R.id.type) TextView mTypeView;
	@SuppressWarnings("unused") @InjectView(R.id.image) ImageView mImageView;
	@SuppressWarnings("unused") @InjectView(R.id.author_container) View mAuthorContainer;
	@SuppressWarnings("unused") @InjectView(R.id.username) TextView mUsernameView;
	@SuppressWarnings("unused") @InjectView(R.id.thumbs) TextView mThumbsView;
	@SuppressWarnings("unused") @InjectView(R.id.posted_date) TextView mPostedDateView;
	@SuppressWarnings("unused") @InjectView(R.id.edited_date) TextView mEditedDateView;
	@SuppressWarnings("unused") @InjectView(R.id.body) WebView mBodyView;
	@SuppressWarnings("unused") @InjectViews({
		R.id.username,
		R.id.thumbs,
		R.id.posted_date,
		R.id.edited_date
	}) List<TextView> mColorizedTextViews;

	private final ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
		= new ViewTreeObserver.OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			ImageUtils.resizeImagePerAspectRatio(mImageView, mRootView.getHeight() / 3, mHeroContainer);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mOrder = intent.getStringExtra(ActivityUtils.KEY_ORDER);
		mTitle = intent.getStringExtra(ActivityUtils.KEY_NAME);
		mType = intent.getStringExtra(ActivityUtils.KEY_TYPE);
		mImageId = intent.getIntExtra(ActivityUtils.KEY_IMAGE_ID, BggContract.INVALID_ID);
		mUsername = intent.getStringExtra(ActivityUtils.KEY_USERNAME);
		mThumbs = intent.getIntExtra(ActivityUtils.KEY_THUMBS, 0);
		mPostedDate = intent.getLongExtra(ActivityUtils.KEY_POSTED_DATE, 0);
		mEditedDate = intent.getLongExtra(ActivityUtils.KEY_EDITED_DATE, 0);
		mBody = intent.getStringExtra(ActivityUtils.KEY_BODY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_item, container, false);
		ButterKnife.inject(this, mRootView);

		applySwatch();
		ScrimUtils.applyDefaultScrim(mHeaderContainer);
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
		mPostedDateView.setTag(mPostedDate);
		mEditedDateView.setTag(mEditedDate);
		String content = new XmlConverter().toHtml(mBody);
		UIUtils.setWebViewText(mBodyView, content);

		updateTimeBasedUi();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
		mUpdaterRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);

		return mRootView;
	}

	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (mPostedDateView != null) {
			mPostedDateView.setText(getString(R.string.posted_prefix, DateTimeUtils.formatForumDate(getActivity(), mPostedDate)));
		}
		if (mEditedDateView != null) {
			mEditedDateView.setText(getString(R.string.edited_prefix, DateTimeUtils.formatForumDate(getActivity(), mEditedDate)));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mUpdaterRunnable != null) {
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
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
		mSwatch = PaletteUtils.getInverseSwatch(palette);
		applySwatch();
	}

	private void applySwatch() {
		if (mAuthorContainer != null && mSwatch != null) {
			mAuthorContainer.setBackgroundColor(mSwatch.getRgb());
			ButterKnife.apply(mColorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, mSwatch);
		}
	}
}
