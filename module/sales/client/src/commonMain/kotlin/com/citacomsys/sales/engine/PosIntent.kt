package com.citacomsys.sales.engine

import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.module.api.customer.CustomerItem

sealed class PosIntent {
    data class AddProduct(val productId: String) : PosIntent()
    data class RemoveProduct(val productId: String) : PosIntent()
    data class IncreaseQty(val productId: String) : PosIntent()
    data class DecreaseQty(val productId: String) : PosIntent()
    
    /**
     * Prepares the checkout by requesting a final quote from the server.
     */
    data object Checkout : PosIntent()

    /**
     * Resets the checkout process (quote) but keeps items in the cart.
     */
    data object CancelCheckout : PosIntent()
    data object ClearState : PosIntent()

    data class ConfirmPayment(val method: PaymentMethod, val reference: String?) : PosIntent()
    data object ClearCart : PosIntent()
    data object DismissError : PosIntent()

    // Multi-cart draft support
    data object CreateNewDraft : PosIntent()
    data class SwitchDraft(val cartId: String) : PosIntent()
    data class DeleteDraft(val cartId: String) : PosIntent()
    data class UpdateDraftName(val name: String) : PosIntent()

    // Customer Integration
    data class SelectCustomer(val customer: CustomerItem?) : PosIntent()
}
