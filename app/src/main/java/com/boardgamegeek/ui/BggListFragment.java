package com.boardgamegeek.ui;

import android.graphics.Color;
import android.os.Bundle;
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
 * 2. sets up typical list view style (background color, fast scroll, etc.)
 * 3. helper methods for loading thumbnails
 */
public abstract class BggListFragment extends ListFragment {
	private static final int LIST_VIEW_STATE_TOP_DEFAULT = 0;
	private static final int LIST_VIEW_STATE_POSITION_DEFAULT = -1;
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";

	private int mListViewStatePosition;
	private int mListViewStateTop;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setDivider(null);
		listView.setCacheColorHint(Color.WHITE);
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
	public void onPause() {
		super.onPause();
		saveScrollState();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		saveScrollState();
		outState.putInt(STATE_POSITION, mListViewStatePosition);
		outState.putInt(STATE_TOP, mListViewStateTop);
		super.onSaveInstanceState(outState);
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
