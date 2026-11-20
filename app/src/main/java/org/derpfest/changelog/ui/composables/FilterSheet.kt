/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.derpfest.changelog.R
import org.derpfest.changelog.ext.icon
import org.derpfest.changelog.ext.labelResId
import org.derpfest.changelog.models.ChangeSource
import org.derpfest.changelog.models.ChangesFilter
import org.derpfest.changelog.models.Period

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun FilterSheet(
    filter: ChangesFilter,
    onPeriodChange: (Period) -> Unit,
    onSourceShownChange: (ChangeSource, Boolean) -> Unit,
    onShowTranslationsChange: (Boolean) -> Unit,
    onShowMergesChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ChangelogBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.filters),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )

            FilterSection(title = stringResource(R.string.filter_period)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween
                    ),
                ) {
                    Period.entries.forEachIndexed { index, period ->
                        ToggleButton(
                            checked = filter.period == period,
                            onCheckedChange = { onPeriodChange(period) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                Period.entries.lastIndex ->
                                    ButtonGroupDefaults.connectedTrailingButtonShapes()

                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) {
                            Text(text = stringResource(period.labelResId))
                        }
                    }
                }
            }

            FilterSection(title = stringResource(R.string.filter_sources)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChangeSource.entries.forEach { source ->
                        val selected = source in filter.sources

                        FilterChip(
                            selected = selected,
                            onClick = { onSourceShownChange(source, !selected) },
                            label = {
                                Text(text = stringResource(source.labelResId))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (selected) {
                                        true -> Icons.Filled.Check
                                        false -> source.icon
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }

            FilterSection(title = stringResource(R.string.filter_types)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    SwitchListItem(
                        icon = Icons.Filled.Translate,
                        title = stringResource(R.string.filter_translations),
                        checked = filter.showTranslations,
                        onCheckedChange = onShowTranslationsChange,
                        index = 0,
                        count = 2,
                    )

                    SwitchListItem(
                        icon = Icons.AutoMirrored.Filled.MergeType,
                        title = stringResource(R.string.filter_merges),
                        checked = filter.showMerges,
                        onCheckedChange = onShowMergesChange,
                        index = 1,
                        count = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )

        content()
    }
}
