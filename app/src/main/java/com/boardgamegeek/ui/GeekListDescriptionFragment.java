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
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class GeekListDescriptionFragment extends Fragment {
	private Unbinder unbinder;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.items) TextView itemCountView;
	@BindView(R.id.thumbs) TextView thumbCountView;
	@BindView(R.id.posted_date) TimestampView postedDateView;
	@BindView(R.id.edited_date) TimestampView editedDateView;
	@BindView(R.id.body) WebView bodyView;
	private GeekList geekList;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		geekList = intent.getParcelableExtra(ActivityUtils.KEY_GEEKLIST);
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_description, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		usernameView.setText(geekList.getUsername());
		itemCountView.setText(getString(R.string.items_suffix, geekList.getNumberOfItems()));
		thumbCountView.setText(getString(R.string.thumbs_suffix, geekList.getThumbs()));
		String content = new XmlConverter().toHtml(geekList.getDescription());
		UIUtils.setWebViewText(bodyView, content);
		postedDateView.setTimestamp(geekList.getPostDate(), R.string.posted_prefix);
		editedDateView.setTimestamp(geekList.getEditDate(), R.string.edited_prefix);

		return rootView;
	}

	@Override
	public void onDestroyView() {
		if (unbinder != null) unbinder.unbind();
		super.onDestroyView();
	}
}
