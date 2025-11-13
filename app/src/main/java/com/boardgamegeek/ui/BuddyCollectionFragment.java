package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.CollectionItemEntity;
import com.boardgamegeek.events.CollectionStatusChangedEvent;
import com.boardgamegeek.extensions.StringUtils;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.model.CollectionItem;
import com.boardgamegeek.io.model.CollectionResponse;
import com.boardgamegeek.mappers.CollectionItemMapper;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

import com.boardgamegeek.databinding.FragmentBuddyCollectionBinding;

public class BuddyCollectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<SafeResponse<CollectionResponse>> {
	private static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	private static final int BUDDY_GAMES_LOADER_ID = 1;

	private BuddyCollectionAdapter adapter;
	private SubMenu subMenu;
	private String buddyName;
	@State String statusValue;
	@State String statusLabel;
	private String[] statusValues;
	private String[] statusEntries;
	private boolean isListShown = false;

	private FragmentBuddyCollectionBinding binding;
	private ViewGroup emptyContainer;
	private TextView emptyTextView;
	private ContentLoadingProgressBar progressBar;
	private RecyclerView listView;

	public static BuddyCollectionFragment newInstance(String username) {
		Bundle args = new Bundle();
		args.putString(KEY_BUDDY_NAME, username);
		BuddyCollectionFragment fragment = new BuddyCollectionFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		buddyName = getArguments() != null ? getArguments().getString(KEY_BUDDY_NAME) : null;

		if (TextUtils.isEmpty(buddyName)) {
			Timber.w("Missing buddy name.");
			return;
		}

		statusEntries = getResources().getStringArray(R.array.pref_sync_status_entries);
		statusValues = getResources().getStringArray(R.array.pref_sync_status_values);

		setHasOptionsMenu(true);
		Icepick.restoreInstanceState(this, savedInstanceState);
		if (TextUtils.isEmpty(statusValue)) {
			statusValue = statusValues[0];
		}
		if (TextUtils.isEmpty(statusLabel)) {
			statusLabel = statusEntries[0];
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		binding = FragmentBuddyCollectionBinding.inflate(inflater, container, false);
		emptyContainer = binding.emptyContainer;
		emptyTextView = binding.empty;
		progressBar = binding.progress;
		listView = binding.list;
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		listView.setLayoutManager(new LinearLayoutManager(getContext()));
		listView.setHasFixedSize(true);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		reload();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.buddy_collection, menu);
		MenuItem mi = menu.findItem(R.id.menu_collection_status);
		if (mi != null) {
			subMenu = mi.getSubMenu();
			if (subMenu != null) {
				for (int i = 0; i < statusEntries.length; i++) {
					subMenu.add(1, Menu.FIRST + i, i, statusEntries[i]);
				}
				subMenu.setGroupCheckable(1, true, true);
			}
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		UIUtils.showMenuItem(menu, R.id.menu_collection_random_game, adapter != null && adapter.getItemCount() > 0);
		// check the proper submenu item
		if (subMenu != null) {
			for (int i = 0; i < subMenu.size(); i++) {
				MenuItem smi = subMenu.getItem(i);
				if (smi.getTitle().equals(statusLabel)) {
					smi.setChecked(true);
					break;
				}
			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		String status = "";
		int i = id - Menu.FIRST;
		if (i >= 0 && i < statusValues.length) {
			status = statusValues[i];
		} else if (id == R.id.menu_collection_random_game) {
			CollectionItemEntity ci = adapter.getRandomItem();
			if (ci != null) {
				GameActivity.start(requireContext(), ci.getGameId(), ci.getGameName(), ci.getThumbnailUrl());
				return true;
			} else {
				return false;
			}
		}

		if (!TextUtils.isEmpty(status) && !status.equals(statusValue)) {
			statusValue = status;
			statusLabel = statusEntries[i];
			reload();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void reload() {
		progressBar.show();
		EventBus.getDefault().postSticky(new CollectionStatusChangedEvent(statusLabel));
		if (adapter != null) adapter.clear();
		requireActivity().invalidateOptionsMenu();
		setListShown(false);
		LoaderManager.getInstance(this).restartLoader(BUDDY_GAMES_LOADER_ID, null, this);
	}

	private void setListShown(boolean animate) {
		AnimationUtils.fadeOut(emptyContainer);
		if (isListShown) return;
		isListShown = true;
		AnimationUtils.fadeIn(listView, animate);
	}

	@NonNull
	@Override
	public Loader<SafeResponse<CollectionResponse>> onCreateLoader(int id, Bundle data) {
		return new BuddyGamesLoader(getActivity(), buddyName, statusValue);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<SafeResponse<CollectionResponse>> loader, SafeResponse<CollectionResponse> data) {
		if (getActivity() == null) return;
		ArrayList<CollectionItemEntity> list = new ArrayList<>();
		if (data != null &&
			data.getBody() != null &&
			data.getBody().items != null) {
			CollectionItemMapper mapper = new CollectionItemMapper();
			for (CollectionItem item : data.getBody().items) {
				list.add(mapper.map(item).getFirst());
			}
		}

		setListAdapter(new BuddyCollectionAdapter(getActivity()));
		adapter.setCollection(list);

		RecyclerSectionItemDecoration sectionItemDecoration =
			new RecyclerSectionItemDecoration(
				getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
				getSectionCallback(list),
				true
			);
		while (listView.getItemDecorationCount() > 0) {
			listView.removeItemDecorationAt(0);
		}
		listView.addItemDecoration(sectionItemDecoration);

		getActivity().invalidateOptionsMenu();

		if (data == null) {
			showError(R.string.empty_buddy_collection);
		} else if (data.hasError()) {
			showError(data.getErrorMessage());
		} else if (data.getBody().totalitems == 0) {
			showError(R.string.empty_buddy_collection);
		} else {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShown(false);
			}
		}
		progressBar.hide();
	}

	private void showError(@StringRes int messageResId) {
		showError(getString(messageResId));
	}

	private void showError(String message) {
		isListShown = false;
		AnimationUtils.fadeOut(listView);
		emptyTextView.setText(message);
		emptyContainer.setVisibility(View.VISIBLE);
		AnimationUtils.fadeIn(emptyContainer);
	}

	@Override
	public void onLoaderReset(@NonNull Loader<SafeResponse<CollectionResponse>> loader) {
		if (adapter != null) adapter.clear();
	}

	public void setListAdapter(BuddyCollectionAdapter adapter) {
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

	private static class BuddyGamesLoader extends BggLoader<SafeResponse<CollectionResponse>> {
		private final BggService bggService;
		private final String username;
		private final ArrayMap<String, String> options;

		public BuddyGamesLoader(Context context, String username, String status) {
			super(context);
			bggService = Adapter.createForXml();
			this.username = username;
			options = new ArrayMap<>();
			options.put(status, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_BRIEF, "1");
		}

		@Override
		public SafeResponse<CollectionResponse> loadInBackground() {
			return new SafeResponse<>(bggService.collection(username, options));
		}
	}

	public static class BuddyCollectionAdapter extends RecyclerView.Adapter<BuddyCollectionAdapter.BuddyGameViewHolder> {
		private final LayoutInflater inflater;
		private final List<CollectionItemEntity> items = new ArrayList<>();
		private final Random random = new Random();

		public BuddyCollectionAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void setCollection(List<CollectionItemEntity> games) {
			items.clear();
			items.addAll(games);
			notifyDataSetChanged();
		}

		public void clear() {
			items.clear();
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		public CollectionItemEntity getItemAt(int position) {
			if (position >= 0 && position < items.size()) {
				return items.get(position);
			} else {
				return null;
			}
		}

		public CollectionItemEntity getRandomItem() {
			return getItemAt(random.nextInt(getItemCount()));
		}

		@NonNull
		@Override
		public BuddyGameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View row = inflater.inflate(R.layout.row_text_2, parent, false);
			return new BuddyGameViewHolder(row);
		}

		@Override
		public void onBindViewHolder(@NonNull BuddyGameViewHolder holder, int position) {
			final CollectionItemEntity item = items.get(position);
			holder.bind(item);
		}

		class BuddyGameViewHolder extends RecyclerView.ViewHolder {
			public final TextView title;
			public final TextView text;

			public BuddyGameViewHolder(View view) {
				super(view);
				title = view.findViewById(android.R.id.title);
				text = view.findViewById(android.R.id.text1);
			}

			public void bind(final CollectionItemEntity item) {
				title.setText(item.getGameName());
				text.setText(String.valueOf(item.getGameId()));

				itemView.setOnClickListener(v -> GameActivity.start(itemView.getContext(), item.getGameId(), item.getGameName()));
			}
		}
	}

	private RecyclerSectionItemDecoration.SectionCallback getSectionCallback(final List<CollectionItemEntity> items) {
		return new RecyclerSectionItemDecoration.SectionCallback() {
			@Override
			public boolean isSection(int position) {
				if (position == RecyclerView.NO_POSITION) return false;
				if (items == null || items.size() == 0) return false;
				if (position == 0) return true;
				String thisLetter = StringUtils.firstChar(items.get(position).getSortName());
				String lastLetter = StringUtils.firstChar(items.get(position - 1).getSortName());
				return !thisLetter.equals(lastLetter);
			}

			@NotNull
			@Override
			public CharSequence getSectionHeader(int position) {
				if (position == RecyclerView.NO_POSITION) return "-";
				if (items == null || items.size() == 0) return "-";
				if (position < 0 || position >= items.size()) return "-";
				return StringUtils.firstChar(items.get(position).getSortName());
			}
		};
	}
}
