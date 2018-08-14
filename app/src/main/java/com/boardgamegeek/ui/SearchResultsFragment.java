package com.boardgamegeek.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.entities.RefreshableResource;
import com.boardgamegeek.entities.SearchResultEntity;
import com.boardgamegeek.ui.adapter.Callback;
import com.boardgamegeek.ui.adapter.SearchResultsAdapter;
import com.boardgamegeek.ui.viewmodel.SearchViewModel;
import com.boardgamegeek.ui.widget.SafeViewTarget;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.SearchEvent;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SearchResultsFragment extends Fragment implements ActionMode.Callback {
	private static final int HELP_VERSION = 2;

	private SearchResultsAdapter searchResultsAdapter;
	private Snackbar snackbar;
	private ShowcaseView showcaseView;

	private ActionMode actionMode;
	private Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	private SearchViewModel viewModel;

	public static SearchResultsFragment newInstance() {
		Bundle args = new Bundle();
		SearchResultsFragment fragment = new SearchResultsFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);


		viewModel = ViewModelProviders.of(getActivity()).get(SearchViewModel.class);
		viewModel.getSearchResults().observe(this, new Observer<RefreshableResource<List<SearchResultEntity>>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<List<SearchResultEntity>> resource) {
				if (resource == null) return;

				switch (resource.getStatus()) {
					case REFRESHING:
						AnimationUtils.fadeIn(progressView);
						break;
					case ERROR:
						if (TextUtils.isEmpty(resource.getMessage())) {
							emptyView.setText(R.string.empty_http_error); // TODO better message?
						} else {
							emptyView.setText(getString(R.string.empty_http_error, resource.getMessage()));
						}
						AnimationUtils.fadeIn(emptyView);
						AnimationUtils.fadeOut(recyclerView);
						AnimationUtils.fadeOut(progressView);
						break;
					case SUCCESS:
						List<SearchResultEntity> data = resource.getData();
						final kotlin.Pair<String, Boolean> query = viewModel.getQuery().getValue();
						if (data == null || data.size() == 0) {
							if (query != null && query.getSecond())
								viewModel.searchInexact(query.getFirst());
							if (query == null || TextUtils.isEmpty(query.getFirst())) {
								emptyView.setText(R.string.search_initial_help);
							} else {
								emptyView.setText(R.string.empty_search);
							}
							searchResultsAdapter.clear();
							AnimationUtils.fadeIn(emptyView);
							AnimationUtils.fadeOut(recyclerView);
						} else {
							searchResultsAdapter.setResults(data);
							AnimationUtils.fadeOut(emptyView);
							AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
						}
						if (query != null) {
							showSnackBar(query.getFirst(), query.getSecond(),
								resource.getData() == null ? 0 : resource.getData().size());
						}
						AnimationUtils.fadeOut(progressView);
						break;
				}

				maybeShowHelp();
			}
		});
	}

	private void showSnackBar(final String searchText, boolean isExactMatch, int count) {
		if (TextUtils.isEmpty(searchText)) {
			if (snackbar != null) snackbar.dismiss();
		} else {
			@PluralsRes final int messageId = isExactMatch ? R.plurals.search_results_exact : R.plurals.search_results;
			if (snackbar == null || !snackbar.isShown()) {
				snackbar = Snackbar.make(containerView,
					getResources().getQuantityString(messageId, count, count, searchText),
					Snackbar.LENGTH_INDEFINITE);
				snackbar.getView().setBackgroundResource(R.color.dark_blue);
				snackbar.setActionTextColor(ContextCompat.getColor(getActivity(), R.color.accent));
			} else {
				snackbar.setText(getResources().getQuantityString(messageId, count, count, searchText));
			}
			if (isExactMatch) {
				snackbar.setAction(R.string.more, new OnClickListener() {
					@Override
					public void onClick(View v) {
						requery(searchText, false);
						Answers.getInstance().logCustom(new CustomEvent("SearchMore"));
					}
				});
			} else {
				snackbar.setAction("", null);
			}
			snackbar.show();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_search_results, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
		searchResultsAdapter = new SearchResultsAdapter(
			new Callback() {
				@Override
				public boolean onItemClick(int position) {
					if (actionMode == null) return false;
					toggleSelection(position);
					return true;
				}

				@Override
				public boolean onItemLongClick(int position) {
					if (actionMode != null) return false;
					actionMode = getActivity().startActionMode(SearchResultsFragment.this);
					if (actionMode == null) return false;
					toggleSelection(position);
					return true;
				}

				private void toggleSelection(int position) {
					searchResultsAdapter.toggleSelection(position);
					int count = searchResultsAdapter.getSelectedItemCount();
					if (count == 0) {
						actionMode.finish();
					} else {
						actionMode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));
						actionMode.invalidate();
					}
				}
			});
		recyclerView.setAdapter(searchResultsAdapter);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.help, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_help) {
			showHelp();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showHelp() {
		final Builder builder = HelpUtils.getShowcaseBuilder(getActivity());
		if (builder != null) {
			builder.setContentText(R.string.help_searchresults)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showcaseView.hide();
						HelpUtils.updateHelp(getContext(), HelpUtils.HELP_SEARCHRESULTS_KEY, HELP_VERSION);
					}
				});
			Target viewTarget = getTarget();
			builder.setTarget(viewTarget == null ? Target.NONE : viewTarget);
			showcaseView = builder.build();
			showcaseView.setButtonPosition(HelpUtils.getCenterLeftLayoutParams(getActivity()));
			showcaseView.show();
		}
	}

	private Target getTarget() {
		final View child = HelpUtils.getRecyclerViewVisibleChild(recyclerView);
		return child == null ? null : new SafeViewTarget(child);
	}

	private void maybeShowHelp() {
		if (HelpUtils.shouldShowHelp(getContext(), HelpUtils.HELP_SEARCHRESULTS_KEY, HELP_VERSION)) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					showHelp();
				}
			}, 100);
		}
	}

	public void requery(@Nullable String query) {
		requery(query, true);
	}

	private void requery(@NonNull String query, boolean shouldSearchExact) {
		if (!isAdded()) return;
		AnimationUtils.fadeIn(progressView);
		Answers.getInstance().logSearch(new SearchEvent().putQuery(query));
		if (shouldSearchExact) {
			viewModel.search(query);
		} else {
			viewModel.searchInexact(query);
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.game_context, menu);
		searchResultsAdapter.clearSelections();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		int count = searchResultsAdapter.getSelectedItemCount();
		menu.findItem(R.id.menu_log_play).setVisible(Authenticator.isSignedIn(getContext()) && count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		menu.findItem(R.id.menu_log_play_quick).setVisible(Authenticator.isSignedIn(getContext()) && PreferencesUtils.showQuickLogPlay(getActivity()));
		menu.findItem(R.id.menu_link).setVisible(count == 1);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (searchResultsAdapter == null || searchResultsAdapter.getSelectedItems().size() == 0) {
			return false;
		}
		SearchResultEntity game = searchResultsAdapter.getItem(searchResultsAdapter.getSelectedItems().get(0));
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				LogPlayActivity.logPlay(getContext(), game.getId(), game.getName(), null, null, null, false);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, searchResultsAdapter.getSelectedItemCount());
				Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
				for (int position : searchResultsAdapter.getSelectedItems()) {
					SearchResultEntity g = searchResultsAdapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), g.getId(), g.getName());
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				final String shareMethod = "Search";
				if (searchResultsAdapter.getSelectedItemCount() == 1) {
					ActivityUtils.shareGame(getActivity(), game.getId(), game.getName(), shareMethod);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(searchResultsAdapter.getSelectedItemCount());
					for (int position : searchResultsAdapter.getSelectedItems()) {
						SearchResultEntity g = searchResultsAdapter.getItem(position);
						games.add(Pair.create(g.getId(), g.getName()));
					}
					ActivityUtils.shareGames(getActivity(), games, shareMethod);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), game.getId());
				return true;
		}
		return false;
	}
}
