package com.boardgamegeek.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

	final private Handler focusHandler = new Handler();

	final private Runnable listViewFocusRunnable = new Runnable() {
		public void run() {
			listView.focusableViewAvailable(listView);
		}
	};

	final private OnItemClickListener onItemClickListener = new OnItemClickListener() {
		public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
			onListItemClick(view, position, id);
		}
	};

	@Nullable final private OnScrollListener onScrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}

		@Override
		public void onScroll(@Nullable AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (swipeRefreshLayout != null) {
				int topRowVerticalPosition = (view == null || view.getChildCount() == 0) ? 0 : view.getChildAt(0).getTop();
				swipeRefreshLayout.setEnabled(isRefreshable() && (firstVisibleItem == 0 && topRowVerticalPosition >= 0));
			}

			if (totalItemCount == 0) {
				return;
			}
			int newScrollY = getTopItemScrollY();
			if (isSameRow(firstVisibleItem)) {
				boolean isSignificantDelta = Math.abs(lastScrollY - newScrollY) > scrollThreshold;
				if (isSignificantDelta) {
					if (lastScrollY > newScrollY) {
						onScrollUp();
					} else {
						onScrollDown();
					}
				}
			} else {
				if (firstVisibleItem > previousFirstVisibleItem) {
					onScrollUp();
				} else {
					onScrollDown();
				}
				previousFirstVisibleItem = firstVisibleItem;
			}
			lastScrollY = newScrollY;
		}
	};

	protected boolean isRefreshable() {
		return getSyncType() != SyncService.FLAG_SYNC_NONE;
	}

	@SuppressWarnings("unused") @InjectView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@Nullable @SuppressWarnings("unused") @InjectView(android.R.id.empty) TextView emptyTextView;
	@Nullable @SuppressWarnings("unused") @InjectView(R.id.progressContainer) View progressContainer;
	@Nullable @SuppressWarnings("unused") @InjectView(R.id.listContainer) View listContainer;
	@SuppressWarnings("unused") @InjectView(R.id.fab) View fabView;
	@Nullable private StickyListHeadersListView listView;
	@Nullable private StickyListHeadersAdapter adapter;
	private CharSequence emptyText;
	private boolean isListShown;
	private int listViewStatePosition;
	private int listViewStateTop;
	private boolean isSyncing;
	private int previousFirstVisibleItem;
	private int lastScrollY;
	private int scrollThreshold = 20;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
		focusHandler.removeCallbacks(listViewFocusRunnable);
		listView = null;
		isListShown = false;
		progressContainer = listContainer = null;
		emptyTextView = null;
		super.onDestroyView();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			listViewStatePosition = savedInstanceState.getInt(STATE_POSITION, LIST_VIEW_STATE_POSITION_DEFAULT);
			listViewStateTop = savedInstanceState.getInt(STATE_TOP, LIST_VIEW_STATE_TOP_DEFAULT);
		} else {
			listViewStatePosition = LIST_VIEW_STATE_POSITION_DEFAULT;
			listViewStateTop = LIST_VIEW_STATE_TOP_DEFAULT;
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		saveScrollState();
		outState.putInt(STATE_POSITION, listViewStatePosition);
		outState.putInt(STATE_TOP, listViewStateTop);
		super.onSaveInstanceState(outState);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEventMainThread(@NonNull SyncEvent event) {
		if ((event.getType() & getSyncType()) == getSyncType()) {
			isSyncing(true);
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
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
		isSyncing = value;
		updateRefreshStatus();
	}

	@DebugLog
	private void updateRefreshStatus() {
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					swipeRefreshLayout.setRefreshing(isSyncing);
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

	@SuppressWarnings("UnusedParameters")
	protected void onListItemClick(View view, int position, long id) {
	}

	public void setListAdapter(StickyListHeadersAdapter adapter) {
		boolean hadAdapter = this.adapter != null;
		this.adapter = adapter;
		if (listView != null) {
			listView.setAdapter(adapter);
			if (!isListShown && !hadAdapter) {
				// The list was hidden, and previously didn't have an adapter. It is now time to show it.
				final View view = getView();
				setListShown(true, view != null && view.getWindowToken() != null);
			}
		}
	}

	@Nullable
	public StickyListHeadersListView getListView() {
		ensureList();
		return listView;
	}

	public void setEmptyText(CharSequence text) {
		ensureList();
		emptyTextView.setText(text);
		if (emptyText == null) {
			listView.setEmptyView(emptyTextView);
		}
		emptyText = text;
	}

	public void setProgressShown(boolean shown) {
		if (shown) {
			if (progressContainer.getVisibility() != View.VISIBLE) {
				progressContainer.clearAnimation();
				progressContainer.setVisibility(View.VISIBLE);
			}
		} else {
			if (progressContainer.getVisibility() != View.GONE) {
				progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				progressContainer.setVisibility(View.GONE);
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
		if (isListShown == shown) {
			return;
		}
		isListShown = shown;
		if (shown) {
			if (animate) {
				progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				listContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
			} else {
				progressContainer.clearAnimation();
				listContainer.clearAnimation();
			}
			progressContainer.setVisibility(View.GONE);
			listContainer.setVisibility(View.VISIBLE);
		} else {
			if (animate) {
				progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
				listContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
			} else {
				progressContainer.clearAnimation();
				listContainer.clearAnimation();
			}
			progressContainer.setVisibility(View.VISIBLE);
			listContainer.setVisibility(View.GONE);
		}
	}

	private void saveScrollState() {
		if (isAdded()) {
			View v = listView.getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			listViewStatePosition = listView.getFirstVisiblePosition();
			listViewStateTop = top;
		}
	}

	protected void restoreScrollState() {
		if (listViewStatePosition != LIST_VIEW_STATE_POSITION_DEFAULT && isAdded()) {
			listView.setSelectionFromTop(listViewStatePosition, listViewStateTop);
		}
	}

	protected void resetScrollState() {
		listViewStatePosition = 0;
		listViewStateTop = LIST_VIEW_STATE_TOP_DEFAULT;
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
		if (listView != null) {
			return;
		}
		View root = getView();
		if (root == null) {
			throw new IllegalStateException("Content view not yet created");
		}

		ButterKnife.inject(this, root);

		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.setEnabled(isRefreshable());
			swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);
		}
		emptyTextView.setVisibility(View.GONE);
		View rawListView = root.findViewById(android.R.id.list);
		if (!(rawListView instanceof StickyListHeadersListView)) {
			throw new RuntimeException("Content has view with id attribute 'android.R.id.list' that is not a StickyListHeadersListView class");
		}
		listView = (StickyListHeadersListView) rawListView;
		//noinspection ConstantConditions
		if (listView == null) {
			throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
		}
		if (emptyText != null) {
			emptyTextView.setText(emptyText);
			listView.setEmptyView(emptyTextView);
		}
		listView.setDivider(null);
		isListShown = true;
		listView.setOnItemClickListener(onItemClickListener);
		listView.setOnScrollListener(onScrollListener);

		if (adapter != null) {
			StickyListHeadersAdapter adapter = this.adapter;
			this.adapter = null;
			setListAdapter(adapter);
		} else {
			// We are starting without an adapter, so assume we won't have our data right away and start with the progress indicator.
			if (progressContainer != null) {
				setListShown(false, false);
			}
		}
		focusHandler.post(listViewFocusRunnable);
	}

	protected void showFab(boolean show) {
		ensureList();
		fabView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@OnClick(R.id.fab)
	protected void onFabClicked(View v) {
		// convenience for overriding
	}

	protected void onScrollUp() {
	}

	protected void onScrollDown() {
	}

	private boolean isSameRow(int firstVisibleItem) {
		return firstVisibleItem == previousFirstVisibleItem;
	}

	private int getTopItemScrollY() {
		StickyListHeadersListView listView = getListView();
		if (listView == null || listView.getChildAt(0) == null) return 0;
		View topChild = listView.getChildAt(0);
		return topChild.getTop();
	}
}
