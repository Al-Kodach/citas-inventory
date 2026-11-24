package com.citacomsys.core.branding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.types.BrandingConfig
import com.materialkolor.PaletteStyle
import kotlin.time.Clock

enum class BrandingStrategy {
    INHERIT_ORG,
    PLATFORM_DEFAULT,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandingConfigScreen(
    modifier: Modifier = Modifier,
    currentAppName: String = "",
    currentBranding: BrandingConfig?,
    allowInheritOption: Boolean = false,
    initialStrategy: BrandingStrategy? = null,
    otherOptions: List<BrandingOptionConfig> = emptyList(),
    onStrategySelected: ((BrandingStrategy) -> Unit)? = null,
    onSaveStrategy: ((strategy: BrandingStrategy, config: BrandingConfig?) -> Unit)? = null,
    onSave: (BrandingConfig?) -> Unit,
) {
    val defaultStrategy = when {
        initialStrategy != null && (initialStrategy != BrandingStrategy.INHERIT_ORG || allowInheritOption) -> initialStrategy
        currentBranding != null -> BrandingStrategy.CUSTOM
        allowInheritOption -> BrandingStrategy.INHERIT_ORG
        else -> BrandingStrategy.PLATFORM_DEFAULT
    }

    var selectedStrategy by remember(currentBranding, initialStrategy, allowInheritOption) {
        mutableStateOf(defaultStrategy)
    }

    var appName by remember(currentBranding, currentAppName) {
        mutableStateOf(currentBranding?.appName ?: currentAppName)
    }

    var primaryColor by remember(currentBranding) {
        mutableStateOf(currentBranding?.primaryColor?.let { Color(it.toInt()) })
    }
    var accentColor by remember(currentBranding) {
        mutableStateOf(currentBranding?.accentColor?.let { Color(it.toInt()) })
    }
    var tertiaryColor by remember(currentBranding) {
        mutableStateOf(currentBranding?.tertiaryColor?.let { Color(it.toInt()) })
    }
    var errorColor by remember(currentBranding) {
        mutableStateOf(currentBranding?.errorColor?.let { Color(it.toInt()) })
    }

    var selectedPaletteStyle by remember(currentBranding) {
        mutableStateOf(
            currentBranding?.paletteStyle?.let { name ->
                runCatching { PaletteStyle.valueOf(name) }.getOrNull()
            } ?: PaletteStyle.Vibrant
        )
    }

    val darkState = remember(currentBranding) {
        mutableStateOf(currentBranding?.defaultDarkMode ?: false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Personalize your display name and branding theme. You can change these anytime in settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        //  Display Name Input
        OutlinedTextField(
            value = appName,
            onValueChange = { appName = it },
            label = { Text("Business / Company Display Name") },
            placeholder = { Text(currentAppName.ifBlank { "e.g. HealthFirst Enterprise" }) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )

        Text(
            text = "Theme Strategy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Strategy Selector Cards
        if (allowInheritOption) {
            BrandingOptionCard(
                title = "Inherit from Organization",
                subtitle = "Use the same colors and logo as the parent company",
                icon = Icons.Default.Business,
                isSelected = selectedStrategy == BrandingStrategy.INHERIT_ORG,
                onClick = {
                    selectedStrategy = BrandingStrategy.INHERIT_ORG
                    onStrategySelected?.invoke(BrandingStrategy.INHERIT_ORG)
                }
            )
        }

        BrandingOptionCard(
            title = "Platform Default Theme",
            subtitle = "Use standard platform colors (Deep Teal & Clean UI)",
            icon = Icons.Default.SettingsSuggest,
            isSelected = selectedStrategy == BrandingStrategy.PLATFORM_DEFAULT,
            onClick = {
                selectedStrategy = BrandingStrategy.PLATFORM_DEFAULT
                onStrategySelected?.invoke(BrandingStrategy.PLATFORM_DEFAULT)
            }
        )

        BrandingOptionCard(
            title = "Custom Brand Palette",
            subtitle = "Choose custom brand archetypes and custom primary colors",
            icon = Icons.Default.Palette,
            isSelected = selectedStrategy == BrandingStrategy.CUSTOM,
            onClick = {
                selectedStrategy = BrandingStrategy.CUSTOM
                onStrategySelected?.invoke(BrandingStrategy.CUSTOM)
            }
        )

        if (otherOptions.isNotEmpty()) {
            otherOptions.forEach { opt ->
                BrandingOptionCard(
                    title = opt.title,
                    subtitle = opt.subtitle,
                    icon = opt.icon,
                    isSelected = opt.isSelected,
                    onClick = opt.onClick
                )
            }
        }

        // Custom Branding Tool (Animated visibility when custom palette is selected)
        AnimatedVisibility(
            visible = selectedStrategy == BrandingStrategy.CUSTOM,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                val effectivePrimary = primaryColor ?: MaterialTheme.colorScheme.primary
                val effectiveAccent = accentColor ?: MaterialTheme.colorScheme.secondary
                val effectiveTertiary = tertiaryColor ?: MaterialTheme.colorScheme.tertiary
                val effectiveError = errorColor ?: MaterialTheme.colorScheme.error
                val effectiveDisplayName = appName.ifBlank { currentAppName }

                BrandingTool(
                    appName = effectiveDisplayName,
                    primaryColor = effectivePrimary,
                    accentColor = effectiveAccent,
                    tertiaryColor = effectiveTertiary,
                    errorColor = effectiveError,
                    selectedStyle = selectedPaletteStyle,
                    onPrimaryChange = { primaryColor = it },
                    onAccentChange = { accentColor = it },
                    onTertiaryChange = { tertiaryColor = it },
                    onErrorChange = { errorColor = it },
                    onStyleChange = { selectedPaletteStyle = it },
                    darkState = darkState
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val defaultPrimary = MaterialTheme.colorScheme.primary

        // 4. Action Button
        Button(
            onClick = {
                val enteredAppName = appName.trim().ifBlank { currentAppName }
                val selectedPrimary = primaryColor ?: defaultPrimary

                when (selectedStrategy) {
                    BrandingStrategy.CUSTOM -> {
                        val matchedArchetype = BrandArchetypes.All.find { it.primary == selectedPrimary }
                        val effectiveAccent = accentColor ?: matchedArchetype?.secondary ?: selectedPrimary
                        val effectiveTertiary = tertiaryColor ?: matchedArchetype?.tertiary ?: selectedPrimary
                        val effectiveError = errorColor ?: matchedArchetype?.error ?: Color(0xFFD32F2F)

                        val config = BrandingConfig(
                            appName = enteredAppName,
                            primaryColor = selectedPrimary.toArgb().toLong() and 0xFFFFFFFFL,
                            accentColor = effectiveAccent.toArgb().toLong() and 0xFFFFFFFFL,
                            tertiaryColor = effectiveTertiary.toArgb().toLong() and 0xFFFFFFFFL,
                            errorColor = effectiveError.toArgb().toLong() and 0xFFFFFFFFL,
                            defaultDarkMode = darkState.value,
                            paletteStyle = selectedPaletteStyle.name,
                            updatedAt = Clock.System.now()
                        )
                        onSaveStrategy?.invoke(BrandingStrategy.CUSTOM, config)
                        onSave(config)
                    }

                    BrandingStrategy.INHERIT_ORG -> {
                        onSaveStrategy?.invoke(BrandingStrategy.INHERIT_ORG, null)
                        onSave(null)
                    }

                    BrandingStrategy.PLATFORM_DEFAULT -> {
                        onSaveStrategy?.invoke(BrandingStrategy.PLATFORM_DEFAULT, null)
                        onSave(null)
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                "Save",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class BrandingOptionConfig(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun BrandingOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    }
}
