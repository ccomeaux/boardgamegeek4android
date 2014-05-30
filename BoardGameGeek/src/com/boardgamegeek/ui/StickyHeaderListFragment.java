package com.boardgamegeek.ui;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.squareup.picasso.Picasso;

public abstract class StickyHeaderListFragment extends SherlockFragment {
	private static final int LIST_VIEW_STATE_TOP_DEFAULT = 0;
	private static final int LIST_VIEW_STATE_POSITION_DEFAULT = -1;
	private static final String STATE_POSITION = "position";
	private static final String STATE_TOP = "top";

	final private Handler mHandler = new Handler();

	final private Runnable mRequestFocus = new Runnable() {
		public void run() {
			mList.focusableViewAvailable(mList);
		}
	};

	final private OnItemClickListener mOnClickListener = new OnItemClickListener() {
		public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
			onListItemClick(view, position, id);
		}
	};

	private StickyListHeadersAdapter mAdapter;
	private StickyListHeadersListView mList;
	private TextView mEmptyView;
	private View mProgressContainer;
	private View mListContainer;
	private CharSequence mEmptyText;
	private boolean mListShown;
	private int mListViewStatePosition;
	private int mListViewStateTop;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_sticky_header_list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ensureList();
	}

	@Override
	public void onDestroyView() {
		mHandler.removeCallbacks(mRequestFocus);
		mList = null;
		mListShown = false;
		mProgressContainer = mListContainer = null;
		mEmptyView = null;
		super.onDestroyView();
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

	public void onListItemClick(View view, int position, long id) {
	}

	public void setListAdapter(StickyListHeadersAdapter adapter) {
		boolean hadAdapter = mAdapter != null;
		mAdapter = adapter;
		if (mList != null) {
			mList.setAdapter(adapter);
			if (!mListShown && !hadAdapter) {
				// The list was hidden, and previously didn't have an
				// adapter. It is now time to show it.
				setListShown(true, getView().getWindowToken() != null);
			}
		}
	}

	public StickyListHeadersAdapter getListAdapter() {
		return mAdapter;
	}

	public StickyListHeadersListView getListView() {
		ensureList();
		return mList;
	}

	public void setEmptyText(CharSequence text) {
		ensureList();
		mEmptyView.setText(text);
		if (mEmptyText == null) {
			mList.setEmptyView(mEmptyView);
		}
		mEmptyText = text;
	}

	public void setProgessShown(boolean shown) {
		if (shown) {
			if (mProgressContainer.getVisibility() != View.VISIBLE) {
				mProgressContainer.clearAnimation();
				mProgressContainer.setVisibility(View.VISIBLE);
			}
		} else {
			if (mProgressContainer.getVisibility() != View.GONE) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				mProgressContainer.setVisibility(View.GONE);
			}
		}
	}

	public void setListShown(boolean shown) {
		setListShown(shown, true);
	}

	public void setListShownNoAnimation(boolean shown) {
		setListShown(shown, false);
	}

	private void setListShown(boolean shown, boolean animate) {
		ensureList();
		if (mListShown == shown) {
			return;
		}
		mListShown = shown;
		if (shown) {
			if (animate) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
			} else {
				mProgressContainer.clearAnimation();
				mListContainer.clearAnimation();
			}
			mProgressContainer.setVisibility(View.GONE);
			mListContainer.setVisibility(View.VISIBLE);
		} else {
			if (animate) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
				mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
			} else {
				mProgressContainer.clearAnimation();
				mListContainer.clearAnimation();
			}
			mProgressContainer.setVisibility(View.VISIBLE);
			mListContainer.setVisibility(View.GONE);
		}
	}

	private void saveScrollState() {
		if (isAdded()) {
			View v = mList.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			mListViewStatePosition = mList.getFirstVisiblePosition();
			mListViewStateTop = top;
		}
	}

	protected void restoreScrollState() {
		if (mListViewStatePosition != LIST_VIEW_STATE_POSITION_DEFAULT && isAdded()) {
			mList.setSelectionFromTop(mListViewStatePosition, mListViewStateTop);
		}
	}

	protected void resetScrollState() {
		mListViewStatePosition = 0;
		mListViewStateTop = LIST_VIEW_STATE_TOP_DEFAULT;
	}

	protected void loadThumbnail(String path, ImageView target) {
		loadThumbnail(path, target, R.drawable.thumbnail_image_empty);
	}

	protected void loadThumbnail(String path, ImageView target, int placeholderResId) {
		Picasso.with(getActivity()).load(path).placeholder(placeholderResId).error(placeholderResId)
			.resizeDimen(R.dimen.thumbnail_list_size, R.dimen.thumbnail_list_size).centerCrop().into(target);
	}

	private void ensureList() {
		if (mList != null) {
			return;
		}
		View root = getView();
		if (root == null) {
			throw new IllegalStateException("Content view not yet created");
		}

		mEmptyView = (TextView) root.findViewById(android.R.id.empty);
		mEmptyView.setVisibility(View.GONE);
		mProgressContainer = root.findViewById(R.id.progressContainer);
		mListContainer = root.findViewById(R.id.listContainer);
		View rawListView = root.findViewById(android.R.id.list);
		if (!(rawListView instanceof StickyListHeadersListView)) {
			throw new RuntimeException("Content has view with id attribute 'android.R.id.list' "
				+ "that is not a ListView class");
		}
		mList = (StickyListHeadersListView) rawListView;
		if (mList == null) {
			throw new RuntimeException("Your content must have a ListView whose id attribute is "
				+ "'android.R.id.list'");
		}
		if (mEmptyText != null) {
			mEmptyView.setText(mEmptyText);
			mList.setEmptyView(mEmptyView);
		}
		mListShown = true;
		mList.setOnItemClickListener(mOnClickListener);
		if (mAdapter != null) {
			StickyListHeadersAdapter adapter = mAdapter;
			mAdapter = null;
			setListAdapter(adapter);
		} else {
			// We are starting without an adapter, so assume we won't
			// have our data right away and start with the progress indicator.
			if (mProgressContainer != null) {
				setListShown(false, false);
			}
		}
		mHandler.post(mRequestFocus);
	}
}
