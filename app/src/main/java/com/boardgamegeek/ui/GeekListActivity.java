package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.RefreshableResource;
import com.boardgamegeek.entities.Status;
import com.boardgamegeek.io.model.GeekListResponse;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.model.GeekList;
import com.boardgamegeek.ui.viewmodel.GeekListViewModel;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class GeekListActivity extends TabActivity {
	private static final String KEY_ID = "GEEK_LIST_ID";
	private static final String KEY_TITLE = "GEEK_LIST_TITLE";
	private int geekListId;
	private String geekListTitle;
	private GeekList geekList;
	private List<GeekListItem> geekListItems;
	private String errorMessage;
	private String descriptionFragmentTag;
	private String itemsFragmentTag;
	private GeekListPagerAdapter adapter;
	private GeekListViewModel viewModel;

	public static void start(Context context, int id, String title) {
		Intent starter = createIntent(context, id, title);
		context.startActivity(starter);
	}

	public static void startUp(Context context, int id, String title) {
		Intent starter = createIntent(context, id, title);
		starter.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@NonNull
	private static Intent createIntent(Context context, int id, String title) {
		Intent starter = new Intent(context, GeekListActivity.class);
		starter.putExtra(KEY_ID, id);
		starter.putExtra(KEY_TITLE, title);
		return starter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		viewModel = new ViewModelProvider(this).get(GeekListViewModel.class);

		final Intent intent = getIntent();
		geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID);
		geekListTitle = intent.getStringExtra(KEY_TITLE);
		safelySetTitle(geekListTitle);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekList")
				.putContentId(String.valueOf(geekListId))
				.putContentName(geekListTitle));
		}

		viewModel.setId(geekListId);
		viewModel.getGeekList().observe(this, resource -> {
			if (resource.getStatus() == Status.REFRESHING) {
				errorMessage = "";
			} else if (resource.getStatus() == Status.ERROR) {
				errorMessage = resource.getMessage();
			} else if (resource.getStatus() == Status.SUCCESS) {
				errorMessage = "";
				loadBody(resource.getData());
			}
		});
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_view:
				//noinspection SpellCheckingInspection
				ActivityUtils.linkToBgg(this, "geeklist", geekListId);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGeekList(this, geekListId, geekListTitle);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@NotNull
	@Override
	protected FragmentPagerAdapter createAdapter() {
		adapter = new GeekListPagerAdapter(getSupportFragmentManager(), this);
		adapter.addTab(GeekListDescriptionFragment.newInstance(), R.string.title_description, tag -> {
			descriptionFragmentTag = tag;
			setDescription();
		});
		adapter.addTab(GeekListItemsFragment.newInstance(), R.string.title_items, tag -> {
			itemsFragmentTag = tag;
			setItems();
		});
		return adapter;
	}

	private interface ItemInstantiatedCallback {
		void itemInstantiated(String tag);
	}

	private final static class GeekListPagerAdapter extends FragmentPagerAdapter {
		static final class TabInfo {
			private final Fragment fragment;
			@StringRes private final int titleRes;
			private final ItemInstantiatedCallback callback;

			TabInfo(Fragment fragment, int titleRes, ItemInstantiatedCallback callback) {
				this.fragment = fragment;
				this.titleRes = titleRes;
				this.callback = callback;
			}
		}

		private final Context context;
		private final ArrayList<TabInfo> tabs = new ArrayList<>();

		public GeekListPagerAdapter(FragmentManager fragmentManager, Context context) {
			super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.context = context;
			tabs.clear();
		}

		public void addTab(Fragment fragment, @StringRes int titleRes, ItemInstantiatedCallback callback) {
			tabs.add(new TabInfo(fragment, titleRes, callback));
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
			return tabInfo.fragment;
		}

		@Override
		@NonNull
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

	private void loadBody(GeekListResponse body) {
		if (body == null) return;

		geekList = new GeekList(
			body.id,
			TextUtils.isEmpty(body.title) ? "" : body.title.trim(),
			body.username,
			body.description,
			StringUtils.parseInt(body.numitems),
			StringUtils.parseInt(body.thumbs),
			DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.postdate, GeekListResponse.FORMAT),
			DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.editdate, GeekListResponse.FORMAT)
		);
		geekListItems = body.getItems();

		setDescription();
		setItems();
	}

	private void setDescription() {
		if (adapter == null) return;
		GeekListDescriptionFragment descriptionFragment = (GeekListDescriptionFragment) getSupportFragmentManager().findFragmentByTag(descriptionFragmentTag);
		if (descriptionFragment != null) descriptionFragment.setData(geekList);
	}

	private void setItems() {
		if (geekList == null || geekListItems == null) return;
		if (adapter == null) return;

		GeekListItemsFragment itemsFragment = (GeekListItemsFragment) getSupportFragmentManager().findFragmentByTag(itemsFragmentTag);
		if (itemsFragment != null) {
			if (!TextUtils.isEmpty(errorMessage)) {
				itemsFragment.setError(errorMessage);
			} else if (geekList.getNumberOfItems() == 0 || geekListItems.size() == 0) {
				itemsFragment.setError();
			} else {
				itemsFragment.setData(geekList, geekListItems);
			}
		}
	}

}
