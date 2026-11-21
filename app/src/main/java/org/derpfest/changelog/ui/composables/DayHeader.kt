/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.derpfest.changelog.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayHeader(
    date: LocalDate,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }

    ListSectionHeader(
        text = when (date) {
            today -> stringResource(R.string.day_today)
            today.minusDays(1) -> stringResource(R.string.day_yesterday)
            else -> remember(date) {
                val skeleton = when (date.year == today.year) {
                    true -> "EEEEMMMMd"
                    false -> "EEEEMMMMdyyyy"
                }

                date.format(
                    DateTimeFormatter.ofPattern(
                        DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
                    )
                )
            }
        },
        modifier = modifier,
    )
}

/**
 * A heading that groups changes in the changelog list.
 *
 * @param text The heading
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)
            .semantics { heading() },
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmallEmphasized,
    )
}
