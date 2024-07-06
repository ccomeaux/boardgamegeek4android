package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class CollectionPrivateInfoEntity(
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "price_paid_currency")
    val privateInfoPricePaidCurrency: String?,
    @ColumnInfo(name = "price_paid")
    val privateInfoPricePaid: Double?,
    @ColumnInfo(name = "current_value_currency")
    val privateInfoCurrentValueCurrency: String?,
    @ColumnInfo(name = "current_value")
    val privateInfoCurrentValue: Double?,
    @ColumnInfo(name = "quantity")
    val privateInfoQuantity: Int?,
    @ColumnInfo(name = "acquisition_date")
    val privateInfoAcquisitionDate: String?,
    @ColumnInfo(name = "acquired_from")
    val privateInfoAcquiredFrom: String?,
    @ColumnInfo("inventory_location")
    val privateInfoInventoryLocation: String?,
    @ColumnInfo("private_info_dirty_timestamp")
    val privateInfoDirtyTimestamp: Long?,
)
