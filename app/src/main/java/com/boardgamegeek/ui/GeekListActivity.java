package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.model.GeekListResponse;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.ui.model.GeekList;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.util.ArrayList;
import java.util.List;

public class GeekListActivity extends TabActivity implements LoaderManager.LoaderCallbacks<SafeResponse<GeekListResponse>> {
	private static final int LOADER_ID = 1;
	private int geekListId;
	private String geekListTitle;
	private GeekList geekList;
	private List<GeekListItem> geekListItems;
	private String errorMessage;
	private String descriptionFragmentTag;
	private String itemsFragmentTag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		safelySetTitle(geekListTitle);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekList")
				.putContentId(String.valueOf(geekListId))
				.putContentName(geekListTitle));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getSupportLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "geeklist", geekListId);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGeekList(this, geekListId, geekListTitle);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void setUpViewPager() {
		GeekListPagerAdapter adapter = new GeekListPagerAdapter(getSupportFragmentManager(), this);
		viewPager.setAdapter(adapter);
		adapter.addTab(GeekListDescriptionFragment.class, UIUtils.intentToFragmentArguments(getIntent()), R.string.title_description, new ItemInstantiatedCallback() {
			@Override
			public void itemInstantiated(String tag) {
				descriptionFragmentTag = tag;
				setDescription();
			}
		});
		adapter.addTab(GeekListItemsFragment.class, UIUtils.intentToFragmentArguments(getIntent()), R.string.title_items, new ItemInstantiatedCallback() {
			@Override
			public void itemInstantiated(String tag) {
				itemsFragmentTag = tag;
				setItems();
			}
		});
	}

	private interface ItemInstantiatedCallback {
		void itemInstantiated(String tag);
	}

	private final static class GeekListPagerAdapter extends FragmentPagerAdapter {
		static final class TabInfo {
			private final Class<?> fragmentClass;
			private final Bundle args;
			@StringRes private final int titleRes;
			private final ItemInstantiatedCallback callback;

			TabInfo(Class<?> fragmentClass, Bundle args, int titleRes, ItemInstantiatedCallback callback) {
				this.fragmentClass = fragmentClass;
				this.args = args;
				this.titleRes = titleRes;
				this.callback = callback;
			}
		}

		private final Context context;
		private final ArrayList<TabInfo> tabs = new ArrayList<>();

		public GeekListPagerAdapter(FragmentManager fragmentManager, Context context) {
			super(fragmentManager);
			this.context = context;
			tabs.clear();
		}

		public void addTab(Class<?> fragmentClass, Bundle args, @StringRes int titleRes, ItemInstantiatedCallback callback) {
			tabs.add(new TabInfo(fragmentClass, args, titleRes, callback));
			notifyDataSetChanged();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo == null) return "";
			return context.getString(tabInfo.titleRes);
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo == null) return null;
			return Fragment.instantiate(context, tabInfo.fragmentClass.getName(), tabInfo.args);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
			TabInfo tabInfo = tabs.get(position);
			if (tabInfo != null) {
				tabInfo.callback.itemInstantiated(createdFragment.getTag());
			}
			return createdFragment;
		}

		@Override
		public int getCount() {
			return tabs.size();
		}
	}

	@Override
	public Loader<SafeResponse<GeekListResponse>> onCreateLoader(int id, Bundle data) {
		return new GeekListLoader(this, geekListId);
	}

	@Override
	public void onLoadFinished(Loader<SafeResponse<GeekListResponse>> loader, SafeResponse<GeekListResponse> data) {
		GeekListResponse body = data.getBody();
		if (body == null) {
			errorMessage = getString(R.string.empty_geeklist);
		} else if (data.hasParseError()) {
			errorMessage = getString(R.string.parse_error);
		} else if (data.hasError()) {
			errorMessage = data.getErrorMessage();
		} else {
			errorMessage = "";
		}

		if (body == null) return;

		geekList = GeekList.builder()
			.setId(body.id)
			.setTitle(TextUtils.isEmpty(body.title) ? "" : body.title.trim())
			.setUsername(body.username)
			.setDescription(body.description)
			.setNumberOfItems(StringUtils.parseInt(body.numitems))
			.setNumberOfThumbs(StringUtils.parseInt(body.thumbs))
			.setPostTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.postdate, GeekListResponse.FORMAT))
			.setEditTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.editdate, GeekListResponse.FORMAT))
			.build();
		geekListItems = body.getItems();

		setDescription();
		setItems();
	}

	private void setDescription() {
		if (viewPager == null) return;
		GeekListPagerAdapter adapter = (GeekListPagerAdapter) viewPager.getAdapter();
		if (adapter == null) return;

		GeekListDescriptionFragment descriptionFragment = (GeekListDescriptionFragment) getSupportFragmentManager().findFragmentByTag(descriptionFragmentTag);
		if (descriptionFragment != null) descriptionFragment.setData(geekList);
	}

	private void setItems() {
		if (geekList == null || geekListItems == null) return;
		if (viewPager == null) return;
		GeekListPagerAdapter adapter = (GeekListPagerAdapter) viewPager.getAdapter();
		if (adapter == null) return;

		GeekListItemsFragment itemsFragment = (GeekListItemsFragment) getSupportFragmentManager().findFragmentByTag(itemsFragmentTag);
		if (itemsFragment != null) {
			if (!TextUtils.isEmpty(errorMessage)) {
				itemsFragment.setError(errorMessage);
			} else if (geekList.numberOfItems() == 0 || geekListItems.size() == 0) {
				itemsFragment.setError();
			} else {
				itemsFragment.setData(geekList, geekListItems);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<SafeResponse<GeekListResponse>> loader) {
	}

	private static class GeekListLoader extends BggLoader<SafeResponse<GeekListResponse>> {
		private final BggService service;
		private final int geekListId;

		public GeekListLoader(Context context, int geekListId) {
			super(context);
			service = Adapter.createForXml();
			this.geekListId = geekListId;
		}

		@Override
		public SafeResponse<GeekListResponse> loadInBackground() {
			return new SafeResponse<>(service.geekList(geekListId, 1));
		}
	}
}
