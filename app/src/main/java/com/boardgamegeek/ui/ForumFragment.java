package com.boardgamegeek.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.ForumPagedListAdapter;
import com.boardgamegeek.ui.viewmodel.ForumViewModel;
import com.boardgamegeek.util.AnimationUtils;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class ForumFragment extends Fragment {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_OBJECT_ID = "OBJECT_ID";
	private static final String KEY_OBJECT_NAME = "OBJECT_NAME";
	private static final String KEY_OBJECT_TYPE = "OBJECT_TYPE";

	private ForumPagedListAdapter adapter;
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
		View rootView = inflater.inflate(R.layout.fragment_forum, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		return rootView;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		readBundle(getArguments());
		setUpRecyclerView();

		adapter = new ForumPagedListAdapter(forumId, forumTitle, objectId, objectName, objectType);
		recyclerView.setAdapter(adapter);

		ForumViewModel viewModel = new ViewModelProvider(requireActivity()).get(ForumViewModel.class);
		viewModel.getThreads().observe(getViewLifecycleOwner(), threads -> {
			Timber.i(threads.toString());
			adapter.submitList(threads);
			if (threads.size() == 0) {
				AnimationUtils.fadeOut(recyclerView);
				AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
			} else {
				AnimationUtils.fadeOut(emptyView);
				AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
			}
			progressView.hide();
		});
		viewModel.setForumId(forumId);
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
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
	}
}
