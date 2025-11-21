package com.boardgamegeek.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.boardgamegeek.databinding.FragmentGeeklistItemBinding;

public class GeekListItemFragment extends Fragment {
	private static final String KEY_ORDER = "GEEK_LIST_ORDER";
	private static final String KEY_TITLE = "GEEK_LIST_TITLE";
	private static final String KEY_TYPE = "GEEK_LIST_TYPE";
	private static final String KEY_USERNAME = "GEEK_LIST_USERNAME";
	private static final String KEY_THUMBS = "GEEK_LIST_THUMBS";
	private static final String KEY_POSTED_DATE = "GEEK_LIST_POSTED_DATE";
	private static final String KEY_EDITED_DATE = "GEEK_LIST_EDITED_DATE";
	private static final String KEY_BODY = "GEEK_LIST_BODY";

	private int order;
	private String geekListTitle;
	private String type;
	private String username;
	private int numberOfThumbs;
	private long postedDate;
	private long editedDate;
	private String body;
	private XmlConverter xmlConverter;

	private FragmentGeeklistItemBinding binding;
	private TextView orderView;
	private TextView geekListTitleView;
	private TextView typeView;
	private View bylineContainer;
	private TextView usernameView;
	private TextView thumbsView;
	private TimestampView postedDateView;
	private View datetimeDividerView;
	private TimestampView editedDateView;
	private WebView bodyView;
	private List<TextView> colorizedTextViews;

	public static GeekListItemFragment newInstance(int order, String title, String type, String username, int numberOfThumbs, long postedDate, long editedDate, String body) {
		Bundle args = new Bundle();
		args.putInt(KEY_ORDER, order);
		args.putString(KEY_TITLE, title);
		args.putString(KEY_TYPE, type);
		args.putString(KEY_USERNAME, username);
		args.putInt(KEY_THUMBS, numberOfThumbs);
		args.putLong(KEY_POSTED_DATE, postedDate);
		args.putLong(KEY_EDITED_DATE, editedDate);
		args.putString(KEY_BODY, body);

		GeekListItemFragment fragment = new GeekListItemFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		xmlConverter = new XmlConverter();
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		order = bundle.getInt(KEY_ORDER, 0);
		geekListTitle = bundle.getString(KEY_TITLE);
		type = bundle.getString(KEY_TYPE);
		username = bundle.getString(KEY_USERNAME);
		numberOfThumbs = bundle.getInt(KEY_THUMBS, 0);
		postedDate = bundle.getLong(KEY_POSTED_DATE, 0);
		editedDate = bundle.getLong(KEY_EDITED_DATE, 0);
		body = bundle.getString(KEY_BODY);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentGeeklistItemBinding.inflate(inflater, container, false);
		orderView = binding.order;
		geekListTitleView = binding.listTitle;
		typeView = binding.type;
		bylineContainer = binding.bylineContainer;
		usernameView = binding.username;
		thumbsView = binding.thumbs;
		postedDateView = binding.postedDate;
		datetimeDividerView = binding.datetimeDivider;
		editedDateView = binding.editedDate;
		bodyView = binding.body;
		
		// Manually create the collection of views that need to be colorized
		colorizedTextViews = java.util.Arrays.asList(
			orderView,
			geekListTitleView,
			usernameView,
			typeView,
			thumbsView,
			(TextView) postedDateView,
			(TextView) editedDateView
		);
		
		populateUi();
		return binding.getRoot();
	}

	private void populateUi() {
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
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
