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

package com.android.axion.axthemestore.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.android.axion.axthemestore.engine.ThemeEngineProxy
import com.android.axion.axthemestore.ui.components.CustomizationToolbar
import com.android.axion.axthemestore.viewmodel.FontViewModel
import com.android.axion.axthemestore.viewmodel.NavbarViewModel
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: ThemeStoreViewModel,
    fontViewModel: FontViewModel,
    fontLaunchRequest: Int = 0,
) {
    val navController = rememberNavController()
    val startRoute = rememberSaveable { if (fontLaunchRequest > 0) "fonts" else "themes" }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var searchingThemes by remember { mutableStateOf(false) }
    val toolbarVisible =
        (route == "themes" || route == "fonts") &&
            !(route == "themes" && searchingThemes) &&
            !WindowInsets.isImeVisible
    val toolbarPadding = if (toolbarVisible) 96.dp else 20.dp

    fun switchSection(fonts: Boolean) {
        val destination = if (fonts) "fonts" else "themes"
        if (navController.currentDestination?.route == destination) return
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(fontLaunchRequest) { if (fontLaunchRequest > 0) switchSection(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkInstallStates()
                fontViewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    viewModel.checkInstallStates()
                    if (intent?.action == Intent.ACTION_OVERLAY_CHANGED) return
                    viewModel.loadThemes(forceRefresh = true)
                    viewModel.loadIconPacks()
                    fontViewModel.refresh()
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_OVERLAY_CHANGED)
                addDataScheme("package")
            }
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    viewModel.checkInstallStates()
                }
            }
        context.registerReceiver(receiver, filter)
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(ThemeEngineProxy.SETTINGS_THEME_ENGINE_DATA),
            false,
            observer,
        )
        onDispose {
            context.unregisterReceiver(receiver)
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    val motion = MaterialTheme.motionScheme
    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startRoute,
            enterTransition = {
                fadeIn(motion.defaultEffectsSpec()) +
                    slideInHorizontally(motion.defaultSpatialSpec()) {
                        if (targetState.destination.route == "themes") -it / 16 else it / 16
                    }
            },
            exitTransition = { fadeOut(motion.fastEffectsSpec()) },
            popEnterTransition = { fadeIn(motion.defaultEffectsSpec()) },
            popExitTransition = {
                fadeOut(motion.fastEffectsSpec()) +
                    slideOutHorizontally(motion.fastSpatialSpec()) { it / 16 }
            },
        ) {
            composable("themes") {
                ThemeStoreScreen(
                    viewModel = viewModel,
                    onThemeClick = { navController.navigate("detail/" + Uri.encode(it.id)) },
                    onNavigateToCategory = { navController.navigate("category/" + Uri.encode(it)) },
                    onNavigateToInstalledComponents = {
                        navController.navigate("installed_components")
                    },
                    onNavigateToNavbarStyles = { navController.navigate("navbar_styles") },
                    bottomPadding = toolbarPadding,
                    onSearchActiveChange = { searchingThemes = it },
                )
            }
            composable("fonts") {
                FontCustomizationScreen(fontViewModel, bottomPadding = toolbarPadding)
            }
            composable("navbar_styles") {
                val navbarViewModel: NavbarViewModel = composeViewModel()
                NavbarCustomizationScreen(
                    navbarViewModel,
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable("installed_components") {
                InstalledComponentsScreen(viewModel, onBackClick = { navController.popBackStack() })
            }
            composable(
                route = "category/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) { backStackEntry ->
                backStackEntry.arguments?.getString("categoryId")?.let { id ->
                    CategoryThemesScreen(
                        categoryId = id,
                        viewModel = viewModel,
                        onThemeClick = { navController.navigate("detail/" + Uri.encode(it.id)) },
                        onBackClick = { navController.popBackStack() },
                    )
                }
            }
            composable(
                route = "detail/{themeId}",
                arguments = listOf(navArgument("themeId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val id = backStackEntry.arguments?.getString("themeId")
                state.themes
                    .find { it.id == id }
                    ?.let { theme ->
                        ThemeDetailScreen(
                            theme,
                            viewModel,
                            onBackClick = { navController.popBackStack() },
                        )
                    }
            }
        }
        AnimatedVisibility(
            visible = toolbarVisible,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            enter =
                fadeIn(motion.defaultEffectsSpec()) +
                    slideInVertically(motion.defaultSpatialSpec()) { it / 2 },
            exit =
                fadeOut(motion.fastEffectsSpec()) +
                    slideOutVertically(motion.fastSpatialSpec()) { it / 2 },
        ) {
            CustomizationToolbar(fontsSelected = route == "fonts", onSelect = ::switchSection)
        }
    }
}
