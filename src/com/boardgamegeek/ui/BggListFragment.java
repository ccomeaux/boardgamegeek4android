package com.boardgamegeek.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockListFragment;

public abstract class BggListFragment extends SherlockListFragment {
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";

	private int mListViewStatePosition;
	private int mListViewStateTop;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		getListView().setCacheColorHint(Color.WHITE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(getEmptyStringResoure()));
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
		saveScrollState();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		saveScrollState();
		outState.putInt(STATE_POSITION, mListViewStatePosition);
		outState.putInt(STATE_TOP, mListViewStateTop);
		super.onSaveInstanceState(outState);
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

	protected abstract int getEmptyStringResoure();
}
