/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
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
    expanded: Boolean,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onShowFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshLabel = stringResource(R.string.refresh)

    HorizontalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = {
            ToolbarTooltip(refreshLabel) {
                FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = refreshLabel,
                    )
                }
            }
        },
        modifier = modifier,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
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
