package com.citacomsys.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.LocalDefaultPadding
import com.citacomsys.core.resources.LocalMediumPadding
import com.citacomsys.core.resources.LocalSmallPadding

@Composable
fun HeaderText(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    start = LocalSmallPadding.current.size,
                    end = LocalSmallPadding.current.size,
                    top = LocalMediumPadding.current.size,
                ),
    )
}

@Composable
fun PrimaryText(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .wrapContentSize()
                .padding(
                    start = LocalSmallPadding.current.size,
                    end = LocalSmallPadding.current.size,
                    top = LocalDefaultPadding.current.size,
                ),
    )
}

@Composable
fun SecondaryText(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .wrapContentSize()
                .padding(
                    start = LocalSmallPadding.current.size,
                    end = LocalSmallPadding.current.size,
                    top = LocalDefaultPadding.current.size,
                ),
    )
}

@Composable
fun TextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    errorMessage: String? = null,
) {
    var showPassword by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it) },
        label = { Text(text = label, color = MaterialTheme.colorScheme.onBackground) },
        shape = RoundedCornerShape(LocalMediumPadding.current.size),
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.Gray,
            ),
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = keyboardType
        ),
        trailingIcon = {
            IconButton(
                onClick = { showPassword = !showPassword }
            ) {
                if (isPassword && showPassword) {
                    Icon(AppIcons.Visibility, "Hide")
                } else {
                    Icon(AppIcons.VisibilityOff, "show")
                }
            }
        },
        supportingText = {
            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        },
        keyboardActions = KeyboardActions(
            onNext = {
                keyboardController?.hide()
            },
        ),
        visualTransformation = if (isPassword && showPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableLazyList(
    onRefresh: () -> Unit = {},
    content: LazyListScope.() -> Unit = {},
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
    ) {
        LazyListView(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(LocalMediumPadding.current.size),
        ) {
            content()
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    showRetry: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        if (showRetry) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onRetry?.invoke() }) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun LoadingView(
    message: String = "Please wait...",
    indicator: @Composable () -> Unit = {
        // Can be replace with a shimmy screen
        CircularProgressIndicator()
    }
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message)
        Spacer(Modifier.height(16.dp))
        indicator()
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun InfoRowMono(
    label: String,
    value: String
) {
    InfoRow(
        label = label,
        value = value,
        modifier = Modifier
    )
}
