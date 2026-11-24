package com.citacomsys.feature.platform.ui.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.citacomsys.api.platform.model.PlatformFeature
import com.citacomsys.api.platform.model.PlatformTestimonial
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.modifiers.ShimmerItem
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.legal.LegalComplianceScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WelcomeScreen(
    isCompact: Boolean = false,
    onNewRegistration: () -> Unit,
    onLoginHub: () -> Unit,
    viewModel: PlatformViewModel = koinViewModel()
) {
    val contentState by viewModel.content.collectAsStateWithLifecycle()
    var showLegalContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = showLegalContent,
        onBackCompleted = {
            showLegalContent = false
        }
    )

    if (showLegalContent) {
        LegalComplianceScreen(
            onBackClick = { showLegalContent = false }
        )
        return
    }

    UiResultView(
        state = contentState,
        onRetry = { viewModel.loadData() },
        loadingView = {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                repeat(12) {
                    ShimmerItem()
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { content ->
        LazyListView(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(0.dp)
        ) {
            // Hero Section
            item {
                WelcomeSectionWrapper {
                    HeroContent(
                        onNewRegistration = onNewRegistration,
                        onLoginHub = onLoginHub
                    )
                }
            }

            // Highlights Section - Global Extensibility
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    HighlightsContent(content.features, isCompact)
                }
            }

            // Continuity & Mesh Section - Future-Proof Industrial Tech
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    ContinuityContent(isCompact)
                }
            }

            // Social Proof Section
            if (content.testimonials.isNotEmpty()) {
                item {
                    TestimonialsContent(
                        testimonials = content.testimonials,
                        isCompact = isCompact
                    )
                }
            }

            // 5. Footer Section
            item {
                FooterContent(
                    toggleLegal = { showLegalContent = !showLegalContent }
                )
            }
        }
    }
}

@Composable
private fun WelcomeSectionWrapper(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 1200.dp)
            .padding(horizontal = 24.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun HeroContent(
    onNewRegistration: () -> Unit,
    onLoginHub: () -> Unit
) {

    ProjectLogos.CitasProjectLogo(size = 96.dp, showWordmark = true)

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "All your inventory and business operations in one secure place. Reliability first, works flawlessly offline.",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.widthIn(max = 700.dp),
        lineHeight = 34.sp
    )

    Spacer(modifier = Modifier.height(56.dp))

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroButtons(
                modifier = Modifier.weight(1f),
                onNewRegistration = onNewRegistration,
                onLoginHub = onLoginHub
            )
        }
    }
}

@Composable
private fun HeroButtons(
    modifier: Modifier = Modifier,
    onNewRegistration: () -> Unit,
    onLoginHub: () -> Unit
) {
    Button(
        onClick = onLoginHub,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(AppIcons.Login, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Sign In",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }

    OutlinedButton(
        onClick = onNewRegistration,
        enabled = false,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Icon(AppIcons.Business, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Register Company",
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightsContent(
    features: List<PlatformFeature>,
    isCompact: Boolean = false
) {
    val padding = if (isCompact) 16.dp else 32.dp

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "One Platform, Infinite Extensions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Modern Features for Pharmacy, Retail, Warehousing, and beyond.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(56.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            // Dynamic features from server
            features.forEach { feature ->
                LandingFeatureCard(
                    innerPadding = padding,
                    icon = getIconForName(feature.featureIcon),
                    title = feature.title,
                    description = feature.description,
                    modifier = Modifier.widthIn(min = 400.dp).heightIn(260.dp).weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ContinuityContent(
    isCompact: Boolean = false
) {

    Column(
        modifier = Modifier
            .widthIn(max = 1200.dp).padding(horizontal = 24.dp)
            .padding(vertical = if (isCompact) 16.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Industrial Reliability & Continuity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Built for mission-critical environments where the internet is optional, but business is mandatory.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 800.dp)
        )

        Spacer(Modifier.height(56.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContinuityFeatureItem(
                modifier = Modifier.widthIn(320.dp).heightIn(240.dp).weight(1f),
                icon = AppIcons.Layers,
                title = "Edge Mesh Sync",
                description = "Multi-device local network synchronization. Even if only one terminal has internet, your entire fleet stays in sync."
            )
            ContinuityFeatureItem(
                modifier = Modifier.widthIn(320.dp).heightIn(240.dp).weight(1f),
                icon = AppIcons.CloudDone,
                title = "Local-First Power",
                description = "Every workstation carries a full local copy of your operational data. Zero latency, zero downtime, 100% availability."
            )
            ContinuityFeatureItem(
                modifier = Modifier.widthIn(320.dp).heightIn(240.dp).weight(1f),
                icon = AppIcons.Shield,
                title = "Hardware Trust",
                description = "Cryptographic physical node identity. We secure your business at the silicon level, preventing unauthorized device access."
            )
        }
    }
}

@Composable
private fun ContinuityFeatureItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TestimonialsContent(
    testimonials: List<PlatformTestimonial>,
    isCompact: Boolean = false
) {
    val padding = if (isCompact) 16.dp else 32.dp
    Column(
        Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Empowering Industrial Growth",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(48.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            testimonials.forEach { testimonial ->
                WelcomeTestimonialCard(
                    name = testimonial.name,
                    message = testimonial.message,
                    business = testimonial.designation,
                    modifier = Modifier.widthIn(min = 400.dp).weight(1f)
                )
            }
        }
    }
}

/**
 * Maps the string icon identifier from the database to an actual AppIcon ImageVector.
 */
private fun getIconForName(name: String): ImageVector = when (name) {
    "PointOfSale" -> AppIcons.PointOfSale
    "Analytics" -> AppIcons.Analytics
    "CloudDone" -> AppIcons.CloudDone
    "Inventory" -> AppIcons.Inventory
    "Security" -> AppIcons.Shield
    "Receipt" -> AppIcons.Receipt
    else -> AppIcons.AutoAwesome
}

@Composable
private fun FooterContent(
    toggleLegal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "© 2026 CITACOM SYSTEMS LTD. Empowering local businesses.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = toggleLegal) {
            Text("Terms of Service & Privacy Policy", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LandingFeatureCard(
    modifier: Modifier = Modifier,
    innerPadding: Dp,
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(innerPadding)) {
            FlowRow(
                itemVerticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun WelcomeTestimonialCard(
    modifier: Modifier = Modifier,
    name: String, message:
    String, business: String
) {
    AppCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = AppIcons.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                fontStyle = FontStyle.Italic,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        AppIcons.Person,
                        null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        business,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
