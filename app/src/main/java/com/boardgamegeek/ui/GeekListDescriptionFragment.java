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
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GeekListDescriptionFragment extends Fragment {
	@InjectView(R.id.username) TextView mUsernameView;
	@InjectView(R.id.items) TextView mItemsView;
	@InjectView(R.id.thumbs) TextView mThumbsView;
	@InjectView(R.id.posted_date) TextView mPostedDateView;
	@InjectView(R.id.edited_date) TextView mEditedDateView;
	@InjectView(R.id.body) WebView mBodyView;
	private GeekList mGeekList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGeekList = intent.getParcelableExtra(ActivityUtils.KEY_GEEKLIST);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_description, container, false);
		ButterKnife.inject(this, rootView);

		mUsernameView.setText(mGeekList.getUsername());
		mItemsView.setText(getString(R.string.items_suffix, mGeekList.getNumberOfItems()));
		mThumbsView.setText(getString(R.string.thumbs_suffix, mGeekList.getThumbs()));
		mPostedDateView.setText(getString(R.string.posted_prefix,
			DateTimeUtils.formatForumDate(getActivity(), mGeekList.getPostDate())));
		mEditedDateView.setText(getString(R.string.edited_prefix,
			DateTimeUtils.formatForumDate(getActivity(), mGeekList.getEditDate())));
		String content = new XmlConverter().toHtml(mGeekList.getDescription());
		UIUtils.setWebViewText(mBodyView, content);

		return rootView;
	}
}
