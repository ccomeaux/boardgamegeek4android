package com.boardgamegeek.ui;

import android.content.Context;
import android.os.Bundle;
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
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.model.ForumListResponse;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.ForumsRecyclerViewAdapter;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.util.AnimationUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import retrofit2.Call;

public class ForumsFragment extends Fragment implements LoaderManager.LoaderCallbacks<SafeResponse<ForumListResponse>> {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final int LOADER_ID = 0;

	private int gameId;
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
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		readBundle(getArguments());
		View rootView = inflater.inflate(R.layout.fragment_forums, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	private void readBundle(Bundle bundle) {
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = bundle.getString(KEY_GAME_NAME);
	}

	@Override
	@DebugLog
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	@Override
	@DebugLog
	public Loader<SafeResponse<ForumListResponse>> onCreateLoader(int id, Bundle data) {
		return new ForumsLoader(getActivity(), gameId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<SafeResponse<ForumListResponse>> loader, SafeResponse<ForumListResponse> data) {
		if (getActivity() == null) return;

		if (adapter == null) {
			adapter = new ForumsRecyclerViewAdapter(getContext(), data.getBody() == null ? new ArrayList<Forum>() : data.getBody().getForums(), gameId, gameName);
			recyclerView.setAdapter(adapter);
		}

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
	@DebugLog
	public void onLoaderReset(Loader<SafeResponse<ForumListResponse>> loader) {
	}

	@DebugLog
	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

		if (gameId == BggContract.INVALID_ID) {
			recyclerView.setPadding(recyclerView.getPaddingLeft(), 0, recyclerView.getRight(), recyclerView.getBottom());
		}
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
