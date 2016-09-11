package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

import java.text.NumberFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class ArticleFragment extends Fragment {
	private String user;
	private long postDate;
	private long editDate;
	private int editCount;
	private String body;

	private Unbinder unbinder;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.post_date) TimestampView postDateView;
	@BindView(R.id.edit_date) TimestampView editDateView;
	@BindView(R.id.body) WebView bodyView;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		unbinder = ButterKnife.bind(this, rootView);

		usernameView.setText(user);
		postDateView.setTimestamp(postDate);
		if (editCount > 0) {
			editDateView.setFormat(getResources().getQuantityString(R.plurals.edit_timestamp, editCount));
			editDateView.setFormatArg(NumberFormat.getNumberInstance().format(editCount));
			editDateView.setTimestamp(editDate);
			editDateView.setVisibility(View.VISIBLE);
		} else {
			editDateView.setVisibility(View.GONE);
		}
		UIUtils.setWebViewText(bodyView, body);

		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}
}
