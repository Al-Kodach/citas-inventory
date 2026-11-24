package com.citacomsys.api.sales.dto

import kotlinx.serialization.Serializable

@Serializable
sealed interface SaleResult {
    /**
     * The sale was successfully created.
     * @param saleId The ID of the newly created sale record on the server.
     */
    @Serializable
    data class Success(val saleId: String) : SaleResult

    /**
     * The sale was not created because the provided quote has expired.
     * The client should request a new salePreview/quote.
     */
    @Serializable
    data object QuoteExpired : SaleResult

    /**
     * The sale was not created because stock levels or prices have changed.
     * The client should request a new salePreview/quote.
     */
    @Serializable
    data object QuoteMismatch : SaleResult

    /**
     * The sale was not created because of a security or data ownership mismatch.
     * This is a more serious error that may require logging the user out.
     */
    @Serializable
    data object InvalidContext : SaleResult

    /**
     * The sale was not created due to insufficient stock for one or more items.
     * The payload contains details about which items are out of stock.
     */
    @Serializable
    data class InsufficientStock(val details: Map<String, Int>) :
        SaleResult // productId -> availableQty

    val isSuccess: Boolean
        get() = this is Success
}