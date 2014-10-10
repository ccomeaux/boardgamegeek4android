package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.util.GeekListUtils;
import com.boardgamegeek.util.UIUtils;

public class GeekListItemFragment extends SherlockFragment {
	private String mTitle;
	private String mBody;

	@InjectView(R.id.title) TextView mTitleView;
	@InjectView(R.id.body) WebView mBodyView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mTitle = intent.getStringExtra(GeekListUtils.KEY_GEEKLIST_TITLE);
		mBody = intent.getStringExtra(GeekListUtils.KEY_GEEKLIST_BODY);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_item, container, false);
		ButterKnife.inject(this, rootView);

		mTitleView.setText(mTitle);
		String content = GeekListUtils.convertBoardGameGeekXmlText(mBody);
		UIUtils.setWebViewText(mBodyView, content);

		return rootView;
	}
}
