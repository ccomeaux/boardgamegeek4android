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
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

public class ArticleFragment extends Fragment {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	private String mUser;
	private long mPostDate;
	private long mEditDate;
	private int mEditCount;
	private String mBody;

	@SuppressWarnings("unused") @InjectView(R.id.article_username) TextView mUserView;
	@SuppressWarnings("unused") @InjectView(R.id.article_postdate) TextView mPostDateView;
	@SuppressWarnings("unused") @InjectView(R.id.article_editdate) TextView mEditDateView;
	@SuppressWarnings("unused") @InjectView(R.id.article_editcount) TextView mEditCountView;
	@SuppressWarnings("unused") @InjectView(R.id.article_body) WebView mBodyView;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mUser = intent.getStringExtra(ActivityUtils.KEY_USER);
		mPostDate = intent.getLongExtra(ActivityUtils.KEY_POST_DATE, 0);
		mEditDate = intent.getLongExtra(ActivityUtils.KEY_EDIT_DATE, 0);
		mEditCount = intent.getIntExtra(ActivityUtils.KEY_EDIT_COUNT, 0);
		mBody = intent.getStringExtra(ActivityUtils.KEY_BODY);
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_article, container, false);
		ButterKnife.inject(this, rootView);

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

		mUserView.setText(mUser);
		if (mEditCount > 0) {
			mEditCountView.setText(getResources().getQuantityString(R.plurals.edit_count, mEditCount, mEditCount));
		} else {
			mEditCountView.setVisibility(View.GONE);
		}
		UIUtils.setWebViewText(mBodyView, mBody);

		return rootView;
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

	@DebugLog
	private void updateTimeBasedUi() {
		mPostDateView.setText(mPostDate == 0 ?
			getString(R.string.text_not_available) :
			getString(R.string.posted_prefix, DateTimeUtils.formatForumDate(getActivity(), mPostDate)));
		if (mEditDate != mPostDate) {
			mEditDateView.setText(mEditDate == 0 ?
				getString(R.string.text_not_available) :
				getString(R.string.last_edited_prefix, DateTimeUtils.formatForumDate(getActivity(), mEditDate)));
		} else {
			mEditDateView.setVisibility(View.GONE);
		}
	}
}
