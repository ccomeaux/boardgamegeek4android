package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.model.GeekList;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class GeekListDescriptionFragment extends Fragment {
	private Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressBar;
	@BindView(R.id.container) View container;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.items) TextView itemCountView;
	@BindView(R.id.thumbs) TextView thumbCountView;
	@BindView(R.id.posted_date) TimestampView postedDateView;
	@BindView(R.id.edited_date) TimestampView editedDateView;
	@BindView(R.id.body) WebView bodyView;
	private XmlConverter xmlConverter;

	public static GeekListDescriptionFragment newInstance() {
		return new GeekListDescriptionFragment();
	}

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		xmlConverter = new XmlConverter();
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_description, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		//noinspection deprecation
		rootView.setBackgroundDrawable(null);
		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	public void setData(GeekList geekList) {
		if (geekList == null) return;
		usernameView.setText(geekList.getUsername());
		itemCountView.setText(String.valueOf(geekList.getNumberOfItems()));
		thumbCountView.setText(String.valueOf(geekList.getNumberOfThumbs()));
		UIUtils.setWebViewText(bodyView, xmlConverter.toHtml(geekList.getDescription()));
		postedDateView.setTimestamp(geekList.getPostTicks());
		editedDateView.setTimestamp(geekList.getEditTicks());

		container.setVisibility(View.VISIBLE);
		progressBar.hide();
	}
}
