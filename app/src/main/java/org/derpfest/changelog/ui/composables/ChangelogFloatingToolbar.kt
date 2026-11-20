/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.derpfest.changelog.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogFloatingToolbar(
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onShowFilters: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
    ) {
        ToolbarButton(
            icon = Icons.Filled.Search,
            label = stringResource(R.string.search),
            onClick = onSearch,
        )

        ToolbarButton(
            icon = Icons.Filled.FilterList,
            label = stringResource(R.string.filters),
            onClick = onShowFilters,
        )

        ToolbarButton(
            icon = Icons.Filled.Devices,
            label = stringResource(R.string.devices),
            onClick = onShowDevices,
        )

        ToolbarButton(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.settings),
            onClick = onShowSettings,
        )

        ToolbarButton(
            icon = Icons.Filled.Refresh,
            label = stringResource(R.string.refresh),
            onClick = onRefresh,
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ToolbarTooltip(label) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
            )
        }
    }
}
