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
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ArticleFragment extends Fragment {
	private String mUser;
	private long mPostDate;
	private long mEditDate;
	private int mEditCount;
	private String mBody;

	@InjectView(R.id.article_username) TextView mUserView;
	@InjectView(R.id.article_postdate) TextView mPostDateView;
	@InjectView(R.id.article_editdate) TextView mEditDateView;
	@InjectView(R.id.article_editcount) TextView mEditCountView;
	@InjectView(R.id.article_body) WebView mBodyView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mUser = intent.getStringExtra(ForumsUtils.KEY_USER);
		mPostDate = intent.getLongExtra(ForumsUtils.KEY_POST_DATE, 0);
		mEditDate = intent.getLongExtra(ForumsUtils.KEY_EDIT_DATE, 0);
		mEditCount = intent.getIntExtra(ForumsUtils.KEY_EDIT_COUNT, 0);
		mBody = intent.getStringExtra(ForumsUtils.KEY_BODY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_article, container, false);
		ButterKnife.inject(this, rootView);

		mUserView.setText(mUser);
		mPostDateView.setText(mPostDate == 0 ? getString(R.string.text_not_available) :
			getString(R.string.posted_prefix, DateTimeUtils.formatForumDate(getActivity(), mPostDate)));
		if (mEditDate != mPostDate) {
			mEditDateView.setText(mEditDate == 0 ? getString(R.string.text_not_available) :
				getString(R.string.last_edited_prefix, DateTimeUtils.formatForumDate(getActivity(), mEditDate)));
		} else {
			mEditDateView.setVisibility(View.GONE);
		}
		if (mEditCount > 0) {
			mEditCountView.setText(getResources().getQuantityString(R.plurals.edit_count, mEditCount, mEditCount));
		} else {
			mEditCountView.setVisibility(View.GONE);
		}
		UIUtils.setWebViewText(mBodyView, mBody);

		return rootView;
	}
}
