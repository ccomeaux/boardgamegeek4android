package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekListComment;
import com.boardgamegeek.ui.adapter.GeekListCommentsRecyclerViewAdapter;
import com.boardgamegeek.util.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class GeekListCommentsFragment extends Fragment {
	private static final String KEY_COMMENTS = "GEEK_LIST_COMMENTS";

	private List<GeekListComment> comments;
	private Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	public static GeekListCommentsFragment newInstance(ArrayList<GeekListComment> comments) {
		Bundle args = new Bundle();
		args.putParcelableArrayList(KEY_COMMENTS, comments);
		GeekListCommentsFragment fragment = new GeekListCommentsFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		comments = getArguments().getParcelableArrayList(KEY_COMMENTS);
		View rootView = inflater.inflate(R.layout.fragment_geeklist_comments, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		bindData();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
	}

	private void bindData() {
		GeekListCommentsRecyclerViewAdapter adapter = new GeekListCommentsRecyclerViewAdapter(getContext(), comments);
		recyclerView.setAdapter(adapter);
		if (comments == null || comments.size() == 0) {
			AnimationUtils.fadeIn(emptyView);
		} else {
			AnimationUtils.fadeIn(recyclerView);
		}
		progressView.hide();
	}
}
