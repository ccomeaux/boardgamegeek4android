package com.boardgamegeek.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public abstract class StickyHeaderListFragment extends Fragment implements OnRefreshListener {
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
				boolean isSignificantDelta = Math.abs(lastScrollY - newScrollY) > SCROLL_THRESHOLD;
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

	private static final int SCROLL_THRESHOLD = 20;
	private Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.empty_container) ViewGroup emptyContainer;
	@BindView(android.R.id.empty) TextView emptyTextView;
	@BindView(R.id.empty_button) Button emptyButton;
	@BindView(R.id.progress) ContentLoadingProgressBar progressBar;
	@BindView(R.id.list_container) View listContainer;
	@BindView(R.id.fab) FloatingActionButton fabView;
	private StickyListHeadersListView listView;
	private StickyListHeadersAdapter adapter;
	private CharSequence emptyText;
	private boolean isListShown;
	@State int listViewStatePosition;
	@State int listViewStateTop;
	protected boolean isSyncing;
	private int previousFirstVisibleItem;
	private int lastScrollY;

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
		int padding = getResources().getDimensionPixelSize(shouldPadForFab() ? R.dimen.fab_buffer : R.dimen.padding_standard);
		assert listView != null;
		listView.setClipToPadding(false);
		listView.setPadding(0, 0, 0, padding);
		if (dividerShown()) {
			int height = getResources().getDimensionPixelSize(R.dimen.divider_height);
			listView.setDivider(ContextCompat.getDrawable(getActivity(), R.drawable.list_divider));
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
		progressBar = null;
		listContainer = null;
		emptyTextView = null;
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		saveScrollState();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		saveScrollState();
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(@NonNull SyncEvent event) {
		if ((event.getType() & getSyncType()) == getSyncType()) {
			isSyncing(true);
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(SyncCompleteEvent event) {
		isSyncing(false);
	}

	protected int getSyncType() {
		return SyncService.FLAG_SYNC_NONE;
	}

	@DebugLog
	protected void isSyncing(boolean value) {
		isSyncing = value;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) {
						swipeRefreshLayout.setRefreshing(isSyncing);
					}
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
			listView.setEmptyView(emptyContainer);
		}
		emptyText = text;
	}

	public void setEmptyButton(CharSequence text, OnClickListener l) {
		emptyButton.setText(text);
		emptyButton.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		emptyButton.setOnClickListener(l);
	}

	public void setListShown(boolean shown) {
		setListShown(shown, true);
	}

	public void setListShownNoAnimation(boolean shown) {
		setListShown(shown, false);
	}

	private void setListShown(boolean shown, boolean animate) {
		ensureList();
		if (isListShown == shown) return;
		isListShown = shown;
		if (shown) {
			if (animate) {
				listContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
			} else {
				listContainer.clearAnimation();
			}
			listContainer.setVisibility(View.VISIBLE);
			progressBar.hide();
		} else {
			progressBar.show();
			if (animate) {
				listContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
			} else {
				listContainer.clearAnimation();
			}
			listContainer.setVisibility(View.GONE);
		}
	}

	private void saveScrollState() {
		if (isAdded()) {
			View v = listView.getWrappedList().getChildAt(0);
			int top = (v == null) ? 0 : v.getTop();
			listViewStatePosition = listView.getFirstVisiblePosition();
			listViewStateTop = top;
		}
	}

	protected void restoreScrollState() {
		if (listViewStatePosition >= 0 && isAdded()) {
			listView.getWrappedList().setSelectionFromTop(listViewStatePosition, listViewStateTop);
		}
	}

	protected void resetScrollState() {
		listViewStatePosition = 0;
		listViewStateTop = -1;
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

		unbinder = ButterKnife.bind(this, root);

		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.setEnabled(isRefreshable());
			swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());
		}
		emptyContainer.setVisibility(View.GONE);
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
			listView.setEmptyView(emptyContainer);
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
			if (progressBar != null) {
				setListShown(false, false);
			}
		}
		focusHandler.post(listViewFocusRunnable);
	}

	protected void showFab(boolean show) {
		ensureList();
		if (show) {
			fabView.show();
		} else {
			fabView.hide();
		}
	}

	@OnClick(R.id.fab)
	protected void onFabClicked() {
		// convenience for overriding
	}

	protected boolean shouldPadForFab() {
		return false;
	}

	@OnClick(R.id.empty_button)
	void onSyncClick() {
		SyncService.clearCollection(getActivity());
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_COLLECTION);
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
