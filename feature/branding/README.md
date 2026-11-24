# README — Theme Service

## Theme Service

The Theme Service is the platform-wide visual identity engine responsible for generating a consistent, accessible, and brand-aware user experience across every application in the SaaS ecosystem.

Unlike a traditional collection of hard-coded colors, the Theme Service generates an entire Material 3 design system dynamically from a small branding configuration. Every application—including Inventory, Pharmacy, POS, Administration, CRM, and future modules—shares the same rendering engine while allowing each tenant to maintain its own visual identity.

---

# Goals

The Theme Service exists to provide:

* Tenant branding
* High contrast accessibility
* Material 3 compatibility
* Cross-platform consistency
* Dynamic light/dark themes
* Glass UI support
* Zero per-screen theme customization

Applications should never construct colors manually.

Every UI component must consume colors exclusively from the generated `ColorScheme`.

---

# Responsibilities

The Theme Service is responsible for:

* Building Material 3 ColorSchemes
* Managing tenant branding
* Dynamic theme generation
* Glass surface colors
* Surface hierarchy
* Contrast validation
* Typography pairing
* Dynamic icon colors
* Elevation colors
* Theme persistence
* Runtime theme switching

It is **not** responsible for:

* Layout
* Component rendering
* Branding assets (logos)
* Navigation

---

# Architecture

```
Tenant Branding
        │
        ▼
 BrandingConfig
        │
        ▼
 ThemeManager
        │
        ▼
 Material3 ColorScheme
        │
        ▼
 AppTheme
        │
        ▼
 Compose UI
```

---

# Branding Configuration

Every tenant owns a branding configuration.

Example

```kotlin
BrandingConfig(
    primaryColor,
    accentColor,
    tertiaryColor,
    errorColor,
    logo,
    darkTheme
)
```

Only a few values are required.

The Theme Service generates every remaining Material token automatically.

---

# Generated Color Roles

The engine generates:

* Primary

* OnPrimary

* PrimaryContainer

* OnPrimaryContainer

* Secondary

* OnSecondary

* SecondaryContainer

* OnSecondaryContainer

* Tertiary

* OnTertiary

* TertiaryContainer

* OnTertiaryContainer

* Background

* Surface

* SurfaceVariant

* Outline

* OutlineVariant

* Error

* OnError

Future additions include:

* SurfaceContainer
* SurfaceBright
* SurfaceDim
* InverseSurface
* InversePrimary

---

# Dynamic Color Generation

The engine derives colors using three operations.

## Tone Mapping

Generates lighter and darker tonal palettes.

```
Primary 40
Primary 80
Primary 90
Primary 30
```

---

## Chroma Adjustment

Controls saturation.

High chroma

```
Corporate branding
```

Low chroma

```
Neutral backgrounds
Glass surfaces
Cards
```

---

## Neutral Extraction

Every brand color generates neutral companions.

Example

```
Brand Blue

↓

Neutral Blue

↓

Background
Surface
Outline
```

This prevents white or gray interfaces from looking disconnected from the brand.

---

# Light Theme

The light theme emphasizes clarity.

Characteristics

* bright
* high contrast
* clean
* lightly tinted backgrounds
* white cards
* subtle glass

Example hierarchy

```
Background
↓

Surface

↓

Surface Variant

↓

Glass Card
```

---

# Dark Theme

Dark mode uses deep neutral tones rather than pure black.

Characteristics

* ink backgrounds
* bright content
* vibrant accents
* high readability

---

# Glass UI

Glass components use generated surface colors.

Rules

Background

↓

Glass Surface

↓

Content

↓

Accent

Glass components never invent colors.

They derive from:

```
Surface

Surface Variant

Outline

Primary
```

Opacity is handled by the component.

Color comes from the Theme Service.

---

# Accessibility

The Theme Service enforces:

* readable foreground/background combinations
* strong icon visibility
* consistent emphasis
* sufficient button contrast

Applications should avoid placing custom colors directly on text.

Instead use

```
onPrimary

onSurface

onBackground

onSecondary
```

---

# Theme Switching

Supported modes

* Light
* Dark
* System

Switching themes regenerates the entire ColorScheme.

No application restart is required.

---

# Tenant Branding

Every tenant may customize

* Primary
* Accent
* Tertiary
* Error
* Logo
* Theme mode

Example

Hospital

```
Primary

Medical Blue
```

Pharmacy

```
Primary

Clinical Green
```

Retail

```
Primary

Orange
```

Corporate

```
Primary

Indigo
```

---

# Branding Tool

The Branding Tool is the administrative interface used to configure tenant appearance.

Features

* Brand archetypes
* Color picker
* Live preview
* Dark mode preview
* Glass preview
* Theme persistence

The preview uses the same Theme Service used by production.

No preview-specific rendering exists.

---

# Theme Service Consumers

Every application consumes the same service.

Examples

```
Inventory

Sales

CRM

Administration

Accounting

Pharmacy

Reporting

POS
```

No application should define its own palette.

---

# Design Principles

The Theme Service follows these principles.

## Single Source of Truth

All colors originate from the generated ColorScheme.

---

## Brand First

Every tenant should immediately recognize their workspace.

---

## Accessible by Default

High contrast is preferred over decorative styling.

---

## Dynamic

Themes are generated, not hard coded.

---

## Consistent

Cards, dialogs, buttons, chips, and navigation all derive colors from the same source.

---

## Glass Ready

Surface colors are intentionally generated to support translucent components without manual tweaking.

---

# Extension Points

Future capabilities include:

* Seasonal branding
* Scheduled themes
* Multiple branding profiles
* High-contrast accessibility mode
* AMOLED dark theme
* Dynamic wallpaper extraction
* Department-specific branding
* Organization-wide theme templates

---

# Testing Requirements

The Theme Service should be validated using automated and manual tests.

## Functional Tests

* Theme generation succeeds for valid branding inputs.
* Missing branding falls back to the platform defaults.
* Light and dark modes generate complete `ColorScheme` instances.
* Runtime theme switching updates the UI without restarting the application.

## Accessibility Tests

* Primary text remains readable on primary backgrounds.
* Surface text remains readable on surface backgrounds.
* Interactive controls maintain sufficient visual contrast.
* Error colors remain distinguishable in both light and dark themes.

## Branding Tests

* Tenant branding is applied consistently across all applications.
* Changing one branding color regenerates dependent theme tokens correctly.
* Logos and branding assets do not affect theme generation.

## Glass UI Tests

* Glass surfaces derive colors from the generated theme rather than hard-coded values.
* Borders, overlays, and blur effects remain visually distinct in both light and dark themes.
* Transparency does not reduce content readability.

## Cross-Platform Tests

Verify consistent rendering on:

* Android
* Desktop (Compose Multiplatform)
* Future iOS targets
* Future Web targets

---

# Compliance Checklist

Before releasing a feature that depends on theming, verify:

* ✓ No hard-coded colors outside the Theme Service (except approved semantic constants such as transparent or debug colors).
* ✓ Components use `MaterialTheme.colorScheme`.
* ✓ Light and dark modes are supported.
* ✓ Tenant branding is respected.
* ✓ Accessibility contrast has been reviewed.
* ✓ Glass components derive colors from theme surfaces.
* ✓ Screens remain usable with default branding and customized branding.
* ✓ Theme changes take effect without data loss or application restart.

The Theme Service should be treated as a foundational platform service, similar to navigation, security, synchronization, and telemetry. It provides a centralized, consistent visual system that allows every application in the platform to share a cohesive user experience while enabling each tenant to express its own brand identity.

# NOTE
This model cleanly separates tenant-owned branding (BrandingConfig and ThemeSnapshot) from user-owned presentation preferences (ThemePreference). That distinction is valuable in a multi-tenant SaaS platform: if a tenant changes their corporate branding, every user's application updates automatically, while individual users can still choose whether to follow the system theme, reduce transparency, or enable high-contrast mode without affecting the organization's brand.