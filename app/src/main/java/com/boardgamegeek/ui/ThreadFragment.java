package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Article;
import com.boardgamegeek.model.ThreadResponse;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.ThreadRecyclerViewAdapter;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.ui.widget.SafeViewTarget;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class ThreadFragment extends Fragment implements LoaderManager.LoaderCallbacks<SafeResponse<ThreadResponse>> {
	private static final int HELP_VERSION = 2;
	private static final int LOADER_ID = 103;
	private ThreadRecyclerViewAdapter adapter;
	private int threadId;
	private ShowcaseView showcaseView;

	Unbinder unbinder;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		threadId = intent.getIntExtra(ActivityUtils.KEY_THREAD_ID, BggContract.INVALID_ID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_thread, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		// If this is called in onActivityCreated as recommended, the loader is finished twice
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	public void onDestroyView() {
		unbinder.unbind();
		super.onDestroyView();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.help, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_help:
				showHelp();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	private void setUpRecyclerView() {
		final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(layoutManager);

		recyclerView.setHasFixedSize(true);
	}

	@DebugLog
	private void showHelp() {
		final Builder builder = HelpUtils.getShowcaseBuilder(getActivity());
		if (builder != null) {
			builder.setContentText(R.string.help_thread)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showcaseView.hide();
						HelpUtils.updateHelp(getContext(), HelpUtils.HELP_THREAD_KEY, HELP_VERSION);
					}
				});
			Target viewTarget = getTarget();
			builder.setTarget(viewTarget == null ? Target.NONE : viewTarget);
			showcaseView = builder.build();
			showcaseView.show();
		}
	}

	@DebugLog
	private Target getTarget() {
		final View child = HelpUtils.getRecyclerViewVisibleChild(recyclerView);
		return child == null ? null : new SafeViewTarget(child.findViewById(R.id.view_button));
	}

	@Override
	@DebugLog
	public Loader<SafeResponse<ThreadResponse>> onCreateLoader(int id, Bundle data) {
		return new ThreadLoader(getActivity(), threadId);
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<SafeResponse<ThreadResponse>> loader, SafeResponse<ThreadResponse> data) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new ThreadRecyclerViewAdapter(getActivity(),
				(data == null || data.getBody() == null) ?
					new ArrayList<Article>(0) :
					data.getBody().getArticles());
			recyclerView.setAdapter(adapter);
		}

		if (adapter.getItemCount() == 0 || data == null) {
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else if (data.hasError()) {
			emptyView.setText(data.getErrorMessage());
			AnimationUtils.fadeIn(getActivity(), emptyView, isResumed());
		} else {
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
			maybeShowHelp();
		}
		progressView.hide();
	}

	@DebugLog
	private void maybeShowHelp() {
		if (HelpUtils.shouldShowHelp(getContext(), HelpUtils.HELP_THREAD_KEY, HELP_VERSION)) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					showHelp();
				}
			}, 100);
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<SafeResponse<ThreadResponse>> loader) {
	}

	private static class ThreadLoader extends BggLoader<SafeResponse<ThreadResponse>> {
		private final BggService bggService;
		private final int threadId;

		public ThreadLoader(Context context, int threadId) {
			super(context);
			bggService = Adapter.createForXml();
			this.threadId = threadId;
		}

		@Override
		public SafeResponse<ThreadResponse> loadInBackground() {
			return new SafeResponse<>(bggService.thread(threadId));
		}
	}
}
