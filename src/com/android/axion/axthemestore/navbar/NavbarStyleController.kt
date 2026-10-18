/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.axthemestore.navbar

import android.content.Context
import android.content.om.OverlayManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.UserHandle
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.android.axion.axthemestore.R
import com.android.internal.util.lunaris.ThemeUtils
import kotlin.math.roundToInt

data class NavbarStyle(val packageName: String, val name: String, val preview: List<Bitmap>?)

data class NavbarStyles(val styles: List<NavbarStyle>, val activePackage: String)

class NavbarStyleController(private val context: Context) {
    private val packageManager = context.packageManager
    private val overlayManager
        get() = checkNotNull(context.getSystemService(OverlayManager::class.java))

    fun load(): NavbarStyles {
        val overlays = overlays()
        val packages = listOf(DEFAULT_PACKAGE) + overlays.map { it.packageName }.distinct().sorted()
        val styles =
            packages.map { packageName ->
                val name =
                    if (packageName == DEFAULT_PACKAGE) {
                        context.getString(R.string.navbar_default)
                    } else {
                        runCatching {
                                packageManager
                                    .getApplicationInfo(packageName, 0)
                                    .loadLabel(packageManager)
                                    .toString()
                            }
                            .getOrDefault(packageName.substringAfterLast('.'))
                    }
                NavbarStyle(packageName, name, preview(packageName))
            }
        return NavbarStyles(
            styles,
            overlays.firstOrNull { it.isEnabled }?.packageName ?: DEFAULT_PACKAGE,
        )
    }

    fun activePackage(): String =
        overlays().firstOrNull { it.isEnabled }?.packageName ?: DEFAULT_PACKAGE

    fun apply(packageName: String): String {
        val overlays = overlays()
        require(packageName == DEFAULT_PACKAGE || overlays.any { it.packageName == packageName })
        if (packageName == DEFAULT_PACKAGE) {
            overlays
                .filter { it.isEnabled }
                .forEach { overlayManager.setEnabled(it.packageName, false, UserHandle.SYSTEM) }
        } else {
            overlayManager.setEnabledExclusiveInCategory(packageName, UserHandle.SYSTEM)
        }
        val active = activePackage()
        check(active == packageName)
        ThemeUtils.getInstance(context)
            .writeSettings(CATEGORY, packageName, packageName == DEFAULT_PACKAGE)
        return active
    }

    private fun overlays() =
        overlayManager.getOverlayInfosForTarget(DEFAULT_PACKAGE, UserHandle.SYSTEM).filter {
            it.category == CATEGORY
        }

    private fun preview(packageName: String): List<Bitmap>? {
        return try {
            val resources =
                if (packageName == DEFAULT_PACKAGE) context.resources
                else packageManager.getResourcesForApplication(packageName)
            val size = (24 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
            val ids =
                if (packageName == DEFAULT_PACKAGE) {
                    listOf(
                        R.drawable.ic_sysbar_back,
                        R.drawable.ic_sysbar_home,
                        R.drawable.ic_sysbar_recent,
                    )
                } else {
                    listOf("ic_sysbar_back", "ic_sysbar_home", "ic_sysbar_recent").map { name ->
                        resources.getIdentifier(name, "drawable", packageName)
                    }
                }
            ids.map { id ->
                check(id != 0)
                resources
                    .getDrawable(id, null)
                    .mutate()
                    .apply {
                        setTint(Color.WHITE)
                        setLayoutDirection(context.resources.configuration.layoutDirection)
                    }
                    .toBitmap(size, size)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load navbar preview for $packageName", e)
            null
        }
    }

    companion object {
        const val DEFAULT_PACKAGE = "com.android.launcher3"
        private const val CATEGORY = "android.theme.customization.navbar"
        private const val TAG = "NavbarStyleController"
    }
}
