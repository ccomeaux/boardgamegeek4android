package com.boardgamegeek.ui;

import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;

import com.boardgamegeek.databinding.FragmentGeeklistDescriptionBinding;

public class GeekListDescriptionFragment extends Fragment {
	private FragmentGeeklistDescriptionBinding binding;
	private ContentLoadingProgressBar progressBar;
	private View container;
	private TextView usernameView;
	private TextView itemCountView;
	private TextView thumbCountView;
	private TimestampView postedDateView;
	private TimestampView editedDateView;
	private WebView bodyView;
	private XmlConverter xmlConverter;

	public static GeekListDescriptionFragment newInstance() {
		return new GeekListDescriptionFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		xmlConverter = new XmlConverter();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentGeeklistDescriptionBinding.inflate(inflater, container, false);
		progressBar = binding.progress;
		this.container = binding.container;
		usernameView = binding.username;
		itemCountView = binding.items;
		thumbCountView = binding.thumbs;
		postedDateView = binding.postedDate;
		editedDateView = binding.editedDate;
		bodyView = binding.body;
		ViewGroup rootView = (ViewGroup) binding.getRoot();
		//noinspection deprecation
		rootView.setBackgroundDrawable(null);
		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
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
