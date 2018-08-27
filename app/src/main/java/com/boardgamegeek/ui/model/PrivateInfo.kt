package com.boardgamegeek.ui.model

data class PrivateInfo(
        val priceCurrency: String? = null,
        val price: Double = 0.toDouble(),
        val currentValueCurrency: String? = null,
        val currentValue: Double = 0.toDouble(),
        val quantity: Int = 1,
        val acquiredFrom: String? = null,
        val acquisitionDate: String? = null,
        val inventoryLocation: String? = null
)