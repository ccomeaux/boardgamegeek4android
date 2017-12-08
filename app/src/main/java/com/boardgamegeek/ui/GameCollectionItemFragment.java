package com.boardgamegeek.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionItemChangedEvent;
import com.boardgamegeek.events.CollectionItemResetEvent;
import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.UpdateCollectionItemPrivateInfoTask;
import com.boardgamegeek.tasks.UpdateCollectionItemRatingTask;
import com.boardgamegeek.tasks.UpdateCollectionItemStatusTask;
import com.boardgamegeek.tasks.UpdateCollectionItemTextTask;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask.CompletedEvent;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment;
import com.boardgamegeek.ui.dialog.PrivateInfoDialogFragment;
import com.boardgamegeek.ui.dialog.PrivateInfoDialogFragment.PrivateInfoDialogListener;
import com.boardgamegeek.ui.model.CollectionItem;
import com.boardgamegeek.ui.model.PrivateInfo;
import com.boardgamegeek.ui.widget.TextEditorCard;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.TaskUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

public class GameCollectionItemFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_COLLECTION_ID = "COLLECTION_ID";
	private static final int _TOKEN = 0x31;
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final DecimalFormat RATING_EDIT_FORMAT = new DecimalFormat("0.#");

	private Unbinder unbinder;
	@BindView(R.id.root_container) ViewGroup rootContainer;

	// rating
	@BindView(R.id.rating_container) View ratingContainer;
	@BindView(R.id.rating) TextView rating;
	@BindView(R.id.rating_timestamp) TimestampView ratingTimestampView;

	// statuses
	@BindView(R.id.status) TextView statusView;
	@BindView(R.id.want_to_buy) CheckBox wantToBuyView;
	@BindView(R.id.preordered) CheckBox preorderedView;
	@BindView(R.id.own) CheckBox ownView;
	@BindView(R.id.want_to_play) CheckBox wantToPlayView;
	@BindView(R.id.previously_owned) CheckBox previouslyOwnedView;

	// wishlist
	@BindView(R.id.wishlist_view_container) ViewGroup wishlistViewContainer;
	@BindView(R.id.wishlist_status) TextView wishlistStatusView;
	@BindView(R.id.wishlist_comment) TextView wishlistCommentView;
	@BindView(R.id.wishlist_comment_view_timestamp) TimestampView wishlistCommentViewTimestampView;
	@BindView(R.id.wishlist_edit_container) ViewGroup wishlistEditContainer;
	@BindView(R.id.wishlist) CheckBox wishlistView;
	@BindView(R.id.wishlist_priority) Spinner wishlistPriorityView;
	@BindView(R.id.edit_wishlist_comment) TextView editWishlistCommentView;
	@BindView(R.id.add_wishlist_comment) TextView addWishlistCommentView;
	@BindView(R.id.wishlist_comment_edit_timestamp) TimestampView wishlistCommentEditTimestampView;

	// trade
	@BindView(R.id.trade_container) ViewGroup tradeContainer;
	@BindView(R.id.trade_view_container) ViewGroup tradeViewContainer;
	@BindView(R.id.trade_status) TextView tradeStatusView;
	@BindView(R.id.want_in_trade) CheckBox wantInTradeView;
	@BindView(R.id.for_trade) CheckBox forTradeView;
	@BindView(R.id.trade_condition_header) View tradeConditionHeader;
	@BindView(R.id.trade_condition) TextView tradeCondition;
	@BindView(R.id.want_parts_header) View wantPartsHeader;
	@BindView(R.id.want_parts) TextView wantParts;
	@BindView(R.id.has_parts_header) View hasPartsHeader;
	@BindView(R.id.has_parts) TextView hasParts;
	@BindView(R.id.condition_card) TextEditorCard conditionCard;
	@BindView(R.id.want_parts_card) TextEditorCard wantPartsCard;
	@BindView(R.id.has_parts_card) TextEditorCard hasPartsCard;

	// comment
	@BindView(R.id.comment_container) ViewGroup commentContainer;
	@BindView(R.id.comment_view_container) ViewGroup commentViewContainer;
	@BindView(R.id.view_comment) TextView commentView;
	@BindView(R.id.view_comment_deleting) TextView commentDeletingView;
	@BindView(R.id.comment_view_timestamp) TimestampView commentViewTimestampView;
	@BindView(R.id.edit_comment) TextView editCommentView;
	@BindView(R.id.add_comment) TextView addCommentView;
	@BindView(R.id.comment_edit_timestamp) TimestampView commentEditTimestampView;

	@BindView(R.id.private_info_container) ViewGroup privateInfoContainer;
	@BindView(R.id.private_info) TextView privateInfo;
	@BindView(R.id.private_info_comments) TextView privateInfoComments;
	@BindView(R.id.private_info_timestamp) TimestampView privateInfoTimestampView;
	@BindView(R.id.last_modified) TimestampView lastModified;
	@BindView(R.id.collection_id) TextView id;
	@BindView(R.id.updated) TimestampView updated;

	@BindViews({
		R.id.add_comment,
		R.id.card_header_private_info,
		R.id.wishlist_view_header,
		R.id.wishlist_edit_header,
		R.id.trade_condition_header,
		R.id.want_parts_header,
		R.id.has_parts_header
	}) List<TextView> colorizedHeaders;
	@BindViews({
		R.id.condition_card,
		R.id.want_parts_card,
		R.id.has_parts_card
	}) List<TextEditorCard> textEditorCards;
	@BindViews({
		R.id.want_to_buy,
		R.id.preordered,
		R.id.own,
		R.id.want_to_play,
		R.id.previously_owned,
		R.id.want_in_trade,
		R.id.for_trade,
		R.id.comment_edit_container,
		R.id.wishlist_edit_container,
		R.id.trade_edit_container
	}) List<View> editFields;
	@BindViews({
		R.id.status
	}) List<View> viewOnlyFields;
	@BindViews({
		R.id.comment_view_container,
		R.id.wishlist_view_container,
		R.id.trade_view_container
	}) List<ViewGroup> visibleByTagViews;
	@BindViews({
		R.id.comment_container,
		R.id.wishlist_container,
		R.id.trade_container
	}) List<ViewGroup> visibleByChildrenViews;
	@BindViews({
		R.id.want_to_buy,
		R.id.preordered,
		R.id.own,
		R.id.want_to_play,
		R.id.previously_owned,
		R.id.want_in_trade,
		R.id.for_trade,
		R.id.wishlist
	}) List<CheckBox> statusViews;
	private EditTextDialogFragment commentDialogFragment;
	private EditTextDialogFragment wishlistCommentDialogFragment;
	private PrivateInfoDialogFragment privateInfoDialogFragment;

	private int gameId = BggContract.INVALID_ID;
	private int collectionId = BggContract.INVALID_ID;
	private long internalId = 0;
	private boolean isRefreshing;
	private boolean mightNeedRefreshing;
	private Palette palette;
	private boolean needsUploading;
	@State boolean isItemEditable;
	private boolean isEditable;

	public static GameCollectionItemFragment newInstance(int gameId, int collectionId) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putInt(KEY_COLLECTION_ID, collectionId);
		GameCollectionItemFragment fragment = new GameCollectionItemFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		collectionId = bundle.getInt(KEY_COLLECTION_ID, BggContract.INVALID_ID);
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_collection_item, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		colorize(palette);

		mightNeedRefreshing = true;
		getLoaderManager().restartLoader(_TOKEN, getArguments(), this);

		return rootView;
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}


	@DebugLog
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@DebugLog
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		if (id != _TOKEN || collectionId == BggContract.INVALID_ID) {
			return null;
		}
		return new CursorLoader(getActivity(),
			CollectionItem.Companion.getUri(),
			CollectionItem.Companion.getProjection(),
			CollectionItem.Companion.getSelection(collectionId),
			CollectionItem.Companion.getSelectionArgs(collectionId, gameId),
			null);
	}

	@DebugLog
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == _TOKEN) {
			if (cursor == null || !cursor.moveToFirst()) {
				if (mightNeedRefreshing) {
					triggerRefresh();
				}
				return;
			}

			CollectionItem item = CollectionItem.Companion.fromCursor(cursor);
			internalId = item.getInternalId();
			updateUi(item);

			if (mightNeedRefreshing) {
				if (DateTimeUtils.howManyDaysOld(item.getUpdated()) > AGE_IN_DAYS_TO_REFRESH) {
					triggerRefresh();
				}
			}
			mightNeedRefreshing = false;
		} else {
			Timber.d("Query complete, Not Actionable: %s", loader.getId());
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@DebugLog
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	public void enableEditMode(boolean enable) {
		isEditable = enable;
		bindVisibility();
	}

	private void bindVisibility() {
		boolean isEdit = isEditable && isItemEditable;

		ButterKnife.apply(viewOnlyFields, PresentationUtils.setVisibility, !isEdit);
		ButterKnife.apply(editFields, PresentationUtils.setVisibility, isEdit);

		commentContainer.setClickable(isEdit);
		ratingContainer.setClickable(isEdit);
		privateInfoContainer.setClickable(isEdit);

		// TODO: 12/8/17 make wishlist comment clickable

		conditionCard.enableEditMode(isEdit);
		wantPartsCard.enableEditMode(isEdit);
		hasPartsCard.enableEditMode(isEdit);

		if (isEdit) {
			ButterKnife.apply(visibleByTagViews, PresentationUtils.setGone);
		} else {
			ButterKnife.apply(visibleByTagViews, PresentationUtils.setVisibilityByTag);
		}

		ButterKnife.apply(visibleByChildrenViews, PresentationUtils.setVisibilityByChildren);
	}

	public void syncChanges() {
		if (needsUploading) {
			SyncService.sync(getContext(), SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
			needsUploading = false;
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe
	public void onEvent(CollectionItemUpdatedEvent event) {
		needsUploading = true;
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe
	public void onEvent(CollectionItemResetEvent event) {
		if (event.getInternalId() == internalId) {
			needsUploading = false;
			TaskUtils.executeAsyncTask(new SyncCollectionByGameTask(getContext(), gameId));
		}
	}

	@DebugLog
	public void onPaletteGenerated(Palette palette) {
		this.palette = palette;
		colorize(palette);
	}

	@DebugLog
	private void colorize(Palette palette) {
		if (palette == null || !isAdded()) return;
		Palette.Swatch swatch = PaletteUtils.getHeaderSwatch(palette);
		ButterKnife.apply(colorizedHeaders, PaletteUtils.colorTextViewSetter, swatch);
		ButterKnife.apply(textEditorCards, TextEditorCard.headerColorSetter, swatch);
	}

	@OnCheckedChanged({
		R.id.want_to_buy,
		R.id.preordered,
		R.id.own,
		R.id.want_to_play,
		R.id.previously_owned,
		R.id.want_in_trade,
		R.id.for_trade,
		R.id.wishlist
	})
	void onStatusCheckChanged(CompoundButton view) {
		if (view.getVisibility() != View.VISIBLE) return;
		if (!isEditable) return;
		if (view == wishlistView) {
			wishlistPriorityView.setEnabled(wishlistView.isChecked());
		}
		updateStatuses();
	}

	@OnItemSelected(R.id.wishlist_priority)
	void onWishlistPrioritySelected() {
		if (wishlistPriorityView.getVisibility() != View.VISIBLE) return;
		if (!wishlistPriorityView.isEnabled()) return;
		if (!wishlistView.isChecked()) return;
		if (!isEditable) return;
		updateStatuses();
	}

	@DebugLog
	@OnClick({ R.id.edit_wishlist_comment, R.id.add_wishlist_comment })
	public void onWishlistCommentClick() {
		ensureWishlistCommentDialogFragment();
		wishlistCommentDialogFragment.setText(wishlistCommentView.getText().toString());
		DialogUtils.showFragment(getActivity(), wishlistCommentDialogFragment, "wishlist_comment_dialog");
	}

	@DebugLog
	private void ensureWishlistCommentDialogFragment() {
		if (wishlistCommentDialogFragment == null) {
			wishlistCommentDialogFragment = EditTextDialogFragment.newLongFormInstance(
				R.string.wishlist_comment,
				wishlistEditContainer,
				new EditTextDialogListener() {
					@Override
					public void onFinishEditDialog(String inputText) {
						UpdateCollectionItemTextTask task =
							new UpdateCollectionItemTextTask(getActivity(),
								gameId, collectionId, internalId, inputText,
								Collection.WISHLIST_COMMENT, Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP);
						TaskUtils.executeAsyncTask(task);
					}
				}
			);
		}
	}

	private void updateStatuses() {
		List<String> statuses = new ArrayList<>();
		for (CheckBox checkBox : statusViews) {
			if (checkBox.isChecked()) {
				String status = (String) checkBox.getTag();
				if (!TextUtils.isEmpty(status)) statuses.add(status);
			}
		}
		int wishlistPriority = wishlistView.isChecked() ?
			wishlistPriorityView.getSelectedItemPosition() + 1 : 0;
		UpdateCollectionItemStatusTask task =
			new UpdateCollectionItemStatusTask(getActivity(),
				gameId, collectionId, internalId,
				statuses, wishlistPriority);
		TaskUtils.executeAsyncTask(task);
	}

	@DebugLog
	@OnClick(R.id.condition_card)
	public void onConditionClick() {
		onTextEditorClick(conditionCard, Collection.CONDITION, Collection.TRADE_CONDITION_DIRTY_TIMESTAMP);
	}

	@DebugLog
	@OnClick(R.id.want_parts_card)
	public void onWantPartsClick() {
		onTextEditorClick(wantPartsCard, Collection.WANTPARTS_LIST, Collection.WANT_PARTS_DIRTY_TIMESTAMP);
	}

	@DebugLog
	@OnClick(R.id.has_parts_card)
	public void onHasPartsClick() {
		onTextEditorClick(hasPartsCard, Collection.HASPARTS_LIST, Collection.HAS_PARTS_DIRTY_TIMESTAMP);
	}

	@DebugLog
	private void onTextEditorClick(TextEditorCard card, final String textColumn, final String timestampColumn) {
		EditTextDialogFragment dialogFragment = EditTextDialogFragment.newLongFormInstance(
			card.getHeaderText(),
			card,
			new EditTextDialogListener() {
				@Override
				public void onFinishEditDialog(String inputText) {
					UpdateCollectionItemTextTask task =
						new UpdateCollectionItemTextTask(getContext(),
							gameId, collectionId, internalId, inputText,
							textColumn, timestampColumn);
					TaskUtils.executeAsyncTask(task);
				}
			}
		);
		dialogFragment.setText(card.getContentText());
		DialogUtils.showFragment(getActivity(), dialogFragment, card.toString());
	}

	@DebugLog
	@OnClick(R.id.comment_edit_container)
	public void onCommentClick() {
		ensureCommentDialogFragment();
		commentDialogFragment.setText(commentView.getText().toString());
		DialogUtils.showFragment(getActivity(), commentDialogFragment, "comment_dialog");
	}

	@DebugLog
	private void ensureCommentDialogFragment() {
		if (commentDialogFragment == null) {
			commentDialogFragment = EditTextDialogFragment.newLongFormInstance(
				R.string.title_comments,
				commentContainer,
				new EditTextDialogListener() {
					@Override
					public void onFinishEditDialog(String inputText) {
						UpdateCollectionItemTextTask task =
							new UpdateCollectionItemTextTask(getActivity(),
								gameId, collectionId, internalId, inputText,
								Collection.COMMENT, Collection.COMMENT_DIRTY_TIMESTAMP);
						TaskUtils.executeAsyncTask(task);
					}
				}
			);
		}
	}

	@DebugLog
	@OnClick(R.id.rating_container)
	public void onRatingClick() {
		String output = RATING_EDIT_FORMAT.format((double) rating.getTag());
		if ("0".equals(output)) {
			output = "";
		}
		final NumberPadDialogFragment fragment = NumberPadDialogFragment.newInstance(getString(R.string.rating), output);
		fragment.setMinValue(1.0);
		fragment.setMaxValue(10.0);
		fragment.setMaxMantissa(6);
		fragment.setOnDoneClickListener(new NumberPadDialogFragment.OnClickListener() {
			@Override
			public void onDoneClick(String output) {
				double rating = StringUtils.parseDouble(output);
				UpdateCollectionItemRatingTask task =
					new UpdateCollectionItemRatingTask(getActivity(), gameId, collectionId, internalId, rating);
				TaskUtils.executeAsyncTask(task);
			}
		});
		DialogUtils.showFragment(getActivity(), fragment, "rating_dialog");
	}

	@DebugLog
	@OnClick(R.id.private_info_container)
	public void onPrivateInfoClick() {
		ensurePrivateInfoDialogFragment();
		privateInfoDialogFragment.setPriceCurrency(String.valueOf(privateInfo.getTag(R.id.price_currency)));
		privateInfoDialogFragment.setPrice(getDoubleFromTag(privateInfo, R.id.price));
		privateInfoDialogFragment.setCurrentValueCurrency(String.valueOf(privateInfo.getTag(R.id.current_value_currency)));
		privateInfoDialogFragment.setCurrentValue(getDoubleFromTag(privateInfo, R.id.current_value));
		privateInfoDialogFragment.setQuantity(getIntFromTag(privateInfo, R.id.quantity));
		privateInfoDialogFragment.setAcquisitionDate(String.valueOf(privateInfo.getTag(R.id.acquisition_date)));
		privateInfoDialogFragment.setAcquiredFrom(String.valueOf(privateInfo.getTag(R.id.acquired_from)));
		privateInfoDialogFragment.setComment(privateInfoComments.getText().toString());
		DialogUtils.showFragment(getActivity(), privateInfoDialogFragment, "private_info_dialog");
	}

	private double getDoubleFromTag(View textView, int key) {
		final Object tag = textView.getTag(key);
		if (tag == null) return 0.0;
		return (double) tag;
	}

	private int getIntFromTag(View textView, int key) {
		final Object tag = textView.getTag(key);
		if (tag == null) return 1;
		return (int) tag;
	}

	@DebugLog
	private void ensurePrivateInfoDialogFragment() {
		if (privateInfoDialogFragment == null) {
			privateInfoDialogFragment = PrivateInfoDialogFragment.newInstance(
				privateInfoContainer,
				new PrivateInfoDialogListener() {
					@Override
					public void onFinishEditDialog(PrivateInfo privateInfo) {
						UpdateCollectionItemPrivateInfoTask task =
							new UpdateCollectionItemPrivateInfoTask(getActivity(), gameId, collectionId, internalId, privateInfo);
						TaskUtils.executeAsyncTask(task);
					}
				}
			);
		}
	}

	@DebugLog
	public boolean triggerRefresh() {
		mightNeedRefreshing = false;
		if (!isRefreshing && gameId != BggContract.INVALID_ID) {
			isRefreshing = true;
			TaskUtils.executeAsyncTask(new SyncCollectionByGameTask(getContext(), gameId));
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CompletedEvent event) {
		if (event.getGameId() == gameId) {
			isRefreshing = false;
		}
	}

	@DebugLog
	private void notifyChange(CollectionItem item) {
		CollectionItemChangedEvent event = new CollectionItemChangedEvent(item.getName(), item.getImageUrl(), item.getYear());
		EventBus.getDefault().post(event);
	}

	@DebugLog
	private void updateUi(CollectionItem item) {
		notifyChange(item);

		isItemEditable = true;

		statusView.setText(getStatusDescription(item));

		rating.setText(PresentationUtils.describePersonalRating(getActivity(), item.getRating()));
		rating.setTag(MathUtils.constrain(item.getRating(), 0.0, 10.0));
		ColorUtils.setTextViewBackground(rating, ColorUtils.getRatingColor(item.getRating()));
		ratingTimestampView.setTimestamp(item.getRatingTimestamp());

		wantToBuyView.setChecked(item.isWantToBuy());
		preorderedView.setChecked(item.isPreordered());
		ownView.setChecked(item.isOwn());
		wantToPlayView.setChecked(item.isWantToPlay());
		previouslyOwnedView.setChecked(item.isPreviouslyOwned());

		bindComment(item);
		bindWishlist(item);
		bindTrade(item);

		privateInfo.setVisibility(hasPrivateInfo(item) ? View.VISIBLE : View.GONE);
		privateInfo.setText(getPrivateInfo(item));
		privateInfo.setTag(R.id.price_currency, item.getPriceCurrency());
		privateInfo.setTag(R.id.price, item.getPrice());
		privateInfo.setTag(R.id.current_value_currency, item.getCurrentValueCurrency());
		privateInfo.setTag(R.id.current_value, item.getCurrentValue());
		privateInfo.setTag(R.id.quantity, item.getQuantity());
		privateInfo.setTag(R.id.acquisition_date, item.getAcquisitionDate());
		privateInfo.setTag(R.id.acquired_from, item.getAcquiredFrom());
		PresentationUtils.setTextOrHide(privateInfoComments, item.getPrivateComment());
		privateInfoTimestampView.setTimestamp(item.getPrivateInfoTimestamp());

		lastModified.setTimestamp(item.getDirtyTimestamp() > 0 ? item.getDirtyTimestamp() :
			item.getStatusTimestamp() > 0 ? item.getStatusTimestamp() : item.getLastModifiedDateTime());
		updated.setTimestamp(item.getUpdated());
		PresentationUtils.setTextOrHide(id, item.getId());

		bindVisibility();
	}

	private void bindComment(CollectionItem item) {
		// view
		PresentationUtils.setTextOrHide(commentView, item.getComment());
		commentDeletingView.setVisibility(TextUtils.isEmpty(item.getComment()) && item.getCommentTimestamp() > 0 ? View.VISIBLE : View.GONE);
		commentViewTimestampView.setTimestamp(item.getCommentTimestamp());
		setVisibleTag(commentViewContainer, !TextUtils.isEmpty(item.getComment()) || item.getCommentTimestamp() > 0);
		// edit
		PresentationUtils.setTextOrHide(editCommentView, item.getComment());
		addCommentView.setVisibility(TextUtils.isEmpty(item.getComment()) ? View.VISIBLE : View.GONE);
		commentEditTimestampView.setTimestamp(item.getCommentTimestamp());
	}

	private void bindWishlist(CollectionItem item) {
		// view
		if (item.isWishlist()) {
			PresentationUtils.setTextOrHide(wishlistStatusView, PresentationUtils.describeWishlist(getContext(), item.getWishlistPriority()));
		} else {
			wishlistStatusView.setVisibility(View.GONE);
		}
		PresentationUtils.setTextOrHide(wishlistCommentView, item.getWishlistComment());
		wishlistCommentViewTimestampView.setTimestamp(item.getWishlistCommentDirtyTimestamp());
		setVisibleTag(wishlistViewContainer, item.isWishlist() || !TextUtils.isEmpty(item.getWishlistComment()));

		// edit
		if (wishlistPriorityView.getAdapter() == null)
			wishlistPriorityView.setAdapter(new WishlistPriorityAdapter(getContext()));
		if (item.isWishlist()) wishlistPriorityView.setSelection(item.getSafeWishlistPriorty() - 1);
		wishlistPriorityView.setEnabled(item.isWishlist());
		wishlistView.setChecked(item.isWishlist());
		PresentationUtils.setTextOrHide(editWishlistCommentView, item.getWishlistComment());
		addWishlistCommentView.setVisibility(TextUtils.isEmpty(item.getWishlistComment()) ? View.VISIBLE : View.GONE);
		wishlistCommentEditTimestampView.setTimestamp(item.getWishlistCommentDirtyTimestamp());
	}

	private void bindTrade(CollectionItem item) {
		// view
		List<String> statusDescriptions = new ArrayList<>();
		if (item.isForTrade()) statusDescriptions.add(getString(R.string.collection_status_for_trade));
		if (item.isWantInTrade()) statusDescriptions.add(getString(R.string.collection_status_want_in_trade));
		tradeStatusView.setText(StringUtils.formatList(statusDescriptions));
		tradeConditionHeader.setVisibility(TextUtils.isEmpty(item.getCondition()) ? View.GONE : View.VISIBLE);
		PresentationUtils.setTextOrHide(tradeCondition, item.getCondition());
		wantPartsHeader.setVisibility(TextUtils.isEmpty(item.getWantParts()) ? View.GONE : View.VISIBLE);
		PresentationUtils.setTextOrHide(wantParts, item.getWantParts());
		hasPartsHeader.setVisibility(TextUtils.isEmpty(item.getHasParts()) ? View.GONE : View.VISIBLE);
		PresentationUtils.setTextOrHide(hasParts, item.getHasParts());
		setVisibleTag(tradeViewContainer, item.isForTrade() || item.isWantInTrade());

		// edit
		wantInTradeView.setChecked(item.isWantInTrade());
		forTradeView.setChecked(item.isForTrade());
		conditionCard.setContentText(item.getCondition());
		conditionCard.setTimestamp(item.getTradeConditionDirtyTimestamp());
		wantPartsCard.setContentText(item.getWantParts());
		wantPartsCard.setTimestamp(item.getWantPartsDirtyTimestamp());
		hasPartsCard.setContentText(item.getHasParts());
		hasPartsCard.setTimestamp(item.getHasPartsDirtyTimestamp());
	}

	private void setVisibleTag(View view, boolean isVisible) {
		view.setTag(R.id.visibility, isVisible);
	}

	private String getStatusDescription(CollectionItem item) {
		List<String> statusDescriptions = new ArrayList<>();
		if (item.isOwn()) statusDescriptions.add(getString(R.string.collection_status_own));
		if (item.isPreviouslyOwned()) statusDescriptions.add(getString(R.string.collection_status_prev_owned));
		if (item.isWantToBuy()) statusDescriptions.add(getString(R.string.collection_status_want_to_buy));
		if (item.isWantToPlay()) statusDescriptions.add(getString(R.string.collection_status_want_to_play));
		if (item.isPreordered()) statusDescriptions.add(getString(R.string.collection_status_preordered));

		String status = StringUtils.formatList(statusDescriptions);
		if (TextUtils.isEmpty(status)) {
			if (item.getNumberOfPlays() > 0) {
				return getString(R.string.played);
			}
			return getString(R.string.invalid_collection_status);
		}
		return status;
	}

	private CharSequence getPrivateInfo(CollectionItem item) {
		String initialText = getResources().getString(R.string.acquired);
		SpannableStringBuilder sb = new SpannableStringBuilder();
		sb.append(initialText);
		if (item.getQuantity() > 1) {
			sb.append(" ");
			StringUtils.appendBold(sb, String.valueOf(item.getQuantity()));
		}
		if (!TextUtils.isEmpty(item.getAcquisitionDate())) {
			String date = null;
			try {
				date = DateUtils.formatDateTime(getContext(), DateTimeUtils.getMillisFromApiDate(item.getAcquisitionDate(), 0), DateUtils.FORMAT_SHOW_DATE);
			} catch (Exception e) {
				Timber.w(e, "Could find a date in here: %s", item.getAcquisitionDate());
			}
			if (!TextUtils.isEmpty(date)) {
				sb.append(" ").append(getString(R.string.on)).append(" ");
				StringUtils.appendBold(sb, date);
			}
		}
		if (!TextUtils.isEmpty(item.getAcquiredFrom())) {
			sb.append(" ").append(getString(R.string.from)).append(" ");
			StringUtils.appendBold(sb, item.getAcquiredFrom());
		}
		if (item.getPrice() > 0.0) {
			sb.append(" ").append(getString(R.string.for_)).append(" ");
			StringUtils.appendBold(sb, PresentationUtils.describeMoney(item.getPriceCurrency(), item.getPrice()));
		}
		if (item.getCurrentValue() > 0.0) {
			sb.append(" (").append(getString(R.string.currently_worth)).append(" ");
			StringUtils.appendBold(sb, PresentationUtils.describeMoney(item.getCurrentValueCurrency(), item.getCurrentValue()));
			sb.append(")");
		}

		if (sb.toString().equals(initialText)) {
			// shouldn't happen
			return null;
		}
		return sb;
	}

	boolean hasPrivateInfo(CollectionItem item) {
		return item.getQuantity() > 1 ||
			!TextUtils.isEmpty(item.getAcquisitionDate()) ||
			!TextUtils.isEmpty(item.getAcquiredFrom()) ||
			item.getPrice() > 0.0 ||
			item.getCurrentValue() > 0.0;
	}

	private static class WishlistPriorityAdapter extends ArrayAdapter<String> {
		public WishlistPriorityAdapter(Context context) {
			super(context,
				android.R.layout.simple_spinner_item,
				context.getResources().getStringArray(R.array.wishlist_priority_finite));
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}
	}
}
