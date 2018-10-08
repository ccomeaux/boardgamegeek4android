package com.boardgamegeek.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.tasks.sync.SyncUserTask;
import com.boardgamegeek.tasks.sync.SyncUserTask.CompletedEvent;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.UpdateBuddyNicknameDialogFragment;
import com.boardgamegeek.ui.model.Buddy;
import com.boardgamegeek.ui.model.Player;
import com.boardgamegeek.ui.model.PlayerColor;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.SelectionBuilder;
import com.boardgamegeek.util.TaskUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

public class BuddyFragment extends Fragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	private static final String KEY_PLAYER_NAME = "PLAYER_NAME";

	private static final int PLAYS_TOKEN = 1;
	private static final int COLORS_TOKEN = 2;
	private static final int TOKEN = 0;

	private String buddyName;
	private String playerName;
	private boolean isRefreshing;
	@State boolean hasBeenRefreshed;

	private Unbinder unbinder;
	private SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.buddy_info) View buddyInfoView;
	@BindView(R.id.full_name) TextView fullNameView;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.avatar) ImageView avatarView;
	@BindView(R.id.nickname) TextView nicknameView;
	@BindView(R.id.collection_card) View collectionCard;
	@BindView(R.id.plays_card) View playsCard;
	@BindView(R.id.plays_label) TextView playsView;
	@BindView(R.id.wins_label) TextView winsView;
	@BindView(R.id.wins_percentage) TextView winPercentageView;
	@BindView(R.id.color_container) LinearLayout colorContainer;
	@BindView(R.id.updated) TimestampView updatedView;
	private int defaultTextColor;
	private int lightTextColor;

	public static BuddyFragment newInstance(String username, String playerName) {
		Bundle args = new Bundle();
		args.putString(KEY_BUDDY_NAME, username);
		args.putString(KEY_PLAYER_NAME, playerName);

		BuddyFragment fragment = new BuddyFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		readBundle(getArguments());

		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_buddy, container, false);

		unbinder = ButterKnife.bind(this, rootView);

		buddyInfoView.setVisibility(isUser() ? View.VISIBLE : View.GONE);
		collectionCard.setVisibility(isUser() ? View.VISIBLE : View.GONE);
		updatedView.setVisibility(isUser() ? View.VISIBLE : View.GONE);

		swipeRefreshLayout = (SwipeRefreshLayout) rootView;
		if (isUser()) {
			swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());
			swipeRefreshLayout.setEnabled(true);
		} else {
			swipeRefreshLayout.setEnabled(false);
		}

		defaultTextColor = nicknameView.getTextColors().getDefaultColor();
		lightTextColor = ContextCompat.getColor(getContext(), R.color.secondary_text);

		if (isUser()) {
			LoaderManager.getInstance(this).restartLoader(TOKEN, null, this);
		} else {
			nicknameView.setTextColor(defaultTextColor);
			nicknameView.setText(playerName);
		}
		LoaderManager.getInstance(this).restartLoader(PLAYS_TOKEN, null, this);
		LoaderManager.getInstance(this).restartLoader(COLORS_TOKEN, null, this);

		return rootView;
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		buddyName = bundle.getString(KEY_BUDDY_NAME);
		playerName = bundle.getString(KEY_PLAYER_NAME);
	}

	private boolean isUser() {
		return !TextUtils.isEmpty(buddyName);
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		if (unbinder != null) unbinder.unbind();
		super.onDestroyView();
	}

	@DebugLog
	private void updateRefreshStatus(boolean value) {
		isRefreshing = value;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}

	@NonNull
	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		if (getContext() == null) return null;
		CursorLoader loader = null;
		switch (id) {
			case TOKEN:
				loader = new CursorLoader(getContext(), Buddies.buildBuddyUri(buddyName), Buddy.PROJECTION, null, null, null);
				break;
			case PLAYS_TOKEN:
				if (isUser()) {
					loader = new CursorLoader(getContext(),
						Plays.buildPlayersByUniqueUserUri(),
						Player.PROJECTION,
						PlayPlayers.USER_NAME + "=? AND " + SelectionBuilder.whereZeroOrNull(Plays.NO_WIN_STATS),
						new String[] { buddyName },
						null);

				} else {
					loader = new CursorLoader(getContext(),
						Plays.buildPlayersByUniquePlayerUri(),
						Player.PROJECTION,
						"(" + PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL) AND play_players." + PlayPlayers.NAME + "=?",
						new String[] { "", playerName },
						null);
				}
				break;
			case COLORS_TOKEN:
				if (!TextUtils.isEmpty(buddyName) || !TextUtils.isEmpty(playerName)) {
					Uri uri = isUser() ? PlayerColors.buildUserUri(buddyName) : PlayerColors.buildPlayerUri(playerName);
					loader = new CursorLoader(getContext(),
						uri,
						PlayerColor.PROJECTION,
						null, null, null);
				}
				break;
		}
		return loader;
	}

	@Override
	@DebugLog
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;

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
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
	}

	@DebugLog
	@OnClick(R.id.nickname)
	public void onEditNicknameClick() {
		if (isUser()) {
			showNicknameDialog(nicknameView.getText().toString());
		} else {
			showPlayerNameDialog(nicknameView.getText().toString());
		}
	}

	@DebugLog
	@OnClick(R.id.collection_root)
	public void onCollectionClick() {
		BuddyCollectionActivity.start(getContext(), buddyName);
	}

	@DebugLog
	@OnClick(R.id.plays_root)
	public void onPlaysClick() {
		if (isUser()) {
			BuddyPlaysActivity.start(getContext(), buddyName);
		} else {
			PlayerPlaysActivity.start(getContext(), playerName);
		}
	}

	@DebugLog
	@OnClick(R.id.colors_root)
	public void onColorsClick() {
		PlayerColorsActivity.start(getContext(), buddyName, playerName);
	}

	private void onBuddyQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			requestRefresh();
			return;
		}

		Buddy buddy = Buddy.fromCursor(cursor);

		ImageUtils.loadThumbnail(avatarView, buddy.getAvatarUrl(), R.drawable.person_image_empty);
		fullNameView.setText(buddy.getFullName());
		usernameView.setText(buddyName);
		if (TextUtils.isEmpty(buddy.getNickName())) {
			nicknameView.setTextColor(lightTextColor);
			nicknameView.setText(buddy.getFirstName());
		} else {
			nicknameView.setTextColor(defaultTextColor);
			nicknameView.setText(buddy.getNickName());
		}
		updatedView.setTimestamp(buddy.getUpdated());
	}

	@DebugLog
	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		Player player = Player.fromCursor(cursor);
		final int playCount = player.getPlayCount();
		final int winCount = player.getWinCount();
		if (playCount > 0 || winCount > 0) {
			playsCard.setVisibility(View.VISIBLE);
			playsView.setText(PresentationUtils.getQuantityText(getContext(), R.plurals.winnable_plays_suffix, playCount, playCount));
			winsView.setText(PresentationUtils.getQuantityText(getContext(), R.plurals.wins_suffix, winCount, winCount));
			winPercentageView.setText(getString(R.string.percentage, (int) ((double) winCount / playCount * 100)));
		} else {
			playsCard.setVisibility(View.GONE);
		}
	}

	@DebugLog
	private void onColorsQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		colorContainer.removeAllViews();

		for (int i = 0; i < 3; i++) {
			if (cursor.moveToNext()) {
				colorContainer.setVisibility(View.VISIBLE);
				ImageView view = createViewToBeColored();
				PlayerColor color = PlayerColor.fromCursor(cursor);
				ColorUtils.setColorViewValue(view, ColorUtils.parseColor(color.getColor()));
				colorContainer.addView(view);
			} else {
				colorContainer.setVisibility(View.GONE);
				return;
			}
		}
	}

	@DebugLog
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
		requestRefresh();
	}

	@DebugLog
	private void requestRefresh() {
		if (!isRefreshing) {
			if (hasBeenRefreshed) {
				updateRefreshStatus(false);
			} else {
				forceRefresh();
			}
		}
	}

	@DebugLog
	public void forceRefresh() {
		if (isUser()) {
			updateRefreshStatus(true);
			TaskUtils.executeAsyncTask(new SyncUserTask(getActivity(), buddyName));
		} else {
			Timber.w("Something tried to refresh a player that wasn't a user!");
		}
		hasBeenRefreshed = true;
	}

	@DebugLog
	private void showNicknameDialog(final String nickname) {
		UpdateBuddyNicknameDialogFragment dialogFragment = UpdateBuddyNicknameDialogFragment.newInstance(nickname);
		DialogUtils.showFragment(getActivity(), dialogFragment, "edit_nickname");
	}

	@DebugLog
	private void showPlayerNameDialog(final String oldName) {
		EditTextDialogFragment editTextDialogFragment = EditTextDialogFragment.newInstance(R.string.title_edit_player, oldName);
		DialogUtils.showFragment(getActivity(), editTextDialogFragment, "edit_player");
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CompletedEvent event) {
		if (event.getUsername().equals(buddyName)) {
			updateRefreshStatus(false);
		}
	}
}
