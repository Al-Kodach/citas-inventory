package com.citacomsys.sales.ui.screen.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons

@Composable
internal fun SaleReceiptActions(
    handler: SaleReceiptHandler,
    saleId: String,
    onOpenSms: () -> Unit
) {
    val state by handler.state.collectAsStateWithLifecycle()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onOpenSms) {
            Icon(AppIcons.Sms, "SMS")
        }
        IconButton(onClick = { handler.print(saleId) }) {
            Icon(AppIcons.Print, "Print")
        }
        IconButton(onClick = { handler.savePdf(saleId) }) {
            Icon(AppIcons.Download, "Save")
        }
    }
}