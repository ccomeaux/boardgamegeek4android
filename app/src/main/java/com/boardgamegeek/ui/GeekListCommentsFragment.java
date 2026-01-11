package com.boardgamegeek.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekListComment;
import com.boardgamegeek.ui.adapter.GeekListCommentsRecyclerViewAdapter;
import com.boardgamegeek.util.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boardgamegeek.databinding.FragmentGeeklistCommentsBinding;

public class GeekListCommentsFragment extends Fragment {
	private static final String KEY_COMMENTS = "GEEK_LIST_COMMENTS";

	private List<GeekListComment> comments;
	private FragmentGeeklistCommentsBinding binding;

	public static GeekListCommentsFragment newInstance(ArrayList<GeekListComment> comments) {
		Bundle args = new Bundle();
		args.putParcelableArrayList(KEY_COMMENTS, comments);
		GeekListCommentsFragment fragment = new GeekListCommentsFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		comments = getArguments().getParcelableArrayList(KEY_COMMENTS);
		binding = FragmentGeeklistCommentsBinding.inflate(inflater, container, false);
		setUpRecyclerView();
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		bindData();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		binding.list.setLayoutManager(layoutManager);
		binding.list.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
	}

	private void bindData() {
		GeekListCommentsRecyclerViewAdapter adapter = new GeekListCommentsRecyclerViewAdapter(getContext(), comments);
		binding.list.setAdapter(adapter);
		if (comments == null || comments.size() == 0) {
			AnimationUtils.fadeIn(binding.empty);
		} else {
			AnimationUtils.fadeIn(binding.list);
		}
		binding.progress.hide();
	}
}
