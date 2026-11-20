/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.screens

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.derpfest.changelog.R
import org.derpfest.changelog.ext.text
import org.derpfest.changelog.models.Change
import org.derpfest.changelog.models.ChangelogEntry
import org.derpfest.changelog.ui.composables.ChangeListItem
import org.derpfest.changelog.ui.composables.DayHeader
import org.derpfest.changelog.ui.composables.EmptyState
import org.derpfest.changelog.viewmodels.ChangelogViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogScreen(
    changelogViewModel: ChangelogViewModel,
    modifier: Modifier = Modifier,
) {
    val changelog by changelogViewModel.changelog.collectAsState()
    val entries by changelogViewModel.entries.collectAsState()
    val isRefreshing by changelogViewModel.isRefreshing.collectAsState()
    val error by changelogViewModel.error.collectAsState()

    val uriHandler = LocalUriHandler.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val buildDate = remember {
        DateFormat.format(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"),
            Build.TIME,
        ).toString()
    }

    val hasChangelog = changelog != null
    val errorText = error?.text
    val retry = stringResource(R.string.retry)

    // Without a changelog the error takes the whole screen instead
    LaunchedEffect(errorText != null, hasChangelog) {
        if (errorText == null || !hasChangelog) {
            return@LaunchedEffect
        }

        val result = snackbarHostState.showSnackbar(
            message = errorText,
            actionLabel = retry,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )

        when (result) {
            SnackbarResult.ActionPerformed -> changelogViewModel.refresh()
            SnackbarResult.Dismissed -> changelogViewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                subtitle = {
                    Text(text = stringResource(R.string.changelog_since_build, buildDate))
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    action = data.visuals.actionLabel?.let {
                        {
                            TextButton(
                                onClick = data::performAction,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = SnackbarDefaults.actionColor,
                                ),
                            ) {
                                Text(text = it)
                            }
                        }
                    },
                    dismissAction = {
                        IconButton(onClick = data::dismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.dismiss),
                            )
                        }
                    },
                ) {
                    // Keep counting down while the snackbar is shown
                    Text(text = errorText ?: data.visuals.message)
                }
            }
        },
    ) { innerPadding ->
        val currentEntries = entries

        when {
            currentEntries != null -> ChangelogList(
                entries = currentEntries,
                isTruncated = changelog?.isTruncated == true,
                isRefreshing = isRefreshing,
                onRefresh = { changelogViewModel.refresh() },
                onChangeClick = { uriHandler.openUri(it.url) },
                contentPadding = innerPadding,
            )

            errorText != null && !isRefreshing -> EmptyState(
                icon = Icons.Filled.CloudOff,
                title = stringResource(R.string.error_title),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                text = errorText,
                action = {
                    Button(
                        onClick = { changelogViewModel.refresh() },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = retry)
                    }
                },
            )

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChangelogList(
    entries: List<ChangelogEntry>,
    isTruncated: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onChangeClick: (Change) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val pullToRefreshState = rememberPullToRefreshState()

    // Padding the box keeps the day headers sticking right below the top app bar
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            if (entries.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.Inbox,
                        title = stringResource(R.string.changelog_empty_title),
                        modifier = Modifier.fillParentMaxSize(),
                        text = stringResource(R.string.changelog_empty_since_build),
                    )
                }
            }

            entries.forEach { entry ->
                when (entry) {
                    is ChangelogEntry.Day -> stickyHeader(
                        key = entry.key,
                        contentType = "day",
                    ) {
                        DayHeader(date = entry.date)
                    }

                    is ChangelogEntry.Item -> item(
                        key = entry.key,
                        contentType = "change",
                    ) {
                        ChangeListItem(
                            change = entry.change,
                            index = entry.index,
                            count = entry.count,
                            onClick = { onChangeClick(entry.change) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            if (isTruncated) {
                item(key = "truncated") {
                    Text(
                        text = stringResource(R.string.changelog_truncated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
