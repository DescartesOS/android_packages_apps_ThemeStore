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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.axion.axthemestore.R
import com.android.axion.axthemestore.data.model.Theme
import com.android.axion.axthemestore.data.model.ThemeCategory
import com.android.axion.axthemestore.data.model.ThemeInstallState
import com.android.axion.axthemestore.ui.components.*
import com.android.axion.axthemestore.viewmodel.ThemeStoreUiState
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.axion.compose.scaffold.ExpressiveBackButton
import kotlinx.coroutines.launch

@Composable
fun ThemeStoreScreen(
    viewModel: ThemeStoreViewModel,
    onThemeClick: (Theme) -> Unit,
    onNavigateToCategory: (String) -> Unit = {},
    onNavigateToInstalledComponents: () -> Unit = {},
    onNavigateToNavbarStyles: () -> Unit = {},
    bottomPadding: Dp = 0.dp,
    onSearchActiveChange: (Boolean) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeStates by viewModel.themeStates.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isSearchActive) { onSearchActiveChange(isSearchActive) }
    val browseState = rememberLazyListState()
    val closeSearch = {
        isSearchActive = false
        searchQuery = ""
        viewModel.searchThemes("")
    }
    BackHandler(enabled = isSearchActive, onBack = closeSearch)
    val motion = MaterialTheme.motionScheme
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        AnimatedContent(
            targetState = isSearchActive,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            transitionSpec = {
                fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
            },
            label = "store search",
        ) { searching ->
            if (searching) {
                val results =
                    remember(uiState.themes, searchQuery) {
                        val query = searchQuery.trim()
                        uiState.themes.filter {
                            it.name.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true) ||
                                it.author.contains(query, ignoreCase = true)
                        }
                    }
                SearchScreen(
                    viewModel,
                    searchQuery,
                    onSearchChange = {
                        searchQuery = it
                        viewModel.searchThemes(it)
                    },
                    onBack = closeSearch,
                    themes = results,
                    themeStates = themeStates,
                    onThemeClick = onThemeClick,
                )
            } else {
                BrowseScreen(
                    uiState,
                    themeStates,
                    browseState,
                    onSearchClick = { isSearchActive = true },
                    onRefresh = { viewModel.loadThemes(forceRefresh = true) },
                    onThemeClick = onThemeClick,
                    onNavigateToCategory = onNavigateToCategory,
                    onNavigateToInstalledComponents = onNavigateToInstalledComponents,
                    onNavigateToNavbarStyles = onNavigateToNavbarStyles,
                    bottomPadding = bottomPadding,
                )
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    uiState: ThemeStoreUiState,
    themeStates: Map<String, ThemeInstallState>,
    listState: LazyListState,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onThemeClick: (Theme) -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToInstalledComponents: () -> Unit,
    onNavigateToNavbarStyles: () -> Unit,
    bottomPadding: Dp,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoreEmblem(Icons.Default.Palette, Modifier.size(36.dp))
            Text(
                stringResource(R.string.themes),
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh, enabled = !uiState.isLoading) {
                Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
            }
            FilledTonalIconButton(onClick = onNavigateToInstalledComponents) {
                Icon(Icons.Default.Layers, stringResource(R.string.installed_components))
            }
        }
        val groups = remember(uiState.themes) { uiState.themes.groupBy { it.category } }
        val featured =
            remember(uiState.themes) {
                val candidates = uiState.themes.filterNot { it.isLocal }.ifEmpty { uiState.themes }
                (candidates.distinctBy { it.category } + candidates).distinctBy { it.id }.take(5)
            }
        val categories =
            remember(uiState.categories, groups) {
                val known = uiState.categories.filter { groups[it.id]?.isNotEmpty() == true }
                known +
                    groups.keys
                        .filter { key -> known.none { it.id == key } }
                        .map { key ->
                            ThemeCategory(
                                key,
                                key.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            )
                        }
            }
        val navbarCategory =
            ThemeCategory("builtin_navbar", stringResource(R.string.navbar_styles_title))
        val browseCategories =
            remember(categories, navbarCategory) {
                categories.toMutableList().apply {
                    val iconCategory = indexOfLast { category ->
                        listOf("wifi", "signal", "icon").any { it in category.id.lowercase() }
                    }
                    add(iconCategory + 1, navbarCategory)
                }
            }
        val installed =
            uiState.themes.filter {
                themeStates[it.id] is ThemeInstallState.Installed ||
                    themeStates[it.id] is ThemeInstallState.InstalledInactive ||
                    it.isLocal
            }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "intro") { BrowseIntro(onSearchClick) }
            if (uiState.isLoading) {
                item(key = "refresh") {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            }
            if (featured.isNotEmpty()) {
                item(key = "featured") { FeaturedCarousel(featured, themeStates, onThemeClick) }
            }
            item(key = "categories") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StoreSectionHeading(
                        stringResource(R.string.browse_categories),
                        Modifier.padding(horizontal = 24.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(browseCategories, key = { it.id }) { category ->
                            val navbar = category.id == navbarCategory.id
                            val count = groups[category.id].orEmpty().size
                            CategoryTile(
                                category,
                                if (navbar) stringResource(R.string.built_in)
                                else pluralStringResource(R.plurals.theme_count, count, count),
                            ) {
                                if (navbar) onNavigateToNavbarStyles()
                                else onNavigateToCategory(category.id)
                            }
                        }
                    }
                }
            }
            if (uiState.themes.isEmpty()) {
                item(key = "catalog_state") {
                    when {
                        uiState.isLoading -> LoadingState()
                        uiState.error != null -> ErrorState(uiState.error, onRefresh)
                        else -> EmptyState(false)
                    }
                }
            }
            item(key = "your_setup") {
                StoreSurface(
                    onClick = onNavigateToInstalledComponents,
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StoreEmblem(
                            Icons.Default.Layers,
                            Modifier.size(56.dp),
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.onSecondary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.your_setup),
                                style = MaterialTheme.typography.titleLargeEmphasized,
                            )
                            Text(
                                stringResource(R.string.your_setup_description),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
            if (installed.isNotEmpty()) {
                item(key = "installed") {
                    ThemeSection(
                        stringResource(R.string.your_collection),
                        installed,
                        themeStates,
                        onThemeClick,
                    )
                }
            }
            categories.forEach { category ->
                item(key = "category_${category.id}") {
                    ThemeSection(
                        category.name,
                        groups[category.id].orEmpty(),
                        themeStates,
                        onThemeClick,
                        onViewAll = { onNavigateToCategory(category.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseIntro(onSearchClick: () -> Unit) {
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.make_it),
                    style = MaterialTheme.typography.displayMediumEmphasized,
                )
                Text(
                    stringResource(R.string.yours),
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.store_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                StoreEmblem(
                    Icons.Default.AutoAwesome,
                    Modifier.size(96.dp).rotate(-12f),
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Box(
                    Modifier.align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Surface(
            onClick = onSearchClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.search_themes),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(category: ThemeCategory, subtitle: String, onClick: () -> Unit) {
    val (container, foreground) = storeCategoryColors(category.id)
    StoreSurface(onClick, Modifier.width(156.dp), container, foreground) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(storeCategoryIcon(category.id), null, Modifier.size(28.dp))
            Column {
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ThemeSection(
    title: String,
    themes: List<Theme>,
    states: Map<String, ThemeInstallState>,
    onThemeClick: (Theme) -> Unit,
    onViewAll: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StoreSectionHeading(title, Modifier.padding(horizontal = 24.dp), onViewAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(if (onViewAll == null) themes else themes.take(12), key = { it.id }) { theme ->
                ThemeCard(
                    theme,
                    states[theme.id] ?: ThemeInstallState.NotInstalled,
                    { onThemeClick(theme) },
                    Modifier.width(176.dp),
                )
            }
            if (themes.size > 12 && onViewAll != null) {
                item(key = "view_all") {
                    FilledTonalButton(
                        onClick = onViewAll,
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.view_all))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCarousel(
    themes: List<Theme>,
    states: Map<String, ThemeInstallState>,
    onThemeClick: (Theme) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { themes.size })
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.featured),
                Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(start = 20.dp, end = 36.dp),
            pageSpacing = 12.dp,
            key = { themes[it].id },
        ) { index ->
            val theme = themes[index]
            val state = states[theme.id] ?: ThemeInstallState.NotInstalled
            StoreSurface(onClick = { onThemeClick(theme) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(6.dp)) {
                    ThemePreview(
                        theme,
                        Modifier.fillMaxWidth().height(164.dp),
                        isInstalled =
                            state is ThemeInstallState.Installed ||
                                state is ThemeInstallState.InstalledInactive,
                    )
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                theme.name,
                                style = MaterialTheme.typography.headlineSmallEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                theme.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            Modifier.size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
        if (themes.size > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                themes.forEachIndexed { index, theme ->
                    val selected = pager.currentPage == index
                    val width by
                        animateDpAsState(
                            if (selected) 24.dp else 8.dp,
                            MaterialTheme.motionScheme.fastSpatialSpec(),
                            label = "featured indicator",
                        )
                    val pageLabel =
                        stringResource(R.string.featured_page, index + 1, themes.size, theme.name)
                    // The visual indicator is small; its button retains a 48 dp touch target.
                    IconButton(
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                        modifier =
                            Modifier.semantics {
                                contentDescription = pageLabel
                                this.selected = selected
                            },
                    ) {
                        Box(
                            Modifier.size(width, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryThemesScreen(
    categoryId: String,
    viewModel: ThemeStoreViewModel,
    onThemeClick: (Theme) -> Unit,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val states by viewModel.themeStates.collectAsStateWithLifecycle()
    val error = uiState.error
    val name =
        uiState.categories.find { it.id == categoryId }?.name ?: stringResource(R.string.themes)
    val themes =
        uiState.themes.filter {
            if (categoryId == "local") it.isLocal else it.category == categoryId
        }
    AxionScaffold(
        title = name,
        onBackClick = onBackClick,
        collapsedByDefault = false,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when {
                uiState.isLoading && themes.isEmpty() -> LoadingState()
                error != null && themes.isEmpty() ->
                    ErrorState(error) { viewModel.loadThemes(forceRefresh = true) }
                themes.isEmpty() -> EmptyState(false)
                else -> {
                    val cellSize = 144.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(cellSize),
                        contentPadding = PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                Modifier.padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                StoreEmblem(storeCategoryIcon(categoryId), Modifier.size(56.dp))
                                Column {
                                    Text(
                                        pluralStringResource(
                                            R.plurals.theme_count,
                                            themes.size,
                                            themes.size,
                                        ),
                                        style = MaterialTheme.typography.titleMediumEmphasized,
                                    )
                                    Text(
                                        stringResource(R.string.collection_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        items(themes, key = { it.id }) { theme ->
                            ThemeCard(
                                theme,
                                states[theme.id] ?: ThemeInstallState.NotInstalled,
                                { onThemeClick(theme) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    viewModel: ThemeStoreViewModel,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    themes: List<Theme>,
    themeStates: Map<String, ThemeInstallState>,
    onThemeClick: (Theme) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExpressiveBackButton(onClick = onBack)
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_themes)) },
                singleLine = true,
                shape = CircleShape,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, stringResource(R.string.clear))
                        }
                    }
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
        }
        if (searchQuery.isNotBlank()) {
            if (themes.isEmpty()) {
                EmptyState(true)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            pluralStringResource(
                                R.plurals.search_result_count,
                                themes.size,
                                themes.size,
                            ),
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            modifier = Modifier.padding(bottom = 12.dp).semantics { heading() },
                        )
                    }
                    items(themes, key = { it.id }) { theme ->
                        ThemeListItem(
                            theme,
                            themeStates[theme.id] ?: ThemeInstallState.NotInstalled,
                        ) {
                            keyboard?.hide()
                            onThemeClick(theme)
                        }
                    }
                }
            }
        } else if (history.isEmpty()) {
            StoreMessage(
                Icons.Default.Search,
                stringResource(R.string.find_your_style),
                stringResource(R.string.search_description),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.recent_searches),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                        TextButton(onClick = viewModel::clearSearchHistory) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }
                }
                items(history, key = { it }) { query ->
                    Surface(
                        onClick = { onSearchChange(query) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Default.History,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                query,
                                Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(onClick = { viewModel.removeSearchHistoryItem(query) }) {
                                Icon(
                                    Icons.Default.Close,
                                    stringResource(R.string.remove_search, query),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun ThemeListItem(theme: Theme, state: ThemeInstallState, onClick: () -> Unit) {
    StoreSurface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemePreview(
                theme,
                Modifier.size(72.dp),
                isInstalled =
                    state is ThemeInstallState.Installed ||
                        state is ThemeInstallState.InstalledInactive,
                compact = true,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    theme.name,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    theme.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ThemeStateLabel(theme, state)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LoadingIndicator(modifier = Modifier.size(64.dp))
            Text(
                stringResource(R.string.loading_themes),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    StoreMessage(Icons.Default.CloudOff, stringResource(R.string.failed_to_load_themes), message) {
        Button(onClick = onRetry, modifier = Modifier.heightIn(min = 56.dp)) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    StoreMessage(
        if (isSearching) Icons.Default.SearchOff else Icons.Default.Palette,
        stringResource(if (isSearching) R.string.no_themes_found else R.string.no_themes_available),
        stringResource(
            if (isSearching) R.string.try_different_search else R.string.check_back_later
        ),
    )
}

@Composable
private fun StoreMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    action: @Composable () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StoreEmblem(icon, Modifier.size(96.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action()
        }
    }
}
