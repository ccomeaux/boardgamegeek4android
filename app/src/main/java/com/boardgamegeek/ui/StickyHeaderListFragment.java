package com.boardgamegeek.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.HttpUtils;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public abstract class StickyHeaderListFragment extends Fragment implements OnRefreshListener {
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

	final private OnScrollListener mOnScrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (mSwipeRefreshLayout != null) {
				int topRowVerticalPosition = (view == null || view.getChildCount() == 0) ? 0 : view.getChildAt(0).getTop();
				mSwipeRefreshLayout.setEnabled(isRefreshable() && (firstVisibleItem == 0 && topRowVerticalPosition >= 0));
			}
		}
	};

	protected boolean isRefreshable() {
		return getSyncType() != SyncService.FLAG_SYNC_NONE;
	}

	private StickyListHeadersAdapter mAdapter;
	private StickyListHeadersListView mList;
	@InjectView(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
	@InjectView(android.R.id.empty) TextView mEmptyView;
	@InjectView(R.id.progressContainer) View mProgressContainer;
	@InjectView(R.id.listContainer) View mListContainer;
	@InjectView(R.id.fab) View mFab;
	private CharSequence mEmptyText;
	private boolean mListShown;
	private int mListViewStatePosition;
	private int mListViewStateTop;
	private boolean mSyncing;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_sticky_header_list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ensureList();
		final StickyListHeadersListView listView = getListView();
		int padding = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		listView.setClipToPadding(false);
		listView.setPadding(0, 0, 0, padding);
		if (dividerShown()) {
			int height = getResources().getDimensionPixelSize(R.dimen.divider_height);
			//noinspection deprecation
			listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
			listView.setDividerHeight(height);
		} else {
			listView.setDivider(null);
		}
		listView.setFastScrollEnabled(true);
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

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
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

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@DebugLog
	public void onEventMainThread(SyncEvent event) {
		if ((event.type & getSyncType()) == getSyncType()) {
			isSyncing(true);
		}
	}

	@DebugLog
	public void onEventMainThread(SyncCompleteEvent event) {
		isSyncing(false);
	}

	@DebugLog
	protected int getSyncType() {
		return SyncService.FLAG_SYNC_NONE;
	}

	@DebugLog
	protected void isSyncing(boolean value) {
		mSyncing = value;
		updateRefreshStatus();
	}

	@DebugLog
	private void updateRefreshStatus() {
		if (mSwipeRefreshLayout != null) {
			mSwipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					mSwipeRefreshLayout.setRefreshing(mSyncing);
				}
			});
		}
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	protected void triggerRefresh() {
	}

	protected boolean dividerShown() {
		return false;
	}

	protected void onListItemClick(View view, int position, long id) {
	}

	public void setListAdapter(StickyListHeadersAdapter adapter) {
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

	public void setProgressShown(boolean shown) {
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
		Picasso.with(getActivity()).load(HttpUtils.ensureScheme(path)).placeholder(placeholderResId)
			.error(placeholderResId).resizeDimen(R.dimen.thumbnail_list_size, R.dimen.thumbnail_list_size).centerCrop()
			.into(target);
	}

	private void ensureList() {
		if (mList != null) {
			return;
		}
		View root = getView();
		if (root == null) {
			throw new IllegalStateException("Content view not yet created");
		}

		ButterKnife.inject(this, root);

		if (mSwipeRefreshLayout != null) {
			mSwipeRefreshLayout.setEnabled(isRefreshable());
			mSwipeRefreshLayout.setOnRefreshListener(this);
			mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);
		}
		mEmptyView.setVisibility(View.GONE);
		View rawListView = root.findViewById(android.R.id.list);
		if (!(rawListView instanceof StickyListHeadersListView)) {
			throw new RuntimeException("Content has view with id attribute 'android.R.id.list' that is not a StickyListHeadersListView class");
		}
		mList = (StickyListHeadersListView) rawListView;
		//noinspection ConstantConditions
		if (mList == null) {
			throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
		}
		if (mEmptyText != null) {
			mEmptyView.setText(mEmptyText);
			mList.setEmptyView(mEmptyView);
		}
		mList.setDivider(null);
		mListShown = true;
		mList.setOnItemClickListener(mOnClickListener);
		mList.setOnScrollListener(mOnScrollListener);

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

	protected void showFab(boolean show) {
		mFab.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@OnClick(R.id.fab)
	protected void onFabClicked(View v) {
		// convenience for overriding
	}
}
