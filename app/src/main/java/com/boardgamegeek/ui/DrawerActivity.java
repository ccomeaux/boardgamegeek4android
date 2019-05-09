package com.boardgamegeek.ui;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.entities.Status;
import com.boardgamegeek.events.SignInEvent;
import com.boardgamegeek.pref.SettingsActivity;
import com.boardgamegeek.ui.viewmodel.SelfUserViewModel;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.google.android.material.navigation.NavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

/**
 * Activity that displays the navigation drawer and allows for content in the root_container FrameLayout.
 */
public abstract class DrawerActivity extends BaseActivity {
	@BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
	@BindView(R.id.navigation) NavigationView navigationView;
	@BindView(R.id.toolbar) Toolbar toolbar;
	@Nullable @BindView(R.id.root_container) ViewGroup rootContainer;

	private SelfUserViewModel viewModel;

	protected int getNavigationItemId() {
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

		navigationView.setNavigationItemSelectedListener(menuItem -> {
			selectItem(menuItem.getItemId());
			return true;
		});
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

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SignInEvent event) {
		refreshUser(event.getUsername());
	}

	private void refreshDrawer() {
		navigationView.getMenu().setGroupVisible(R.id.personal, Authenticator.isSignedIn(this));
		refreshHeader();
		navigationView.setCheckedItem(getNavigationItemId());
	}

	private void selectItem(int menuItemId) {
		if (menuItemId != getNavigationItemId()) {
			Intent intent = null;
			boolean shouldFinish = true;
			switch (menuItemId) {
				case R.id.collection:
					intent = new Intent(this, CollectionActivity.class);
					break;
				case R.id.designers:
					intent = new Intent(this, DesignersActivity.class);
					break;
				case R.id.artists:
					intent = new Intent(this, ArtistsActivity.class);
					break;
				case R.id.search:
					intent = new Intent(this, SearchResultsActivity.class);
					shouldFinish = false;
					break;
				case R.id.hotness:
					intent = new Intent(this, HotnessActivity.class);
					break;
				case R.id.top_games:
					intent = new Intent(this, TopGamesActivity.class);
					break;
				case R.id.geeklists:
					intent = new Intent(this, GeekListsActivity.class);
					break;
				case R.id.plays:
					intent = new Intent(this, PlaysSummaryActivity.class);
					break;
				case R.id.geek_buddies:
					intent = new Intent(this, BuddiesActivity.class);
					break;
				case R.id.forums:
					intent = new Intent(this, ForumsActivity.class);
					break;
				case R.id.data:
					startActivity(new Intent(this, DataActivity.class));
					break;
				case R.id.settings:
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
		drawerLayout.closeDrawer(navigationView);
	}

	private void refreshHeader() {
		View view = navigationView.getHeaderView(0);
		TextView primaryView = view.findViewById(R.id.account_info_primary);
		TextView secondaryView = view.findViewById(R.id.account_info_secondary);
		ImageView imageView = view.findViewById(R.id.account_image);

		if (Authenticator.isSignedIn(this)) {
			String fullName = AccountUtils.getFullName(this);
			String username = AccountUtils.getUsername(this);
			if (TextUtils.isEmpty(fullName)) {
				if (TextUtils.isEmpty(username)) {
					Account account = Authenticator.getAccount(this);
					if (account != null) refreshUser(account.name);
					return;
				} else {
					primaryView.setText(username);
					secondaryView.setText("");
					refreshUser(username);
				}
			} else {
				primaryView.setText(fullName);
				secondaryView.setText(username);
			}

			String avatarUrl = AccountUtils.getAvatarUrl(this);
			if (TextUtils.isEmpty(avatarUrl)) {
				imageView.setVisibility(View.GONE);
			} else {
				imageView.setVisibility(View.VISIBLE);
				if (avatarUrl != null) {
					ImageUtils.loadThumbnail(imageView, avatarUrl, R.drawable.person_image_empty);
				}
			}
		} else {
			primaryView.setText(R.string.title_signin);
			primaryView.setOnClickListener(v -> startActivity(new Intent(DrawerActivity.this, LoginActivity.class)));
			secondaryView.setText("");
			imageView.setVisibility(View.GONE);
		}
	}

	private void refreshUser(String username) {
		if (TextUtils.isEmpty(username)) return;
		viewModel.setUsername(username);
		viewModel.getUser().observe(this, userEntityRefreshableResource -> {
			if (userEntityRefreshableResource != null &&
				userEntityRefreshableResource.getStatus() == Status.SUCCESS &&
				userEntityRefreshableResource.getData() != null &&
				!TextUtils.isEmpty(userEntityRefreshableResource.getData().getUserName())) {
				refreshDrawer();
			}
		});
	}
}
