/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axthemestore.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.axion.axthemestore.R
import com.android.axion.axthemestore.navbar.NavbarStyle
import com.android.axion.axthemestore.ui.components.StoreEmblem
import com.android.axion.axthemestore.ui.components.StoreSectionHeading
import com.android.axion.axthemestore.ui.components.StoreSurface
import com.android.axion.axthemestore.viewmodel.NavbarViewModel
import com.android.axion.compose.scaffold.AxionScaffold

@Composable
fun NavbarCustomizationScreen(viewModel: NavbarViewModel, onBackClick: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val busy = state.loading || state.applyingPackage != null
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = state.error?.let { stringResource(it) }
    LaunchedEffect(state.error) {
        if (state.error == R.string.navbar_apply_failed && errorMessage != null) {
            snackbar.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    AxionScaffold(
        title = stringResource(R.string.navbar_styles_title),
        onBackClick = onBackClick,
        collapsedByDefault = false,
        containerColor = MaterialTheme.colorScheme.surface,
        actions = {
            IconButton(onClick = viewModel::refresh, enabled = !busy) {
                Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            LazyVerticalGrid(
                columns =
                    GridCells.Adaptive(156.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)),
                modifier = Modifier.fillMaxSize().selectableGroup(),
                contentPadding =
                    PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "intro", span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                StoreEmblem(
                                    Icons.Default.SmartButton,
                                    Modifier.size(48.dp),
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.onTertiary,
                                )
                                Text(
                                    stringResource(R.string.built_in),
                                    style = MaterialTheme.typography.titleLargeEmphasized,
                                )
                            }
                            Text(
                                stringResource(R.string.navbar_styles_description),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(R.string.navbar_navigation_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (state.loading) {
                    item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LoadingIndicator(Modifier.size(36.dp))
                            Text(
                                stringResource(R.string.navbar_loading),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                state.error
                    ?.takeIf { it != R.string.navbar_apply_failed }
                    ?.let { error ->
                        item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Column(
                                    Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        stringResource(error),
                                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    FilledTonalButton(
                                        onClick = viewModel::refresh,
                                        enabled = !busy,
                                    ) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }
                if (state.styles.isNotEmpty()) {
                    item(key = "styles", span = { GridItemSpan(maxLineSpan) }) {
                        StoreSectionHeading(
                            stringResource(R.string.navbar_choose_style),
                            Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                }
                items(state.styles, key = { it.packageName }) { style ->
                    NavbarStyleCard(
                        style,
                        active = style.packageName == state.activePackage,
                        applying = style.packageName == state.applyingPackage,
                        enabled = !busy,
                        onClick = { viewModel.apply(style) },
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(20.dp))
        }
    }
}

@Composable
private fun NavbarStyleCard(
    style: NavbarStyle,
    active: Boolean,
    applying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val container by
        animateColorAsState(
            if (active) colors.primaryContainer else colors.surfaceBright,
            motion.defaultEffectsSpec(),
            label = "navbar selection color",
        )
    val content by
        animateColorAsState(
            if (active) colors.onPrimaryContainer else colors.onSurface,
            motion.defaultEffectsSpec(),
            label = "navbar selection content",
        )
    val status =
        stringResource(
            when {
                applying -> R.string.navbar_applying
                active -> R.string.active
                else -> R.string.navbar_tap_to_apply
            }
        )
    StoreSurface(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth().semantics {
                selected = active
                role = Role.RadioButton
                stateDescription = status
            },
        color = container,
        contentColor = content,
        enabled = enabled,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = colors.surfaceContainerHighest) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val preview = style.preview
                    if (preview == null) {
                        Text(
                            stringResource(R.string.navbar_preview_unavailable),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        preview.forEach { bitmap ->
                            Image(
                                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(colors.onSurface),
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        style.name,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(status, style = MaterialTheme.typography.labelMedium)
                }
                if (applying) {
                    LoadingIndicator(Modifier.size(24.dp))
                } else if (active) {
                    StoreEmblem(
                        Icons.Default.Check,
                        Modifier.size(28.dp),
                        colors.primary,
                        colors.onPrimary,
                    )
                }
            }
        }
    }
}
