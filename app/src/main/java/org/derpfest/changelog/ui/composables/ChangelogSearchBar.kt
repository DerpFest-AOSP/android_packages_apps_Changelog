/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.derpfest.changelog.R
import org.derpfest.changelog.models.Change

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    FlowPreview::class,
)
@Composable
fun ChangelogSearchBar(
    searchBarState: SearchBarState,
    changes: List<Change>,
    newChangeIds: Set<String>,
    highlightNew: Boolean,
    onChangeClick: (Change) -> Unit,
    onOpen: (Change) -> Unit,
    onShare: (Change) -> Unit,
    onCopyLink: (Change) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .debounce(150)
            .distinctUntilChanged()
            .collectLatest { query = it }
    }

    val results = remember(changes, query) {
        changes.filter { it.matches(query) }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {},
            placeholder = {
                Text(text = stringResource(R.string.search_hint))
            },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    val backLabel = stringResource(R.string.search_back)

                    ToolbarTooltip(backLabel) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    searchBarState.animateToCollapsed()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = backLabel,
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                    )
                }
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    val clearLabel = stringResource(R.string.search_clear)

                    ToolbarTooltip(clearLabel) {
                        IconButton(onClick = { textFieldState.clearText() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = clearLabel,
                            )
                        }
                    }
                }
            },
        )
    }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        when {
            query.isBlank() -> {}

            results.isEmpty() -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(R.string.search_empty_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                text = stringResource(R.string.search_empty, query),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                itemsIndexed(
                    items = results,
                    key = { _, change -> change.id },
                ) { index, change ->
                    ChangeListItem(
                        change = change,
                        index = index,
                        count = results.size,
                        isExpanded = false,
                        isNew = highlightNew && change.id in newChangeIds,
                        onClick = { onChangeClick(change) },
                        onOpen = { onOpen(change) },
                        onShare = { onShare(change) },
                        onCopyLink = { onCopyLink(change) },
                    )
                }
            }
        }
    }
}

/**
 * Whether the change matches the search query.
 *
 * @param query The search query
 */
fun Change.matches(query: String): Boolean {
    if (query.isBlank()) {
        return true
    }

    val needle = query.trim()

    return subject.contains(needle, ignoreCase = true) ||
            message.contains(needle, ignoreCase = true) ||
            author.contains(needle, ignoreCase = true) ||
            displayPath.contains(needle, ignoreCase = true) ||
            repository.contains(needle, ignoreCase = true)
}
