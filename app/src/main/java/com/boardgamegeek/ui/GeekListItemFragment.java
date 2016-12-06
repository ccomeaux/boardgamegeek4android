package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class GeekListItemFragment extends Fragment {
	private int order;
	private String geekListTitle;
	private String type;
	private String username;
	private int numberOfThumbs;
	private long postedDate;
	private long editedDate;
	private String body;
	private Palette.Swatch swatch;
	private XmlConverter xmlConverter;

	private Unbinder unbinder;
	@BindView(R.id.order) TextView orderView;
	@BindView(R.id.list_title) TextView geekListTitleView;
	@BindView(R.id.type) TextView typeView;
	@BindView(R.id.byline_container) View bylineContainer;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.thumbs) TextView thumbsView;
	@BindView(R.id.posted_date) TimestampView postedDateView;
	@BindView(R.id.datetime_divider) View datetimeDividerView;
	@BindView(R.id.edited_date) TimestampView editedDateView;
	@BindView(R.id.body) WebView bodyView;
	@BindViews({
		R.id.order,
		R.id.list_title,
		R.id.username,
		R.id.type,
		R.id.thumbs,
		R.id.posted_date,
		R.id.edited_date
	}) List<TextView> colorizedTextViews;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		order = intent.getIntExtra(ActivityUtils.KEY_ORDER, 0);
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		type = intent.getStringExtra(ActivityUtils.KEY_TYPE);
		username = intent.getStringExtra(ActivityUtils.KEY_USERNAME);
		numberOfThumbs = intent.getIntExtra(ActivityUtils.KEY_THUMBS, 0);
		postedDate = intent.getLongExtra(ActivityUtils.KEY_POSTED_DATE, 0);
		editedDate = intent.getLongExtra(ActivityUtils.KEY_EDITED_DATE, 0);
		body = intent.getStringExtra(ActivityUtils.KEY_BODY);
		xmlConverter = new XmlConverter();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_item, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		applySwatch();

		orderView.setText(String.valueOf(order));
		geekListTitleView.setText(geekListTitle);
		typeView.setText(type);
		usernameView.setText(username);
		thumbsView.setText(String.valueOf(numberOfThumbs));
		String content = xmlConverter.toHtml(body);
		UIUtils.setWebViewText(bodyView, content);
		postedDateView.setTimestamp(postedDate);
		if (editedDate == postedDate) {
			editedDateView.setVisibility(View.GONE);
			datetimeDividerView.setVisibility(View.GONE);
		} else {
			editedDateView.setVisibility(View.VISIBLE);
			datetimeDividerView.setVisibility(View.VISIBLE);
			editedDateView.setTimestamp(editedDate);
		}
		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	public void setSwatch(Swatch swatch) {
		this.swatch = swatch;
		applySwatch();
	}

	private void applySwatch() {
		if (swatch != null) {
			if (bylineContainer != null) bylineContainer.setBackgroundColor(swatch.getRgb());
			ButterKnife.apply(colorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
		}
	}
}
