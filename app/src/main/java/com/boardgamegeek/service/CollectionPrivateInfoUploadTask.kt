package com.boardgamegeek.service

import android.content.ContentValues
import com.boardgamegeek.provider.BggContract.Collection
import okhttp3.FormBody
import okhttp3.OkHttpClient
import timber.log.Timber
import java.text.DecimalFormat

class CollectionPrivateInfoUploadTask(client: OkHttpClient) : CollectionUploadTask(client) {
    override val timestampColumn = Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP

    override val isDirty: Boolean
        get() = collectionItem.privateInfoTimestamp > 0

    override fun createForm(): FormBody {
        @Suppress("SpellCheckingInspection")
        return createFormBuilder()
                .add("fieldname", "ownership")
                .add("pp_currency", collectionItem.pricePaidCurrency)
                .add("pricepaid", formatCurrency(collectionItem.pricePaid))
                .add("cv_currency", collectionItem.currentValueCurrency)
                .add("currvalue", formatCurrency(collectionItem.currentValue))
                .add("quantity", collectionItem.quantity.toString())
                .add("acquisitiondate", collectionItem.acquisitionDate)
                .add("acquiredfrom", collectionItem.acquiredFrom)
                .add("privatecomment", collectionItem.privateComment)
                .add("invlocation", collectionItem.inventoryLocation)
                .build()
    }

    private fun formatCurrency(pricePaid: Double): String {
        return if (pricePaid == 0.0) {
            ""
        } else CURRENCY_FORMAT.format(pricePaid)
    }

    override fun saveContent(content: String) {
        Timber.d(content)
    }

    override fun appendContentValues(contentValues: ContentValues) {
        contentValues.put(Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP, 0)
    }

    companion object {
        private val CURRENCY_FORMAT = DecimalFormat("0.00")
    }

    // Example response body
    //	<table>
    //	<tr>
    //	<td nowrap width='100'>Quantity:</td>
    //	<td nowrap>1</td>
    //	</tr>
    //	<tr>
    //	<td width='100'>Comments:</td>
    //	<td>Private</td>
    //	</tr>
    //	</table>
}
