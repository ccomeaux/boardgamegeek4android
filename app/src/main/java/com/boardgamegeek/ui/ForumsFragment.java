package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ForumEntity;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.model.Forum;
import com.boardgamegeek.io.model.ForumListResponse;
import com.boardgamegeek.mappers.ForumMapper;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.ForumsRecyclerViewAdapter;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import retrofit2.Call;

public class ForumsFragment extends Fragment implements LoaderManager.LoaderCallbacks<SafeResponse<ForumListResponse>> {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final int LOADER_ID = 0;

	private int gameId = BggContract.INVALID_ID;
	private String gameName;
	private ForumsRecyclerViewAdapter adapter;

	Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	public static ForumsFragment newInstance() {
		return new ForumsFragment();
	}

	public static ForumsFragment newInstance(int gameId, String gameName) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);

		ForumsFragment fragment = new ForumsFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_forums, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
	}

	@Override
	@DebugLog
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getArguments() != null) {
			gameId = getArguments().getInt(KEY_GAME_ID, BggContract.INVALID_ID);
			gameName = getArguments().getString(KEY_GAME_NAME);
		}
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	@NonNull
	@Override
	@DebugLog
	public Loader<SafeResponse<ForumListResponse>> onCreateLoader(int id, Bundle data) {
		return new ForumsLoader(getActivity(), gameId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(@NonNull Loader<SafeResponse<ForumListResponse>> loader, SafeResponse<ForumListResponse> data) {
		if (getActivity() == null) return;

		if (adapter == null) {
			List<ForumEntity> forums = new ArrayList<>();
			if (data.getBody() != null && data.getBody().forums != null) {
				ForumMapper mapper = new ForumMapper();
				for (Forum f : data.getBody().forums) {
					forums.add(mapper.map(f));
				}
			}
			adapter = new ForumsRecyclerViewAdapter(forums, gameId, gameName);
		} else {
			adapter.notifyDataSetChanged();
		}
		if (recyclerView.getAdapter() == null) recyclerView.setAdapter(adapter);

		if (data.hasError()) {
			emptyView.setText(data.getErrorMessage());
			AnimationUtils.fadeIn(getContext(), emptyView, isResumed());
		} else {
			if (adapter.getItemCount() == 0) {
				AnimationUtils.fadeIn(getContext(), emptyView, isResumed());
			} else {
				AnimationUtils.fadeIn(getContext(), recyclerView, isResumed());
			}
		}
		progressView.hide();
	}

	@Override
	public void onLoaderReset(@NonNull Loader<SafeResponse<ForumListResponse>> loader) {
	}

	private static class ForumsLoader extends BggLoader<SafeResponse<ForumListResponse>> {
		private final BggService bggService;
		private final int gameId;

		@DebugLog
		public ForumsLoader(Context context, int gameId) {
			super(context);
			bggService = Adapter.createForXml();
			this.gameId = gameId;
		}

		@Override
		@DebugLog
		public SafeResponse<ForumListResponse> loadInBackground() {
			Call<ForumListResponse> call;
			if (gameId == BggContract.INVALID_ID) {
				call = bggService.forumList(BggService.FORUM_TYPE_REGION, BggService.FORUM_REGION_BOARDGAME);
			} else {
				call = bggService.forumList(BggService.FORUM_TYPE_THING, gameId);
			}
			return new SafeResponse<>(call);
		}
	}
}
