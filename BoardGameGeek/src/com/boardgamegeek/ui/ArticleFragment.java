package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

public class ArticleFragment extends SherlockFragment {
	private String mUser;
	private long mDate;
	private String mBody;

	private TextView mUserView;
	private TextView mDateView;
	private WebView mBodyView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mUser = intent.getStringExtra(ForumsUtils.KEY_USER);
		mDate = intent.getLongExtra(ForumsUtils.KEY_DATE, 0);
		mBody = intent.getStringExtra(ForumsUtils.KEY_BODY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_article, null);

		mUserView = (TextView) rootView.findViewById(R.id.article_username);
		mDateView = (TextView) rootView.findViewById(R.id.article_editdate);
		mBodyView = (WebView) rootView.findViewById(R.id.article_body);

		mUserView.setText(mUser);
		mDateView.setText(mDate == 0 ? getString(R.string.text_not_available) : DateTimeUtils.formatForumDate(
			getActivity(), mDate));
		UIUtils.setWebViewText(mBodyView, mBody);

		return rootView;
	}
}
