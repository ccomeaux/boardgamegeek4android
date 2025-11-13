package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Thread;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.ForumRecyclerViewAdapter;
import com.boardgamegeek.ui.loader.PaginatedLoader;
import com.boardgamegeek.ui.model.ForumThreads;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.util.AnimationUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ForumFragment extends Fragment implements LoaderManager.LoaderCallbacks<PaginatedData<Thread>> {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_OBJECT_ID = "OBJECT_ID";
	private static final String KEY_OBJECT_NAME = "OBJECT_NAME";
	private static final String KEY_OBJECT_TYPE = "OBJECT_TYPE";
	private static final int LOADER_ID = 0;
	private static final int VISIBLE_THRESHOLD = 3;

	private ForumRecyclerViewAdapter adapter;
	private int forumId;
	private String forumTitle;
	private int objectId;
	private String objectName;
	private ForumType objectType;

	Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	public static ForumFragment newInstance(int forumId, String forumTitle, int objectId, String objectName, ForumType objectType) {
		Bundle args = new Bundle();
		args.putInt(KEY_FORUM_ID, forumId);
		args.putString(KEY_FORUM_TITLE, forumTitle);
		args.putInt(KEY_OBJECT_ID, objectId);
		args.putString(KEY_OBJECT_NAME, objectName);
		args.putSerializable(KEY_OBJECT_TYPE, objectType);

		ForumFragment fragment = new ForumFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		readBundle(getArguments());
		View rootView = inflater.inflate(R.layout.fragment_forum, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		forumId = bundle.getInt(KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = bundle.getString(KEY_FORUM_TITLE);
		objectId = bundle.getInt(KEY_OBJECT_ID, BggContract.INVALID_ID);
		objectName = bundle.getString(KEY_OBJECT_NAME);
		objectType = (ForumType) bundle.getSerializable(KEY_OBJECT_TYPE);
	}

	@Override
	public void onResume() {
		super.onResume();
		LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NotNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);

				final ForumLoader loader = getLoader();
				if (loader != null && !loader.isLoading() && loader.hasMoreResults()) {
					int totalItemCount = layoutManager.getItemCount();
					int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
					if (lastVisibleItemPosition + VISIBLE_THRESHOLD >= totalItemCount) {
						loadMoreResults();
					}
				}
			}
		});
	}

	@Nullable
	private ForumLoader getLoader() {
		if (isAdded()) {
			Loader<PaginatedData<Thread>> loader = LoaderManager.getInstance(this).getLoader(LOADER_ID);
			return (ForumLoader) loader;
		}
		return null;
	}

	private void loadMoreResults() {
		if (isAdded()) {
			Loader<List<Thread>> loader = LoaderManager.getInstance(this).getLoader(LOADER_ID);
			if (loader != null) {
				loader.forceLoad();
			}
		}
	}

	@NotNull
	@Override
	public Loader<PaginatedData<Thread>> onCreateLoader(int id, Bundle data) {
		return new ForumLoader(getActivity(), forumId);
	}

	@Override
	public void onLoadFinished(@NotNull Loader<PaginatedData<Thread>> loader, PaginatedData<Thread> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new ForumRecyclerViewAdapter(getActivity(), data, forumId, forumTitle, objectId, objectName, objectType);
			recyclerView.setAdapter(adapter);
		} else {
			adapter.update(data);
		}

		if (adapter.getItemCount() == 0) {
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else {
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		}
		progressView.hide();
	}

	@Override
	public void onLoaderReset(@NotNull Loader<PaginatedData<Thread>> loader) {
	}

	private static class ForumLoader extends PaginatedLoader<Thread> {
		private final BggService bggService;
		private final int forumId;

		public ForumLoader(Context context, int forumId) {
			super(context);
			bggService = Adapter.createForXml();
			this.forumId = forumId;
		}

		@Override
		protected PaginatedData<Thread> fetchPage(int pageNumber) {
			ForumThreads data;
			try {
				data = new ForumThreads(bggService.forum(forumId, pageNumber).execute().body(), pageNumber);
			} catch (Exception e) {
				data = new ForumThreads(e);
			}
			return data;
		}
	}
}
