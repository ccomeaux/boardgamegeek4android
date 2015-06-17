package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

public class GeekListDescriptionFragment extends Fragment {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	@SuppressWarnings("unused") @InjectView(R.id.username) TextView mUsernameView;
	@SuppressWarnings("unused") @InjectView(R.id.items) TextView mItemsView;
	@SuppressWarnings("unused") @InjectView(R.id.thumbs) TextView mThumbsView;
	@SuppressWarnings("unused") @InjectView(R.id.posted_date) TextView mPostedDateView;
	@SuppressWarnings("unused") @InjectView(R.id.edited_date) TextView mEditedDateView;
	@SuppressWarnings("unused") @InjectView(R.id.body) WebView mBodyView;
	private GeekList mGeekList;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGeekList = intent.getParcelableExtra(ActivityUtils.KEY_GEEKLIST);
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_description, container, false);
		ButterKnife.inject(this, rootView);

		mUsernameView.setText(mGeekList.getUsername());
		mItemsView.setText(getString(R.string.items_suffix, mGeekList.getNumberOfItems()));
		mThumbsView.setText(getString(R.string.thumbs_suffix, mGeekList.getThumbs()));
		String content = new XmlConverter().toHtml(mGeekList.getDescription());
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

		return rootView;
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		if (mUpdaterRunnable != null) {
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	@DebugLog
	public void onPause() {
		super.onPause();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
	}

	@DebugLog
	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (mPostedDateView != null) {
			mPostedDateView.setText(getString(R.string.posted_prefix, DateTimeUtils.formatForumDate(getActivity(), mGeekList.getPostDate())));
		}
		if (mEditedDateView != null) {
			mEditedDateView.setText(getString(R.string.edited_prefix, DateTimeUtils.formatForumDate(getActivity(), mGeekList.getEditDate())));
		}
	}
}
