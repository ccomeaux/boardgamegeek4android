package com.boardgamegeek.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.BuddiesCountChangedEvent;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.model.Buddy;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class BuddiesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int TOKEN = 0;
	private static final String SORT_COLUMN = Buddies.BUDDY_LASTNAME;
	private BuddiesAdapter adapter = null;
	private boolean isSyncing = false;

	private Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.empty_container) ViewGroup emptyContainer;
	@BindView(android.R.id.empty) TextView emptyTextView;
	@BindView(R.id.empty_button) Button emptyButton;
	@BindView(R.id.progress) ContentLoadingProgressBar progressBar;
	@BindView(R.id.list_container) View listContainer;
	@BindView(android.R.id.list) RecyclerView listView;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		LoaderManager.getInstance(this).restartLoader(TOKEN, getArguments(), this);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_buddies, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);
		setEmptyText();

		listView.setLayoutManager(new LinearLayoutManager(getContext()));
		listView.setHasFixedSize(true);

		swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				triggerRefresh();
			}
		});
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private void triggerRefresh() {
		SyncService.sync(getContext(), SyncService.FLAG_SYNC_BUDDIES);
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(@NonNull SyncEvent event) {
		if ((event.getType() & SyncService.FLAG_SYNC_BUDDIES) == SyncService.FLAG_SYNC_BUDDIES) {
			isSyncing(true);
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(SyncCompleteEvent event) {
		isSyncing(false);
	}

	private void isSyncing(boolean value) {
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

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = new CursorLoader(getContext(),
			Buddies.CONTENT_URI,
			Buddy.PROJECTION,
			String.format("%s!=? AND %s=1", Buddies.BUDDY_ID, Buddies.BUDDY_FLAG),
			new String[] { Authenticator.getUserId(getContext()) },
			null);
		loader.setUpdateThrottle(2000);
		return loader;
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;

		int token = loader.getId();
		if (token == TOKEN) {
			if (adapter == null) {
				setListAdapter(new BuddiesAdapter(getActivity()));
			}
			adapter.changeCursor(cursor);
			if (listView.getItemDecorationCount() != 0) {
				listView.removeItemDecorationAt(0);
			}
			RecyclerSectionItemDecoration sectionItemDecoration =
				new RecyclerSectionItemDecoration(
					getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
					true,
					getSectionCallback(adapter));
			listView.addItemDecoration(sectionItemDecoration);
			EventBus.getDefault().postSticky(new BuddiesCountChangedEvent(cursor.getCount()));
		} else {
			Timber.d("Query complete, Not Actionable: %s", token);
			cursor.close();
		}
	}

	private boolean isListShown = false;

	public void setListAdapter(BuddiesAdapter adapter) {
		boolean hadAdapter = this.adapter != null;
		this.adapter = adapter;
		if (listView != null) {
			listView.setAdapter(adapter);
			if (!isListShown && !hadAdapter) {
				// The list was hidden, and previously didn't have an adapter. It is now time to show it.
				setListShown(listView.getWindowToken() != null);
			}
		}
	}

	private void setListShown(boolean animate) {
		emptyContainer.setVisibility(View.GONE);
		if (isListShown) return;
		isListShown = true;
		AnimationUtils.fadeIn(listContainer, animate);
		progressBar.hide();
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		adapter.changeCursor(null);
	}

	private void setEmptyText() {
		if (PreferencesUtils.getSyncBuddies(getActivity())) {
			emptyTextView.setText(R.string.empty_buddies);
			emptyButton.setVisibility(View.GONE);
		} else {
			emptyTextView.setText(R.string.empty_buddies_sync_off);
			emptyButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					PreferencesUtils.setSyncBuddies(getContext());
					setEmptyText();
					triggerRefresh();
				}
			});
			emptyButton.setVisibility(View.VISIBLE);
		}
	}

	public class BuddiesAdapter extends RecyclerView.Adapter<BuddiesAdapter.BuddyViewHolder> {
		private final LayoutInflater inflater;
		private Cursor cursor;

		public BuddiesAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void changeCursor(Cursor cursor) {
			this.cursor = cursor;
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			return cursor == null ? 0 : cursor.getCount();
		}

		@NonNull
		@Override
		public BuddyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View row = inflater.inflate(R.layout.row_buddy, parent, false);
			return new BuddyViewHolder(row);
		}

		@Override
		public void onBindViewHolder(@NonNull BuddyViewHolder holder, int position) {
			if (cursor == null) return;
			if (!cursor.moveToPosition(position)) return;

			final Buddy buddy = Buddy.fromCursor(cursor);

			ImageUtils.loadThumbnail(holder.avatar, buddy.getAvatarUrl(), R.drawable.person_image_empty);

			if (TextUtils.isEmpty(buddy.getFullName())) {
				holder.fullName.setText(buddy.getUserName());
				holder.name.setVisibility(View.GONE);
			} else {
				holder.fullName.setText(buddy.getFullName());
				holder.name.setVisibility(View.VISIBLE);
				holder.name.setText(buddy.getUserName());
			}

			holder.itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					EventBus.getDefault().postSticky(new BuddySelectedEvent(buddy.getId(), buddy.getUserName(), buddy.getFullName()));
				}
			});
		}

		class BuddyViewHolder extends RecyclerView.ViewHolder {
			@BindView(R.id.full_name) TextView fullName;
			@BindView(R.id.name) TextView name;
			@BindView(R.id.avatar) ImageView avatar;

			public BuddyViewHolder(View view) {
				super(view);
				ButterKnife.bind(this, view);
			}
		}
	}

	private RecyclerSectionItemDecoration.SectionCallback getSectionCallback(final BuddiesAdapter adapter) {
		return new RecyclerSectionItemDecoration.SectionCallback() {
			@Override
			public boolean isSection(int position) {
				if (adapter == null || adapter.cursor == null) return false;
				if (position == 0) return true;
				Character thisLetter = CursorUtils.getFirstCharacter(adapter.cursor, position, SORT_COLUMN, "-").charAt(0);
				Character lastLetter = CursorUtils.getFirstCharacter(adapter.cursor, position - 1, SORT_COLUMN, "-").charAt(0);
				return thisLetter != lastLetter;
			}

			@Override
			public CharSequence getSectionHeader(int position) {
				if (adapter == null || adapter.cursor == null) return "-";
				return CursorUtils.getFirstCharacter(adapter.cursor, position, SORT_COLUMN, "-").subSequence(0, 1);
			}
		};
	}
}