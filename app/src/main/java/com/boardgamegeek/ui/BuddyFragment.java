package com.boardgamegeek.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.tasks.BuddyNicknameUpdateTask;
import com.boardgamegeek.ui.model.Buddy;
import com.boardgamegeek.ui.model.BuddyColor;
import com.boardgamegeek.ui.model.Player;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class BuddyFragment extends Fragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final String KEY_REFRESHED = "REFRESHED";
	private static final int PLAYS_TOKEN = 1;
	private static final int COLORS_TOKEN = 2;
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec
	private static final int TOKEN = 0;

	private Handler timeHintUpdateHandler = new Handler();
	private Runnable timeHintUpdateRunnable = null;

	private String buddyName;
	private String playerName;
	private boolean isRefreshing;
	private boolean hasBeenRefreshed;

	private ViewGroup rootView;
	private SwipeRefreshLayout swipeRefreshLayout;
	@SuppressWarnings("unused") @InjectView(R.id.buddy_info) View buddyInfoView;
	@SuppressWarnings("unused") @InjectView(R.id.full_name) TextView fullNameView;
	@SuppressWarnings("unused") @InjectView(R.id.username) TextView usernameView;
	@SuppressWarnings("unused") @InjectView(R.id.avatar) ImageView avatarView;
	@SuppressWarnings("unused") @InjectView(R.id.nickname) TextView nicknameView;
	@SuppressWarnings("unused") @InjectView(R.id.collection_card) View collectionCard;
	@SuppressWarnings("unused") @InjectView(R.id.plays_label) TextView playsView;
	@SuppressWarnings("unused") @InjectView(R.id.color_container) LinearLayout colorContainer;
	@SuppressWarnings("unused") @InjectView(R.id.updated) TextView updatedView;
	private int defaultTextColor;
	private int lightTextColor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		timeHintUpdateHandler = new Handler();
		if (savedInstanceState != null) {
			hasBeenRefreshed = savedInstanceState.getBoolean(KEY_REFRESHED);
		}

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		buddyName = intent.getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
		playerName = intent.getStringExtra(ActivityUtils.KEY_PLAYER_NAME);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_REFRESHED, hasBeenRefreshed);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = (ViewGroup) inflater.inflate(R.layout.fragment_buddy, container, false);

		ButterKnife.inject(this, rootView);

		buddyInfoView.setVisibility(isUser() ? View.VISIBLE : View.GONE);
		collectionCard.setVisibility(isUser() ? View.VISIBLE : View.GONE);
		updatedView.setVisibility(isUser() ? View.VISIBLE : View.GONE);

		swipeRefreshLayout = (SwipeRefreshLayout) rootView;
		if (isUser()) {
			swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);
			swipeRefreshLayout.setEnabled(true);
		} else {
			swipeRefreshLayout.setEnabled(false);
		}

		defaultTextColor = nicknameView.getTextColors().getDefaultColor();
		lightTextColor = getResources().getColor(R.color.light_text);

		if (isUser()) {
			getLoaderManager().restartLoader(TOKEN, null, this);
		} else {
			nicknameView.setTextColor(defaultTextColor);
			nicknameView.setText(playerName);
		}
		getLoaderManager().restartLoader(PLAYS_TOKEN, null, this);
		getLoaderManager().restartLoader(COLORS_TOKEN, null, this);

		return rootView;
	}

	private boolean isUser() {
		return !TextUtils.isEmpty(buddyName);
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	@DebugLog
	public void onPause() {
		super.onPause();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@SuppressWarnings("unused")
	public void onEventMainThread(UpdateEvent event) {
		isRefreshing = event.getType() == UpdateService.SYNC_TYPE_BUDDY;
		updateRefreshStatus();
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	public void onEventMainThread(UpdateCompleteEvent event) {
		isRefreshing = false;
		updateRefreshStatus();
	}

	private void updateRefreshStatus() {
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case TOKEN:
				loader = new CursorLoader(getActivity(), Buddies.buildBuddyUri(buddyName), Buddy.PROJECTION, null, null, null);
				break;
			case PLAYS_TOKEN:
				if (isUser()) {
					loader = new CursorLoader(getActivity(),
						Plays.buildPlayersByUniqueUserUri(),
						Player.PROJECTION,
						PlayPlayers.USER_NAME + "=?",
						new String[] { buddyName },
						null);

				} else {
					loader = new CursorLoader(getActivity(),
						Plays.buildPlayersByUniquePlayerUri(),
						Player.PROJECTION,
						"(" + PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL) AND play_players." + PlayPlayers.NAME + "=?",
						new String[] { "", playerName },
						null);
				}
				break;
			case COLORS_TOKEN:
				loader = new CursorLoader(getActivity(),
					isUser() ? PlayerColors.buildUserUri(buddyName) : PlayerColors.buildPlayerUri(playerName),
					BuddyColor.PROJECTION,
					null, null, null);
				break;
		}
		return loader;
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		switch (loader.getId()) {
			case TOKEN:
				onBuddyQueryComplete(cursor);
				break;
			case PLAYS_TOKEN:
				onPlaysQueryComplete(cursor);
				break;
			case COLORS_TOKEN:
				onColorsQueryComplete(cursor);
				break;
			default:
				cursor.close();
				break;
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@DebugLog
	@SuppressWarnings("unused")
	@OnClick(R.id.nickname)
	public void onEditNicknameClick(View v) {
		showDialog(nicknameView.getText().toString(), buddyName);
	}

	@DebugLog
	@SuppressWarnings("unused")
	@OnClick(R.id.collection_root)
	public void onCollectionClick(View v) {
		Intent intent = new Intent(getActivity(), BuddyCollectionActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, buddyName);
		startActivity(intent);
	}

	@DebugLog
	@SuppressWarnings("unused")
	@OnClick(R.id.plays_root)
	public void onPlaysClick(View v) {
		if (isUser()) {
			Intent intent = new Intent(getActivity(), BuddyPlaysActivity.class);
			intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, buddyName);
			startActivity(intent);
		} else {
			ActivityUtils.startPlayerPlaysActivity(getActivity(), playerName, buddyName);
		}
	}

	@DebugLog
	@SuppressWarnings("unused")
	@OnClick(R.id.colors_root)
	public void onColorsClick(View v) {
		Intent intent = new Intent(getActivity(), BuddyColorsActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, buddyName);
		intent.putExtra(ActivityUtils.KEY_PLAYER_NAME, playerName);
		startActivity(intent);
	}

	private void onBuddyQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			requestRefresh();
			return;
		}

		Buddy buddy = Buddy.fromCursor(cursor);

		Picasso.with(getActivity())
			.load(HttpUtils.ensureScheme(buddy.getAvatarUrl()))
			.placeholder(R.drawable.person_image_empty)
			.error(R.drawable.person_image_empty)
			.fit().into(avatarView);
		fullNameView.setText(buddy.getFullName());
		usernameView.setText(buddyName);
		if (TextUtils.isEmpty(buddy.getNickName())) {
			nicknameView.setTextColor(lightTextColor);
			nicknameView.setText(buddy.getFirstName());
		} else {
			nicknameView.setTextColor(defaultTextColor);
			nicknameView.setText(buddy.getNickName());
		}
		updatedView.setTag(buddy.getUpdated());

		updateTimeBasedUi();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
		timeHintUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
	}

	@DebugLog
	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (updatedView != null) {
			long updated = (long) updatedView.getTag();
			updatedView.setText(PresentationUtils.describePastTimeSpan(updated, getString(R.string.needs_updating), getString(R.string.updated)));
		}
	}

	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		Player player = Player.fromCursor(cursor);
		final int playCount = player.getPlayCount();
		playsView.setText(PresentationUtils.getQuantityText(getContext(), R.plurals.plays_suffix, playCount, playCount));
	}

	private void onColorsQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		colorContainer.removeAllViews();

		for (int i = 0; i < 3; i++) {
			if (cursor.moveToNext()) {
				colorContainer.setVisibility(View.VISIBLE);
				ImageView view = createViewToBeColored();
				BuddyColor color = BuddyColor.fromCursor(cursor);
				ColorUtils.setColorViewValue(view, ColorUtils.parseColor(color.getColor()));
				colorContainer.addView(view);
			} else {
				colorContainer.setVisibility(View.GONE);
				return;
			}
		}
	}

	private ImageView createViewToBeColored() {
		ImageView view = new ImageView(getActivity());
		int size = getResources().getDimensionPixelSize(R.dimen.color_circle_diameter_small);
		int margin = getResources().getDimensionPixelSize(R.dimen.color_circle_diameter_small_margin);
		LayoutParams lp = new LayoutParams(size, size);
		lp.setMargins(margin, margin, margin, margin);
		view.setLayoutParams(lp);
		return view;
	}

	@DebugLog
	@Override
	public void onRefresh() {
		forceRefresh();
	}

	private void requestRefresh() {
		if (!hasBeenRefreshed) {
			forceRefresh();
			hasBeenRefreshed = true;
		}
	}

	public void forceRefresh() {
		if (isUser()) {
			UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_BUDDY, buddyName);
		} else {
			Timber.w("Something tried to refresh a player that wasn't a user!");
		}
	}

	private void showDialog(final String nickname, final String username) {
		final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_edit_nickname, rootView, false);

		final EditText editText = (EditText) view.findViewById(R.id.edit_nickname);
		final CheckBox checkBox = (CheckBox) view.findViewById(R.id.change_plays);
		if (!TextUtils.isEmpty(nickname)) {
			editText.setText(nickname);
			editText.setSelection(0, nickname.length());
		}

		AlertDialog dialog = new AlertDialog.Builder(getActivity()).setView(view)
			.setTitle(R.string.title_edit_nickname).setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String newNickname = editText.getText().toString();
					BuddyNicknameUpdateTask task = new BuddyNicknameUpdateTask(getActivity(), username, newNickname, checkBox.isChecked());
					TaskUtils.executeAsyncTask(task);
				}
			}).create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		dialog.show();
	}
}
