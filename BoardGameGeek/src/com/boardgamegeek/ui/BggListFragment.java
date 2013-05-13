package com.boardgamegeek.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.UIUtils;

public abstract class BggListFragment extends SherlockListFragment implements AbsListView.OnScrollListener {
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";

	private ImageFetcher mImageFetcher;
	private int mListViewStatePosition;
	private int mListViewStateTop;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getLoadingImage() != 0) {
			mImageFetcher = UIUtils.getImageFetcher(getActivity());
			mImageFetcher.setLoadingImage(getLoadingImage());
			mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.thumbnail_list_size));
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			mListViewStatePosition = savedInstanceState.getInt(STATE_POSITION, -1);
			mListViewStateTop = savedInstanceState.getInt(STATE_TOP, 0);
		} else {
			mListViewStatePosition = -1;
			mListViewStateTop = 0;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mImageFetcher != null) {
			mImageFetcher.setPauseWork(false);
			mImageFetcher.flushCache();
		}
		saveScrollState();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mImageFetcher != null) {
			mImageFetcher.closeCache();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		saveScrollState();
		outState.putInt(STATE_POSITION, mListViewStatePosition);
		outState.putInt(STATE_TOP, mListViewStateTop);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView listView, int scrollState) {
		if (mImageFetcher != null) {
			// Pause disk cache access to ensure smoother scrolling
			if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
				|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
				mImageFetcher.setPauseWork(true);
			} else {
				mImageFetcher.setPauseWork(false);
			}
		}
	}

	private void saveScrollState() {
		if (isAdded()) {
			View v = getListView().getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			mListViewStatePosition = getListView().getFirstVisiblePosition();
			mListViewStateTop = top;
		}
	}

	protected void restoreScrollState() {
		if (mListViewStatePosition != -1 && isAdded()) {
			getListView().setSelectionFromTop(mListViewStatePosition, mListViewStateTop);
		}
	}

	protected ImageFetcher getImageFetcher() {
		return mImageFetcher;
	}

	protected int getLoadingImage() {
		return 0;
	}
}
