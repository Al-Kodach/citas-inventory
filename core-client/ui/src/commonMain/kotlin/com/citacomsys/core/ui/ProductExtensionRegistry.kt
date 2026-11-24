package com.citacomsys.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.ProductType
import io.github.aakira.napier.Napier

private val TAG = "ProductExtensionViewRegistry"

interface ProductExtensionViewProvider {
    val type: ProductType
    @Composable
    fun RenderExtensionDetails(product: BaseProduct)

    @Composable
    fun RenderExtensionForm(viewModel: Any)
}

object ProductExtensionViewRegistry {

    private val providers = mutableMapOf<ProductType, ProductExtensionViewProvider>()

    fun register(
        type: ProductType,
        provider: ProductExtensionViewProvider
    ) {
        providers[type] = provider
        Napier.i(tag = TAG) { "Registered extension provider for product type: $type" }
    }

    fun unregister(type: ProductType) {
        providers.remove(type)
        Napier.i(tag = TAG) { "Unregistered extension provider for product type: $type" }
    }


    @Composable
    fun ExtensionContent(
        product: BaseProduct
    ) {
        val content = providers[product.type]
        Napier.d { "Provider type: ${content?.type}" }
        content?.RenderExtensionDetails(product)
        Napier.i(tag = TAG) { "Rendering extension content for product type: ${product.type}" }
    }

    @Composable
    fun ExtensionForm(
        type: ProductType,
        viewModel: Any
    ) {
        val form = providers[type]
        form?.RenderExtensionForm(viewModel)
        Napier.i(tag = TAG) { "Rendering extension form for product type: $type" }
    }
}


/**
 * A reusable labeled tag for displaying technical metadata.
 */
@Composable
fun InfoTag(
    label: String,
    value: String?,
    color: Color = MaterialTheme.colorScheme.secondary
) {
//    if (value.isNullOrBlank()) return

    Surface(
        color = color.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value?.uppercase() ?: "N/A",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}