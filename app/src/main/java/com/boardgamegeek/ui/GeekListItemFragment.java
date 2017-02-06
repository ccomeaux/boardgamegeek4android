package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class GeekListItemFragment extends Fragment implements ImageUtils.Callback {
	private String order;
	private String title;
	private String type;
	private int imageId;
	private String username;
	private int numberOfThumbs;
	private long postedDate;
	private long editedDate;
	private String body;
	private Palette.Swatch swatch;
	private XmlConverter xmlConverter;

	private Unbinder unbinder;
	private ViewGroup rootView;
	@BindView(R.id.hero_container) View heroContainer;
	@BindView(R.id.header_container) View headerContainer;
	@BindView(R.id.order) TextView orderView;
	@BindView(R.id.title) TextView titleView;
	@BindView(R.id.type) TextView typeView;
	@BindView(R.id.image) ImageView imageView;
	@BindView(R.id.author_container) View authorContainer;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.thumbs) TextView thumbsView;
	@BindView(R.id.posted_date) TimestampView postedDateView;
	@BindView(R.id.edited_date) TimestampView editedDateView;
	@BindView(R.id.body) WebView bodyView;
	@BindViews({
		R.id.username,
		R.id.thumbs,
		R.id.posted_date,
		R.id.edited_date
	}) List<TextView> colorizedTextViews;

	private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener
		= new ViewTreeObserver.OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			ImageUtils.resizeImagePerAspectRatio(imageView, rootView.getHeight() / 3, heroContainer);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		order = intent.getStringExtra(ActivityUtils.KEY_ORDER);
		title = intent.getStringExtra(ActivityUtils.KEY_NAME);
		type = intent.getStringExtra(ActivityUtils.KEY_TYPE);
		imageId = intent.getIntExtra(ActivityUtils.KEY_IMAGE_ID, BggContract.INVALID_ID);
		username = intent.getStringExtra(ActivityUtils.KEY_USERNAME);
		numberOfThumbs = intent.getIntExtra(ActivityUtils.KEY_THUMBS, 0);
		postedDate = intent.getLongExtra(ActivityUtils.KEY_POSTED_DATE, 0);
		editedDate = intent.getLongExtra(ActivityUtils.KEY_EDITED_DATE, 0);
		body = intent.getStringExtra(ActivityUtils.KEY_BODY);
		xmlConverter = new XmlConverter();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_item, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		applySwatch();
		ScrimUtils.applyDefaultScrim(headerContainer);
		ViewTreeObserver vto = rootView.getViewTreeObserver();
		if (vto.isAlive()) {
			vto.addOnGlobalLayoutListener(globalLayoutListener);
		}

		orderView.setText(order);
		titleView.setText(title);
		typeView.setText(type);
		ImageUtils.safelyLoadImage(imageView, imageId, this);
		usernameView.setText(username);
		thumbsView.setText(getResources().getQuantityString(R.plurals.thumbs_suffix, numberOfThumbs, numberOfThumbs));
		String content = xmlConverter.toHtml(body);
		UIUtils.setWebViewText(bodyView, content);
		postedDateView.setTimestamp(postedDate);
		editedDateView.setTimestamp(editedDate);

		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (rootView == null) {
			return;
		}

		ViewTreeObserver vto = rootView.getViewTreeObserver();
		if (vto.isAlive()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				vto.removeOnGlobalLayoutListener(globalLayoutListener);
			} else {
				//noinspection deprecation
				vto.removeGlobalOnLayoutListener(globalLayoutListener);
			}
		}
	}

	@Override
	public void onSuccessfulImageLoad(Palette palette) {
		if (!isAdded()) {
			return;
		}
		swatch = PaletteUtils.getInverseSwatch(palette, ContextCompat.getColor(getActivity(), R.color.info_background));
		applySwatch();
	}

	@Override
	public void onFailedImageLoad() {
	}

	private void applySwatch() {
		if (authorContainer != null && swatch != null) {
			authorContainer.setBackgroundColor(swatch.getRgb());
			ButterKnife.apply(colorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
		}
	}
}
