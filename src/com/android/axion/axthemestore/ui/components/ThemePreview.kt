/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.axthemestore.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.axthemestore.R
import com.android.axion.axthemestore.data.model.Theme

@Composable
fun ThemePreview(
    theme: Theme,
    modifier: Modifier = Modifier,
    isInstalled: Boolean = false,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val iconSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val previewMap =
        remember(context) {
            context.resources
                .getStringArray(R.array.overlay_preview_map)
                .mapNotNull { entry ->
                    val parts = entry.split("|", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()
        }
    val packageName = theme.overlays.firstOrNull()?.packageName.orEmpty()
    val category = theme.category.ifEmpty { theme.overlays.firstOrNull()?.componentId.orEmpty() }
    val resIds =
        remember(packageName, previewMap, context) {
            previewMap[packageName]
                ?.let { prefix ->
                    (1..4).mapNotNull { index ->
                        context.resources
                            .getIdentifier("${prefix}_$index", "drawable", context.packageName)
                            .takeIf { it != 0 }
                    }
                }
                .orEmpty()
        }
    val (container, foreground) = storeCategoryColors(theme.id)
    Box(
        modifier.clip(MaterialTheme.shapes.large).background(container),
        contentAlignment = Alignment.Center,
    ) {
        if (!compact) {
            Box(
                Modifier.align(Alignment.TopEnd)
                    .offset(x = 28.dp, y = (-36).dp)
                    .size(144.dp)
                    .rotate(18f)
                    .clip(StoreFlowerShape)
                    .background(foreground.copy(alpha = 0.06f))
            )
            Box(
                Modifier.align(Alignment.BottomStart)
                    .offset(x = (-24).dp, y = 32.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(foreground.copy(alpha = 0.06f))
            )
        }
        when {
            resIds.isNotEmpty() -> {
                Row(
                    Modifier.fillMaxWidth().padding(if (compact) 12.dp else 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (if (compact) resIds.take(1) else resIds).forEach { resId ->
                        // The catalog includes layer-list and inset drawables as well as vectors.
                        val bitmap =
                            remember(context, configuration, resId, iconSizePx) {
                                ContextCompat.getDrawable(context, resId)
                                    ?.toBitmap(width = iconSizePx, height = iconSizePx)
                                    ?.asImageBitmap()
                            }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.sizeIn(maxWidth = 48.dp, maxHeight = 48.dp)
                                            .aspectRatio(1f),
                                    colorFilter = ColorFilter.tint(foreground),
                                )
                            }
                        }
                    }
                }
            }
            "charging_animation" in packageName || "charging_animation" in category -> {
                if (!compact && packageName.substringAfterLast('.') in listOf("moto", "nothing")) {
                    ChargingAnimationBannerPreview(packageName, Modifier.fillMaxSize())
                } else {
                    StoreEmblem(
                        storeCategoryIcon(category),
                        Modifier.fillMaxSize(0.6f),
                        container,
                        foreground,
                    )
                }
            }
            "battery" in packageName || "battery" in category -> {
                BatteryStylePreview(
                    packageName,
                    Modifier.fillMaxSize().padding(if (compact) 4.dp else 24.dp),
                )
            }
            "back_gesture" in packageName || "back_gesture" in category -> {
                BackGesturePreview(Modifier.fillMaxSize())
            }
            theme.previewImages.isNotEmpty() -> {
                AsyncNetworkImage(
                    url = theme.previewImages.first(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    errorContent = {
                        if (isInstalled && packageName.isNotEmpty()) {
                            ThemePackagePreview(
                                packageName,
                                Modifier.fillMaxSize(),
                                showSingleIcon = compact,
                            )
                        } else {
                            StoreEmblem(
                                storeCategoryIcon(category),
                                Modifier.size(if (compact) 48.dp else 88.dp),
                                container,
                                foreground,
                            )
                        }
                    },
                )
            }
            isInstalled && packageName.isNotEmpty() -> {
                ThemePackagePreview(packageName, Modifier.fillMaxSize(), showSingleIcon = compact)
            }
            else -> {
                StoreEmblem(
                    storeCategoryIcon(category),
                    Modifier.size(if (compact) 48.dp else 88.dp),
                    container,
                    foreground,
                )
            }
        }
    }
}
