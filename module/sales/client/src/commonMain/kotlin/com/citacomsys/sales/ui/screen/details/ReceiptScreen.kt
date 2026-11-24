package com.citacomsys.sales.ui.screen.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.sales.ui.screen.share.ReceiptPreviewContent
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    saleId: String,
    onBack: () -> Unit,
    viewModel: SaleDetailsViewModel = koinViewModel { parametersOf(saleId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt Preview") },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (state.receiptOptState is UiResult.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            ReceiptPreviewContent(
                pdfPreview = state.receiptPreview,
                operationState = state.receiptOptState,
                onPrint = { viewModel.print() },
                onDownload = { viewModel.savePdf() },
            )
        }
    }
}
