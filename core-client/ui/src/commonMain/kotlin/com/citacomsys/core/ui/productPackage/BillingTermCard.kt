package com.citacomsys.core.ui.productPackage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.api.platform.uiModel.BillTermUiState
import com.citacomsys.core.components.AppCard
import com.citacomsys.types.BillingCycle


@Composable
fun SelectableBillingTermCard(
    modifier: Modifier = Modifier,
    billingTerm: BillTermUiState,
    isSelected: Boolean,
    canSelect: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    AppCard(
        onClick = onSelect,
        modifier = modifier,
        enabled = canSelect
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // "Best Value" or "Promo" Tag
            if (billingTerm.hasPromo) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = billingTerm.campaignCode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = billingTerm.billingCycleName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = billingTerm.promoPrice ?: billingTerm.originalPrice,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    if (billingTerm.savings != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save ${billingTerm.savings}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Show a hint about the bonus
                if (billingTerm.effects.isNotEmpty()) {
                    Text(
                        text = "+ ${billingTerm.effects.first().name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}


fun BillingCycle.displayName(): String {
    return when (this) {
        BillingCycle.MONTHLY -> "Monthly"
        BillingCycle.QUARTERLY -> "Quarterly"
        BillingCycle.SEMI_ANNUAL -> "6 Months"
        BillingCycle.YEARLY -> "Yearly"
        BillingCycle.TWO_YEARLY -> "2 Years"
        BillingCycle.LIFETIME -> "Life"
    }
}
