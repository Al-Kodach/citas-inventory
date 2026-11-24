package com.citacomsys.retail.shared.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.ui.InfoTag
import com.citacomsys.core.ui.ProductExtensionViewProvider
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ProductType
import io.github.aakira.napier.Napier

class RetailProductRenderer : ProductExtensionViewProvider {
    override val type: ProductType = ProductType.RETAIL

    @Composable
    override fun RenderExtensionDetails(product: BaseProduct) {
        val retailProduct = product as? RetailProduct ?: return
        if (product.type != ProductType.RETAIL) {
            Napier.e("Unsupported product type: ${product.type}")
            return
        }

        if (retailProduct.tags.isEmpty() && retailProduct.attributes.isEmpty()) return

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Product Specifications",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                retailProduct.tags.forEach { tag ->
                    InfoTag(label = "Tag", value = tag)
                }

                retailProduct.attributes.forEach { (key, value) ->
                    InfoTag(label = key, value = value, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }

    @Composable
    override fun RenderExtensionForm(viewModel: Any) {
        // Retail products currently use only base fields. 
        // We can add dynamic attribute support here later.
    }
}
