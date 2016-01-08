package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.ui.model.BuddyColor;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.RandomUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class BuddyColorsActivity extends BaseActivity {
	private QueryHandler queryHandler;
	private String buddyName;
	private List<BuddyColor> colors;
	private Adapter adapter;

	@SuppressWarnings("unused") @InjectView(R.id.toolbar) Toolbar toolbar;
	@SuppressWarnings("unused") @InjectView(android.R.id.progress) View progressView;
	@SuppressWarnings("unused") @InjectView(android.R.id.empty) View emptyView;
	@SuppressWarnings("unused") @InjectView(android.R.id.list) DragSortListView list;

	@SuppressLint("HandlerLeak")
	private class QueryHandler extends AsyncQueryHandler {
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@DebugLog
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// If the query didn't return a cursor for some reason return
			if (cursor == null) {
				return;
			}

			// If the Activity is finishing, then close the cursor
			if (isFinishing()) {
				cursor.close();
				return;
			}

			colors = new ArrayList<>(cursor.getCount());
			try {
				while (cursor.moveToNext()) {
					colors.add(BuddyColor.fromCursor(cursor));
				}
				adapter = new Adapter();
				list.setAdapter(adapter);
			} finally {
				cursor.close();
			}

			bindUi();
		}
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_buddy_colors);
		ButterKnife.inject(this);

		buddyName = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
		if (TextUtils.isEmpty(buddyName)) {
			Timber.w("Can't launch - missing buddy name.");
			finish();
		}
		setSubtitle(buddyName);

		list.setSelector(android.R.color.transparent);

		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		queryHandler = new QueryHandler(getContentResolver());
		startQuery();
	}

	@DebugLog
	@Override
	protected void onStop() {
		super.onStop();
		if (colors != null) {
			final ContentResolver resolver = getContentResolver();
			if (colors.size() == 0) {
				resolver.delete(PlayerColors.buildUserUri(buddyName), null, null);
			} else {
				ArrayList<ContentProviderOperation> batch = new ArrayList<>();
				for (int i = 0; i < colors.size(); i++) {
					final String color = colors.get(i).getColor();
					final int sortOrder = colors.get(i).getSortOrder();

					Builder builder;
					if (ResolverUtils.rowExists(resolver, PlayerColors.buildUserUri(buddyName, sortOrder))) {
						builder = ContentProviderOperation.newUpdate(PlayerColors.buildUserUri(buddyName, sortOrder));
					} else {
						builder = ContentProviderOperation.newInsert(PlayerColors.buildUserUri(buddyName))
							.withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, sortOrder);
					}
					batch.add(builder.withValue(PlayerColors.PLAYER_COLOR, color).build());
				}
				ResolverUtils.applyBatch(this, batch);
			}
			colors = null; // to force a load from cursor
		}
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.player_colors;
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clear:
				DialogUtils.createConfirmationDialog(this, R.string.are_you_sure_clear_colors, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (colors != null) {
							colors.clear();
						}
						if (adapter != null) {
							adapter.notifyDataSetChanged();
						}
						bindUi();
					}
				}).show();
				break;
			case android.R.id.home:
				ActivityUtils.navigateUpToBuddy(this, buddyName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	private void startQuery() {
		if (colors != null) {
			// we already have the play from the saved instance
			bindUi();
		} else {
			queryHandler.startQuery(0, null, PlayerColors.buildUserUri(buddyName), BuddyColor.PROJECTION, null, null, null);
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@OnClick(R.id.empty_button)
	void onEmptyClick(View view) {
		List<Pair<String, Integer>> colors = ColorUtils.getColorList();
		Random r = RandomUtils.getRandom();
		int order = 1;
		while (colors.size() > 0) {
			int i = r.nextInt(colors.size());
			BuddyColor color = new BuddyColor(colors.remove(i).first, order++);
			this.colors.add(color);
		}
		bindUi();
	}

	@DebugLog
	private void bindUi() {
		if (colors == null) {
			progressView.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
			list.setVisibility(View.GONE);
		} else if (colors.size() == 0) {
			progressView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
			list.setVisibility(View.GONE);
		} else {
			progressView.setVisibility(View.GONE);
			emptyView.setVisibility(View.GONE);
			list.setVisibility(View.VISIBLE);
			adapter.notifyDataSetChanged();
		}
	}

	private class Adapter extends BaseAdapter implements DropListener {
		@DebugLog
		@Override
		public int getCount() {
			return colors == null ? 0 : colors.size();
		}

		@DebugLog
		@Override
		public Object getItem(int position) {
			if (colors == null) {
				return null;
			}
			for (BuddyColor color : colors) {
				if (color.getSortOrder() == (position + 1)) {
					return color;
				}
			}
			return null;
		}

		@DebugLog
		@Override
		public long getItemId(int position) {
			return position;
		}

		@DebugLog
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(BuddyColorsActivity.this).inflate(R.layout.row_player_color, parent, false);
			}
			BuddyColor color = (BuddyColor) getItem(position);
			if (color != null) {
				((TextView) convertView.findViewById(android.R.id.title)).setText(color.getColor());
				ColorUtils.setColorViewValue(convertView.findViewById(R.id.color_view), ColorUtils.parseColor(color.getColor()));
			}
			return convertView;
		}

		@DebugLog
		@Override
		public void drop(int from, int to) {
			if (colors == null) {
				return;
			}
			final BuddyColor remove = colors.remove(from);
			colors.add(to, remove);
			for (int i = 0; i < colors.size(); i++) {
				final BuddyColor color = colors.get(i);
				color.setSortOrder(i + 1);
			}

			notifyDataSetChanged();
		}
	}
}
