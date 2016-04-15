package com.boardgamegeek.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.LinkedList;
import java.util.Queue;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.Icepick;
import icepick.State;

/**
 * A {@link android.support.v4.app.ListFragment} with a few extra features:
 * 1. retains scroll state on rotation
 * 2. sets up typical list view style (dividers, top/bottom padding, fast scroll, etc.)
 * 3. helper methods for loading thumbnails
 */
public abstract class BggListFragment extends Fragment {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	@State int listViewStatePosition;
	@State int listViewStateTop;
	private CharSequence mEmptyText;
	private boolean mListShown;
	private ListAdapter mAdapter;
	@Bind(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
	@Bind(android.R.id.empty) TextView mEmptyView;
	@Bind(R.id.progress_container) View mProgressContainer;
	@Bind(R.id.list_container) View mListContainer;
	@Bind(android.R.id.list) ListView mList;
	@Bind(R.id.fab) View mFab;

	final private Runnable mRequestFocus = new Runnable() {
		public void run() {
			mList.focusableViewAvailable(mList);
		}
	};

	final private OnItemClickListener mOnClickListener = new OnItemClickListener() {
		public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
			onListItemClick(mList, view, position, id);
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_list, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ensureList();

		int paddingTop = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		int paddingBottom = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		if (padBottomForSnackBar()) {
			paddingBottom = getResources().getDimensionPixelSize(R.dimen.snackbar_buffer);
		}
		mList.setClipToPadding(false);
		if (padTop()) {
			mList.setPadding(0, paddingTop, 0, paddingBottom);
		} else {
			mList.setPadding(0, 0, 0, paddingBottom);
		}

		if (dividerShown()) {
			int height = getResources().getDimensionPixelSize(R.dimen.divider_height);
			mList.setDivider(getResources().getDrawable(R.drawable.list_divider));
			mList.setDividerHeight(height);
		} else {
			mList.setDivider(null);
		}
		mList.setFastScrollEnabled(true);
	}

	@Override
	public void onDestroyView() {
		mHandler.removeCallbacks(mRequestFocus);
		mList = null;
		mListShown = false;
		mProgressContainer = null;
		mListContainer = null;
		mEmptyView = null;
		super.onDestroyView();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
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
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	protected boolean padTop() {
		return false;
	}

	protected boolean padBottomForSnackBar() {
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

	public void setListAdapter(ListAdapter adapter) {
		boolean hadAdapter = mAdapter != null;
		mAdapter = adapter;
		if (mList != null) {
			mList.setAdapter(adapter);
			if (!mListShown && !hadAdapter) {
				// The list was hidden, and previously didn't have an
				// adapter. It is now time to show it.
				final View view = getView();
				setListShown(true, view != null && view.getWindowToken() != null);
			}
		}
	}

	public void setEmptyText(CharSequence text) {
		ensureList();
		mEmptyView.setText(text);
		if (mEmptyText == null) {
			mList.setEmptyView(mEmptyView);
		}
		mEmptyText = text;
	}

	public void setProgressShown(boolean shown) {
		if (mProgressContainer != null) {
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

	protected void saveScrollState() {
		if (isAdded()) {
			View v = mList.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			listViewStatePosition = mList.getFirstVisiblePosition();
			listViewStateTop = top;
		}
	}

	protected void restoreScrollState() {
		if (listViewStatePosition >= 0 && isAdded()) {
			mList.setSelectionFromTop(listViewStatePosition, listViewStateTop);
		}
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

	private static void safelyLoadThumbnail(final ImageView imageView, final Queue<String> imageUrls, final int placeholderResId) {
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

	private void ensureList() {
		if (mList != null) {
			return;
		}
		View root = getView();
		if (root == null) {
			throw new IllegalStateException("Content view not yet created");
		}

		ButterKnife.bind(this, root);

		mSwipeRefreshLayout.setEnabled(false);

		mEmptyView.setVisibility(View.GONE);
		if (mEmptyText != null) {
			mEmptyView.setText(mEmptyText);
			mList.setEmptyView(mEmptyView);
		}
		mList.setDivider(null);
		mListShown = true;
		mList.setOnItemClickListener(mOnClickListener);

		if (mAdapter != null) {
			setListAdapter(mAdapter);
		} else {
			// We are starting without an adapter, so assume we won't
			// have our data right away and start with the progress indicator.
			if (mProgressContainer != null) {
				setListShown(false, false);
			}
		}
		mHandler.post(mRequestFocus);
	}

	protected ListView getListView() {
		return mList;
	}

	protected View getListContainer() {
		return mListContainer;
	}

	protected void onListItemClick(ListView view, View convertView, int position, long id) {
	}

	protected void showFab(boolean show) {
		ensureList();
		mFab.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@OnClick(R.id.fab)
	protected void onFabClicked(View v) {
		// convenience for overriding
	}
}
