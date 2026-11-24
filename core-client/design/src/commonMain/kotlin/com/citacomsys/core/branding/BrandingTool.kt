package com.citacomsys.core.branding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.modifiers.glassEffect
import com.materialkolor.PaletteStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandingTool(
    appName: String,
    darkState: MutableState<Boolean>,
    primaryColor: Color,
    accentColor: Color,
    tertiaryColor: Color,
    errorColor: Color,
    selectedStyle: PaletteStyle = PaletteStyle.Vibrant,
    onPrimaryChange: (Color) -> Unit,
    onAccentChange: (Color) -> Unit,
    onTertiaryChange: (Color) -> Unit,
    onErrorChange: (Color) -> Unit,
    onStyleChange: (PaletteStyle) -> Unit = {}
) {
    val activeArchetype = remember(primaryColor, accentColor, tertiaryColor, errorColor) {
        BrandArchetypes.All.find { it.primary == primaryColor }
            ?: BrandArchetype.fromColors(
                primary = primaryColor,
                secondary = accentColor,
                tertiary = tertiaryColor,
                error = errorColor
            )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Brand Personality & Style",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // 1. Archetype Selector
        Text(
            "Visual Archetype",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandArchetypes.All.forEach { archetype ->
                ArchetypeTile(
                    archetype = archetype,
                    isSelected = activeArchetype.id == archetype.id || primaryColor == archetype.primary,
                    onClick = {
                        onPrimaryChange(archetype.primary)
                        onAccentChange(archetype.secondary)
                        onTertiaryChange(archetype.tertiary)
                        onErrorChange(archetype.error)
                    }
                )
            }
        }

        // 2. Palette Style Selector (MaterialKolor Styles)
        Text(
            "Theme Palette Mood",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val styles = listOf(
                PaletteStyle.Vibrant to "Vibrant",
                PaletteStyle.Expressive to "Expressive",
                PaletteStyle.TonalSpot to "Balanced",
                PaletteStyle.Fidelity to "Fidelity",
                PaletteStyle.FruitSalad to "Fruit Salad",
                PaletteStyle.Monochrome to "Monochrome"
            )

            styles.forEach { (style, label) ->
                FilterChip(
                    selected = selectedStyle == style,
                    onClick = { onStyleChange(style) },
                    label = { Text(label, fontWeight = if (selectedStyle == style) FontWeight.Bold else FontWeight.Normal) },
                    shape = CircleShape
                )
            }
        }

        HorizontalDivider(modifier = Modifier.alpha(0.3f))

        // 3. Color Picker Presets
        Text(
            "Fine-Tune Seed Colors (${activeArchetype.name})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        ColorPickerItem(
            label = "Primary Seed Color",
            currentColor = primaryColor,
            onColorChange = { newPrimary ->
                onPrimaryChange(newPrimary)
            }
        )

        Spacer(Modifier.height(8.dp))

        ColorPickerItem(
            label = "Accent / Secondary Color",
            currentColor = accentColor,
            onColorChange = { newAccent ->
                onAccentChange(newAccent)
            }
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "Live Interactive Theme Preview",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // Dynamic Preview Card wrapped with live AppTheme & MaterialKolor PaletteStyle
        BrandingPreviewCard(
            appName = appName,
            primary = primaryColor,
            accent = accentColor,
            tertiary = tertiaryColor,
            error = errorColor,
            style = selectedStyle,
            darkState = darkState,
        )
    }
}

@Composable
private fun ArchetypeTile(
    archetype: BrandArchetype,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .glassEffect(
                    shape = MaterialTheme.shapes.medium,
                    opacity = if (isSelected) 0.8f else 0.4f,
                )
                .background(archetype.primary.copy(alpha = if (isSelected) 0.4f else 0.2f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(archetype.secondary))
                Box(Modifier.size(14.dp).clip(CircleShape).background(archetype.tertiary))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = archetype.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ColorPickerItem(label: String, currentColor: Color, onColorChange: (Color) -> Unit) {
    // Curated commercial & clinical color presets
    val presetColors = listOf(
        Color(0xFF00677D), Color(0xFF00875A), Color(0xFF36B37E), // Ocean Teal, Clinical Emerald, Bright Mint
        Color(0xFF0052CC), Color(0xFF2684FF), Color(0xFF00B8D9), // Corporate Sapphire, Sky Blue, Electric Aqua
        Color(0xFFFF5630), Color(0xFFFFAB00), Color(0xFFFF6F00), // Crimson Red, Amber Gold, Retail Orange
        Color(0xFF6554C0), Color(0xFF00C7E6), Color(0xFF172B4D)  // Electric Indigo, Cyber Cyan, Midnight Carbon
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(currentColor)
                .border(3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(Modifier.height(12.dp))

        // Preset Color Chips Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            presetColors.forEach { color ->
                PresetColorBox(color, isSelected = color == currentColor, onColorChange = onColorChange)
            }
        }
    }
}

@Composable
private fun PresetColorBox(color: Color, isSelected: Boolean = false, onColorChange: (Color) -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                if (isSelected) 3.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                CircleShape
            )
            .clickable { onColorChange(color) }
    )
}
