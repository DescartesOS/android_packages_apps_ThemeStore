/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axthemestore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.axion.axthemestore.R

@Composable
fun CustomizationToolbar(fontsSelected: Boolean, onSelect: (Boolean) -> Unit) {
    val width = 224.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.3f)
    HorizontalFloatingToolbar(
        expanded = true,
        shape = CircleShape,
        contentPadding = PaddingValues(6.dp),
        expandedShadowElevation = 6.dp,
        colors =
            FloatingToolbarDefaults.standardFloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                toolbarContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
    ) {
        BoxWithConstraints(Modifier.widthIn(max = width).width(width).height(48.dp)) {
            val itemWidth = maxWidth / 2
            val offset by
                animateDpAsState(
                    if (fontsSelected) itemWidth else 0.dp,
                    MaterialTheme.motionScheme.defaultSpatialSpec(),
                    label = "customization tab",
                )
            Box(
                Modifier.offset(x = offset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            )
            Row(Modifier.fillMaxSize().selectableGroup()) {
                listOf(false, true).forEach { fonts ->
                    val selected = fonts == fontsSelected
                    val color by
                        animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            MaterialTheme.motionScheme.fastEffectsSpec(),
                            label = "customization icon",
                        )
                    val description =
                        stringResource(
                            if (fonts) R.string.store_font_customization
                            else R.string.store_ui_customization
                        )
                    Row(
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .selectable(selected, role = Role.Tab, onClick = { onSelect(fonts) })
                            .semantics { contentDescription = description },
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (fonts) Icons.Default.TextFields else Icons.Default.Palette,
                            null,
                            Modifier.size(22.dp),
                            tint = color,
                        )
                        Text(
                            stringResource(
                                if (fonts) R.string.store_tab_fonts else R.string.store_tab_ui
                            ),
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = color,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                    }
                }
            }
        }
    }
}
