package com.boardgamegeek.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A {@link android.support.v4.app.ListFragment} with a few extra features:
 * 1. retains scroll state on rotation
 * 2. sets up typical list view style (dividers, top/bottom padding, fast scroll, etc.)
 * 3. helper methods for loading thumbnails
 */
public abstract class BggListFragment extends ListFragment {
	private static final int LIST_VIEW_STATE_TOP_DEFAULT = 0;
	private static final int LIST_VIEW_STATE_POSITION_DEFAULT = -1;
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	private int mListViewStatePosition;
	private int mListViewStateTop;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListView listView = getListView();

		int padding = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		listView.setClipToPadding(false);
		if (padTop()) {
			listView.setPadding(0, padding, 0, padding);
		} else {
			listView.setPadding(0, 0, 0, padding);
		}

		if (dividerShown()) {
			int height = getResources().getDimensionPixelSize(R.dimen.divider_height);
			listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
			listView.setDividerHeight(height);
		} else {
			listView.setDivider(null);
		}
		listView.setFastScrollEnabled(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			mListViewStatePosition = savedInstanceState.getInt(STATE_POSITION, LIST_VIEW_STATE_POSITION_DEFAULT);
			mListViewStateTop = savedInstanceState.getInt(STATE_TOP, LIST_VIEW_STATE_TOP_DEFAULT);
		} else {
			mListViewStatePosition = LIST_VIEW_STATE_POSITION_DEFAULT;
			mListViewStateTop = LIST_VIEW_STATE_TOP_DEFAULT;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mUpdaterRunnable != null) {
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
		saveScrollState();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		saveScrollState();
		outState.putInt(STATE_POSITION, mListViewStatePosition);
		outState.putInt(STATE_TOP, mListViewStateTop);
		super.onSaveInstanceState(outState);
	}

	protected boolean padTop() {
		return false;
	}

	protected boolean dividerShown() {
		return false;
	}

	/**
	 * Update time-based UI and continue to update it periodically.
	 */
	protected void initializeTimeBasedUi() {
		updateTimeBasedUi();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
		mUpdaterRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
	}

	/**
	 * Add any code that needs to update the UI with time-based information. Note that this can be called before the UI has been initialized.
	 */
	protected void updateTimeBasedUi() {
	}

	protected void saveScrollState() {
		if (isAdded()) {
			View v = getListView().getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			mListViewStatePosition = getListView().getFirstVisiblePosition();
			mListViewStateTop = top;
		}
	}

	protected void restoreScrollState() {
		if (mListViewStatePosition != LIST_VIEW_STATE_POSITION_DEFAULT && isAdded()) {
			getListView().setSelectionFromTop(mListViewStatePosition, mListViewStateTop);
		}
	}

	protected void resetScrollState() {
		mListViewStatePosition = 0;
		mListViewStateTop = LIST_VIEW_STATE_TOP_DEFAULT;
	}

	protected void loadThumbnail(int imageId, ImageView target) {
		Queue<String> queue = new LinkedList<>();
		queue.add(ImageUtils.createThumbnailJpgUrl(imageId));
		queue.add(ImageUtils.createThumbnailPngUrl(imageId));
		safelyLoadThumbnail(target, queue, R.drawable.thumbnail_image_empty);
	}

	protected void loadThumbnail(String path, ImageView target) {
		loadThumbnail(path, target, R.drawable.thumbnail_image_empty);
	}

	protected void loadThumbnail(String path, ImageView target, int placeholderResId) {
		Queue<String> queue = new LinkedList<>();
		queue.add(path);
		safelyLoadThumbnail(target, queue, placeholderResId);
	}

	private static void safelyLoadThumbnail(final ImageView imageView, final Queue<String> imageUrls,
											final int placeholderResId) {
		String imageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(imageUrl)) {
			return;
		}
		Picasso.with(imageView.getContext()).load(HttpUtils.ensureScheme(imageUrl)).placeholder(placeholderResId)
			.error(placeholderResId).resizeDimen(R.dimen.thumbnail_list_size, R.dimen.thumbnail_list_size).centerCrop()
			.into(imageView, new Callback() {
				@Override
				public void onSuccess() {
				}

				@Override
				public void onError() {
					safelyLoadThumbnail(imageView, imageUrls, placeholderResId);
				}
			});
	}
}
