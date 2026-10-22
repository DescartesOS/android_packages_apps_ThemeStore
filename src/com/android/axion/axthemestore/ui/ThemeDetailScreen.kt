/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axthemestore.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.axion.axthemestore.R
import com.android.axion.axthemestore.data.model.Theme
import com.android.axion.axthemestore.data.model.ThemeInstallState
import com.android.axion.axthemestore.data.model.ThemeOverlay
import com.android.axion.axthemestore.data.model.formatFileSize
import com.android.axion.axthemestore.data.model.hasUpdate
import com.android.axion.axthemestore.ui.components.*
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel
import com.android.axion.compose.preferences.ExpressiveSwitch
import com.android.axion.compose.scaffold.AxionPinnedTopAppBar
import com.android.axion.compose.scaffold.ExpressiveBackButton

@Composable
fun ThemeDetailScreen(theme: Theme, viewModel: ThemeStoreViewModel, onBackClick: () -> Unit) {
    val states by viewModel.themeStates.collectAsStateWithLifecycle()
    val state = states[theme.id] ?: ThemeInstallState.NotInstalled
    val pending by viewModel.pendingComponentChanges.collectAsStateWithLifecycle()
    val categoryThemes by viewModel.categoryThemesState.collectAsStateWithLifecycle()
    val isInstalled =
        state is ThemeInstallState.Installed || state is ThemeInstallState.InstalledInactive
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AxionPinnedTopAppBar(
                title = stringResource(R.string.themes),
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIcon = { ExpressiveBackButton(onClick = onBackClick) },
            )
        },
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Box(
                    Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    InstallSection(
                        theme = theme,
                        installState = state,
                        hasPendingChanges = pending.containsKey(theme.id),
                        onDownloadClick = { viewModel.downloadTheme(theme) },
                        onInstallClick = { viewModel.installTheme(theme) },
                        onApplyClick = { viewModel.applyTheme(theme) },
                        onApplyPendingClick = { viewModel.applyPendingChanges(theme) },
                        onDisableClick = { viewModel.disableTheme(theme) },
                        onUninstallClick = {
                            viewModel.uninstallTheme(theme, onComplete = onBackClick)
                        },
                        onClearError = { viewModel.clearError(theme.id) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ThemePreview(theme, Modifier.fillMaxWidth().height(232.dp), isInstalled = isInstalled)
            Column(
                Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ThemeStateLabel(theme, state)
                Text(theme.name, style = MaterialTheme.typography.displaySmallEmphasized)
                if (theme.author.isNotBlank()) {
                    Text(
                        stringResource(R.string.theme_by_author, theme.author),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (
                theme.description.isNotBlank() ||
                    theme.version.isNotBlank() ||
                    theme.totalFileSize > 0
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceBright,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.about_theme),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                        if (theme.description.isNotBlank()) {
                            Text(
                                theme.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (theme.version.isNotBlank()) {
                                InfoChip(
                                    stringResource(R.string.theme_version),
                                    theme.version,
                                    Modifier.weight(1f),
                                )
                            }
                            if (theme.totalFileSize > 0) {
                                InfoChip(
                                    stringResource(R.string.theme_size),
                                    theme.totalFileSize.formatFileSize(),
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
            if (theme.isUnified && theme.overlays.isNotEmpty()) {
                val overlay = theme.overlays.first()
                if (overlay.targets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.customize_components),
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Text(
                            stringResource(R.string.customize_components_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                        )
                        if (pending.containsKey(theme.id)) {
                            Text(
                                stringResource(R.string.pending_changes),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        overlay.targets.forEachIndexed { index, target ->
                            val isFromThisTheme = categoryThemes[target] == overlay.packageName
                            val pendingSet = pending[theme.id]
                            ComponentSelectionItem(
                                componentId = target,
                                isEnabled = pendingSet?.contains(target) ?: isFromThisTheme,
                                isToggleable = isInstalled,
                                currentSource =
                                    categoryThemes[target]
                                        ?.takeUnless { isFromThisTheme }
                                        ?.substringAfterLast('.'),
                                shape = storeGroupedShape(index, overlay.targets.size),
                                onToggle = { enabled ->
                                    if (isInstalled)
                                        viewModel.toggleComponent(theme, target, enabled)
                                },
                            )
                        }
                    }
                }
            } else if (theme.overlays.isNotEmpty() && !theme.isUiStyle) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StoreSectionHeading(
                        stringResource(R.string.components),
                        Modifier.padding(start = 4.dp, bottom = 12.dp),
                    )
                    theme.overlays.forEachIndexed { index, overlay ->
                        ComponentOverlayItem(overlay, storeGroupedShape(index, theme.overlays.size))
                    }
                }
            }
            if (theme.tags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StoreSectionHeading(
                        stringResource(R.string.tags),
                        Modifier.padding(horizontal = 4.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(theme.tags.size) { index ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    theme.tags[index],
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier =
                                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleMediumEmphasized)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComponentOverlayItem(overlay: ThemeOverlay, shape: Shape) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceBright) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                storeCategoryIcon(overlay.componentId),
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    getComponentDisplayName(overlay.componentId),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Text(
                    getComponentDescription(overlay.componentId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ComponentSelectionItem(
    componentId: String,
    isEnabled: Boolean,
    isToggleable: Boolean,
    currentSource: String?,
    shape: Shape,
    onToggle: (Boolean) -> Unit,
) {
    val background by
        animateColorAsState(
            if (isEnabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceBright,
            MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "component selection",
        )
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(background)
            .toggleable(
                value = isEnabled,
                enabled = isToggleable,
                role = Role.Switch,
                onValueChange = onToggle,
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                getComponentDisplayName(componentId),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color =
                    if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (currentSource != null)
                    stringResource(R.string.current_component_source, currentSource)
                else getComponentDescription(componentId),
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ExpressiveSwitch(checked = isEnabled, onCheckedChange = null, enabled = isToggleable)
    }
}

private fun getComponentDisplayName(componentId: String): String {
    return when (componentId) {
        "statusbar_wifi",
        "wifi" -> "WiFi Icons"
        "statusbar_signal",
        "signal" -> "Signal Icons"
        "android" -> "Android Framework"
        "systemui",
        "systemui_icons" -> "System UI"
        "ui_qs" -> "QuickSettings Style"
        "ui_volume" -> "Volume Panel Style"
        "ui_style" -> "UI Style"
        else -> componentId.replaceFirstChar { it.uppercase() }
    }
}

private fun getComponentDescription(componentId: String): String {
    return when (componentId) {
        "statusbar_wifi",
        "wifi" -> "WiFi signal indicators in status bar"
        "statusbar_signal",
        "signal" -> "Mobile network indicators in status bar"
        "android" -> "Core Android framework icons"
        "systemui",
        "systemui_icons" -> "Status bar and quick settings icons"
        "ui_qs" -> "QuickSettings tiles and brightness slider style"
        "ui_volume" -> "Volume panel appearance"
        "ui_style" -> "Overall UI appearance"
        else -> "Theme component"
    }
}

@Composable
private fun InstallSection(
    theme: Theme,
    installState: ThemeInstallState,
    hasPendingChanges: Boolean,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onApplyClick: () -> Unit,
    onApplyPendingClick: () -> Unit,
    onDisableClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onClearError: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (theme.isUiStyle) {
            when (installState) {
                is ThemeInstallState.Installed -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                disabledContainerColor =
                                    MaterialTheme.colorScheme.secondaryContainer,
                                disabledContentColor =
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.active))
                    }
                }
                is ThemeInstallState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text =
                                stringResource(R.string.error_prefix, installState.message ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onClearError()
                                onApplyClick()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onApplyClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }
            return
        }

        when (installState) {
            is ThemeInstallState.NotInstalled -> {
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.download))
                }
            }

            is ThemeInstallState.Downloaded -> {
                Button(
                    onClick = onInstallClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.install))
                }
            }

            is ThemeInstallState.InstalledInactive -> {
                val hasUpdate = theme.hasUpdate(installState.installedVersionCode)

                val hasSelection =
                    if (theme.isUnified) {
                        hasPendingChanges
                    } else {
                        true
                    }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (hasUpdate) {
                        Button(
                            onClick = onInstallClick,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Icon(imageVector = Icons.Default.Update, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.update_to, theme.version))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onApplyClick,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            enabled = hasSelection,
                        ) {
                            Text(stringResource(R.string.apply))
                        }

                        OutlinedButton(
                            onClick = onUninstallClick,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                        ) {
                            Text(stringResource(R.string.uninstall))
                        }
                    }
                }
            }

            is ThemeInstallState.Installed -> {
                val hasUpdate = theme.hasUpdate(installState.installedVersionCode)

                if (hasUpdate) {
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Update, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.update_to, theme.version))
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (hasPendingChanges) {
                            Button(
                                onClick = onApplyPendingClick,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            ) {
                                Text(stringResource(R.string.apply_changes))
                            }
                        } else {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        disabledContainerColor =
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        disabledContentColor =
                                            MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.active))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = onDisableClick,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                            ) {
                                Text(stringResource(R.string.disable))
                            }

                            OutlinedButton(
                                onClick = onUninstallClick,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                            ) {
                                Text(stringResource(R.string.uninstall))
                            }
                        }
                    }
                }
            }

            is ThemeInstallState.Downloading -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                stringResource(R.string.downloading, installState.currentOverlay),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${(installState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val motionScheme = MaterialTheme.motionScheme
                    val animatedProgress by
                        animateFloatAsState(
                            targetValue = installState.progress,
                            animationSpec = motionScheme.defaultEffectsSpec(),
                            label = "download_progress",
                        )
                    LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            is ThemeInstallState.Installing -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.installing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            is ThemeInstallState.PartiallyInstalled -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text =
                            stringResource(
                                R.string.components_installed,
                                installState.installedOverlays.size,
                                installState.totalOverlays,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.resume_installation))
                    }
                }
            }

            is ThemeInstallState.Error -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.error_prefix, installState.message ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onClearError()
                            onInstallClick()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}
