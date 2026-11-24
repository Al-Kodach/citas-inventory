package com.citacomsys.sales.engine

import com.citacomsys.api.sales.dto.SalePreview
import com.citacomsys.database.PosCartEntity
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.types.Money

data class PosUiState(
    val items: List<CartItem> = emptyList(),
    val totalItems: Int = 0,
    val total: Money = Money.zero(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorDetails: Map<String, String> = emptyMap(),
    val quote: SalePreview? = null,
    val completedSaleId: String? = null,
    val canWriteSale: Boolean = true,
    val canProcessSales: Boolean = false,
    
    // Multi-cart support
    val activeCartId: String? = null,
    val activeCartName: String = "",
    val drafts: List<PosCartEntity> = emptyList(),

    // Customer integration
    val selectedCustomer: CustomerItem? = null
)

data class CartItem(
    val productId: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Int
) {
    val totalPrice: Money
        get() = unitPrice * quantity
}
