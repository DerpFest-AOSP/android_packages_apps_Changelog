/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.derpfest.changelog.R
import org.derpfest.changelog.ext.labelResId
import org.derpfest.changelog.ext.summaryResId
import org.derpfest.changelog.models.TapAction
import org.derpfest.changelog.ui.composables.SwitchListItem
import org.derpfest.changelog.ui.composables.ToolbarTooltip
import org.derpfest.changelog.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tapAction by settingsViewModel.tapAction.collectAsState()
    val highlightNew by settingsViewModel.highlightNew.collectAsState()

    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val sourceCodeUrl = stringResource(R.string.source_code_url)
    val websiteUrl = stringResource(R.string.website_url)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                navigationIcon = {
                    val backLabel = stringResource(R.string.back)

                    ToolbarTooltip(backLabel) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backLabel,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(key = "list") {
                SettingsSection(title = stringResource(R.string.settings_list)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_tap_action),
                                style = MaterialTheme.typography.titleSmall,
                            )

                            Text(
                                text = stringResource(tapAction.summaryResId),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(
                                    ButtonGroupDefaults.ConnectedSpaceBetween
                                ),
                            ) {
                                TapAction.entries.forEachIndexed { index, action ->
                                    ToggleButton(
                                        checked = tapAction == action,
                                        onCheckedChange = {
                                            settingsViewModel.setTapAction(action)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            TapAction.entries.lastIndex ->
                                                ButtonGroupDefaults.connectedTrailingButtonShapes()

                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    ) {
                                        Text(text = stringResource(action.labelResId))
                                    }
                                }
                            }
                        }

                        SwitchListItem(
                            icon = Icons.Filled.NewReleases,
                            title = stringResource(R.string.settings_highlight_new),
                            checked = highlightNew,
                            onCheckedChange = settingsViewModel::setHighlightNew,
                            index = 0,
                            count = 1,
                            summary = stringResource(R.string.settings_highlight_new_summary),
                        )
                    }
                }
            }

            item(key = "about") {
                SettingsSection(title = stringResource(R.string.settings_about)) {
                    // The version isn't an action. It stays out of the click path, and uses the
                    // regular row colors so it reads with the rest of About.
                    val rowColors = ListItemDefaults.segmentedColors()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        SegmentedListItem(
                            onClick = {},
                            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 3),
                            enabled = false,
                            colors = ListItemDefaults.segmentedColors(
                                disabledContainerColor = rowColors.containerColor,
                                disabledContentColor = rowColors.contentColor,
                                disabledLeadingContentColor = rowColors.leadingContentColor,
                                disabledSupportingContentColor = rowColors.supportingContentColor,
                            ),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                )
                            },
                            supportingContent = {
                                Text(text = settingsViewModel.versionName)
                            },
                        ) {
                            Text(text = stringResource(R.string.settings_version))
                        }

                        SegmentedListItem(
                            onClick = { uriHandler.openUri(sourceCodeUrl) },
                            shapes = ListItemDefaults.segmentedShapes(index = 1, count = 3),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Code,
                                    contentDescription = null,
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                )
                            },
                        ) {
                            Text(text = stringResource(R.string.settings_source_code))
                        }

                        SegmentedListItem(
                            onClick = { uriHandler.openUri(websiteUrl) },
                            shapes = ListItemDefaults.segmentedShapes(index = 2, count = 3),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = null,
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                )
                            },
                        ) {
                            Text(text = stringResource(R.string.settings_website))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )

        content()
    }
}
