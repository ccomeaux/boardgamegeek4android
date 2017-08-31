package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.UIUtils;

import java.text.NumberFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class ArticleFragment extends Fragment {
	private static final String KEY_USER = "USER";
	private static final String KEY_POST_DATE = "POST_DATE";
	private static final String KEY_EDIT_DATE = "EDIT_DATE";
	private static final String KEY_EDIT_COUNT = "EDIT_COUNT";
	private static final String KEY_BODY = "BODY";

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

	public static ArticleFragment newInstance(String user, long postDate, long editDate, int editCount, String body) {
		Bundle args = new Bundle();
		args.putString(KEY_USER, user);
		args.putLong(KEY_POST_DATE, postDate);
		args.putLong(KEY_EDIT_DATE, editDate);
		args.putInt(KEY_EDIT_COUNT, editCount);
		args.putString(KEY_BODY, body);

		ArticleFragment fragment = new ArticleFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		readBundle(getArguments());
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

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		user = bundle.getString(KEY_USER);
		postDate = bundle.getLong(KEY_POST_DATE, 0);
		editDate = bundle.getLong(KEY_EDIT_DATE, 0);
		editCount = bundle.getInt(KEY_EDIT_COUNT, 0);
		body = bundle.getString(KEY_BODY);
	}
}
