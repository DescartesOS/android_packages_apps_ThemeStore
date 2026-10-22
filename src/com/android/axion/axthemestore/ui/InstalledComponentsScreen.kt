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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axthemestore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.axion.axthemestore.R
import com.android.axion.axthemestore.ui.components.*
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel
import com.android.axion.compose.scaffold.AxionScaffold

@Composable
fun InstalledComponentsScreen(viewModel: ThemeStoreViewModel, onBackClick: () -> Unit) {
    val categoryThemes by viewModel.categoryThemesState.collectAsStateWithLifecycle()
    val components = remember(categoryThemes) { categoryThemes.entries.sortedBy { it.key } }
    AxionScaffold(
        title = stringResource(R.string.your_setup),
        onBackClick = onBackClick,
        collapsedByDefault = false,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "summary") {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StoreEmblem(
                            Icons.Default.AutoAwesome,
                            Modifier.size(64.dp),
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            stringResource(R.string.setup_title),
                            style = MaterialTheme.typography.headlineLargeEmphasized,
                        )
                        Text(
                            stringResource(R.string.setup_description),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                pluralStringResource(
                                    R.plurals.active_component_count,
                                    components.size,
                                    components.size,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
            item(key = "heading") {
                StoreSectionHeading(
                    stringResource(R.string.active_theme_components),
                    Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp),
                )
            }
            if (components.isEmpty()) {
                item(key = "empty") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceBright,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            StoreEmblem(Icons.Default.Palette, Modifier.size(80.dp))
                            Text(
                                stringResource(R.string.no_icon_themes_installed),
                                style = MaterialTheme.typography.titleLargeEmphasized,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                stringResource(R.string.browse_for_icon_themes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            FilledTonalButton(
                                onClick = onBackClick,
                                modifier = Modifier.heightIn(min = 56.dp),
                            ) {
                                Text(stringResource(R.string.explore_themes))
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(components, key = { _, entry -> entry.key }) { index, entry ->
                    Surface(
                        shape = storeGroupedShape(index, components.size),
                        color = MaterialTheme.colorScheme.surfaceBright,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            val (container, foreground) = storeCategoryColors(entry.key)
                            StoreEmblem(
                                storeCategoryIcon(entry.key),
                                Modifier.size(48.dp),
                                container,
                                foreground,
                            )
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    getCategoryDisplayName(entry.key),
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                )
                                Text(
                                    entry.value.substringAfterLast('.'),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.Default.CheckCircle,
                                stringResource(R.string.active),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryDisplayName(categoryId: String): String =
    when (categoryId) {
        "android" -> "Android Framework"
        "systemui" -> "System UI"
        "wifi",
        "statusbar_wifi" -> "Wi-Fi icons"
        "signal",
        "statusbar_signal" -> "Signal icons"
        "back_gesture" -> "Back gesture"
        "charging_animation" -> "Charging animation"
        "battery_style" -> "Battery style"
        else -> categoryId.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
