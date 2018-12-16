package com.boardgamegeek.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.PlayerColorEntity;
import com.boardgamegeek.ui.dialog.ColorPickerDialogFragment;
import com.boardgamegeek.ui.viewmodel.PlayerColorsViewModel;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.fabric.PlayerColorsManipulationEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class PlayerColorsActivity extends BaseActivity implements ColorPickerDialogFragment.Listener {
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";

	private String buddyName;
	private String playerName;
	private final PlayerColorsAdapter adapter = new PlayerColorsAdapter();


	@BindView(R.id.toolbar) Toolbar toolbar;
	@BindView(R.id.progressView) ContentLoadingProgressBar progressView;
	@BindView(R.id.emptyView) View emptyView;
	@BindView(R.id.recyclerView) RecyclerView recyclerView;
	@BindView(R.id.coordinator) CoordinatorLayout coordinator;

	private final Paint swipePaint = new Paint();
	private Bitmap deleteIcon;
	@BindDimen(R.dimen.material_margin_horizontal) float horizontalPadding;
	private ItemTouchHelper itemTouchHelper;
	private PlayerColorsViewModel viewModel;
	private final ArrayList<String> usedColors = new ArrayList<>();

	public static void start(Context context, String buddyName, String playerName) {
		Intent starter = new Intent(context, PlayerColorsActivity.class);
		starter.putExtra(KEY_BUDDY_NAME, buddyName);
		starter.putExtra(KEY_PLAYER_NAME, playerName);
		context.startActivity(starter);
	}

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

		viewModel = new PlayerColorsViewModel(getApplication());
		viewModel.setUsername(buddyName);
		if (!TextUtils.isEmpty(buddyName)) {
			viewModel.setUsername(buddyName);
		} else {
			viewModel.setPlayerName(playerName);
		}
		viewModel.getColors().observe(this, new Observer<List<PlayerColorEntity>>() {
			@Override
			public void onChanged(List<PlayerColorEntity> playerColorEntities) {
				usedColors.clear();
				if (playerColorEntities != null) {
					for (PlayerColorEntity color : playerColorEntities) {
						usedColors.add(color.getDescription());
					}
				}

				adapter.setColors(playerColorEntities);
				progressView.hide();
				if (playerColorEntities == null || playerColorEntities.size() == 0) {
					AnimationUtils.fadeIn(emptyView);
					AnimationUtils.fadeOut(recyclerView);
				} else {
					AnimationUtils.fadeOut(emptyView);
					AnimationUtils.fadeIn(recyclerView);
				}
			}
		});

		if (savedInstanceState == null) {
			final ContentViewEvent event = new ContentViewEvent()
				.putContentType("PlayerColors");
			if (!TextUtils.isEmpty(buddyName)) event.putContentId(buddyName);
			if (!TextUtils.isEmpty(playerName)) event.putContentName(playerName);
			Answers.getInstance().logContentView(event);
		}
	}

	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
		recyclerView.setHasFixedSize(true);
		recyclerView.setAdapter(adapter);

		swipePaint.setColor(ContextCompat.getColor(this, R.color.medium_blue));
		deleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete_white);

		itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
				int fromPosition = viewHolder.getAdapterPosition();
				int toPosition = target.getAdapterPosition();
				return viewModel.move(fromPosition, toPosition);
				//return false;
			}

			@Override
			public void onSelectedChanged(ViewHolder viewHolder, int actionState) {
				PlayerColorsAdapter.ColorViewHolder colorViewHolder = (PlayerColorsAdapter.ColorViewHolder) viewHolder;
				if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
					colorViewHolder.onItemDragging();
				} else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
					colorViewHolder.onItemSwiping();
				}
				super.onSelectedChanged(viewHolder, actionState);
			}

			@Override
			public void clearView(RecyclerView recyclerView, ViewHolder viewHolder) {
				PlayerColorsAdapter.ColorViewHolder colorViewHolder = (PlayerColorsAdapter.ColorViewHolder) viewHolder;
				colorViewHolder.onItemClear();
				super.clearView(recyclerView, viewHolder);
			}

			@Override
			public void onSwiped(final ViewHolder viewHolder, int swipeDir) {
				final PlayerColorEntity color = adapter.getItem(viewHolder.getAdapterPosition());
				if (color == null) return;
				Snackbar.make(coordinator, getString(R.string.removed_suffix, color.getDescription()), Snackbar.LENGTH_LONG)
					.setAction(R.string.undo, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							viewModel.add(color);
							PlayerColorsManipulationEvent.log("UndoDelete", color.getDescription());
						}
					})
					.setActionTextColor(ContextCompat.getColor(PlayerColorsActivity.this, R.color.light_blue))
					.show();
				viewModel.remove(color);
				PlayerColorsManipulationEvent.log("Delete", color.getDescription());
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
						iconSrc = new Rect(
							0,
							0,
							Math.min((int) (dX - itemView.getLeft() - horizontalPadding), deleteIcon.getWidth()),
							deleteIcon.getHeight());
						iconDst = new RectF(
							(float) itemView.getLeft() + horizontalPadding,
							(float) itemView.getTop() + verticalPadding,
							Math.min(itemView.getLeft() + horizontalPadding + deleteIcon.getWidth(), dX),
							(float) itemView.getBottom() - verticalPadding);
					} else {
						background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
						iconSrc = new Rect(
							Math.max(deleteIcon.getWidth() + (int) horizontalPadding + (int) dX, 0),
							0,
							deleteIcon.getWidth(),
							deleteIcon.getHeight());
						iconDst = new RectF(
							Math.max((float) itemView.getRight() + dX, (float) itemView.getRight() - horizontalPadding - deleteIcon.getWidth()),
							(float) itemView.getTop() + verticalPadding,
							(float) itemView.getRight() - horizontalPadding,
							(float) itemView.getBottom() - verticalPadding);
					}
					c.drawRect(background, swipePaint);
					c.drawBitmap(deleteIcon, iconSrc, iconDst, swipePaint);
				}
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}
		});
		itemTouchHelper.attachToRecyclerView(recyclerView);
	}

	@Override
	protected void onStop() {
		viewModel.save(); // TODO - where is the observer removed?
		super.onStop();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.player_colors;
	}

	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clear:
				DialogUtils.createThemedBuilder(this)
					.setMessage(R.string.are_you_sure_clear_colors)
					.setPositiveButton(R.string.clear, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							PlayerColorsManipulationEvent.log("Clear");
							viewModel.clear();
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

	@OnClick(R.id.empty_button)
	void onEmptyClick() {
		PlayerColorsManipulationEvent.log("Generate");
		viewModel.createRandom();
	}

	@OnClick(R.id.fab)
	void onFabClick() {
		ColorPickerDialogFragment fragment = ColorPickerDialogFragment.newInstance(R.string.title_add_color,
			ColorUtils.getColorList(), null, null, null, 4, 0, usedColors);
		fragment.show(getSupportFragmentManager(), "color_picker");
	}

	@Override
	public void onColorSelected(@NotNull String description, int color, int requestCode) {
		PlayerColorsManipulationEvent.log("Add", description);
		viewModel.add(description);
	}

	public class PlayerColorsAdapter extends RecyclerView.Adapter<PlayerColorsAdapter.ColorViewHolder> {
		private List<PlayerColorEntity> colors;

		public PlayerColorsAdapter() {
		}

		public void setColors(List<PlayerColorEntity> colors) {
			this.colors = colors;
			notifyDataSetChanged();
		}

		@NonNull
		@Override
		public PlayerColorsAdapter.ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new PlayerColorsAdapter.ColorViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_player_color, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull PlayerColorsAdapter.ColorViewHolder holder, int position) {
			PlayerColorEntity color = getItem(position);
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

		public PlayerColorEntity getItem(int position) {
			if (colors == null) return null;
			for (PlayerColorEntity color : colors) {
				if (color.getSortOrder() == position + 1) {
					return color;
				}
			}
			return null;
		}

		public class ColorViewHolder extends RecyclerView.ViewHolder {
			@BindView(android.R.id.title) TextView titleView;
			@BindView(R.id.color_view) View colorView;
			@BindView(R.id.drag_handle) View dragHandle;

			public ColorViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final PlayerColorEntity color) {
				if (color == null) return;

				titleView.setText(color.getDescription());
				ColorUtils.setColorViewValue(colorView, color.getRgb());

				dragHandle.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if (event.getAction() == MotionEvent.ACTION_DOWN) {
							if (itemTouchHelper != null) {
								itemTouchHelper.startDrag(PlayerColorsAdapter.ColorViewHolder.this);
							}
						} else if (event.getAction() == MotionEvent.ACTION_UP) {
							v.performClick();
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
