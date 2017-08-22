package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.ui.PlayerColorsActivity.RecyclerViewAdapter.ColorViewHolder;
import com.boardgamegeek.ui.dialog.ColorPickerDialogFragment;
import com.boardgamegeek.ui.model.PlayerColor;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.RandomUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.fabric.PlayerColorsManipulationEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class PlayerColorsActivity extends BaseActivity {
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";

	private QueryHandler queryHandler;
	private String buddyName;
	private String playerName;
	private List<PlayerColor> colors;
	private RecyclerViewAdapter adapter;

	@BindView(R.id.toolbar) Toolbar toolbar;
	@BindView(android.R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(android.R.id.empty) View emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;
	@BindView(R.id.coordinator) CoordinatorLayout coordinator;

	private final Paint swipePaint = new Paint();
	private Bitmap deleteIcon;
	@BindDimen(R.dimen.material_margin_horizontal) float horizontalPadding;
	private ItemTouchHelper itemTouchHelper;

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
					colors.add(PlayerColor.fromCursor(cursor));
				}
				adapter = new RecyclerViewAdapter(PlayerColorsActivity.this);
				recyclerView.setAdapter(adapter);
			} finally {
				cursor.close();
			}

			showData();
			adapter.notifyDataSetChanged();
		}
	}

	public static void start(Context context, String buddyName, String playerName) {
		Intent starter = new Intent(context, PlayerColorsActivity.class);
		starter.putExtra(KEY_BUDDY_NAME, buddyName);
		starter.putExtra(KEY_PLAYER_NAME, playerName);
		context.startActivity(starter);
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_player_colors);
		ButterKnife.bind(this);

		buddyName = getIntent().getStringExtra(KEY_BUDDY_NAME);
		playerName = getIntent().getStringExtra(KEY_PLAYER_NAME);
		if (TextUtils.isEmpty(buddyName) && TextUtils.isEmpty(playerName)) {
			Timber.w("Can't launch - missing both buddy name and username.");
			finish();
		}
		setSubtitle(TextUtils.isEmpty(buddyName) ? playerName : buddyName);

		setUpRecyclerView();

		if (toolbar != null) {
			setSupportActionBar(toolbar);
		}
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		queryHandler = new QueryHandler(getContentResolver());
		startQuery();

		if (savedInstanceState == null) {
			final ContentViewEvent event = new ContentViewEvent()
				.putContentType("PlayerColors");
			if (!TextUtils.isEmpty(buddyName)) event.putContentId(buddyName);
			if (!TextUtils.isEmpty(playerName)) event.putContentName(playerName);
			Answers.getInstance().logContentView(event);
		}
	}

	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
		recyclerView.setHasFixedSize(true);

		swipePaint.setColor(ContextCompat.getColor(this, R.color.medium_blue));
		deleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete_white);

		itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
				if (colors == null) return false;

				int fromPosition = viewHolder.getAdapterPosition();
				int toPosition = target.getAdapterPosition();

				PlayerColor colorMoving = adapter.getItem(fromPosition);
				if (colorMoving == null) return false;

				if (fromPosition < toPosition) {
					// dragging down
					for (PlayerColor color : colors) {
						if (color.getSortOrder() > fromPosition + 1 &&
							color.getSortOrder() <= toPosition + 1) {
							Timber.d("Moving %s up!", color.getColor());
							color.setSortOrder(color.getSortOrder() - 1);
						}
					}
				} else {
					// dragging up
					for (PlayerColor color : colors) {
						if (color.getSortOrder() >= toPosition + 1 &&
							color.getSortOrder() < fromPosition + 1) {
							Timber.d("Moving %s down!", color.getColor());
							color.setSortOrder(color.getSortOrder() + 1);
						}
					}
				}

				for (PlayerColor color : colors) {
					if (color.getColor().equals(colorMoving.getColor())) {
						Timber.d("Moving %s to %d!", color.getColor(), toPosition + 1);
						color.setSortOrder(toPosition + 1);
						break;
					}
				}
				adapter.notifyItemMoved(fromPosition, toPosition);
				return true;
			}

			@Override
			public void onSelectedChanged(ViewHolder viewHolder, int actionState) {
				ColorViewHolder colorViewHolder = (ColorViewHolder) viewHolder;
				if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
					colorViewHolder.onItemDragging();
				} else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
					colorViewHolder.onItemSwiping();
				}
				super.onSelectedChanged(viewHolder, actionState);
			}

			@Override
			public void clearView(RecyclerView recyclerView, ViewHolder viewHolder) {
				ColorViewHolder colorViewHolder = (ColorViewHolder) viewHolder;
				colorViewHolder.onItemClear();
				super.clearView(recyclerView, viewHolder);
			}

			@Override
			public void onSwiped(ViewHolder viewHolder, int swipeDir) {
				final PlayerColor color = adapter.getItem(viewHolder.getAdapterPosition());
				if (color == null) return;
				Snackbar.make(coordinator, getString(R.string.removed_suffix, color.getColor()), Snackbar.LENGTH_LONG)
					.setAction(R.string.undo, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							adapter.add(color);
							PlayerColorsManipulationEvent.log("UndoDelete", color.getColor());
						}
					})
					.setActionTextColor(ContextCompat.getColor(PlayerColorsActivity.this, R.color.light_blue))
					.show();
				adapter.remove(color);
				PlayerColorsManipulationEvent.log("Delete", color.getColor());
			}

			@Override
			public void onChildDraw(Canvas c, RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
				if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
					View itemView = viewHolder.itemView;

					// fade and slide item
					float width = (float) itemView.getWidth();
					float alpha = 1.0f - Math.abs(dX) / width;
					itemView.setAlpha(alpha);
					itemView.setTranslationX(dX);

					// show background with delete icon
					float verticalPadding = (itemView.getHeight() - deleteIcon.getHeight()) / 2;
					RectF background;
					Rect iconSrc;
					RectF iconDst;

					if (dX > 0) {
						background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
						iconSrc = new Rect(0, 0, (int) (dX - itemView.getLeft() - horizontalPadding), deleteIcon.getHeight());
						iconDst = new RectF((float) itemView.getLeft() + horizontalPadding, (float) itemView.getTop() + verticalPadding, Math.min(itemView.getLeft() + horizontalPadding + deleteIcon.getWidth(), dX), (float) itemView.getBottom() - verticalPadding);
					} else {
						background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
						iconSrc = new Rect(Math.max(deleteIcon.getWidth() + (int) horizontalPadding + (int) dX, 0), 0, deleteIcon.getWidth(), deleteIcon.getHeight());
						iconDst = new RectF(Math.max((float) itemView.getRight() + dX, (float) itemView.getRight() - horizontalPadding - deleteIcon.getWidth()), (float) itemView.getTop() + verticalPadding, (float) itemView.getRight() - horizontalPadding, (float) itemView.getBottom() - verticalPadding);
					}
					c.drawRect(background, swipePaint);
					c.drawBitmap(deleteIcon, iconSrc, iconDst, swipePaint);
				}
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}
		});
		itemTouchHelper.attachToRecyclerView(recyclerView);
	}

	@DebugLog
	@Override
	protected void onStop() {
		super.onStop();
		if (colors != null) {
			final ContentResolver resolver = getContentResolver();
			deleteColors(resolver);
			if (colors.size() > 0) {
				ArrayList<ContentProviderOperation> batch = new ArrayList<>();
				for (int i = 0; i < colors.size(); i++) {
					final String color = colors.get(i).getColor();
					final int sortOrder = colors.get(i).getSortOrder();

					if (!TextUtils.isEmpty(color)) {
						Builder builder = createCpoBuilder(resolver, sortOrder);
						batch.add(builder.withValue(PlayerColors.PLAYER_COLOR, color).build());
					}
				}
				ResolverUtils.applyBatch(this, batch);
			}
			colors = null; // to force a load from cursor
		}
	}

	@DebugLog
	private void deleteColors(ContentResolver resolver) {
		if (TextUtils.isEmpty(buddyName)) {
			resolver.delete(PlayerColors.buildPlayerUri(playerName), null, null);
		} else {
			resolver.delete(PlayerColors.buildUserUri(buddyName), null, null);
		}
	}

	@DebugLog
	private Builder createCpoBuilder(ContentResolver resolver, int sortOrder) {
		Builder builder;
		if (TextUtils.isEmpty(buddyName)) {
			if (ResolverUtils.rowExists(resolver, PlayerColors.buildPlayerUri(playerName, sortOrder))) {
				builder = ContentProviderOperation.newUpdate(PlayerColors.buildPlayerUri(playerName, sortOrder));
			} else {
				builder = ContentProviderOperation.newInsert(PlayerColors.buildPlayerUri(playerName))
					.withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, sortOrder);
			}
		} else {
			if (ResolverUtils.rowExists(resolver, PlayerColors.buildUserUri(buddyName, sortOrder))) {
				builder = ContentProviderOperation.newUpdate(PlayerColors.buildUserUri(buddyName, sortOrder));
			} else {
				builder = ContentProviderOperation.newInsert(PlayerColors.buildUserUri(buddyName))
					.withValue(PlayerColors.PLAYER_COLOR_SORT_ORDER, sortOrder);
			}
		}
		return builder;
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
				DialogUtils.createThemedBuilder(this)
					.setMessage(R.string.are_you_sure_clear_colors)
					.setPositiveButton(R.string.clear, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							PlayerColorsManipulationEvent.log("Clear");
							if (colors != null) {
								colors.clear();
								if (adapter != null) adapter.notifyDataSetChanged();
								showData();
							}
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.setCancelable(true)
					.show();
				break;
			case android.R.id.home:
				BuddyActivity.startUp(this, buddyName, playerName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	private void startQuery() {
		if (colors != null) {
			// we already have the play from the saved instance
			showData();
			adapter.notifyDataSetChanged();
		} else {
			queryHandler.startQuery(0, null,
				TextUtils.isEmpty(buddyName) ? PlayerColors.buildPlayerUri(playerName) : PlayerColors.buildUserUri(buddyName),
				PlayerColor.PROJECTION, null, null, null);
		}
	}

	private void showData() {
		progressView.hide();
		if (colors == null || colors.size() == 0) {
			AnimationUtils.fadeIn(emptyView);
			AnimationUtils.fadeOut(recyclerView);
		} else {
			AnimationUtils.fadeOut(emptyView);
			AnimationUtils.fadeIn(recyclerView);
		}
	}

	@DebugLog
	@OnClick(R.id.empty_button)
	void onEmptyClick() {
		List<Pair<String, Integer>> colors = ColorUtils.getLimitedColorList();
		if (this.colors == null) {
			this.colors = new ArrayList<>(colors.size());
		}
		Random r = RandomUtils.getRandom();
		int order = 1;
		while (colors.size() > 0) {
			int i = r.nextInt(colors.size());
			PlayerColor color = new PlayerColor(colors.remove(i).first, order++);
			this.colors.add(color);
		}
		PlayerColorsManipulationEvent.log("Generate");
		showData();
		adapter.notifyItemRangeInserted(0, colors.size());
	}

	@OnClick(R.id.fab)
	void onFabClick() {
		if (colors == null) {
			return;
		}

		ArrayList<String> usedColors = new ArrayList<>(colors.size());
		for (PlayerColor color : colors) {
			usedColors.add(color.getColor());
		}

		ColorPickerDialogFragment fragment = ColorPickerDialogFragment.newInstance(R.string.title_add_color,
			ColorUtils.getColorList(), null, null, null, usedColors, 4);
		fragment.setOnColorSelectedListener(new ColorPickerDialogFragment.OnColorSelectedListener() {
			@Override
			public void onColorSelected(String description, int color) {
				if (colors != null) {
					PlayerColorsManipulationEvent.log("Add", description);
					colors.add(new PlayerColor(description, colors.size() + 1));
					showData();
					adapter.notifyItemInserted(colors.size());
				}
			}
		});

		fragment.show(getSupportFragmentManager(), "color_picker");
	}

	public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ColorViewHolder> {
		private final LayoutInflater inflater;

		public RecyclerViewAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		@Override
		public ColorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new ColorViewHolder(inflater.inflate(R.layout.row_player_color, parent, false));
		}

		@Override
		public void onBindViewHolder(ColorViewHolder holder, int position) {
			PlayerColor color = getItem(position);
			if (color == null) return;
			holder.bind(color);
		}

		@Override
		public int getItemCount() {
			return colors == null ? 0 : colors.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public PlayerColor getItem(int position) {
			if (colors == null) return null;
			for (PlayerColor color : colors) {
				if (color.getSortOrder() == position + 1) {
					return color;
				}
			}
			return null;
		}

		public void add(PlayerColor color) {
			if (colors == null) return;
			for (PlayerColor c : colors) {
				if (c.getSortOrder() >= color.getSortOrder()) {
					Timber.d("Moving %s down!", c.getColor());
					c.setSortOrder(c.getSortOrder() + 1);
				}
			}
			Timber.d("Re-adding %s!", color);
			colors.add(color);
			notifyItemInserted(color.getSortOrder() - 1);

		}

		public void remove(PlayerColor color) {
			if (colors == null) return;
			Timber.d("Removing %s!", color);
			colors.remove(color);
			for (PlayerColor c : colors) {
				if (c.getSortOrder() >= color.getSortOrder()) {
					Timber.d("Moving %s up!", c.getColor());
					c.setSortOrder(c.getSortOrder() - 1);
				}
			}
			notifyItemRemoved(color.getSortOrder() - 1);
		}

		public class ColorViewHolder extends RecyclerView.ViewHolder {
			@BindView(android.R.id.title) TextView titleView;
			@BindView(R.id.color_view) View colorView;
			@BindView(R.id.drag_handle) View dragHandle;

			public ColorViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final PlayerColor color) {
				if (color == null) return;

				titleView.setText(color.getColor());
				ColorUtils.setColorViewValue(colorView, ColorUtils.parseColor(color.getColor()));

				dragHandle.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if (itemTouchHelper != null &&
							MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
							itemTouchHelper.startDrag(ColorViewHolder.this);
						}
						return false;
					}
				});
			}

			public void onItemDragging() {
				itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.light_blue_transparent));
			}

			public void onItemSwiping() {
				itemView.setBackgroundColor(Color.WHITE);
			}

			public void onItemClear() {
				itemView.setBackgroundColor(Color.TRANSPARENT);
			}
		}
	}
}
