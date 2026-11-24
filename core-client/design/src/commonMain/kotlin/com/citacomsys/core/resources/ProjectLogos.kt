package com.citacomsys.core.resources

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core_client_design.design.generated.resources.Res
import com.citacomsys.core_client_design.design.generated.resources.ic_citas_logo
import com.citacomsys.types.ProductType
import com.design.AppTheme
import org.jetbrains.compose.resources.painterResource

/**
 * Centralized Logo & Emblem Registry for Citas Inventory SaaS.
 * Provides project logo components and product-specific dashboard emblems that dynamically adapt to the active business theme.
 */
object ProjectLogos {

    /**
     * Primary Citas SaaS Platform Logo with optional wordmark, dynamically themed with [MaterialTheme.colorScheme].
     */
    @Composable
    fun CitasProjectLogo(
        modifier: Modifier = Modifier,
        size: Dp = 40.dp,
        showWordmark: Boolean = true,
        tint: Color? = null
    ) {
        val effectiveTint = tint ?: MaterialTheme.colorScheme.primary
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_citas_logo),
                    contentDescription = "Citas Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showWordmark) {
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CITAS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "INVENTORY PLATFORM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = effectiveTint
                    )
                }
            }
        }
    }

    /**
     * Product-specific Dashboard Logo Emblem tailored for each [ProductType], dynamically styled with theme colors.
     */
    @Composable
    fun ProductDashboardLogo(
        title: String? = null,
        type: ProductType,
        modifier: Modifier = Modifier,
        size: Dp = 44.dp,
        showTitle: Boolean = true,
        tint: Color? = null
    ) {
        val config = getProductLogoConfig(type)
        val iconTint = tint ?: config.iconTint

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(14.dp))
                    .background(config.backgroundGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = config.title,
                    tint = iconTint,
                    modifier = Modifier.size(size * 0.55f)
                )
            }

            if (showTitle) {
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title ?: config.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = config.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    fun ProductIcon(
        type: String,
        tint: Color = MaterialTheme.colorScheme.primary,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        val productType = ProductType.safeValueOf(type)
        val config = getProductLogoConfig(productType)
        Icon(
            imageVector = config.icon,
            contentDescription = config.title,
            tint = tint,
            modifier = modifier.size(size)
        )
    }

    @Composable
    fun ProductLogo(
        type: ProductType,
        tint: Color? = null,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp
    ) {
        val config = getProductLogoConfig(type)
        Icon(
            imageVector = config.icon,
            contentDescription = config.title,
            tint = tint ?: config.iconTint,
            modifier = modifier.size(size)
        )
    }

    /**
     * Resolves and renders the appropriate logo based on operating mode and active product type.
     */
    @Composable
    fun AppLogo(
        isWsMode: Boolean = false,
        productType: ProductType? = null,
        tint: Color? = null,
        modifier: Modifier = Modifier,
        size: Dp = 40.dp,
        showTitle: Boolean = true
    ) {
        if (isWsMode && productType != null) {
            ProductDashboardLogo(
                type = productType,
                tint = tint,
                modifier = modifier,
                size = size,
                showTitle = showTitle
            )
        } else {
            CitasProjectLogo(
                tint = tint,
                modifier = modifier,
                size = size,
                showWordmark = showTitle
            )
        }
    }

    data class ProductLogoConfig(
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val backgroundGradient: Brush,
        val iconTint: Color
    )

    @Composable
    fun getProductIcon(type: ProductType): ImageVector {
        return when (type) {
            ProductType.PHARMACY -> AppIcons.Medication
            ProductType.RETAIL -> AppIcons.Storefront
            ProductType.RESTAURANT -> AppIcons.Category
            ProductType.AUTOMOTIVE -> AppIcons.HomeRepairService
            ProductType.AGRICULTURE -> AppIcons.Science
            ProductType.INVENTORY -> AppIcons.Inventory2
        }
    }

    @Composable
    fun getProductLogoConfig(type: ProductType): ProductLogoConfig {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val onPrimary = MaterialTheme.colorScheme.onPrimary

        return when (type) {
            ProductType.PHARMACY -> ProductLogoConfig(
                title = "Pharmacy & Healthcare",
                subtitle = "Dispensing & Rx Inventory",
                icon = AppIcons.Medication,
                backgroundGradient = Brush.linearGradient(listOf(primary, secondary)),
                iconTint = onPrimary
            )

            ProductType.RETAIL -> ProductLogoConfig(
                title = "Smart Retail POS",
                subtitle = "Storefront & Multi-Checkout",
                icon = AppIcons.Storefront,
                backgroundGradient = Brush.linearGradient(listOf(primary, secondary)),
                iconTint = onPrimary
            )

            ProductType.RESTAURANT -> ProductLogoConfig(
                title = "Dining & Hospitality",
                subtitle = "Kitchen & POS Operations",
                icon = AppIcons.Category,
                backgroundGradient = Brush.linearGradient(listOf(secondary, tertiary)),
                iconTint = onPrimary
            )

            ProductType.AUTOMOTIVE -> ProductLogoConfig(
                title = "Auto & Spare Parts",
                subtitle = "Part Tracking & Service Hub",
                icon = AppIcons.HomeRepairService,
                backgroundGradient = Brush.linearGradient(listOf(primary, tertiary)),
                iconTint = onPrimary
            )

            ProductType.AGRICULTURE -> ProductLogoConfig(
                title = "Agribusiness & Farm",
                subtitle = "Crop Inputs & Farm Depot",
                icon = AppIcons.Science,
                backgroundGradient = Brush.linearGradient(listOf(primary, secondary)),
                iconTint = onPrimary
            )

            ProductType.INVENTORY -> ProductLogoConfig(
                title = "Enterprise Inventory",
                subtitle = "Central Distribution & SIEM",
                icon = AppIcons.Inventory2,
                backgroundGradient = Brush.linearGradient(listOf(primary, tertiary)),
                iconTint = onPrimary
            )
        }
    }
}

@AllAdaptivePreviews
@Composable
private fun ProjectLogosPreview() {
    AppTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Project Logo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ProjectLogos.CitasProjectLogo(size = 48.dp)

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Product Dashboard Logos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ProductType.entries.forEach { productType ->
                ProjectLogos.ProductDashboardLogo(
                    type = productType,
                    size = 44.dp
                )
            }
        }
    }
}
