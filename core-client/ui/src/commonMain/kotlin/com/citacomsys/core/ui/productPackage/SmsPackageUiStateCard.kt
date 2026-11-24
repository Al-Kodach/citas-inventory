package com.citacomsys.core.ui.productPackage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.citacomsys.api.platform.uiModel.SmsPackageUiState
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons

@Composable
fun SmsPackageUiStateCard(
    pkg: SmsPackageUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine the active display price
    val displayPrice =
        if (pkg.hasPromo && pkg.promoPrice != null) pkg.promoPrice else pkg.originalPrice

    AppCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Promo / Urgency Badge
            if (pkg.hasPromo) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = pkg.savings ?: "PROMO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Units Quantity
                Text(
                    text = pkg.units.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "SMS CREDITS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(20.dp))

                // Pricing Area
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (pkg.hasPromo && pkg.promoPrice != null) {
                        // Strikethrough Original
                        Text(
                            text = pkg.originalPrice,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = displayPrice!!,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (pkg.hasPromo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Urgency / Remaining Days
                if (pkg.hasPromo && pkg.remainingDaysToEnd > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Ends in ${pkg.remainingDaysToEnd} days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Effects (e.g., "+ 50 Extra Units")
                pkg.effects.forEach { effect ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = effect.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32), // Success Green
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}