/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axthemestore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.axion.axthemestore.R

val StoreFlowerShape: Shape
    @Composable get() = MaterialShapes.Cookie9Sided.toShape()

fun storeGroupedShape(index: Int, count: Int) =
    RoundedCornerShape(
        topStart = if (index == 0) 24.dp else 4.dp,
        topEnd = if (index == 0) 24.dp else 4.dp,
        bottomStart = if (index == count - 1) 24.dp else 4.dp,
        bottomEnd = if (index == count - 1) 24.dp else 4.dp,
    )

@Composable
fun StoreEmblem(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = modifier.clip(StoreFlowerShape).background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.fillMaxSize(0.44f),
        )
    }
}

@Composable
fun StoreSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            if (pressed) 0.97f else 1f,
            MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "card scale",
        )
    val corner by
        animateFloatAsState(
            if (pressed) 20f else 28f,
            MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "card corner",
        )
    Surface(
        onClick = onClick,
        modifier =
            modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(corner.dp),
        color = color,
        contentColor = contentColor,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun StoreSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLargeEmphasized,
        )
        if (onClick != null) {
            FilledTonalIconButton(onClick = onClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    stringResource(R.string.view_collection, title),
                )
            }
        }
    }
}

@Composable
fun storeCategoryColors(key: String): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when ((key.hashCode() and Int.MAX_VALUE) % 3) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
}

fun storeCategoryIcon(category: String): ImageVector =
    when {
        "charging" in category -> Icons.Default.Bolt
        "battery" in category -> Icons.Default.BatteryFull
        "wifi" in category -> Icons.Default.Wifi
        "signal" in category -> Icons.Default.SignalCellularAlt
        "icon" in category -> Icons.Default.Apps
        "gesture" in category -> Icons.Default.Gesture
        "volume" in category -> Icons.Default.VolumeUp
        "qs" in category || "systemui" in category -> Icons.Default.Dashboard
        "local" in category -> Icons.Default.Layers
        else -> Icons.Default.Palette
    }
