/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.derpfest.changelog.R
import org.derpfest.changelog.ext.text
import org.derpfest.changelog.models.Device
import org.derpfest.changelog.ui.composables.EmptyState
import org.derpfest.changelog.ui.composables.SwitchListItem
import org.derpfest.changelog.ui.composables.ToolbarTooltip
import org.derpfest.changelog.viewmodels.DevicesViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    FlowPreview::class,
)
@Composable
fun DevicesScreen(
    devicesViewModel: DevicesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val devices by devicesViewModel.filteredDevices.collectAsState()
    val showAllDevices by devicesViewModel.showAllDevices.collectAsState()
    val watchedDevices by devicesViewModel.watchedDevices.collectAsState()
    val isRefreshing by devicesViewModel.isRefreshing.collectAsState()
    val error by devicesViewModel.error.collectAsState()
    val query by devicesViewModel.query.collectAsState()

    val textFieldState = rememberTextFieldState(initialText = query)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val errorText = error?.text

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .debounce(150)
            .distinctUntilChanged()
            .collectLatest(devicesViewModel::setQuery)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.devices))
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
        val currentDevices = devices

        when {
            currentDevices != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                item(key = "search") {
                    TextField(
                        state = textFieldState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = {
                            Text(text = stringResource(R.string.devices_search_hint))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (textFieldState.text.isNotEmpty()) {
                                IconButton(onClick = { textFieldState.clearText() }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.search_clear),
                                    )
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }

                item(key = "all_devices") {
                    SwitchListItem(
                        icon = Icons.Filled.Devices,
                        title = stringResource(R.string.devices_show_all),
                        checked = showAllDevices,
                        onCheckedChange = devicesViewModel::setShowAllDevices,
                        index = 0,
                        count = 1,
                        summary = stringResource(R.string.devices_show_all_summary),
                    )
                }

                if (currentDevices.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.devices_empty_title),
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(top = 32.dp),
                            text = stringResource(R.string.devices_empty, query),
                        )
                    }
                } else {
                    itemsIndexed(
                        items = currentDevices,
                        key = { _, device -> device.codename },
                    ) { index, device ->
                        DeviceListItem(
                            device = device,
                            isThisDevice = device.codename == devicesViewModel.thisDeviceCodename,
                            watched = device.codename in watchedDevices,
                            enabled = !showAllDevices,
                            index = index,
                            count = currentDevices.size,
                            onWatchedChange = {
                                devicesViewModel.setDeviceWatched(device.codename, it)
                            },
                        )
                    }
                }
            }

            errorText != null && !isRefreshing -> EmptyState(
                icon = Icons.Filled.CloudOff,
                title = stringResource(R.string.devices_error_title),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                text = errorText,
                action = {
                    Button(
                        onClick = { devicesViewModel.refresh() },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.retry))
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeviceListItem(
    device: Device,
    isThisDevice: Boolean,
    watched: Boolean,
    enabled: Boolean,
    index: Int,
    count: Int,
    onWatchedChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        checked = watched,
        onCheckedChange = onWatchedChange,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        enabled = enabled,
        leadingContent = {
            Icon(
                imageVector = when {
                    isThisDevice -> Icons.Filled.PhoneAndroid
                    else -> Icons.Filled.Devices
                },
                contentDescription = null,
            )
        },
        trailingContent = {
            if (watched) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.devices_watched),
                )
            }
        },
        overlineContent = {
            Text(
                text = device.codename,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                device.maintainer?.let {
                    Text(
                        text = stringResource(R.string.devices_maintainer, it),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (isThisDevice) {
                    Badge {
                        Text(text = stringResource(R.string.devices_this_device))
                    }
                }
            }
        },
    ) {
        Text(
            text = device.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
