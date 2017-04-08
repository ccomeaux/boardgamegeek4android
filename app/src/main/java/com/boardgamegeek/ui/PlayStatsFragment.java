package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TableLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlayStatsUpdatedEvent;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.CalculatePlayStatsTask;
import com.boardgamegeek.ui.dialog.PlayStatsIncludeSettingsDialogFragment;
import com.boardgamegeek.ui.model.HIndexEntry;
import com.boardgamegeek.ui.model.PlayStats;
import com.boardgamegeek.ui.widget.PlayStatView.Builder;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.TaskUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class PlayStatsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
	private Unbinder unbinder;
	@BindView(R.id.progress) View progressView;
	@BindView(R.id.empty) View emptyView;
	@BindView(R.id.data) ViewGroup dataView;
	@BindView(R.id.table_play_count) TableLayout playCountTable;
	@BindView(R.id.game_h_index) TextView hIndexView;
	@BindView(R.id.table_hindex) TableLayout hIndexTable;
	@BindView(R.id.table_advanced) TableLayout advancedTable;
	@BindView(R.id.collection_status_container) ViewGroup collectionStatusContainer;
	@BindView(R.id.accuracy_container) ViewGroup accuracyContainer;
	@BindView(R.id.accuracy_message) TextView accuracyMessage;
	private boolean isOwnedSynced;
	private boolean isPlayedSynced;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		bindCollectionStatusMessage();
		bindAccuracyMessage();
		return rootView;
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
		TaskUtils.executeAsyncTask(new CalculatePlayStatsTask(getContext()));
	}

	@Override
	public void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(PlayStatsUpdatedEvent event) {
		bindUi(event.getPlayStats());
	}

	private void bindCollectionStatusMessage() {
		isOwnedSynced = PreferencesUtils.isSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_OWN);
		isPlayedSynced = PreferencesUtils.isSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_PLAYED);
		collectionStatusContainer.setVisibility(isOwnedSynced && isPlayedSynced ? View.GONE : View.VISIBLE);
	}

	private void bindAccuracyMessage() {
		List<String> things = new ArrayList<>(3);
		if (!PreferencesUtils.logPlayStatsIncomplete(getContext())) {
			things.add(getString(R.string.incomplete_games).toLowerCase());
		}
		if (!PreferencesUtils.logPlayStatsExpansions(getContext())) {
			things.add(getString(R.string.expansions).toLowerCase());
		}
		if (!PreferencesUtils.logPlayStatsAccessories(getContext())) {
			things.add(getString(R.string.accessories).toLowerCase());
		}
		accuracyContainer.setVisibility(things.size() == 0 ? View.GONE : View.VISIBLE);
		accuracyMessage.setText(getString(R.string.play_stat_accuracy,
			StringUtils.formatList(things, getString(R.string.or).toLowerCase(), ",")));
	}

	private void bindUi(PlayStats stats) {
		if (stats == null) {
			showEmpty();
			return;
		}

		playCountTable.removeAllViews();
		addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_play_count).value(stats.getNumberOfPlays()));
		addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_distinct_games).value(stats.getNumberOfGames()));
		addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_quarters).value(stats.getNumberOfQuarters()));
		addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_dimes).value(stats.getNumberOfDimes()));
		addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_nickels).value(stats.getNumberOfNickels()));

		if (isPlayedSynced)
			addStatRow(playCountTable, new Builder().labelId(R.string.play_stat_top_100).value(stats.getTop100Count() + "%"));

		hIndexView.setText(String.valueOf(stats.getGameHIndex()));
		hIndexTable.removeAllViews();
		boolean addDivider = true;
		for (HIndexEntry game : stats.getHIndexGames()) {
			final Builder builder = new Builder().labelText(game.getDescription()).value(game.getPlayCount());
			if (game.getPlayCount() == stats.getGameHIndex()) {
				builder.backgroundResource(R.color.light_blue_transparent);
				addDivider = false;
			} else if (game.getPlayCount() < stats.getGameHIndex() && addDivider) {
				addDivider(hIndexTable);
				addDivider = false;
			}
			addStatRow(hIndexTable, builder);
		}

		advancedTable.removeAllViews();
		if (stats.getFriendless() != PlayStats.INVALID_FRIENDLESS)
			addStatRow(advancedTable, new Builder()
				.labelId(R.string.play_stat_friendless)
				.value(stats.getFriendless())
				.infoId(R.string.play_stat_friendless_info));
		if (stats.getUtilization() != PlayStats.INVALID_UTILIZATION)
			addStatRow(advancedTable, new Builder()
				.labelId(R.string.play_stat_utilization)
				.valueAsPercentage(stats.getUtilization())
				.infoId(R.string.play_stat_utilization_info));
		if (stats.getCfm() != PlayStats.INVALID_CFM)
			addStatRow(advancedTable, new Builder()
				.labelId(R.string.play_stat_cfm)
				.value(stats.getCfm())
				.infoId(R.string.play_stat_cfm_info));

		showData();
	}

	private void showEmpty() {
		progressView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		emptyView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		progressView.setVisibility(View.GONE);
		emptyView.setVisibility(View.VISIBLE);
		dataView.setVisibility(View.GONE);
	}

	private void showData() {
		progressView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		dataView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		progressView.setVisibility(View.GONE);
		emptyView.setVisibility(View.GONE);
		dataView.setVisibility(View.VISIBLE);
	}

	private void addStatRow(ViewGroup container, Builder builder) {
		container.addView(builder.build(getActivity()));
	}

	private void addDivider(ViewGroup container) {
		View view = new View(getActivity());
		view.setLayoutParams(new TableLayout.LayoutParams(0, 1));
		view.setBackgroundResource(R.color.dark_blue);
		container.addView(view);
	}

	@OnClick(R.id.game_h_index_info)
	void onGameHIndexInfoClick() {
		showAlertDialog(R.string.play_stat_game_h_index, R.string.play_stat_game_h_index_info);
	}

	private void showAlertDialog(@StringRes int titleResId, @StringRes int messageResId) {
		SpannableString spannableMessage = new SpannableString(getString(messageResId));
		Linkify.addLinks(spannableMessage, Linkify.ALL);
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
			.setTitle(titleResId)
			.setMessage(spannableMessage);
		AlertDialog dialog = builder.show();
		TextView textView = (TextView) dialog.findViewById(android.R.id.message);
		if (textView != null) {
			textView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	@OnClick(R.id.settings_collection_status)
	void onSettingsCollectionStatusClick() {
		DialogUtils.createConfirmationDialog(getContext(), R.string.play_stat_msg_collection_status, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PreferencesUtils.addSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_OWN);
				PreferencesUtils.addSyncStatus(getContext(), BggService.COLLECTION_QUERY_STATUS_PLAYED);
				SyncService.sync(getContext(), SyncService.FLAG_SYNC_COLLECTION);
				bindCollectionStatusMessage();
			}
		}).show();
	}

	@OnClick(R.id.settings_include)
	void onSettingsIncludeClick() {
		PlayStatsIncludeSettingsDialogFragment df = PlayStatsIncludeSettingsDialogFragment.newInstance(accuracyContainer);
		DialogUtils.showFragment(getActivity(), df, "play_stats_settings_include");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.startsWith(PreferencesUtils.LOG_PLAY_STATS_PREFIX)) {
			bindAccuracyMessage();
			TaskUtils.executeAsyncTask(new CalculatePlayStatsTask(getContext()));
		}
	}
}
