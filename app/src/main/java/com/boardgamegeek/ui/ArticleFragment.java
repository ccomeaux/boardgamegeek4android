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

import butterknife.Bind;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class ArticleFragment extends Fragment {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler timeHintUpdateHandler = new Handler();
	private Runnable timeHintUpdateRunnable = null;
	private String user;
	private long postDate;
	private long editDate;
	private int editCount;
	private String body;

	@SuppressWarnings("unused") @Bind(R.id.username) TextView usernameView;
	@SuppressWarnings("unused") @Bind(R.id.post_date) TextView postDateView;
	@SuppressWarnings("unused") @Bind(R.id.edit_date) TextView editDateView;
	@SuppressWarnings("unused") @Bind(R.id.edit_count) TextView editCountView;
	@SuppressWarnings("unused") @Bind(R.id.body) WebView bodyView;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		timeHintUpdateHandler = new Handler();
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		user = intent.getStringExtra(ActivityUtils.KEY_USER);
		postDate = intent.getLongExtra(ActivityUtils.KEY_POST_DATE, 0);
		editDate = intent.getLongExtra(ActivityUtils.KEY_EDIT_DATE, 0);
		editCount = intent.getIntExtra(ActivityUtils.KEY_EDIT_COUNT, 0);
		body = intent.getStringExtra(ActivityUtils.KEY_BODY);
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_article, container, false);
		ButterKnife.bind(this, rootView);

		updateTimeBasedUi();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
		timeHintUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);

		usernameView.setText(user);
		if (editCount > 0) {
			editCountView.setText(getResources().getQuantityString(R.plurals.edit_count, editCount, editCount));
		} else {
			editCountView.setVisibility(View.GONE);
		}
		UIUtils.setWebViewText(bodyView, body);

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
	}

	@DebugLog
	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		postDateView.setText(postDate == 0 ?
			getString(R.string.text_not_available) :
			getString(R.string.posted_prefix, DateTimeUtils.formatForumDate(getActivity(), postDate)));
		if (editDate != postDate) {
			editDateView.setText(editDate == 0 ?
				getString(R.string.text_not_available) :
				getString(R.string.last_edited_prefix, DateTimeUtils.formatForumDate(getActivity(), editDate)));
		} else {
			editDateView.setVisibility(View.GONE);
		}
	}
}
