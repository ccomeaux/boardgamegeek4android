package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlConverter;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;

public class GeekListItemFragment extends Fragment implements ImageUtils.Callback {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler timeHintUpdateHandler = new Handler();
	private Runnable timeHintUpdateRunnable = null;
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

	private ViewGroup rootView;
	@SuppressWarnings("unused") @InjectView(R.id.hero_container) View heroContainer;
	@SuppressWarnings("unused") @InjectView(R.id.header_container) View headerContainer;
	@SuppressWarnings("unused") @InjectView(R.id.order) TextView orderView;
	@SuppressWarnings("unused") @InjectView(R.id.title) TextView titleView;
	@SuppressWarnings("unused") @InjectView(R.id.type) TextView typeView;
	@SuppressWarnings("unused") @InjectView(R.id.image) ImageView imageView;
	@SuppressWarnings("unused") @InjectView(R.id.author_container) View authorContainer;
	@SuppressWarnings("unused") @InjectView(R.id.username) TextView usernameView;
	@SuppressWarnings("unused") @InjectView(R.id.thumbs) TextView thumbsView;
	@SuppressWarnings("unused") @InjectView(R.id.posted_date) TextView postedDateView;
	@SuppressWarnings("unused") @InjectView(R.id.edited_date) TextView editedDateView;
	@SuppressWarnings("unused") @InjectView(R.id.body) WebView bodyView;
	@SuppressWarnings("unused") @InjectViews({
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
		timeHintUpdateHandler = new Handler();
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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = (ViewGroup) inflater.inflate(R.layout.fragment_geeklist_item, container, false);
		ButterKnife.inject(this, rootView);

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
		thumbsView.setText(getString(R.string.thumbs_suffix, numberOfThumbs));
		postedDateView.setTag(postedDate);
		editedDateView.setTag(editedDate);
		String content = new XmlConverter().toHtml(body);
		UIUtils.setWebViewText(bodyView, content);

		updateTimeBasedUi();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
		timeHintUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);

		return rootView;
	}

	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (postedDateView != null) {
			postedDateView.setText(getString(R.string.posted_prefix, DateTimeUtils.formatForumDate(getActivity(), postedDate)));
		}
		if (editedDateView != null) {
			editedDateView.setText(getString(R.string.edited_prefix, DateTimeUtils.formatForumDate(getActivity(), editedDate)));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ButterKnife.reset(this);
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
	public void onPaletteGenerated(Palette palette) {
		if (!isAdded()) {
			return;
		}
		swatch = PaletteUtils.getInverseSwatch(palette, getResources().getColor(R.color.info_background));
		applySwatch();
	}

	private void applySwatch() {
		if (authorContainer != null && swatch != null) {
			authorContainer.setBackgroundColor(swatch.getRgb());
			ButterKnife.apply(colorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
		}
	}
}
