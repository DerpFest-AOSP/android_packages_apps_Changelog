/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.screens

import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.derpfest.changelog.R
import org.derpfest.changelog.ext.copyLink
import org.derpfest.changelog.ext.shareLink
import org.derpfest.changelog.ext.text
import org.derpfest.changelog.models.Change
import org.derpfest.changelog.models.ChangelogEntry
import org.derpfest.changelog.models.Period
import org.derpfest.changelog.models.TapAction
import org.derpfest.changelog.ui.composables.ChangeDetailsSheet
import org.derpfest.changelog.ui.composables.ChangeListItem
import org.derpfest.changelog.ui.composables.ChangelogFloatingToolbar
import org.derpfest.changelog.ui.composables.ChangelogSearchBar
import org.derpfest.changelog.ui.composables.DayHeader
import org.derpfest.changelog.ui.composables.ListSectionHeader
import org.derpfest.changelog.ui.composables.EmptyState
import org.derpfest.changelog.ui.composables.FilterSheet
import org.derpfest.changelog.viewmodels.ChangelogViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogScreen(
    changelogViewModel: ChangelogViewModel,
    onShowDevices: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changelog by changelogViewModel.changelog.collectAsState()
    val filteredChanges by changelogViewModel.filteredChanges.collectAsState()
    val entries by changelogViewModel.entries.collectAsState()
    val filter by changelogViewModel.filter.collectAsState()
    val isRefreshing by changelogViewModel.isRefreshing.collectAsState()
    val error by changelogViewModel.error.collectAsState()
    val tapAction by changelogViewModel.tapAction.collectAsState()
    val highlightNew by changelogViewModel.highlightNew.collectAsState()
    val newChangeIds by changelogViewModel.newChangeIds.collectAsState()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var selectedChange by remember { mutableStateOf<Change?>(null) }
    val expandedChangeIds = remember { mutableStateSetOf<String>() }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var isToolbarVisible by rememberSaveable { mutableStateOf(true) }
    val searchBarState = rememberSearchBarState()

    val onOpen: (Change) -> Unit = { uriHandler.openUri(it.url) }
    val onShare: (Change) -> Unit = { context.shareLink(it.url, it.subject) }
    val onCopyLink: (Change) -> Unit = { context.copyLink(it.url, it.subject) }
    val onChangeClick: (Change) -> Unit = { change ->
        when (tapAction) {
            TapAction.DETAILS -> selectedChange = change

            TapAction.EXPAND -> if (!expandedChangeIds.remove(change.id)) {
                expandedChangeIds.add(change.id)
            }
        }
    }

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
                    Text(text = stringResource(R.string.changelog_title))
                },
                subtitle = {
                    Text(
                        text = when (filter.period) {
                            Period.SINCE_BUILD -> stringResource(
                                R.string.changelog_since_build, buildDate
                            )

                            Period.LAST_30_DAYS -> stringResource(R.string.period_last_30_days)
                        }
                    )
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
        floatingActionButton = {
            val searchOpen = searchBarState.targetValue != SearchBarValue.Collapsed ||
                    searchBarState.currentValue != SearchBarValue.Collapsed
            ChangelogFloatingToolbar(
                visible = isToolbarVisible,
                // The toolbar window is above the activity, so it has to leave while a sheet or
                // the search screen is open. Those cover it anyway.
                covered = showFilterSheet || selectedChange != null || searchOpen,
                onRefresh = { changelogViewModel.refresh() },
                onSearch = {
                    scope.launch {
                        searchBarState.animateToExpanded()
                    }
                },
                onShowFilters = { showFilterSheet = true },
                onShowDevices = onShowDevices,
                onShowSettings = onShowSettings,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        val currentEntries = entries

        when {
            currentEntries != null -> ChangelogList(
                entries = currentEntries,
                expandedChangeIds = expandedChangeIds,
                newChangeIds = newChangeIds,
                highlightNew = highlightNew,
                isTruncated = changelog?.isTruncated == true,
                isRefreshing = isRefreshing,
                onRefresh = { changelogViewModel.refresh() },
                onChangeClick = onChangeClick,
                onOpen = onOpen,
                onShare = onShare,
                onCopyLink = onCopyLink,
                emptyState = {
                    when (filter.hidesChanges) {
                        true -> EmptyState(
                            icon = Icons.Filled.FilterAlt,
                            title = stringResource(R.string.changelog_empty_filtered_title),
                            modifier = it,
                            text = stringResource(R.string.changelog_empty_filtered),
                        )

                        false -> EmptyState(
                            mark = painterResource(R.drawable.ic_derp_owl),
                            title = stringResource(R.string.changelog_empty_title),
                            modifier = it,
                            text = stringResource(
                                when (filter.period) {
                                    Period.SINCE_BUILD -> R.string.changelog_empty_since_build
                                    Period.LAST_30_DAYS -> R.string.changelog_empty_last_30_days
                                }
                            ),
                        )
                    }
                },
                contentPadding = innerPadding,
                modifier = with(FloatingToolbarDefaults) {
                    Modifier.floatingToolbarVerticalNestedScroll(
                        expanded = isToolbarVisible,
                        onExpand = { isToolbarVisible = true },
                        onCollapse = { isToolbarVisible = false },
                    )
                },
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

    ChangelogSearchBar(
        searchBarState = searchBarState,
        changes = filteredChanges.orEmpty(),
        newChangeIds = newChangeIds,
        highlightNew = highlightNew,
        onChangeClick = onChangeClick,
        onOpen = onOpen,
        onShare = onShare,
        onCopyLink = onCopyLink,
    )

    selectedChange?.let { change ->
        ChangeDetailsSheet(
            change = change,
            onOpen = { onOpen(change) },
            onShare = { onShare(change) },
            onCopyLink = { onCopyLink(change) },
            onDismissRequest = { selectedChange = null },
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            filter = filter,
            onPeriodChange = changelogViewModel::setPeriod,
            onSourceShownChange = changelogViewModel::setSourceShown,
            onShowTranslationsChange = changelogViewModel::setShowTranslations,
            onShowMergesChange = changelogViewModel::setShowMerges,
            onDismissRequest = { showFilterSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChangelogList(
    entries: List<ChangelogEntry>,
    expandedChangeIds: Set<String>,
    newChangeIds: Set<String>,
    highlightNew: Boolean,
    isTruncated: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onChangeClick: (Change) -> Unit,
    onOpen: (Change) -> Unit,
    onShare: (Change) -> Unit,
    onCopyLink: (Change) -> Unit,
    emptyState: @Composable (Modifier) -> Unit,
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
                // Leave room for the floating toolbar
                bottom = contentPadding.calculateBottomPadding() +
                        FloatingToolbarDefaults.ContainerSize +
                        FloatingToolbarDefaults.ScreenOffset * 2,
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            if (entries.isEmpty()) {
                item(key = "empty") {
                    emptyState(Modifier.fillParentMaxSize())
                }
            }

            entries.forEach { entry ->
                when (entry) {
                    is ChangelogEntry.DeviceSection -> item(
                        key = entry.key,
                        contentType = "device",
                    ) {
                        ListSectionHeader(
                            text = stringResource(R.string.changelog_this_device, entry.name),
                            modifier = Modifier.animateItem(),
                        )
                    }

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
                            isExpanded = entry.change.id in expandedChangeIds,
                            isNew = highlightNew && entry.change.id in newChangeIds,
                            onClick = { onChangeClick(entry.change) },
                            onOpen = { onOpen(entry.change) },
                            onShare = { onShare(entry.change) },
                            onCopyLink = { onCopyLink(entry.change) },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    is ChangelogEntry.BuildMarker -> item(
                        key = entry.key,
                        contentType = "build",
                    ) {
                        BuildMarker(
                            time = entry.time,
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

@Composable
private fun BuildMarker(
    time: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val date = remember(time) {
        DateUtils.formatDateTime(
            context,
            time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or
                    DateUtils.FORMAT_SHOW_TIME,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
        )

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Text(
                    text = stringResource(R.string.changelog_your_build, date),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
