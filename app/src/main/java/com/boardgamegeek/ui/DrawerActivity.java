package com.boardgamegeek.ui;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.entities.RefreshableResource;
import com.boardgamegeek.entities.Status;
import com.boardgamegeek.entities.UserEntity;
import com.boardgamegeek.events.SignInEvent;
import com.boardgamegeek.pref.SettingsActivity;
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

/**
 * Activity that displays the navigation drawer and allows for content in the root_container FrameLayout.
 */
public abstract class DrawerActivity extends BaseActivity {
	@BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
	@BindView(R.id.drawer_container) View drawerListContainer;
	@BindView(R.id.left_drawer) LinearLayout drawerList;
	@BindView(R.id.toolbar) Toolbar toolbar;
	@Nullable @BindView(R.id.root_container) ViewGroup rootContainer;

	private SelfUserViewModel viewModel;

	protected int getDrawerResId() {
		return 0;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutResId());
		ButterKnife.bind(this);
		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}
		if (drawerLayout != null) {
			drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
			drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark));
		}
		viewModel = ViewModelProviders.of(this).get(SelfUserViewModel.class);
	}

	@LayoutRes
	protected int getLayoutResId() {
		return R.layout.activity_drawer_base;
	}

	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
		if (!PreferencesUtils.hasSeenNavDrawer(this)) {
			drawerLayout.openDrawer(GravityCompat.START);
			PreferencesUtils.sawNavDrawer(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshDrawer();
	}

	@Override
	protected void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SignInEvent event) {
		refreshUser(event.getUsername());
	}

	private void refreshDrawer() {
		if (drawerList == null) return;

		drawerList.removeAllViews();
		drawerList.addView(makeNavDrawerBuffer(drawerList));
		if (!Authenticator.isSignedIn(this)) {
			drawerList.addView(makeNavDrawerSpacer(drawerList));
			drawerList.addView(makeNavDrawerItem(R.string.title_signin, R.drawable.ic_account_circle_black_24dp, drawerList));
		} else {
			View view = makeNavDrawerHeader(drawerList);
			if (view != null) drawerList.addView(view);
			drawerList.addView(makeNavDrawerSpacer(drawerList));
			drawerList.addView(makeNavDrawerItem(R.string.title_collection, R.drawable.ic_collection, drawerList));
			drawerList.addView(makeNavDrawerItem(R.string.title_plays, R.drawable.ic_log_play, drawerList));
			drawerList.addView(makeNavDrawerItem(R.string.title_buddies, R.drawable.ic_user, drawerList));
		}
		drawerList.addView(makeNavDrawerSpacerWithDivider(drawerList));

		drawerList.addView(makeNavDrawerSpacer(drawerList));
		drawerList.addView(makeNavDrawerItem(R.string.title_search, R.drawable.ic_action_search, drawerList));
		drawerList.addView(makeNavDrawerItem(R.string.title_hotness, R.drawable.ic_hotness, drawerList));
		drawerList.addView(makeNavDrawerItem(R.string.title_top_games, R.drawable.ic_top_games, drawerList));
		drawerList.addView(makeNavDrawerItem(R.string.title_geeklists, R.drawable.ic_geek_list, drawerList));
		drawerList.addView(makeNavDrawerItem(R.string.title_forums, R.drawable.ic_forums, drawerList));
		drawerList.addView(makeNavDrawerSpacerWithDivider(drawerList));

		drawerList.addView(makeNavDrawerSpacer(drawerList));
		drawerList.addView(makeNavDrawerItem(R.string.title_backup, R.drawable.ic_data, drawerList));
		drawerList.addView(makeNavDrawerItem(R.string.title_settings, R.drawable.ic_settings, drawerList));
		drawerList.addView(makeNavDrawerSpacer(drawerList));
	}

	private void selectItem(int titleResId) {
		if (titleResId != getDrawerResId()) {
			Intent intent = null;
			boolean shouldFinish = true;
			switch (titleResId) {
				case R.string.title_collection:
					intent = new Intent(this, CollectionActivity.class);
					break;
				case R.string.title_search:
					intent = new Intent(this, SearchResultsActivity.class);
					shouldFinish = false;
					break;
				case R.string.title_hotness:
					intent = new Intent(this, HotnessActivity.class);
					break;
				case R.string.title_top_games:
					intent = new Intent(this, TopGamesActivity.class);
					break;
				case R.string.title_geeklists:
					intent = new Intent(this, GeekListsActivity.class);
					break;
				case R.string.title_plays:
					intent = new Intent(this, PlaysSummaryActivity.class);
					break;
				case R.string.title_buddies:
					intent = new Intent(this, BuddiesActivity.class);
					break;
				case R.string.title_forums:
					intent = new Intent(this, ForumsActivity.class);
					break;
				case R.string.title_signin:
					startActivity(new Intent(this, LoginActivity.class));
					break;
				case R.string.title_backup:
					startActivity(new Intent(this, DataActivity.class));
					break;
				case R.string.title_settings:
					startActivity(new Intent(this, SettingsActivity.class));
					break;
			}
			if (intent != null) {
				startActivity(intent);
				if (shouldFinish) {
					finish();
				}
			}
		}
		drawerLayout.closeDrawer(drawerListContainer);
	}

	private View makeNavDrawerHeader(ViewGroup container) {
		final View view = getLayoutInflater().inflate(R.layout.row_header_drawer, container, false);

		String fullName = AccountUtils.getFullName(this);
		String username = AccountUtils.getUsername(this);
		if (TextUtils.isEmpty(fullName)) {
			if (TextUtils.isEmpty(username)) {
				Account account = Authenticator.getAccount(this);
				if (account != null) refreshUser(account.name);
				return null;
			} else {
				((TextView) view.findViewById(R.id.account_info_primary)).setText(username);
				refreshUser(username);
			}
		} else {
			((TextView) view.findViewById(R.id.account_info_primary)).setText(fullName);
			((TextView) view.findViewById(R.id.account_info_secondary)).setText(username);
		}

		String avatarUrl = AccountUtils.getAvatarUrl(this);
		final ImageView imageView = view.findViewById(R.id.account_image);
		if (TextUtils.isEmpty(avatarUrl)) {
			imageView.setVisibility(View.GONE);
		} else {
			imageView.setVisibility(View.VISIBLE);
			ImageUtils.loadThumbnail(imageView, avatarUrl, R.drawable.person_image_empty);
		}

		return view;
	}

	private void refreshUser(String username) {
		if (TextUtils.isEmpty(username)) return;
		viewModel.setUsername(username);
		viewModel.getUser().observe(this, new Observer<RefreshableResource<UserEntity>>() {
			@Override
			public void onChanged(RefreshableResource<UserEntity> userEntityRefreshableResource) {
				if (userEntityRefreshableResource != null &&
					userEntityRefreshableResource.getStatus() == Status.SUCCESS &&
					userEntityRefreshableResource.getData() != null &&
					!TextUtils.isEmpty(userEntityRefreshableResource.getData().getUserName())) {
					refreshDrawer();
				}
			}
		});
	}

	private View makeNavDrawerBuffer(ViewGroup container) {
		return getLayoutInflater().inflate(R.layout.row_buffer_drawer, container, false);
	}

	private View makeNavDrawerSpacer(ViewGroup container) {
		return getLayoutInflater().inflate(R.layout.row_spacer_drawer, container, false);
	}

	private View makeNavDrawerSpacerWithDivider(ViewGroup container) {
		final View view = makeNavDrawerSpacer(container);
		view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
		return view;
	}

	private View makeNavDrawerItem(final int titleId, int iconId, ViewGroup container) {
		View view = getLayoutInflater().inflate(R.layout.row_drawer, container, false);

		TextView titleView = view.findViewById(android.R.id.title);
		ImageView iconView = view.findViewById(android.R.id.icon);

		titleView.setText(titleId);
		if (iconId != 0) {
			iconView.setImageResource(iconId);
			iconView.setVisibility(View.VISIBLE);
		} else {
			iconView.setVisibility(View.GONE);
		}
		if (titleId == getDrawerResId()) {
			view.setBackgroundResource(R.color.navdrawer_selected_row);
			titleView.setTextColor(ContextCompat.getColor(this, R.color.primary));
			iconView.setColorFilter(ContextCompat.getColor(this, R.color.primary));
		} else {
			iconView.setColorFilter(ContextCompat.getColor(this, R.color.navdrawer_icon_tint));
		}

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectItem(titleId);
			}
		});

		return view;
	}
}
