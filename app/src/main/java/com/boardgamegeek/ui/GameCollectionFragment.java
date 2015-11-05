package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.graphics.Palette;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.ui.widget.ObservableScrollView;
import com.boardgamegeek.ui.widget.ObservableScrollView.Callbacks;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.VersionUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GameCollectionFragment extends Fragment implements
	LoaderCallbacks<Cursor>,
	Callback,
	Callbacks, OnRefreshListener {

	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	private boolean mSyncing;
	@SuppressWarnings("unused") @InjectView(R.id.progress) View progress;
	@SuppressWarnings("unused") @InjectView(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
	@SuppressWarnings("unused") @InjectView(R.id.scroll_container) ObservableScrollView scrollContainer;
	@SuppressWarnings("unused") @InjectView(R.id.hero_container) View heroContainer;
	@SuppressWarnings("unused") @InjectView(R.id.image) ImageView image;
	@SuppressWarnings("unused") @InjectView(R.id.header_container) View headerContainer;
	@SuppressWarnings("unused") @InjectView(R.id.name) TextView name;
	@SuppressWarnings("unused") @InjectView(R.id.year) TextView year;
	@SuppressWarnings("unused") @InjectView(R.id.status_container) View statusContainer;
	@SuppressWarnings("unused") @InjectView(R.id.status) TextView status;
	@SuppressWarnings("unused") @InjectView(R.id.last_modified) TextView lastModified;
	@SuppressWarnings("unused") @InjectView(R.id.rating) TextView rating;
	@SuppressWarnings("unused") @InjectView(R.id.comment) TextView comment;
	@SuppressWarnings("unused") @InjectView(R.id.private_info_container) View privateInfoContainer;
	@SuppressWarnings("unused") @InjectView(R.id.private_info) TextView privateInfo;
	@SuppressWarnings("unused") @InjectView(R.id.private_info_comments) TextView privateInfoComments;
	@SuppressWarnings("unused") @InjectView(R.id.wishlist_container) View wishlistContainer;
	@SuppressWarnings("unused") @InjectView(R.id.wishlist_comment) TextView wishlistComment;
	@SuppressWarnings("unused") @InjectView(R.id.condition_container) View conditionContainer;
	@SuppressWarnings("unused") @InjectView(R.id.condition_comment) TextView conditionComment;
	@SuppressWarnings("unused") @InjectView(R.id.want_parts_container) View wantPartsContainer;
	@SuppressWarnings("unused") @InjectView(R.id.want_parts_comment) TextView wantPartsComment;
	@SuppressWarnings("unused") @InjectView(R.id.has_parts_container) View hasPartsContainer;
	@SuppressWarnings("unused") @InjectView(R.id.has_parts_comment) TextView hasPartsComment;
	@SuppressWarnings("unused") @InjectView(R.id.collection_id) TextView id;
	@SuppressWarnings("unused") @InjectView(R.id.updated) TextView updated;
	@SuppressWarnings("unused") @InjectViews({
		R.id.status,
		R.id.last_modified
	}) List<TextView> mColorizedTextViews;
	@SuppressWarnings("unused") @InjectViews({
		R.id.card_header_private_info,
		R.id.card_header_wishlist,
		R.id.card_header_condition,
		R.id.card_header_want_parts,
		R.id.card_header_has_parts
	}) List<TextView> mColorizedHeaders;

	private int mGameId = BggContract.INVALID_ID;
	private int mCollectionId = BggContract.INVALID_ID;
	private String mImageUrl;
	private boolean mMightNeedRefreshing;
	private Palette mPalette;

	private final ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
		= new ViewTreeObserver.OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			ImageUtils.resizeImagePerAspectRatio(image, scrollContainer.getHeight() / 2, heroContainer);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mCollectionId = intent.getIntExtra(ActivityUtils.KEY_COLLECTION_ID, BggContract.INVALID_ID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_collection, container, false);
		ButterKnife.inject(this, rootView);

		mSwipeRefreshLayout.setOnRefreshListener(this);
		mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);

		colorize(mPalette);
		scrollContainer.addCallbacks(this);
		ViewTreeObserver vto = scrollContainer.getViewTreeObserver();
		if (vto.isAlive()) {
			vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
		}

		mMightNeedRefreshing = true;
		getLoaderManager().restartLoader(CollectionItem._TOKEN, getArguments(), this);

		return rootView;
	}

	@Override
	@DebugLog
	public void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		if (mUpdaterRunnable != null) {
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	@DebugLog
	public void onPause() {
		super.onPause();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
	}

	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ButterKnife.reset(this);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (scrollContainer == null) {
			return;
		}

		ViewTreeObserver vto = scrollContainer.getViewTreeObserver();
		if (vto.isAlive()) {
			if (VersionUtils.hasJellyBean()) {
				vto.removeOnGlobalLayoutListener(mGlobalLayoutListener);
			} else {
				//noinspection deprecation
				vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
			}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		if (id != CollectionItem._TOKEN || mCollectionId == BggContract.INVALID_ID) {
			return null;
		}
		return new CursorLoader(getActivity(), Collection.CONTENT_URI, new CollectionItem().PROJECTION,
			Collection.COLLECTION_ID + "=?", new String[] { String.valueOf(mCollectionId) }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == CollectionItem._TOKEN) {
			if (cursor == null || !cursor.moveToFirst()) {
				if (mMightNeedRefreshing) {
					triggerRefresh();
				}
				return;
			}

			CollectionItem item = new CollectionItem(cursor);
			updateUi(item);
			AnimationUtils.fadeOut(getActivity(), progress, true);
			AnimationUtils.fadeIn(getActivity(), mSwipeRefreshLayout, true);

			if (mMightNeedRefreshing) {
				long u = cursor.getLong(new CollectionItem().UPDATED);
				if (DateTimeUtils.howManyDaysOld(u) > AGE_IN_DAYS_TO_REFRESH) {
					triggerRefresh();
				}
			}
			mMightNeedRefreshing = false;
		} else {
			Timber.d("Query complete, Not Actionable: " + loader.getId());
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onScrollChanged(int deltaX, int deltaY) {
		if (VersionUtils.hasHoneycomb()) {
			int scrollY = scrollContainer.getScrollY();
			image.setTranslationY(scrollY * 0.5f);
			headerContainer.setTranslationY(scrollY * 0.5f);
		}
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	public void onEventMainThread(UpdateEvent event) {
		mSyncing = event.getType() == UpdateService.SYNC_TYPE_GAME_COLLECTION;
		updateRefreshStatus();
	}

	@DebugLog
	public void onEventMainThread(UpdateCompleteEvent event) {
		mSyncing = false;
		updateRefreshStatus();
	}

	@DebugLog
	private void updateRefreshStatus() {
		if (mSwipeRefreshLayout != null) {
			mSwipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					mSwipeRefreshLayout.setRefreshing(mSyncing);
				}
			});
		}
	}

	@Override
	public void onPaletteGenerated(Palette palette) {
		mPalette = palette;
		colorize(palette);
	}

	private void colorize(Palette palette) {
		if (palette == null || scrollContainer == null) {
			return;
		}
		Palette.Swatch swatch = PaletteUtils.getInverseSwatch(palette, getResources().getColor(R.color.info_background));
		statusContainer.setBackgroundColor(swatch.getRgb());
		ButterKnife.apply(mColorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
		swatch = PaletteUtils.getHeaderSwatch(palette);
		ButterKnife.apply(mColorizedHeaders, PaletteUtils.colorTextViewSetter, swatch);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.image)
	public void onThumbnailClick(View v) {
		if (!TextUtils.isEmpty(mImageUrl)) {
			final Intent intent = new Intent(getActivity(), ImageActivity.class);
			intent.putExtra(ActivityUtils.KEY_IMAGE_URL, mImageUrl);
			startActivity(intent);
		}
	}

	private void triggerRefresh() {
		mMightNeedRefreshing = false;
		if (mGameId != BggContract.INVALID_ID) {
			UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_COLLECTION, mGameId);
		}
	}

	private void updateUi(CollectionItem item) {
		ScrimUtils.applyDefaultScrim(headerContainer);

		ImageUtils.safelyLoadImage(image, item.imageUrl, this);
		mImageUrl = item.imageUrl;
		name.setText(item.name.trim());
		year.setText(item.getYearDescription());
		lastModified.setTag(item.lastModifiedDateTime);
		rating.setText(item.getRatingDescription());
		ColorUtils.setViewBackground(rating, ColorUtils.getRatingColor(item.rating));

		status.setText(item.getStatus());
		comment.setVisibility(TextUtils.isEmpty(item.comment) ? View.GONE : View.VISIBLE);
		comment.setText(item.comment);

		// Private info
		if (item.hasPrivateInfo() || item.hasPrivateComment()) {
			privateInfoContainer.setVisibility(View.VISIBLE);
			privateInfo.setVisibility(item.hasPrivateInfo() ? View.VISIBLE : View.GONE);
			privateInfo.setText(item.getPrivateInfo());
			privateInfoComments.setVisibility(item.hasPrivateComment() ? View.VISIBLE : View.GONE);
			privateInfoComments.setText(item.privateComment);
		} else {
			privateInfoContainer.setVisibility(View.GONE);
		}

		showSection(item.wishlistComment, wishlistContainer, wishlistComment);
		showSection(item.condition, conditionContainer, conditionComment);
		showSection(item.wantParts, wantPartsContainer, wantPartsComment);
		showSection(item.hasParts, hasPartsContainer, hasPartsComment);

		id.setText(String.valueOf(item.id));
		id.setVisibility(item.id == 0 ? View.INVISIBLE : View.VISIBLE);
		updated.setTag(item.updated);

		image.setTag(R.id.image, item.imageUrl);
		image.setTag(R.id.name, item.name);

		updateTimeBasedUi();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
		mUpdaterRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
	}

	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (lastModified != null) {
			String lastModifiedDateTime = (String) lastModified.getTag();
			String s = getString(R.string.last_modified) + " ";
			if (!TextUtils.isEmpty(lastModifiedDateTime)) {
				if (String.valueOf(DateTimeUtils.UNKNOWN_DATE).equals(lastModifiedDateTime)) {
					s = ""; // probably not in the collection at all
				} else if (TextUtils.isDigitsOnly(lastModifiedDateTime)) {
					try {
						long lastModified = Long.parseLong(lastModifiedDateTime);
						s += DateUtils.getRelativeTimeSpanString(lastModified);
					} catch (NumberFormatException e) {
						s += lastModifiedDateTime;
					}
				} else {
					s += lastModifiedDateTime;
				}
			} else {
				s += getString(R.string.text_unknown);
			}
			lastModified.setText(s);
		}
		if (updated != null) {
			long u = (long) updated.getTag();
			updated.setText(PresentationUtils.describePastTimeSpan(u,
				getResources().getString(R.string.needs_updating),
				getResources().getString(R.string.updated)));
		}
	}

	private void showSection(String text, View container, TextView comment) {
		container.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		comment.setText(text);
	}

	private class CollectionItem {
		static final int _TOKEN = 0x31;

		String[] PROJECTION = { Collection._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
			Collection.COLLECTION_SORT_NAME, Collection.COMMENT, Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
			Collection.PRIVATE_INFO_PRICE_PAID, Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
			Collection.PRIVATE_INFO_CURRENT_VALUE, Collection.PRIVATE_INFO_QUANTITY,
			Collection.PRIVATE_INFO_ACQUISITION_DATE, Collection.PRIVATE_INFO_ACQUIRED_FROM,
			Collection.PRIVATE_INFO_COMMENT, Collection.LAST_MODIFIED, Collection.COLLECTION_THUMBNAIL_URL,
			Collection.COLLECTION_IMAGE_URL, Collection.COLLECTION_YEAR_PUBLISHED, Collection.CONDITION,
			Collection.HASPARTS_LIST, Collection.WANTPARTS_LIST, Collection.WISHLIST_COMMENT, Collection.RATING,
			Collection.UPDATED, Collection.STATUS_OWN, Collection.STATUS_PREVIOUSLY_OWNED, Collection.STATUS_FOR_TRADE,
			Collection.STATUS_WANT, Collection.STATUS_WANT_TO_BUY, Collection.STATUS_WISHLIST,
			Collection.STATUS_WANT_TO_PLAY, Collection.STATUS_PREORDERED, Collection.STATUS_WISHLIST_PRIORITY,
			Collection.NUM_PLAYS };

		int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		// int COLLECTION_SORT_NAME = 3;
		int COMMENT = 4;
		int PRIVATE_INFO_PRICE_PAID_CURRENCY = 5;
		int PRIVATE_INFO_PRICE_PAID = 6;
		int PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 7;
		int PRIVATE_INFO_CURRENT_VALUE = 8;
		int PRIVATE_INFO_QUANTITY = 9;
		int PRIVATE_INFO_ACQUISITION_DATE = 10;
		int PRIVATE_INFO_ACQUIRED_FROM = 11;
		int PRIVATE_INFO_COMMENT = 12;
		int LAST_MODIFIED = 13;
		// int COLLECTION_THUMBNAIL_URL = 14;
		int COLLECTION_IMAGE_URL = 15;
		int COLLECTION_YEAR_PUBLISHED = 16;
		int CONDITION = 17;
		int HAS_PARTS_LIST = 18;
		int WANT_PARTS_LIST = 19;
		int WISHLIST_COMMENT = 20;
		int RATING = 21;
		int UPDATED = 22;
		int STATUS_OWN = 23;
		// int STATUS_PREVIOUSLY_OWNED = 24;
		// int STATUS_FOR_TRADE = 25;
		// int STATUS_WANT = 26;
		// int STATUS_WANT_TO_BUY = 27;
		int STATUS_WISHLIST = 28;
		// int STATUS_WANT_TO_PLAY = 29;
		int STATUS_PREORDERED = 30;
		int STATUS_WISHLIST_PRIORITY = 31;
		int NUM_PLAYS = 32;

		final DecimalFormat currencyFormat = new DecimalFormat("#0.00");

		Resources r;
		int id;
		String name;
		// String sortName;
		String comment;
		private String lastModifiedDateTime;
		private double rating;
		private long updated;
		private String priceCurrency;
		private double price;
		private String currentValueCurrency;
		private double currentValue;
		int quantity;
		private String acquiredFrom;
		private String acquisitionDate;
		String privateComment;
		String imageUrl;
		private int year;
		String condition;
		String wantParts;
		String hasParts;
		int wishlistPriority;
		String wishlistComment;
		int numPlays;

		private ArrayList<String> mStatus;

		public CollectionItem() {
			// TODO: delete this, here just to get the projection; gotta be a better way
		}

		public CollectionItem(Cursor cursor) {
			r = getResources();
			id = cursor.getInt(COLLECTION_ID);
			name = cursor.getString(COLLECTION_NAME);
			// sortName = cursor.getString(COLLECTION_SORT_NAME);
			comment = cursor.getString(COMMENT);
			rating = cursor.getDouble(RATING);
			lastModifiedDateTime = cursor.getString(LAST_MODIFIED);
			updated = cursor.getLong(UPDATED);
			priceCurrency = cursor.getString(PRIVATE_INFO_PRICE_PAID_CURRENCY);
			price = cursor.getDouble(PRIVATE_INFO_PRICE_PAID);
			currentValueCurrency = cursor.getString(PRIVATE_INFO_CURRENT_VALUE_CURRENCY);
			currentValue = cursor.getDouble(PRIVATE_INFO_CURRENT_VALUE);
			quantity = cursor.getInt(PRIVATE_INFO_QUANTITY);
			privateComment = cursor.getString(PRIVATE_INFO_COMMENT);
			acquiredFrom = cursor.getString(PRIVATE_INFO_ACQUIRED_FROM);
			acquisitionDate = CursorUtils.getFormattedDate(cursor, getActivity(), PRIVATE_INFO_ACQUISITION_DATE);
			imageUrl = cursor.getString(COLLECTION_IMAGE_URL);
			year = cursor.getInt(COLLECTION_YEAR_PUBLISHED);
			wishlistPriority = cursor.getInt(STATUS_WISHLIST_PRIORITY);
			wishlistComment = cursor.getString(WISHLIST_COMMENT);
			condition = cursor.getString(CONDITION);
			wantParts = cursor.getString(WANT_PARTS_LIST);
			hasParts = cursor.getString(HAS_PARTS_LIST);
			numPlays = cursor.getInt(NUM_PLAYS);

			mStatus = new ArrayList<>();
			for (int i = STATUS_OWN; i <= STATUS_PREORDERED; i++) {
				if (cursor.getInt(i) == 1) {
					if (i == STATUS_WISHLIST) {
						mStatus.add(getWishlistPriority());
					} else {
						mStatus.add(r.getStringArray(R.array.collection_status_filter_entries)[i - STATUS_OWN]);
					}
				}
			}
		}

		String getStatus() {
			String status = StringUtils.formatList(mStatus);
			if (TextUtils.isEmpty(status)) {
				if (numPlays > 0) {
					return r.getString(R.string.played);
				}
				return r.getString(R.string.invalid_collection_status);
			}
			return status;
		}

		String getRatingDescription() {
			return PresentationUtils.describeRating(getActivity(), rating);
		}

		String getYearDescription() {
			return PresentationUtils.describeYear(getActivity(), year);
		}

		String getWishlistPriority() {
			return PresentationUtils.describeWishlist(getActivity(), wishlistPriority);
		}

		String getPrice() {
			return formatCurrency(priceCurrency) + currencyFormat.format(price);
		}

		String getValue() {
			return formatCurrency(currentValueCurrency) + currencyFormat.format(currentValue);
		}

		boolean hasPrivateInfo() {
			return hasQuantity() || hasAcquisitionDate() || hasAcquiredFrom() || hasPrice() || hasValue();
		}

		boolean hasQuantity() {
			return quantity > 1;
		}

		boolean hasAcquisitionDate() {
			return !TextUtils.isEmpty(acquisitionDate);
		}

		boolean hasAcquiredFrom() {
			return !TextUtils.isEmpty(acquiredFrom);
		}

		boolean hasPrice() {
			return price > 0.0;
		}

		boolean hasValue() {
			return currentValue > 0.0;
		}

		boolean hasPrivateComment() {
			return !TextUtils.isEmpty(privateComment);
		}

		CharSequence getPrivateInfo() {
			String initialText = r.getString(R.string.acquired);
			SpannableStringBuilder sb = new SpannableStringBuilder();
			sb.append(initialText);
			if (hasQuantity()) {
				sb.append(" ");
				StringUtils.appendBold(sb, String.valueOf(quantity));
			}
			if (hasAcquisitionDate()) {
				sb.append(" ").append(r.getString(R.string.on)).append(" ");
				StringUtils.appendBold(sb, acquisitionDate);
			}
			if (hasAcquiredFrom()) {
				sb.append(" ").append(r.getString(R.string.from)).append(" ");
				StringUtils.appendBold(sb, acquiredFrom);
			}
			if (hasPrice()) {
				sb.append(" ").append(r.getString(R.string.for_)).append(" ");
				StringUtils.appendBold(sb, getPrice());
			}
			if (hasValue()) {
				sb.append(" (").append(r.getString(R.string.currently_worth)).append(" ");
				StringUtils.appendBold(sb, getValue());
				sb.append(")");
			}

			if (sb.toString().equals(initialText)) {
				// shouldn't happen
				return null;
			}
			return sb;

		}

		private String formatCurrency(String currency) {
			if (currency == null) {
				return "$";
			}
			switch (currency) {
				case "USD":
				case "CAD":
				case "AUD":
					return "$";
				case "EUR":
					return "\u20AC";
				case "GBP":
					return "\u00A3";
				case "YEN":
					return "\u00A5";
			}
			return "";
		}
	}
}
