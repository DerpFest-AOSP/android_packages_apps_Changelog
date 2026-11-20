/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import android.text.format.DateFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.derpfest.changelog.R
import org.derpfest.changelog.ext.openLabelResId
import org.derpfest.changelog.models.Change
import org.derpfest.changelog.utils.CommitMessageUtils
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangeListItem(
    change: Change,
    index: Int,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val time = remember(change.timestamp) {
        DateFormat.getTimeFormat(context).format(Date(change.timestamp))
    }

    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SegmentedListItem(
            onClick = onClick,
            shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
            modifier = Modifier.animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            ),
            leadingContent = {
                SourceAvatar(source = change.source)
            },
            overlineContent = {
                Text(
                    text = change.displayPath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.change_author_time, change.author, time),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (isExpanded) {
                        val body = remember(change.message) {
                            CommitMessageUtils.unwrap(CommitMessageUtils.getBody(change.message))
                        }

                        if (body.isNotEmpty()) {
                            CommitMessage(text = body)
                        }
                    }
                }
            },
            onLongClick = { showMenu = true },
            onLongClickLabel = stringResource(R.string.change_actions),
        ) {
            Text(
                text = change.subject,
                maxLines = when (isExpanded) {
                    true -> Int.MAX_VALUE
                    false -> 2
                },
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            ActionMenuItem(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                text = stringResource(change.source.openLabelResId),
            ) {
                showMenu = false
                onOpen()
            }

            ActionMenuItem(
                icon = Icons.Filled.ContentCopy,
                text = stringResource(R.string.change_copy_link),
            ) {
                showMenu = false
                onCopyLink()
            }

            ActionMenuItem(
                icon = Icons.Filled.Share,
                text = stringResource(R.string.change_share),
            ) {
                showMenu = false
                onShare()
            }
        }
    }
}
