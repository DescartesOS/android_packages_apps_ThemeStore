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

package com.android.axion.axthemestore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.axion.axthemestore.R
import com.android.axion.axthemestore.data.model.Theme
import com.android.axion.axthemestore.data.model.ThemeInstallState
import com.android.axion.axthemestore.data.model.hasUpdate

@Composable
fun ThemeCard(
    theme: Theme,
    installState: ThemeInstallState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StoreSurface(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(6.dp)) {
            ThemePreview(
                theme,
                Modifier.fillMaxWidth().aspectRatio(1.12f),
                isInstalled =
                    installState is ThemeInstallState.Installed ||
                        installState is ThemeInstallState.InstalledInactive,
            )
            Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
                Text(
                    theme.name,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    minLines = 2,
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
                Spacer(Modifier.height(12.dp))
                ThemeStateLabel(theme, installState)
            }
        }
    }
}

@Composable
fun ThemeStateLabel(theme: Theme, state: ThemeInstallState, modifier: Modifier = Modifier) {
    val update =
        when (state) {
            is ThemeInstallState.Installed -> theme.hasUpdate(state.installedVersionCode)
            is ThemeInstallState.InstalledInactive -> theme.hasUpdate(state.installedVersionCode)
            else -> false
        }
    val label =
        when {
            update -> stringResource(R.string.update_available)
            state is ThemeInstallState.Installed -> stringResource(R.string.active)
            state is ThemeInstallState.InstalledInactive -> stringResource(R.string.installed)
            state is ThemeInstallState.Downloading ->
                stringResource(
                    R.string.download_progress,
                    (state.progress.coerceIn(0f, 1f) * 100).toInt(),
                )
            state is ThemeInstallState.Installing -> stringResource(R.string.installing)
            state is ThemeInstallState.Downloaded -> stringResource(R.string.ready_to_install)
            state is ThemeInstallState.PartiallyInstalled ->
                stringResource(R.string.resume_installation)
            state is ThemeInstallState.Error -> stringResource(R.string.retry)
            theme.isUiStyle -> stringResource(R.string.built_in)
            else -> stringResource(R.string.available)
        }
    val active = state is ThemeInstallState.Installed
    val failed = state is ThemeInstallState.Error
    Surface(
        modifier = modifier.semantics { stateDescription = label },
        shape = CircleShape,
        color =
            when {
                failed -> MaterialTheme.colorScheme.errorContainer
                active -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            },
        contentColor =
            when {
                failed -> MaterialTheme.colorScheme.onErrorContainer
                active -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state is ThemeInstallState.Downloading || state is ThemeInstallState.Installing) {
                LoadingIndicator(modifier = Modifier.size(16.dp))
            } else {
                Icon(
                    when {
                        update -> Icons.Default.Update
                        active -> Icons.Default.Check
                        failed -> Icons.Default.ErrorOutline
                        else -> Icons.Default.ArrowOutward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
