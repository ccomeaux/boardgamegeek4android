package com.boardgamegeek.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.SearchResponse;
import com.boardgamegeek.model.SearchResult;
import com.boardgamegeek.ui.SearchResultsFragment.SearchData;
import com.boardgamegeek.ui.decoration.VerticalDividerItemDecoration;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.ui.widget.SafeViewTarget;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.SearchEvent;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import retrofit2.Call;

public class SearchResultsFragment extends Fragment implements LoaderCallbacks<SearchData>, ActionMode.Callback {
	private static final int HELP_VERSION = 2;
	private static final int LOADER_ID = 0;
	private static final int MESSAGE_QUERY_UPDATE = 1;
	private static final int QUERY_UPDATE_DELAY_MILLIS = 2000;
	private static final String KEY_SEARCH_TEXT = "SEARCH_TEXT";
	private static final String KEY_SEARCH_EXACT = "SEARCH_EXACT";

	private String previousSearchText;
	private boolean previousShouldSearchExact;
	private SearchResultsAdapter searchResultsAdapter;
	private Snackbar snackbar;
	private ShowcaseView showcaseView;

	private ActionMode actionMode;
	private Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	private final Handler requeryHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MESSAGE_QUERY_UPDATE) {
				@SuppressWarnings("unchecked") Pair<String, Boolean> pair = (Pair<String, Boolean>) msg.obj;
				requery(pair.first, pair.second);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_search_results, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		restartLoader(intent.getStringExtra(SearchManager.QUERY), true);
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

	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setHasFixedSize(true);
		recyclerView.addItemDecoration(new VerticalDividerItemDecoration(getActivity()));
	}

	@DebugLog
	private void showHelp() {
		Builder builder = HelpUtils.getShowcaseBuilder(getActivity())
			.setContentText(R.string.help_searchresults)
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

	@DebugLog
	private Target getTarget() {
		final View child = HelpUtils.getRecyclerViewVisibleChild(recyclerView);
		return child == null ? null : new SafeViewTarget(child);
	}

	@DebugLog
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

	@Override
	public Loader<SearchData> onCreateLoader(int id, Bundle data) {
		return new SearchLoader(getActivity(),
			data.getString(KEY_SEARCH_TEXT),
			data.getBoolean(KEY_SEARCH_EXACT, true));
	}

	@DebugLog
	@Override
	public void onLoadFinished(Loader<SearchData> loader, SearchData data) {
		AnimationUtils.fadeOut(progressView);

		if (getActivity() == null) {
			return;
		}

		int count = data == null ? 0 : data.getCount();
		final String searchText = data == null ? "" : data.getSearchText();
		boolean isExactMatch = data != null && data.isExactMatch();

		if (data != null) {
			searchResultsAdapter = new SearchResultsAdapter(getActivity(), data.getList(),
				new Callback() {
					@Override
					public boolean onItemClick(int position) {
						if (actionMode == null) {
							return false;
						}
						toggleSelection(position);
						return true;
					}

					@Override
					public boolean onItemLongClick(int position) {
						if (actionMode != null) {
							return false;
						}
						actionMode = getActivity().startActionMode(SearchResultsFragment.this);
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
		} else if (searchResultsAdapter != null) {
			searchResultsAdapter.clear();
		}

		if (data == null) {
			if (TextUtils.isEmpty(searchText)) {
				emptyView.setText(R.string.search_initial_help);
			} else {
				emptyView.setText(R.string.empty_search);
			}
			AnimationUtils.fadeIn(emptyView);
			AnimationUtils.fadeOut(recyclerView);
		} else if (data.hasError()) {
			emptyView.setText(data.getErrorMessage());
			AnimationUtils.fadeIn(emptyView);
			AnimationUtils.fadeOut(recyclerView);
		} else if (data.getCount() == 0) {
			if (TextUtils.isEmpty(searchText)) {
				emptyView.setText(R.string.search_initial_help);
			} else {
				emptyView.setText(R.string.empty_search);
			}
			AnimationUtils.fadeIn(emptyView);
			AnimationUtils.fadeOut(recyclerView);
		} else {
			AnimationUtils.fadeOut(emptyView);
			AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
		}

		maybeShowHelp();

		if (TextUtils.isEmpty(searchText)) {
			if (snackbar != null) {
				snackbar.dismiss();
			}
		} else {
			@PluralsRes final int messageId = isExactMatch ? R.plurals.search_results_exact : R.plurals.search_results;
			if (snackbar == null || !snackbar.isShown()) {
				snackbar = Snackbar.make(containerView,
					getResources().getQuantityString(messageId, count, count, searchText),
					Snackbar.LENGTH_INDEFINITE);
				snackbar.getView().setBackgroundResource(R.color.dark_blue);
				snackbar.setActionTextColor(ContextCompat.getColor(getActivity(), R.color.inverse_text));
			} else {
				snackbar.setText(getResources().getQuantityString(messageId, count, count, searchText));
			}
			if (isExactMatch) {
				snackbar.setAction(R.string.more, new OnClickListener() {
					@Override
					public void onClick(View v) {
						requeryHandler.removeMessages(MESSAGE_QUERY_UPDATE);
						//setProgressShown(true);
						requery(searchText, false);
					}
				});
			} else {
				snackbar.setAction("", null);
			}
			snackbar.show();
		}
	}

	@Override
	public void onLoaderReset(Loader<SearchData> results) {
	}

	public void requestQueryUpdate(String query) {
		AnimationUtils.fadeIn(progressView);
		if (TextUtils.isEmpty(query)) {
			requery(query, true);
		} else {
			requeryHandler.removeMessages(MESSAGE_QUERY_UPDATE);
			requeryHandler.sendMessageDelayed(Message.obtain(requeryHandler, MESSAGE_QUERY_UPDATE, new Pair<>(query, true)), QUERY_UPDATE_DELAY_MILLIS);
		}
	}

	public void forceQueryUpdate(String query) {
		requeryHandler.removeMessages(MESSAGE_QUERY_UPDATE);
		AnimationUtils.fadeIn(progressView);
		requery(query, true);
	}

	private void requery(@Nullable String query, boolean shouldSearchExact) {
		if (!isAdded()) {
			return;
		}
		if (query == null && previousSearchText == null) {
			return;
		}
		if (previousSearchText != null && previousSearchText.equals(query) && shouldSearchExact == previousShouldSearchExact) {
			return;
		}
		Answers.getInstance().logSearch(new SearchEvent().putQuery(query));
		restartLoader(query, shouldSearchExact);
	}

	private void restartLoader(String query, boolean shouldSearchExact) {
		previousSearchText = query;
		previousShouldSearchExact = shouldSearchExact;
		Bundle args = new Bundle();
		args.putString(KEY_SEARCH_TEXT, query);
		args.putBoolean(KEY_SEARCH_EXACT, shouldSearchExact);
		getLoaderManager().restartLoader(LOADER_ID, args, SearchResultsFragment.this);
	}

	private static class SearchLoader extends BggLoader<SearchData> {
		private final BggService bggService;
		private final String searchText;
		private final boolean shouldSearchExact;

		public SearchLoader(Context context, String searchText, boolean shouldSearchExact) {
			super(context);
			bggService = Adapter.createForXml();
			this.searchText = searchText;
			this.shouldSearchExact = shouldSearchExact;
		}

		@Override
		public SearchData loadInBackground() {
			if (TextUtils.isEmpty(searchText)) {
				return null;
			}
			SearchData response = null;
			if (shouldSearchExact) {
				response = new SearchData(bggService.search(searchText, BggService.SEARCH_TYPE_BOARD_GAME, 1), searchText, true);
			}
			if (response == null || response.getBody() == null || response.getBody().games == null || response.getBody().games.isEmpty()) {
				return new SearchData(bggService.search(searchText, BggService.SEARCH_TYPE_BOARD_GAME, 0), searchText, false);
			} else {
				return response;
			}
		}
	}

	static class SearchData extends SafeResponse<SearchResponse> {
		private final String searchText;
		private final boolean isExactMatch;

		public SearchData(Call<SearchResponse> call, String searchText, boolean isExactMatch) {
			super(call);
			this.searchText = searchText;
			this.isExactMatch = isExactMatch;
		}

		public String getSearchText() {
			return searchText;
		}


		public boolean isExactMatch() {
			return isExactMatch;
		}

		public int getCount() {
			if (getBody() == null || getBody().games == null) {
				return 0;
			}
			return getBody().games.size();
		}

		public List<SearchResult> getList() {
			if (getBody() == null || getBody().games == null) {
				return new ArrayList<>();
			}
			return getBody().games;
		}
	}

	public interface Callback {
		boolean onItemClick(int position);

		boolean onItemLongClick(int position);
	}

	public static class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder> {
		private final LayoutInflater inflater;
		private final List<SearchResult> results;
		private final Callback callback;
		private final SparseBooleanArray selectedItems;

		public SearchResultsAdapter(Activity activity, List<SearchResult> results, Callback callback) {
			this.results = results;
			this.callback = callback;
			inflater = activity.getLayoutInflater();
			selectedItems = new SparseBooleanArray();
			setHasStableIds(true);
		}

		@Override
		public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.row_search, parent, false);
			return new SearchResultViewHolder(view);
		}

		@Override
		public void onBindViewHolder(SearchResultViewHolder holder, int position) {
			holder.bind(getItem(position), position);
		}

		@Override
		public int getItemCount() {
			return results == null ? 0 : results.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public SearchResult getItem(int position) {
			return results.get(position);
		}

		public void clear() {
			results.clear();
		}

		public class SearchResultViewHolder extends RecyclerView.ViewHolder {
			@BindView(R.id.name) TextView nameView;
			@BindView(R.id.year) TextView yearView;
			@BindView(R.id.game_id) TextView gameIdView;

			public SearchResultViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final SearchResult game, final int position) {
				if (game == null) return;
				nameView.setText(game.name);
				int style;
				switch (game.getNameType()) {
					case SearchResult.NAME_TYPE_ALTERNATE:
						style = Typeface.ITALIC;
						break;
					case SearchResult.NAME_TYPE_PRIMARY:
					case SearchResult.NAME_TYPE_UNKNOWN:
					default:
						style = Typeface.NORMAL;
						break;
				}
				nameView.setTypeface(nameView.getTypeface(), style);
				yearView.setText(PresentationUtils.describeYear(yearView.getContext(), game.getYearPublished()));
				gameIdView.setText(gameIdView.getContext().getString(R.string.id_list_text, game.id));

				itemView.setActivated(selectedItems.get(position, false));

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean handled = false;
						if (callback != null) {
							handled = callback.onItemClick(position);
						}
						if (!handled) {
							ActivityUtils.launchGame(itemView.getContext(), game.id, game.name);
						}
					}
				});

				itemView.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						return callback != null && callback.onItemLongClick(position);
					}
				});
			}
		}

		public void toggleSelection(int position) {
			if (selectedItems.get(position, false)) {
				selectedItems.delete(position);
			} else {
				selectedItems.put(position, true);
			}
			notifyItemChanged(position);
		}

		public void clearSelections() {
			selectedItems.clear();
			notifyDataSetChanged();
		}

		public int getSelectedItemCount() {
			return selectedItems.size();
		}

		public List<Integer> getSelectedItems() {
			List<Integer> items = new ArrayList<>(selectedItems.size());
			for (int i = 0; i < selectedItems.size(); i++) {
				items.add(selectedItems.keyAt(i));
			}
			return items;
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
		menu.findItem(R.id.menu_log_play).setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(getActivity()));
		menu.findItem(R.id.menu_link).setVisible(count == 1);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (searchResultsAdapter == null ||
			searchResultsAdapter.getSelectedItems() == null ||
			searchResultsAdapter.getSelectedItems().size() == 0) {
			return false;
		}
		SearchResult game = searchResultsAdapter.getItem(searchResultsAdapter.getSelectedItems().get(0));
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), game.id, game.name, null, null, false);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, searchResultsAdapter.getSelectedItemCount());
				Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
				for (int position : searchResultsAdapter.getSelectedItems()) {
					SearchResult g = searchResultsAdapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), g.id, g.name);
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				final String shareMethod = "Search";
				if (searchResultsAdapter.getSelectedItemCount() == 1) {
					ActivityUtils.shareGame(getActivity(), game.id, game.name, shareMethod);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(searchResultsAdapter.getSelectedItemCount());
					for (int position : searchResultsAdapter.getSelectedItems()) {
						SearchResult g = searchResultsAdapter.getItem(position);
						games.add(Pair.create(g.id, g.name));
					}
					ActivityUtils.shareGames(getActivity(), games, shareMethod);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), game.id);
				return true;
		}
		return false;
	}
}
