package com.boardgamegeek.ui;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.extensions.ActivityUtils;
import com.boardgamegeek.extensions.PreferenceUtils;
import com.boardgamegeek.extensions.SwipeRefreshLayoutUtils;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.extensions.TextViewUtils;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask;
import com.boardgamegeek.ui.adapter.PlayPlayerAdapter;
import com.boardgamegeek.ui.viewmodel.PlayViewModel;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.XmlApiMarkupConverter;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class PlayFragment extends Fragment implements OnRefreshListener {
	private static final String KEY_ID = "ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private static final String KEY_HAS_BEEN_NOTIFIED = "HAS_BEEN_NOTIFIED";

	private long internalId = BggContract.INVALID_ID;
	private int gameId = BggContract.INVALID_ID;
	private String gameName = "";
	private String thumbnailUrl;
	private String imageUrl;
	private String heroImageUrl;
	private boolean customPlayerSort = false;

	private int playId = BggContract.INVALID_ID;
	private boolean hasStarted = false;
	private boolean isDirty = false;
	String shortDescription = "";
	String longDescription = "";
	private boolean isRefreshing;
	private SharedPreferences prefs;
	private XmlApiMarkupConverter markupConverter;

	private Unbinder unbinder;
	@BindView(R.id.recyclerView) RecyclerView playersView;
	@BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.progressBar) ContentLoadingProgressBar progressBar;
	@BindView(R.id.listContainer) View listContainer;
	@BindView(R.id.emptyView) TextView emptyView;
	@BindView(R.id.thumbnailView) ImageView thumbnailView;
	@BindView(R.id.gameNameView) TextView gameNameView;
	@BindView(R.id.dateView) TextView dateView;
	@BindView(R.id.quantityView) TextView quantityView;
	@BindView(R.id.lengthContainer) View lengthContainer;
	@BindView(R.id.lengthView) TextView lengthView;
	@BindView(R.id.timerContainer) View timerContainer;
	@BindView(R.id.timerView) Chronometer timerView;
	@BindView(R.id.locationContainer) View locationContainer;
	@BindView(R.id.locationView) TextView locationView;
	@BindView(R.id.incompleteView) View incompleteView;
	@BindView(R.id.noWinStatsView) View noWinStatsView;
	@BindView(R.id.commentsView) TextView commentsView;
	@BindView(R.id.commentsLabel) View commentsLabel;
	@BindView(R.id.playersLabel) View playersLabel;
	@BindView(R.id.playIdView) TextView playIdView;
	@BindView(R.id.pendingTimestampView) TimestampView pendingTimestampView;
	@BindView(R.id.dirtyTimestampView) TimestampView dirtyTimestampView;
	@BindView(R.id.syncTimestampView) TimestampView syncTimestampView;
	private PlayPlayerAdapter adapter;
	private boolean hasBeenNotified;
	private FirebaseAnalytics firebaseAnalytics;

	private PlayViewModel viewModel;

	public static PlayFragment newInstance(long internalId, int gameId, String gameName, String imageUrl, String thumbnailUrl, String heroImageUrl, boolean customPlayerSort) {
		Bundle args = new Bundle();
		args.putLong(KEY_ID, internalId);
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);
		args.putString(KEY_IMAGE_URL, imageUrl);
		args.putString(KEY_THUMBNAIL_URL, thumbnailUrl);
		args.putString(KEY_HERO_IMAGE_URL, heroImageUrl);
		args.putBoolean(KEY_CUSTOM_PLAYER_SORT, customPlayerSort);
		PlayFragment fragment = new PlayFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());
		if (savedInstanceState != null) {
			hasBeenNotified = savedInstanceState.getBoolean(KEY_HAS_BEEN_NOTIFIED);
		}
		setHasOptionsMenu(true);
		prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
		markupConverter = new XmlApiMarkupConverter(requireContext());
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		internalId = bundle.getLong(KEY_ID, BggContract.INVALID_ID);
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = bundle.getString(KEY_GAME_NAME);
		thumbnailUrl = bundle.getString(KEY_THUMBNAIL_URL);
		imageUrl = bundle.getString(KEY_IMAGE_URL);
		heroImageUrl = bundle.getString(KEY_HERO_IMAGE_URL);
		customPlayerSort = bundle.getBoolean(KEY_CUSTOM_PLAYER_SORT, false);

		if (thumbnailUrl == null) thumbnailUrl = "";
		if (imageUrl == null) imageUrl = "";
		if (heroImageUrl == null) heroImageUrl = "";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_play, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel = new ViewModelProvider(requireActivity()).get(PlayViewModel.class);

		unbinder = ButterKnife.bind(this, view);

		if (swipeRefreshLayout != null) {
			SwipeRefreshLayoutUtils.setBggColors(swipeRefreshLayout);
			swipeRefreshLayout.setOnRefreshListener(this);
		}

		adapter = new PlayPlayerAdapter();
		playersView.setAdapter(adapter);

		viewModel.getPlay().observe(getViewLifecycleOwner(), play -> {
			if (play == null) {
				emptyView.setText(getResources().getString(R.string.empty_play, String.valueOf(internalId)));
				emptyView.setVisibility(View.VISIBLE); // TODO fade in
				listContainer.setVisibility(View.GONE);
				return;
			}

			playId = play.getPlayId();
			hasStarted = play.hasStarted();
			isDirty = play.getDirtyTimestamp() > 0;
			shortDescription = getString(R.string.play_description_game_segment, gameName) + getString(R.string.play_description_date_segment, play.dateForDisplay(requireContext()));
			longDescription = play.describe(requireContext(), true);

			gameNameView.setText(play.getGameName());

			dateView.setText(play.dateForDisplay(requireContext()));

			quantityView.setText(getResources().getQuantityString(R.plurals.times_suffix, play.getQuantity(), play.getQuantity()));
			quantityView.setVisibility((play.getQuantity() == 1) ? View.GONE : View.VISIBLE);

			if (play.getLength() > 0) {
				lengthContainer.setVisibility(View.VISIBLE);
				lengthView.setText(DateTimeUtils.describeMinutes(requireContext(), play.getLength()));
				lengthView.setVisibility(View.VISIBLE);
				timerContainer.setVisibility(View.GONE);
				timerView.stop();
			} else if (play.hasStarted()) {
				lengthContainer.setVisibility(View.VISIBLE);
				lengthView.setVisibility(View.GONE);
				timerContainer.setVisibility(View.VISIBLE);
				UIUtils.startTimerWithSystemTime(timerView, play.getStartTime());
			} else {
				lengthContainer.setVisibility(View.GONE);
			}

			locationView.setText(play.getLocation());
			locationContainer.setVisibility(TextUtils.isEmpty(play.getLocation()) ? View.GONE : View.VISIBLE);

			incompleteView.setVisibility(play.getIncomplete() ? View.VISIBLE : View.GONE);
			noWinStatsView.setVisibility(play.getNoWinStats() ? View.VISIBLE : View.GONE);

			TextViewUtils.setTextMaybeHtml(commentsView, markupConverter.toHtml(play.getComments()));
			commentsView.setVisibility(TextUtils.isEmpty(play.getComments()) ? View.GONE : View.VISIBLE);
			commentsLabel.setVisibility(TextUtils.isEmpty(play.getComments()) ? View.GONE : View.VISIBLE);

			if (play.getDeleteTimestamp() > 0) {
				pendingTimestampView.setVisibility(View.VISIBLE);
				pendingTimestampView.setFormat(getString(R.string.delete_pending_prefix));
				pendingTimestampView.setTimestamp(play.getDeleteTimestamp());
			} else if (play.getUpdateTimestamp() > 0) {
				pendingTimestampView.setVisibility(View.VISIBLE);
				pendingTimestampView.setFormat(getString(R.string.update_pending_prefix));
				pendingTimestampView.setTimestamp(play.getUpdateTimestamp());
			} else {
				pendingTimestampView.setVisibility(View.GONE);
			}

			if (play.getDirtyTimestamp() > 0) {
				dirtyTimestampView.setFormat(getString(play.getPlayId() > 0 ? R.string.editing_prefix : R.string.draft_prefix));
				dirtyTimestampView.setTimestamp(play.getDirtyTimestamp());
			} else {
				dirtyTimestampView.setVisibility(View.GONE);
			}

			if (play.getPlayId() > 0) {
				playIdView.setText(getResources().getString(R.string.play_id_prefix, String.valueOf(play.getPlayId())));
			}

			syncTimestampView.setTimestamp(play.getSyncTimestamp());

			// players
			playersLabel.setVisibility(play.getPlayers().size() == 0 ? View.GONE : View.VISIBLE);
			adapter.setPlayers(play.getPlayers());
			maybeShowNotification();

			requireActivity().invalidateOptionsMenu();

			emptyView.setVisibility(View.GONE); // TODO fade
			listContainer.setVisibility(View.VISIBLE);
			progressBar.hide();
		});
		viewModel.setId(internalId);

		ImageUtils.safelyLoadImage(thumbnailView, imageUrl, thumbnailUrl, heroImageUrl, new Callback() {
			@Override
			public void onSuccessfulImageLoad(Palette palette) {
				if (gameNameView != null && isAdded()) {
					gameNameView.setBackgroundResource(R.color.black_overlay_light);
				}
			}

			@Override
			public void onFailedImageLoad() {
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (hasStarted) {
			showNotification();
			hasBeenNotified = true;
		}
	}

	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_HAS_BEEN_NOTIFIED, hasBeenNotified);
	}

	@Override
	public void onCreateOptionsMenu(@NotNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.play, menu);
	}

	@Override
	public void onPrepareOptionsMenu(@NotNull Menu menu) {
		UIUtils.showMenuItem(menu, R.id.menu_send, isDirty);
		UIUtils.showMenuItem(menu, R.id.menu_discard, playId > 0 && isDirty);
		UIUtils.enableMenuItem(menu, R.id.menu_share, playId > 0);
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_discard:
				// TODO move to ViewModel
//				DialogUtils.createDiscardDialog(getActivity(), R.string.play, false, false, () -> {
//					play.dirtyTimestamp = 0;
//					play.updateTimestamp = 0;
//					play.deleteTimestamp = 0;
//					save("Discard");
//				}).show();
				return true;
			case R.id.menu_edit:
				logDataManipulationAction("Edit");
				LogPlayActivity.editPlay(getActivity(), internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl);
				return true;
			case R.id.menu_send:
				// TODO move to ViewModel
//				play.updateTimestamp = System.currentTimeMillis();
//				save("Save");
//				EventBus.getDefault().post(new PlaySentEvent());
				return true;
			case R.id.menu_delete: {
				DialogUtils.createThemedBuilder(getContext())
					.setMessage(R.string.are_you_sure_delete_play)
					.setPositiveButton(R.string.delete, (dialog, id) -> {
						// TODO move to ViewModel
//						if (hasStarted) cancelNotification();
//						play.end(); // this prevents the timer from reappearing
//						play.deleteTimestamp = System.currentTimeMillis();
//						save("Delete");
//						EventBus.getDefault().post(new PlayDeletedEvent());
					})
					.setNegativeButton(R.string.cancel, null)
					.setCancelable(true)
					.show();
				return true;
			}
			case R.id.menu_rematch:
				logDataManipulationAction("Rematch");
				LogPlayActivity.rematch(getContext(), internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort);
				getActivity().finish(); // don't want to show the "old" play upon return
				return true;
			case R.id.menu_change_game:
				logDataManipulationAction("ChangeGame");
				startActivity(CollectionActivity.createIntentForGameChange(requireContext(), internalId));
				getActivity().finish(); // don't want to show the "old" play upon return
				return true;
			case R.id.menu_share:
				ActivityUtils.share(requireActivity(), shortDescription, longDescription, R.string.share_play_title);
				Bundle bundle = new Bundle();
				bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Play");
				bundle.putString(Param.ITEM_ID, String.valueOf(playId));
				bundle.putString(Param.ITEM_NAME, shortDescription);
				firebaseAnalytics.logEvent(Event.SHARE, bundle);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void logDataManipulationAction(String action) {
		Bundle bundle = new Bundle();
		bundle.putString(Param.CONTENT_TYPE, "Play");
		bundle.putString("Action", action);
		bundle.putString("GameName", gameName);
		firebaseAnalytics.logEvent("DataManipulation", bundle);
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(SyncPlaysByGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			if (!TextUtils.isEmpty(event.getErrorMessage()) && PreferenceUtils.getSyncShowErrors(prefs)) {
				// TODO: 3/30/17 change to a snackbar (will need to change from a ListFragment)
				Toast.makeText(getContext(), event.getErrorMessage(), Toast.LENGTH_LONG).show();
			}
			updateRefreshStatus(false);
		}
	}

	private void updateRefreshStatus(final boolean value) {
		isRefreshing = value;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(() -> {
				if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isRefreshing);
			});
		}
	}

	@OnClick(R.id.headerContainer)
	void viewGame() {
		GameActivity.start(requireContext(), gameId, gameName);
	}

	@OnClick(R.id.timerEndButton)
	void onTimerClick() {
		LogPlayActivity.endPlay(getContext(), internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl);
	}

	/**
	 * @return true if the we're done loading
	 */
	private boolean onPlayQueryComplete(Cursor cursor) {
		// Move to refreshable resource loader
//		private static final int AGE_IN_DAYS_TO_REFRESH = 7;
//		if (playId > 0 &&
//			(play.syncTimestamp == 0 || DateTimeUtils.howManyDaysOld(play.syncTimestamp) > AGE_IN_DAYS_TO_REFRESH)) {
//			triggerRefresh();
//		}

		return false;
	}

	private void maybeShowNotification() {
		if (hasStarted) {
			showNotification();
		} else if (hasBeenNotified) {
			cancelNotification();
		}
	}

	private void showNotification() {
//		NotificationUtils.launchPlayingNotification(getActivity(), internalId, play, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort);
	}

	private void cancelNotification() {
		NotificationUtils.cancel(getActivity(), NotificationUtils.TAG_PLAY_TIMER, internalId);
	}

	private void triggerRefresh() {
		// TODO move this logic to the view model
		if (!isRefreshing) {
			TaskUtils.executeAsyncTask(new SyncPlaysByGameTask((BggApplication) requireActivity().getApplication(), gameId));
			updateRefreshStatus(true);
		}
	}

	private void save(String action) {
		// TODO move this logic to the view model
//		logDataManipulationAction(TextUtils.isEmpty(action) ? "Save" : action);
//		new PlayPersister(requireContext()).save(play, internalId, false);
//		triggerRefresh();
	}
}
