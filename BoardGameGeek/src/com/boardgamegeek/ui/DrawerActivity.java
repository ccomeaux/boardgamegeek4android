package com.boardgamegeek.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.pref.Preferences;

public abstract class DrawerActivity extends BaseActivity {
	private static final int REQUEST_SIGNIN = 1;
	protected DrawerLayout mDrawerLayout;
	private View mDrawerListContainer;
	private LinearLayout mDrawerList;

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

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (mDrawerLayout != null) {
			mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		}
		mDrawerListContainer = findViewById(R.id.drawer_container);
		mDrawerList = (LinearLayout) findViewById(R.id.left_drawer);

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
			mDrawerList.addView(makeNavDrawerItem(R.string.title_signin, mDrawerList));
		} else {
			mDrawerList.addView(makeNavDrawerItem(R.string.title_collection, mDrawerList));
			mDrawerList.addView(makeNavDrawerItem(R.string.title_plays, mDrawerList));
			mDrawerList.addView(makeNavDrawerItemIndent(R.string.title_players, mDrawerList));
			mDrawerList.addView(makeNavDrawerItemIndent(R.string.title_locations, mDrawerList));
			mDrawerList.addView(makeNavDrawerItem(R.string.title_buddies, mDrawerList));
            mDrawerList.addView(makeNavDrawerItem(R.string.title_subscriptions, mDrawerList));
		}

		mDrawerList.addView(makeNavDrawerSeparator(R.string.title_browse, mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_hotness, mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_geeklists, mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_forums, mDrawerList));

		mDrawerList.addView(makeNavDrawerSeparator(0, mDrawerList));
		mDrawerList.addView(makeNavDrawerItem(R.string.title_settings, mDrawerList));
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
                case R.string.title_subscriptions:
                    intent = new Intent(this, SubscriptionsActivity.class);
                    break;
			}
			if (intent != null) {
				startActivity(intent);
				finish();
			}
		}
		mDrawerLayout.closeDrawer(mDrawerListContainer);
	}

	private View makeNavDrawerSeparator(final int titleId, ViewGroup container) {
		int layoutToInflate = R.layout.row_header;
		View view = getLayoutInflater().inflate(layoutToInflate, container, false);
		if (titleId != 0) {
			TextView titleView = (TextView) view.findViewById(android.R.id.title);
			titleView.setText(getString(titleId));
		}
		return view;
	}

	private View makeNavDrawerItem(final int titleId, ViewGroup container) {
		return makeNavDrawerItem(R.layout.row_drawer, titleId, container);
	}

	private View makeNavDrawerItemIndent(final int titleId, ViewGroup container) {
		return makeNavDrawerItem(R.layout.row_drawer_2, titleId, container);
	}

	private View makeNavDrawerItem(int layoutId, final int titleId, ViewGroup container) {
		View view = getLayoutInflater().inflate(layoutId, container, false);

		TextView titleView = (TextView) view.findViewById(android.R.id.title);
		if (titleId == getDrawerResId()) {
			String text = getString(titleId);
			SpannableString ss = new SpannableString(text);
			ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			titleView.setText(ss);
		} else {
			titleView.setText(titleId);
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
