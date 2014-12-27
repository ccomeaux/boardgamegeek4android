package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.pref.Preferences;

public abstract class DrawerActivity extends BaseActivity {
	private static final int REQUEST_SIGNIN = 1;
	@InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
	@InjectView(R.id.drawer_container) View mDrawerListContainer;
	@InjectView(R.id.left_drawer) LinearLayout mDrawerList;

	protected abstract int getContentViewId();

	protected int getDrawerResId() {
		return 0;
	}

	protected boolean isTitleHidden() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getContentViewId());
		ButterKnife.inject(this);
		if (mDrawerLayout != null) {
			mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		}

		// TODO open the drawer upon launch until user opens it themselves
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshDrawer();
	}

	public boolean isDrawerOpen() {
		return mDrawerLayout != null && mDrawerListContainer != null
			&& mDrawerLayout.isDrawerOpen(mDrawerListContainer);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_SIGNIN && resultCode == RESULT_OK) {
			onSignInSuccess();
		}
	}

	protected void onSignInSuccess() {
		refreshDrawer();
	}

	private void refreshDrawer() {
		if (mDrawerList == null) {
			return;
		}

		mDrawerList.removeAllViews();

		mDrawerList.addView(makeNavDrawerSeparator(R.string.title_my_geek, mDrawerList));
		if (!Authenticator.isSignedIn(DrawerActivity.this)) {
			mDrawerList.addView(makeNavDrawerItem(R.string.title_signin, R.drawable.ic_account_circle_black_24dp, mDrawerList));
		} else {
			mDrawerList.addView(makeNavDrawerItem(R.string.title_collection, R.drawable.ic_my_library_books_black_24dp, mDrawerList));
			mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
			mDrawerList.addView(makeNavDrawerDivider(mDrawerList));
			mDrawerList.addView(makeNavDrawerItem(R.string.title_plays, R.drawable.ic_event_note_black_24dp, mDrawerList));
			mDrawerList.addView(makeNavDrawerItem(R.string.title_players, R.drawable.ic_people_black_24dp, mDrawerList));
			mDrawerList.addView(makeNavDrawerItem(R.string.title_locations, R.drawable.ic_place_black_24dp, mDrawerList));
			mDrawerList.addView(makeNavDrawerItem(R.string.title_play_stats, R.drawable.ic_poll_black_24dp, mDrawerList));
			mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
			mDrawerList.addView(makeNavDrawerDivider(mDrawerList));
			mDrawerList.addView(makeNavDrawerItem(R.string.title_buddies, R.drawable.ic_person_black_24dp, mDrawerList));
			mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
		}

		mDrawerList.addView(makeNavDrawerSeparator(R.string.title_browse, mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_hotness, R.drawable.ic_whatshot_black_24dp, mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_geeklists, R.drawable.ic_list_black_24dp, mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_forums, R.drawable.ic_forum_black_24dp, mDrawerList));
		mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
		mDrawerList.addView(makeNavDrawerDivider(mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_settings, R.drawable.ic_settings_black_24dp, mDrawerList));
	}

	private void selectItem(int titleResId) {
		if (titleResId != getDrawerResId()) {
			Intent intent = null;
			switch (titleResId) {
				case R.string.title_collection:
					intent = new Intent(this, CollectionActivity.class);
					break;
				case R.string.title_hotness:
					intent = new Intent(this, HotnessActivity.class);
					break;
				case R.string.title_geeklists:
					intent = new Intent(this, GeekListsActivity.class);
					break;
				case R.string.title_plays:
					intent = new Intent(this, PlaysActivity.class);
					break;
				case R.string.title_players:
					intent = new Intent(this, PlayersActivity.class);
					break;
				case R.string.title_locations:
					intent = new Intent(this, LocationsActivity.class);
					break;
				case R.string.title_play_stats:
					intent = new Intent(this, PlayStatsActivity.class);
					break;
				case R.string.title_buddies:
					intent = new Intent(this, BuddiesActivity.class);
					break;
				case R.string.title_forums:
					intent = new Intent(this, ForumsActivity.class);
					break;
				case R.string.title_signin:
					startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_SIGNIN);
					break;
				case R.string.title_settings:
					startActivity(new Intent(this, Preferences.class));
					break;
			}
			if (intent != null) {
				startActivity(intent);
				finish();
			}
		}
		mDrawerLayout.closeDrawer(mDrawerListContainer);
	}

	private View makeNavDrawerSpacer(ViewGroup container) {
		return getLayoutInflater().inflate(R.layout.row_spacer, container, false);
	}

	private View makeNavDrawerDivider(ViewGroup container) {
		return getLayoutInflater().inflate(R.layout.row_divider, container, false);
	}

	private View makeNavDrawerSeparator(final int titleId, ViewGroup container) {
		int layoutToInflate = R.layout.row_header_drawer;
		View view = getLayoutInflater().inflate(layoutToInflate, container, false);
		if (titleId != 0) {
			TextView titleView = (TextView) view.findViewById(android.R.id.title);
			titleView.setText(getString(titleId));
		}
		return view;
	}

	private View makeNavDrawerItem(final int titleId, int iconId, ViewGroup container) {
		View view = getLayoutInflater().inflate(R.layout.row_drawer, container, false);

		TextView titleView = (TextView) view.findViewById(android.R.id.title);
		ImageView iconView = (ImageView) view.findViewById(android.R.id.icon);

		titleView.setText(titleId);
		if (iconId != 0) {
			iconView.setImageResource(iconId);
			iconView.setVisibility(View.VISIBLE);
		} else {
			iconView.setVisibility(View.GONE);
		}
		if (titleId == getDrawerResId()) {
			view.setBackgroundResource(R.color.primary);
			titleView.setTextColor(getResources().getColor(R.color.primary_dark));
			iconView.setColorFilter(getResources().getColor(R.color.primary_dark));
		} else {
			iconView.setColorFilter(getResources().getColor(R.color.navdrawer_icon_tint));
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
