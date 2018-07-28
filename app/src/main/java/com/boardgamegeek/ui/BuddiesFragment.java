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
import com.boardgamegeek.extensions.StringUtils;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.model.Buddy;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration.SectionCallback;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

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

public class BuddiesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
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
		LoaderManager.getInstance(this).restartLoader(0, getArguments(), this);
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

		List<Buddy> buddies = new ArrayList<>();
		if (cursor.moveToFirst()) {
			do {
				buddies.add(Buddy.fromCursor(cursor));
			} while (cursor.moveToNext());
		}

		if (adapter == null) {
			adapter = new BuddiesAdapter(getContext());
			listView.setAdapter(adapter);
			RecyclerSectionItemDecoration sectionItemDecoration =
				new RecyclerSectionItemDecoration(
					getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
					true,
					adapter);
			listView.addItemDecoration(sectionItemDecoration);
		}
		adapter.changeData(buddies);

		EventBus.getDefault().postSticky(new BuddiesCountChangedEvent(cursor.getCount()));

		progressBar.hide();
		setListShown(listView.getWindowToken() != null);
	}

	private void setListShown(boolean animate) {
		if (adapter.getItemCount() == 0) {
			AnimationUtils.fadeOut(listContainer);
			AnimationUtils.fadeIn(emptyContainer);
		} else {
			AnimationUtils.fadeOut(emptyContainer);
			AnimationUtils.fadeIn(listContainer, animate);
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		adapter.clear();
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

	public class BuddiesAdapter extends RecyclerView.Adapter<BuddiesAdapter.BuddyViewHolder> implements SectionCallback {
		private final LayoutInflater inflater;
		private final List<Buddy> buddies = new ArrayList<>();

		public BuddiesAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void clear() {
			this.buddies.clear();
			notifyDataSetChanged();
		}

		public void changeData(@NonNull List<Buddy> buddies) {
			this.buddies.clear();
			this.buddies.addAll(buddies);
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			return buddies == null ? 0 : buddies.size();
		}

		@NonNull
		@Override
		public BuddyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new BuddyViewHolder(inflater.inflate(R.layout.row_buddy, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull BuddyViewHolder holder, int position) {
			holder.bind(buddies.get(position));

		}

		@Override
		public boolean isSection(int position) {
			if (buddies.size() == 0) return false;
			if (position == 0) return true;
			String thisLetter = StringUtils.firstChar(buddies.get(position).getLastName());
			String lastLetter = StringUtils.firstChar(buddies.get(position - 1).getLastName());
			return !thisLetter.equals(lastLetter);
		}

		@NonNull
		@Override
		public CharSequence getSectionHeader(int position) {
			if (buddies.size() == 0) return "-";
			return StringUtils.firstChar(buddies.get(position).getLastName());
		}

		class BuddyViewHolder extends RecyclerView.ViewHolder {
			@BindView(R.id.full_name) TextView fullName;
			@BindView(R.id.name) TextView name;
			@BindView(R.id.avatar) ImageView avatar;

			public BuddyViewHolder(View view) {
				super(view);
				ButterKnife.bind(this, view);
			}

			public void bind(final Buddy buddy) {
				ImageUtils.loadThumbnail(avatar, buddy.getAvatarUrl(), R.drawable.person_image_empty);

				if (TextUtils.isEmpty(buddy.getFullName())) {
					fullName.setText(buddy.getUserName());
					name.setVisibility(View.GONE);
				} else {
					fullName.setText(buddy.getFullName());
					name.setVisibility(View.VISIBLE);
					name.setText(buddy.getUserName());
				}

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						EventBus.getDefault().postSticky(new BuddySelectedEvent(buddy.getId(), buddy.getUserName(), buddy.getFullName()));
					}
				});
			}
		}
	}
}
