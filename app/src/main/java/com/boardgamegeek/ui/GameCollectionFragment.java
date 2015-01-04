package com.boardgamegeek.ui;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class GameCollectionFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;

	private int mGameId = BggContract.INVALID_ID;
	private CursorAdapter mAdapter;
	private boolean mMightNeedRefreshing;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
		listView.setDivider(new ColorDrawable(getResources().getColor(R.color.accent)));
		listView.setDividerHeight(getResources().getDimensionPixelSize(R.dimen.padding_standard));
		listView.setFooterDividersEnabled(false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_game_collection));
		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
		if (uri != null && Games.isGameUri(uri)) {
			mMightNeedRefreshing = true;
			mGameId = Games.getGameId(uri);
			getLoaderManager().restartLoader(CollectionItem._TOKEN, getArguments(), this);
		} else {
			setListShown(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			triggerRefresh();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		if (id != CollectionItem._TOKEN) {
			return null;
		}
		// TODO: don't use table name directly
		return new CursorLoader(getActivity(), Collection.CONTENT_URI, new CollectionItem().PROJECTION, "collection."
			+ Collection.GAME_ID + "=?", new String[] { String.valueOf(mGameId) }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new CollectionAdapter(getActivity());
			setListAdapter(mAdapter);
		}

		if (cursor == null || !cursor.moveToFirst()) {
			if (mMightNeedRefreshing) {
				triggerRefresh();
			}
			return;
		}

		int token = loader.getId();
		if (token == CollectionItem._TOKEN) {
			mAdapter.changeCursor(cursor);
			if (mMightNeedRefreshing) {
				cursor.moveToFirst();
				do {
					long u = cursor.getLong(new CollectionItem().UPDATED);
					if (DateTimeUtils.howManyDaysOld(u) > AGE_IN_DAYS_TO_REFRESH) {
						triggerRefresh();
						break;
					}
				} while (cursor.moveToNext());
				cursor.moveToPosition(-1);
			}
		} else {
			Timber.d("Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}

		mMightNeedRefreshing = false;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (mAdapter != null) {
			mAdapter.changeCursor(null);
		}
	}

	private void triggerRefresh() {
		mMightNeedRefreshing = false;
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_COLLECTION, mGameId, null);
	}

	private class CollectionAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public CollectionAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_game_collection, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			CollectionItem item = new CollectionItem(cursor);

			ActivityUtils.safelyLoadImage(holder.image, item.imageUrl);
			holder.name.setText(item.name.trim());
			holder.year.setText(item.getYearDescription());
			holder.lastModified.setText(item.getLastModifiedDescription());
			holder.rating.setText(item.getRatingDescription());
			ColorUtils.setTextViewBackground(holder.rating, ColorUtils.getRatingColor(item.rating));
			holder.id.setText(String.valueOf(item.id));
			holder.id.setVisibility(item.id == 0 ? View.INVISIBLE : View.VISIBLE);
			holder.updated.setText(item.getUpdatedDescription());

			holder.status.setText(item.getStatus());
			holder.comment.setVisibility(TextUtils.isEmpty(item.comment) ? View.GONE : View.VISIBLE);
			holder.comment.setText(item.comment);

			// Private info
			if (item.hasPrivateInfo() || item.hasPrivateComment()) {
				holder.privateInfoRoot.setVisibility(View.VISIBLE);
				holder.privateInfo.setVisibility(item.hasPrivateInfo() ? View.VISIBLE : View.GONE);
				holder.privateInfo.setText(item.getPrivateInfo());
				holder.privateComments.setVisibility(item.hasPrivateComment() ? View.VISIBLE : View.GONE);
				holder.privateComments.setText(item.privateComment);
			} else {
				holder.privateInfoRoot.setVisibility(View.GONE);
			}

			holder.wishlistRoot.setVisibility(TextUtils.isEmpty(item.wishlistComment) ? View.GONE : View.VISIBLE);
			holder.wishlistContent.setText(item.wishlistComment);
			holder.conditionRoot.setVisibility(TextUtils.isEmpty(item.condition) ? View.GONE : View.VISIBLE);
			holder.conditionContent.setText(item.condition);
			holder.wantPartsRoot.setVisibility(TextUtils.isEmpty(item.wantParts) ? View.GONE : View.VISIBLE);
			holder.wantPartsContent.setText(item.wantParts);
			holder.hasPartsRoot.setVisibility(TextUtils.isEmpty(item.hasParts) ? View.GONE : View.VISIBLE);
			holder.hasPartsContent.setText(item.hasParts);

			holder.image.setTag(R.id.image, item.imageUrl);
			holder.image.setTag(R.id.name, item.name);
		}
	}

	static class ViewHolder {
		@InjectView(R.id.image) ImageView image;
		@InjectView(R.id.name) TextView name;
		@InjectView(R.id.rating) TextView rating;
		@InjectView(R.id.collection_id) TextView id;
		@InjectView(R.id.last_modified) TextView lastModified;
		@InjectView(R.id.updated) TextView updated;
		@InjectView(R.id.year) TextView year;
		@InjectView(R.id.status) TextView status;
		@InjectView(R.id.comment) TextView comment;
		@InjectView(R.id.private_info_root) View privateInfoRoot;
		@InjectView(R.id.private_info) TextView privateInfo;
		@InjectView(R.id.private_comments) TextView privateComments;
		@InjectView(R.id.wishlist_root) View wishlistRoot;
		@InjectView(R.id.wishlist_content) TextView wishlistContent;
		@InjectView(R.id.condition_root) View conditionRoot;
		@InjectView(R.id.condition_content) TextView conditionContent;
		@InjectView(R.id.want_parts_root) View wantPartsRoot;
		@InjectView(R.id.want_parts_content) TextView wantPartsContent;
		@InjectView(R.id.has_parts_root) View hasPartsRoot;
		@InjectView(R.id.has_parts_content) TextView hasPartsContent;

		public ViewHolder(View view) {
			ButterKnife.inject(this, view);
		}
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
		int HASPARTS_LIST = 18;
		int WANTPARTS_LIST = 19;
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

		DecimalFormat currencyFormat = new DecimalFormat("#0.00");

		Resources r;
		int id;
		String name;
		// String sortName;
		String comment;
		private String lastModifiedDateTime;
		private double rating;
		private long lastModified;
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
			if (!TextUtils.isEmpty(lastModifiedDateTime) && TextUtils.isDigitsOnly(lastModifiedDateTime)) {
				this.lastModified = Long.parseLong(lastModifiedDateTime);
				lastModifiedDateTime = null;
			}
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
			wantParts = cursor.getString(WANTPARTS_LIST);
			hasParts = cursor.getString(HASPARTS_LIST);
			numPlays = cursor.getInt(NUM_PLAYS);

			mStatus = new ArrayList<String>();
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

		CharSequence getLastModifiedDescription() {
			String s = r.getString(R.string.last_modified) + " ";
			if (TextUtils.isEmpty(lastModifiedDateTime)) {
				return s + DateUtils.getRelativeTimeSpanString(lastModified);
			} else if (String.valueOf(DateTimeUtils.UNKNOWN_DATE).equals(lastModifiedDateTime)) {
				return ""; // probably not in the collection at all
			}
			return s + lastModifiedDateTime;
		}

		CharSequence getUpdatedDescription() {
			if (updated == 0) {
				return r.getString(R.string.needs_updating);
			}
			return r.getString(R.string.updated) + ": " + DateUtils.getRelativeTimeSpanString(updated);
		}

		String getRatingDescription() {
			if (rating > 0.0) {
				return new DecimalFormat("#0.00").format(rating);
			} else {
				return getString(R.string.unrated);
			}
		}

		String getYearDescription() {
			if (year > 0) {
				return getString(R.string.year_positive, year);
			} else if (year == 0) {
				return getString(R.string.year_zero, year);
			} else {
				return getString(R.string.year_negative, -year);
			}
		}

		String getWishlistPriority() {
			int i = wishlistPriority;
			if (wishlistPriority < 0 || wishlistPriority > 5) {
				i = 0;
			}
			return r.getStringArray(R.array.wishlist_priority)[i];
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
			String intialText = r.getString(R.string.acquired);
			SpannableStringBuilder sb = new SpannableStringBuilder();
			sb.append(intialText);
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

			if (sb.toString().equals(intialText)) {
				// shouldn't happen
				return null;
			}
			return sb;

		}

		private String formatCurrency(String currency) {
			if ("USD".equals(currency) || "CAD".equals(currency) || "AUD".equals(currency)) {
				return "$";
			} else if ("EUR".equals(currency)) {
				return "\u20AC";
			} else if ("GBP".equals(currency)) {
				return "\u00A3";
			} else if ("YEN".equals(currency)) {
				return "\u00A5";
			}
			return "";
		}
	}
}
