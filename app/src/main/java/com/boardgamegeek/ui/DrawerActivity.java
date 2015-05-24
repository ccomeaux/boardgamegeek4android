package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.pref.SettingsActivity;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HttpUtils;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;

public abstract class DrawerActivity extends BaseActivity {
    private static final int REQUEST_SIGNIN = 1;
    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.drawer_container) View mDrawerListContainer;
    @InjectView(R.id.left_drawer) LinearLayout mDrawerList;
    @InjectView(R.id.toolbar) Toolbar mToolbar;

    protected int getDrawerResId() {
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);
        ButterKnife.inject(this);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));
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
        mDrawerList.addView(makeNavDrawerBuffer(mDrawerList));
        if (!Authenticator.isSignedIn(DrawerActivity.this)) {
            mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
            mDrawerList.addView(makeNavDrawerItem(R.string.title_signin, R.drawable.ic_account_circle_black_24dp, mDrawerList));
        } else {
            View view = makeNavDrawerHeader(mDrawerList);
            if (view != null) {
                mDrawerList.addView(view);
            }
            mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
            mDrawerList.addView(makeNavDrawerItem(R.string.title_collection, R.drawable.ic_my_library_books_black_24dp, mDrawerList));
            mDrawerList.addView(makeNavDrawerSpacerWithDivider(mDrawerList));

            mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
            mDrawerList.addView(makeNavDrawerItem(R.string.title_plays, R.drawable.ic_event_note_black_24dp, mDrawerList));
            mDrawerList.addView(makeNavDrawerItem(R.string.title_players, R.drawable.ic_people_black_24dp, mDrawerList));
            mDrawerList.addView(makeNavDrawerItem(R.string.title_locations, R.drawable.ic_place_black_24dp, mDrawerList));
            if (!TextUtils.isEmpty(AccountUtils.getUsername(this))) {
                mDrawerList.addView(makeNavDrawerItem(R.string.title_colors, R.drawable.ic_action_colors_light, mDrawerList));
            }
            mDrawerList.addView(makeNavDrawerItem(R.string.title_play_stats, R.drawable.ic_action_pie_chart, mDrawerList));
            mDrawerList.addView(makeNavDrawerSpacerWithDivider(mDrawerList));

            mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
            mDrawerList.addView(makeNavDrawerItem(R.string.title_buddies, R.drawable.ic_person_black_24dp, mDrawerList));
        }
        mDrawerList.addView(makeNavDrawerSpacerWithDivider(mDrawerList));

        mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
        mDrawerList.addView(makeNavDrawerItem(R.string.title_hotness, R.drawable.ic_whatshot_black_24dp, mDrawerList));
        mDrawerList.addView(makeNavDrawerItem(R.string.title_geeklists, R.drawable.ic_list_black_24dp, mDrawerList));
        mDrawerList.addView(makeNavDrawerItem(R.string.title_forums, R.drawable.ic_action_forum, mDrawerList));
        mDrawerList.addView(makeNavDrawerSpacerWithDivider(mDrawerList));

        mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
        mDrawerList.addView(makeNavDrawerItem(R.string.title_settings, R.drawable.ic_settings_black_24dp, mDrawerList));
        mDrawerList.addView(makeNavDrawerSpacer(mDrawerList));
    }

    private void selectItem(int titleResId) {
        if (titleResId != getDrawerResId()) {
            Intent intent = null;
            boolean shouldFinish = true;
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
                case R.string.title_colors:
                    intent = new Intent(this, BuddyColorsActivity.class);
                    intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, AccountUtils.getUsername(this));
                    shouldFinish = false;
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
        mDrawerLayout.closeDrawer(mDrawerListContainer);
    }

    private View makeNavDrawerHeader(ViewGroup container) {
        final View view = getLayoutInflater().inflate(R.layout.row_header_drawer, container, false);

        String fullName = AccountUtils.getFullName(this);
        String username = AccountUtils.getUsername(this);
        if (TextUtils.isEmpty(fullName)) {
            if (TextUtils.isEmpty(username)) {
                if (Authenticator.isSignedIn(this)) {
                    UpdateService.start(this, UpdateService.SYNC_TYPE_BUDDY_SELF, null);
                }
                return null;
            } else {
                ((TextView) view.findViewById(R.id.account_info_primary)).setText(username);
            }
        } else {
            ((TextView) view.findViewById(R.id.account_info_primary)).setText(fullName);
            ((TextView) view.findViewById(R.id.account_info_secondary)).setText(username);
        }

        String avatarUrl = AccountUtils.getAvatarUrl(this);
        final ImageView imageView = (ImageView) view.findViewById(R.id.account_image);
        if (TextUtils.isEmpty(avatarUrl)) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            Picasso.with(this)
                    .load(HttpUtils.ensureScheme(avatarUrl))
                    .placeholder(R.drawable.person_image_empty)
                    .error(R.drawable.person_image_empty)
                    .resizeDimen(R.dimen.drawer_header_image_size, R.dimen.drawer_header_image_size)
                    .centerCrop()
                    .into(imageView);
        }

        return view;
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
            view.setBackgroundResource(R.color.navdrawer_selected_row);
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
