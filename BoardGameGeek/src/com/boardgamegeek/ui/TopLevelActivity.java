package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Pair;
import android.view.ActionProvider;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

public abstract class TopLevelActivity extends BaseActivity {
	private static final String EXTRA_NAVIGATION_POSITION = "EXTRA_NAVIGATION_POSITION";
	private CharSequence mTitle;
	private CharSequence mDrawerTitle;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private int mPosition;
	private NavigationAdapter mAdapter;

	protected abstract int getContentViewId();

	protected boolean isTitleHidden() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getContentViewId());

		mPosition = getIntent().getIntExtra(EXTRA_NAVIGATION_POSITION, -1);

		mDrawerTitle = getString(R.string.app_name);
		mTitle = getTitle();

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mAdapter = new NavigationAdapter();
		mDrawerList.setAdapter(mAdapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
			R.string.drawer_close) {
			// TODO: finish and start CAB with the drawer open/close
			public void onDrawerClosed(View view) {
				final ActionBar actionBar = getSupportActionBar();
				actionBar.setTitle(mTitle);
				if (isTitleHidden()) {
					actionBar.setDisplayShowTitleEnabled(false);
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
				}
				supportInvalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				final ActionBar actionBar = getSupportActionBar();
				if (isTitleHidden()) {
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
					actionBar.setDisplayShowTitleEnabled(true);
				}
				actionBar.setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// TODO open the drawer upon launch until user opens it themselves
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	public boolean isDrawerOpen() {
		return mDrawerLayout.isDrawerOpen(mDrawerList);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(getMenuItem(item))) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mAdapter.init();
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {
		if (position != mPosition) {
			Intent intent = null;
			switch (mAdapter.getItem(position).first) {
				case R.string.title_collection:
					intent = new Intent(Intent.ACTION_VIEW, Collection.CONTENT_URI);
					break;
				case R.string.title_hotness:
					intent = new Intent(this, HotnessActivity.class);
					break;
				case R.string.title_plays:
					intent = new Intent(Intent.ACTION_VIEW, Plays.CONTENT_URI);
					break;
				case R.string.title_buddies:
					intent = new Intent(Intent.ACTION_VIEW, Buddies.CONTENT_URI);
					break;
				case R.string.title_forums:
					intent = new Intent(this, ForumsActivity.class);
					break;
				case R.string.title_signin:
					startActivityForResult(new Intent(this, LoginActivity.class), 0);
					break;
			}
			if (intent != null) {
				intent.putExtra(EXTRA_NAVIGATION_POSITION, position);
				startActivity(intent);
			}
		}
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	private class NavigationAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private List<Pair<Integer, Integer>> mTitles;

		public NavigationAdapter() {
			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			init();
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			init();
		}

		public void init() {
			mTitles = new ArrayList<Pair<Integer, Integer>>();
			if (!Authenticator.isSignedIn(TopLevelActivity.this)) {
				mTitles.add(new Pair<Integer, Integer>(R.string.title_signin, R.drawable.home_btn_signin));
			} else {
				if (hasCollection()) {
					mTitles.add(new Pair<Integer, Integer>(R.string.title_collection, R.drawable.home_btn_collection));
				}
				if (hasPlays()) {
					mTitles.add(new Pair<Integer, Integer>(R.string.title_plays, R.drawable.home_btn_plays));
				}
				if (hasBuddies()) {
					mTitles.add(new Pair<Integer, Integer>(R.string.title_buddies, R.drawable.home_btn_buddies));
				}
			}
			mTitles.add(new Pair<Integer, Integer>(R.string.title_hotness, R.drawable.home_btn_hotness));
			mTitles.add(new Pair<Integer, Integer>(R.string.title_forums, R.drawable.home_btn_forums));
		}

		private boolean hasCollection() {
			String[] statuses = PreferencesUtils.getSyncStatuses(TopLevelActivity.this);
			return (statuses != null && statuses.length > 0);
		}

		private boolean hasPlays() {
			return PreferencesUtils.getSyncPlays(TopLevelActivity.this);
		}

		private boolean hasBuddies() {
			return PreferencesUtils.getSyncBuddies(TopLevelActivity.this);
		}

		@Override
		public int getCount() {
			return mTitles.size();
		}

		@Override
		public Pair<Integer, Integer> getItem(int position) {
			return mTitles.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).first;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.row_drawer, parent, false);
			} else {
				view = convertView;
			}

			Pair<Integer, Integer> item = getItem(position);

			TextView text = (TextView) view.findViewById(android.R.id.text1);
			ImageView image = (ImageView) view.findViewById(android.R.id.icon);
			TextView separator = (TextView) view.findViewById(R.id.separator);

			text.setText(item.first);
			image.setImageResource(item.second);
			if (item.first == R.string.title_hotness) {
				separator.setVisibility(View.VISIBLE);
				separator.setText(R.string.title_browse);
			} else if (position == 0) {
				separator.setVisibility(View.VISIBLE);
				separator.setText(R.string.title_my_geek);
			} else {
				separator.setVisibility(View.GONE);
			}

			if (position == mPosition) {
				// TODO change background to be activate-able
				UIUtils.setActivatedCompat(view, true);
			}

			return view;
		}
	}

	private android.view.MenuItem getMenuItem(final MenuItem item) {
		return new android.view.MenuItem() {
			@Override
			public int getItemId() {
				return item.getItemId();
			}

			public boolean isEnabled() {
				return item.isEnabled();
			}

			@Override
			public boolean collapseActionView() {
				return item.collapseActionView();
			}

			@Override
			public boolean expandActionView() {
				return expandActionView();
			}

			@Override
			public ActionProvider getActionProvider() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public View getActionView() {
				return item.getActionView();
			}

			@Override
			public char getAlphabeticShortcut() {
				return item.getAlphabeticShortcut();
			}

			@Override
			public int getGroupId() {
				return item.getGroupId();
			}

			@Override
			public Drawable getIcon() {
				return item.getIcon();
			}

			@Override
			public Intent getIntent() {
				return item.getIntent();
			}

			@Override
			public ContextMenuInfo getMenuInfo() {
				return item.getMenuInfo();
			}

			@Override
			public char getNumericShortcut() {
				return item.getNumericShortcut();
			}

			@Override
			public int getOrder() {
				return item.getOrder();
			}

			@Override
			public SubMenu getSubMenu() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public CharSequence getTitle() {
				return item.getTitle();
			}

			@Override
			public CharSequence getTitleCondensed() {
				return item.getTitleCondensed();
			}

			@Override
			public boolean hasSubMenu() {
				return item.hasSubMenu();
			}

			@Override
			public boolean isActionViewExpanded() {
				return item.isActionViewExpanded();
			}

			@Override
			public boolean isCheckable() {
				return item.isCheckable();
			}

			@Override
			public boolean isChecked() {
				return item.isChecked();
			}

			@Override
			public boolean isVisible() {
				return item.isVisible();
			}

			@Override
			public android.view.MenuItem setActionProvider(ActionProvider actionProvider) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public android.view.MenuItem setActionView(View view) {
				item.setActionView(view);
				return this;
			}

			@Override
			public android.view.MenuItem setActionView(int resId) {
				item.setActionView(resId);
				return this;
			}

			@Override
			public android.view.MenuItem setAlphabeticShortcut(char alphaChar) {
				item.setAlphabeticShortcut(alphaChar);
				return this;
			}

			@Override
			public android.view.MenuItem setCheckable(boolean checkable) {
				item.setCheckable(checkable);
				return this;
			}

			@Override
			public android.view.MenuItem setChecked(boolean checked) {
				item.setChecked(checked);
				return this;
			}

			@Override
			public android.view.MenuItem setEnabled(boolean enabled) {
				item.setEnabled(enabled);
				return this;
			}

			@Override
			public android.view.MenuItem setIcon(Drawable icon) {
				item.setIcon(icon);
				return this;
			}

			@Override
			public android.view.MenuItem setIcon(int iconRes) {
				item.setIcon(iconRes);
				return this;
			}

			@Override
			public android.view.MenuItem setIntent(Intent intent) {
				item.setIntent(intent);
				return this;
			}

			@Override
			public android.view.MenuItem setNumericShortcut(char numericChar) {
				item.setNumericShortcut(numericChar);
				return this;
			}

			@Override
			public android.view.MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public android.view.MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public android.view.MenuItem setShortcut(char numericChar, char alphaChar) {
				item.setShortcut(numericChar, alphaChar);
				return this;
			}

			@Override
			public void setShowAsAction(int actionEnum) {
				item.setShowAsAction(actionEnum);
			}

			@Override
			public android.view.MenuItem setShowAsActionFlags(int actionEnum) {
				item.setShowAsActionFlags(actionEnum);
				return this;
			}

			@Override
			public android.view.MenuItem setTitle(CharSequence title) {
				item.setTitle(title);
				return this;
			}

			@Override
			public android.view.MenuItem setTitle(int title) {
				item.setTitle(title);
				return this;
			}

			@Override
			public android.view.MenuItem setTitleCondensed(CharSequence title) {
				item.setTitleCondensed(title);
				return this;
			}

			@Override
			public android.view.MenuItem setVisible(boolean visible) {
				item.setVisible(visible);
				return this;
			}
		};
	}
}
