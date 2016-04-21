package com.boardgamegeek.service;

import android.content.ContentValues;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.CollectionItem;

import java.text.DecimalFormat;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;

public class CollectionPrivateInfoUploadTask extends CollectionUploadTask {
	private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#.00");
	private String comment;

	public CollectionPrivateInfoUploadTask(OkHttpClient client) {
		super(client);
	}

	@Override
	public String getTimestampColumn() {
		return Collection.PRIVATE_INFO_DIRTY_TIMESTAMP;
	}

	@Override
	public boolean isDirty() {
		return collectionItem.getPrivateInfoTimestamp() > 0;
	}

	@Override
	protected FormBody createForm(CollectionItem collectionItem) {
		return createFormBuilder()
			.add("fieldname", "ownership")
			.add("pp_currency", collectionItem.getPricePaidCurrency())
			.add("pricepaid", CURRENCY_FORMAT.format(collectionItem.getPricePaid()))
			.add("cv_currency", collectionItem.getCurrentValueCurrency())
			.add("currvalue", CURRENCY_FORMAT.format(collectionItem.getCurrentValue())) // TODO handle empty string (not the same as 0)
			.add("quantity", String.valueOf(collectionItem.getQuantity()))
			.add("acquisitiondate", collectionItem.getAcquisitionDate()) // TODO ensure YYYY-MM-DD
			.add("acquiredfrom", collectionItem.getAcquiredFrom())
			.add("privatecomment", collectionItem.getPrivateComment())
			.build();
	}

	@Override
	protected void saveContent(String content) {
		comment = content;
	}

//	<table cellspacing=1 cellpadding=1 width='100%' class='collectiontable_ownership'>
//
//
//	<tr>
//	<td nowrap width='100'>Quantity:</td>
//	<td nowrap>1</td>
//	</tr>
//
//
//
//	<tr>
//	<td width='100'>Comments:</td>
//	<td>Privenn</td>
//	</tr>
//
//
//
//	</table>

	@Override
	public void appendContentValues(ContentValues contentValues) {
		// TODO
		contentValues.put(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, 0);
	}
}
