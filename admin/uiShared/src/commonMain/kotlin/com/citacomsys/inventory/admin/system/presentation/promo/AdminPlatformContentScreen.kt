@file:OptIn(ExperimentalMaterial3Api::class)

package com.citacomsys.inventory.admin.system.presentation.promo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.platform.model.PlatformDemo
import com.citacomsys.api.platform.model.PlatformFeature
import com.citacomsys.api.platform.model.PlatformTestimonial
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.types.FeatureIcon
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

/**
 * Marketing CMS Screen for managing platform features, demos, and testimonials.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPlatformContentScreen(
    onBack: () -> Unit,
    viewModel: AdminPlatformContentViewModel = koinViewModel()
) {
    val state by viewModel.contentState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Features", "Demos", "Testimonials")

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marketing Content CMS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadContent() }) {
                        Icon(AppIcons.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            UiResultView(
                state = state,
                onRetry = { viewModel.loadContent() }
            ) { content ->
                when (selectedTab) {
                    0 -> FeaturesList(content.features, onSave = viewModel::saveFeature)
                    1 -> DemosList(content.demos, onSave = viewModel::saveDemo)
                    2 -> TestimonialsList(content.testimonials, onSave = viewModel::saveTestimonial)
                }
            }
        }
    }
}

@Composable
private fun FeaturesList(
    features: List<PlatformFeature>,
    onSave: (PlatformFeature) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var featureToEdit by remember { mutableStateOf<PlatformFeature?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGridView(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            columns = GridCells.Adaptive(360.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(features.sortedBy { it.orderRank }, key = { it.id }) { feature ->
                val featureIcon =
                    runCatching { FeatureIcon.valueOf(feature.featureIcon) }.getOrNull()
                ContentItemCard(
                    title = feature.title,
                    subtitle = "Order: ${feature.orderRank} | Module: ${feature.module ?: "Generic"}",
                    description = feature.description,
                    isActive = feature.isActive,
                    onClick = { featureToEdit = feature },
                    icon = featureIcon?.toIcon() ?: AppIcons.AutoAwesome,
                    iconColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(AppIcons.Add, contentDescription = "Add Feature")
        }
    }

    if (showAddDialog) {
        FeatureEditDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { onSave(it); showAddDialog = false }
        )
    }

    featureToEdit?.let { feature ->
        FeatureEditDialog(
            feature = feature,
            onDismiss = { featureToEdit = null },
            onConfirm = { onSave(it); featureToEdit = null }
        )
    }
}

@Composable
private fun DemosList(
    demos: List<PlatformDemo>,
    onSave: (PlatformDemo) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var demoToEdit by remember { mutableStateOf<PlatformDemo?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGridView(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            columns = GridCells.Adaptive(320.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(demos.sortedBy { it.orderRank }, key = { it.id }) { demo ->
                ContentItemCard(
                    title = demo.title,
                    subtitle = "Order: ${demo.orderRank}",
                    description = demo.videoUrl ?: "No video URL",
                    isActive = demo.isActive,
                    onClick = { demoToEdit = demo },
                    icon = AppIcons.Smartphone,
                    iconColor = MaterialTheme.colorScheme.secondary
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(AppIcons.Add, contentDescription = "Add Demo")
        }
    }

    if (showAddDialog) {
        DemoEditDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { onSave(it); showAddDialog = false }
        )
    }

    demoToEdit?.let { demo ->
        DemoEditDialog(
            demo = demo,
            onDismiss = { demoToEdit = null },
            onConfirm = { onSave(it); demoToEdit = null }
        )
    }
}

@Composable
private fun TestimonialsList(
    testimonials: List<PlatformTestimonial>,
    onSave: (PlatformTestimonial) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var testimonialToEdit by remember { mutableStateOf<PlatformTestimonial?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGridView(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            columns = GridCells.Adaptive(320.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(testimonials.sortedBy { it.orderRank }, key = { it.id }) { testimonial ->
                ContentItemCard(
                    title = testimonial.name,
                    subtitle = testimonial.designation,
                    description = testimonial.message,
                    isActive = testimonial.isActive,
                    onClick = { testimonialToEdit = testimonial },
                    icon = AppIcons.Person,
                    iconColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(AppIcons.Add, contentDescription = "Add Testimonial")
        }
    }

    if (showAddDialog) {
        TestimonialEditDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { onSave(it); showAddDialog = false }
        )
    }

    testimonialToEdit?.let { testimonial ->
        TestimonialEditDialog(
            testimonial = testimonial,
            onDismiss = { testimonialToEdit = null },
            onConfirm = { onSave(it); testimonialToEdit = null }
        )
    }
}

@Composable
private fun ContentItemCard(
    title: String,
    subtitle: String,
    description: String,
    isActive: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Column {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(description, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                }
            },
            leadingContent = {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        tint = iconColor
                    )
                }
            },
            trailingContent = {
                if (isActive) {
                    Icon(
                        AppIcons.CheckCircle,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        AppIcons.Cancel,
                        contentDescription = "Inactive",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun FeatureEditDialog(
    feature: PlatformFeature? = null,
    onDismiss: () -> Unit,
    onConfirm: (PlatformFeature) -> Unit
) {
    var title by remember { mutableStateOf(feature?.title ?: "") }
    var description by remember { mutableStateOf(feature?.description ?: "") }
    var icon by remember { mutableStateOf(feature?.featureIcon ?: "INVENTORY") }
    var module by remember { mutableStateOf(feature?.module ?: "") }
    var orderRank by remember { mutableIntStateOf(feature?.orderRank ?: 0) }
    var isActive by remember { mutableStateOf(feature?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (feature == null) "New Feature Highlight" else "Edit Feature") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = icon,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Icon") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        ).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        FeatureIcon.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = { icon = t.name; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = module,
                    onValueChange = { module = it },
                    label = { Text("Module (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderRank.toString(),
                    onValueChange = { orderRank = it.toIntOrNull() ?: 0 },
                    label = { Text("Order Rank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Visible to Public")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    PlatformFeature(
                        feature?.id ?: "",
                        title,
                        icon,
                        module.takeIf { it.isNotBlank() },
                        description,
                        orderRank,
                        isActive
                    )
                )
            }, enabled = title.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DemoEditDialog(
    demo: PlatformDemo? = null,
    onDismiss: () -> Unit,
    onConfirm: (PlatformDemo) -> Unit
) {
    var title by remember { mutableStateOf(demo?.title ?: "") }
    var videoUrl by remember { mutableStateOf(demo?.videoUrl ?: "") }
    var imageUrl by remember { mutableStateOf(demo?.imageUrl ?: "") }
    var orderRank by remember { mutableIntStateOf(demo?.orderRank ?: 0) }
    var isActive by remember { mutableStateOf(demo?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (demo == null) "New Demo Entry" else "Edit Demo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Demo Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Video URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Thumbnail Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderRank.toString(),
                    onValueChange = { orderRank = it.toIntOrNull() ?: 0 },
                    label = { Text("Order Rank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Visible to Public")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    PlatformDemo(
                        demo?.id ?: "",
                        title,
                        videoUrl.takeIf { it.isNotBlank() },
                        imageUrl.takeIf { it.isNotBlank() },
                        orderRank,
                        isActive
                    )
                )
            }, enabled = title.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun FeatureIcon.toIcon(): ImageVector =
    when (this) {
        FeatureIcon.SMS -> AppIcons.Sms
        FeatureIcon.INVENTORY -> AppIcons.Inventory
        FeatureIcon.SALE -> AppIcons.PointOfSale
        FeatureIcon.ANALYTICS -> AppIcons.QueryStats
        FeatureIcon.BRANCH -> AppIcons.Place
        FeatureIcon.SECURITY -> AppIcons.Security
        FeatureIcon.PAYMENT -> AppIcons.Payment
        FeatureIcon.USER -> AppIcons.Person
        FeatureIcon.DEVICE -> AppIcons.Devices
        FeatureIcon.Inventory -> AppIcons.Inventory2
        FeatureIcon.Layers -> AppIcons.Layers
        FeatureIcon.Storefront -> AppIcons.Storefront
        FeatureIcon.Medication -> AppIcons.Medication
        FeatureIcon.Business -> AppIcons.Business
        FeatureIcon.Shield -> AppIcons.Shield
        FeatureIcon.CloudDone -> AppIcons.CloudDone
    }

@Composable
private fun TestimonialEditDialog(
    testimonial: PlatformTestimonial? = null,
    onDismiss: () -> Unit,
    onConfirm: (PlatformTestimonial) -> Unit
) {
    var name by remember { mutableStateOf(testimonial?.name ?: "") }
    var designation by remember { mutableStateOf(testimonial?.designation ?: "") }
    var message by remember { mutableStateOf(testimonial?.message ?: "") }
    var imageUrl by remember { mutableStateOf(testimonial?.imageUrl ?: "") }
    var orderRank by remember { mutableIntStateOf(testimonial?.orderRank ?: 0) }
    var isActive by remember { mutableStateOf(testimonial?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (testimonial == null) "New Testimonial" else "Edit Testimonial") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Testimonial Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Avatar URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderRank.toString(),
                    onValueChange = { orderRank = it.toIntOrNull() ?: 0 },
                    label = { Text("Order Rank") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Visible to Public")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    PlatformTestimonial(
                        testimonial?.id ?: "",
                        name,
                        designation,
                        message,
                        imageUrl.takeIf { it.isNotBlank() },
                        orderRank,
                        isActive
                    )
                )
            }, enabled = name.isNotBlank() && message.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
