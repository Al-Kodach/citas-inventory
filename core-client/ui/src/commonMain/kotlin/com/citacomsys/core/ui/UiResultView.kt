package com.citacomsys.core.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.citacomsys.core.components.LoadingView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.core.ui.result.UiResult
import kotlinx.coroutines.launch

@Composable
fun <T> UiResultView(
    state: UiResult<T>,
    modifier: Modifier = Modifier,
    loadingView: (@Composable () -> Unit)? = null,
    errorView: (@Composable () -> Unit)? = null,
    idleView: (@Composable () -> Unit)? = null,
    onRetry: (suspend () -> Unit)? = null,
    content: @Composable (T) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        when (state) {
            UiResult.Idle -> {
                idleView?.invoke() ?: Box(modifier = Modifier.fillMaxSize())
            }

            UiResult.Loading -> {
                loadingView?.invoke() ?: LoadingView("Loading...")
            }

            is UiResult.Error -> {
                errorView?.invoke() ?: DetailsPlaceholder(
                    icon = AppIcons.Error,
                    title = "Operation Failed",
                    description = state.message,
                    isError = true,
                    actionContent = {
                        if (state.canRetry && onRetry != null) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { scope.launch { onRetry.invoke() } }) {
                                Text("Try Again")
                            }
                        }
                    }
                )
            }

            is UiResult.Success -> {
                content(state.data)
            }
        }
    }
}
