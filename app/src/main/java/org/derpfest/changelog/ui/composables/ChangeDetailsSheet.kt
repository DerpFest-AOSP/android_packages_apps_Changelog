/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import android.text.format.DateUtils
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.derpfest.changelog.R
import org.derpfest.changelog.ext.labelResId
import org.derpfest.changelog.ext.openLabelResId
import org.derpfest.changelog.models.Change
import org.derpfest.changelog.models.ChangeSource
import org.derpfest.changelog.utils.CommitMessageUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangeDetailsSheet(
    change: Change,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    val body = remember(change.message) {
        CommitMessageUtils.unwrap(CommitMessageUtils.getBody(change.message))
    }

    val details = remember(change) {
        listOfNotNull(
            Detail(Icons.Filled.Code, R.string.change_details_repository, change.repository),
            change.path?.let {
                Detail(Icons.Filled.Folder, R.string.change_details_path, it)
            },
            change.branch?.let {
                Detail(Icons.Filled.AccountTree, R.string.change_details_branch, it)
            },
            Detail(Icons.Filled.Person, R.string.change_details_author, change.author),
            Detail(
                Icons.Filled.Schedule,
                R.string.change_details_date,
                DateUtils.formatDateTime(
                    context,
                    change.timestamp,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or
                            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME,
                ),
            ),
            Detail(
                Icons.Filled.Commit,
                R.string.change_details_commit,
                change.revision.take(SHORT_SHA_LENGTH),
            ),
        )
    }

    ChangelogBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceAvatar(source = change.source)

                Column {
                    Text(
                        text = stringResource(change.source.labelResId),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Text(
                        text = change.displayPath,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SelectionContainer {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = change.subject,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )

                    if (body.isNotEmpty()) {
                        CommitMessage(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                details.forEachIndexed { index, detail ->
                    ListItem(
                        headlineContent = {
                            Text(text = detail.value)
                        },
                        modifier = Modifier.clip(
                            ListItemDefaults.segmentedShapes(
                                index = index,
                                count = details.size,
                            ).shape
                        ),
                        overlineContent = {
                            Text(text = stringResource(detail.labelResId))
                        },
                        leadingContent = {
                            Icon(
                                imageVector = detail.icon,
                                contentDescription = null,
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    )
                }
            }

            ChangeActions(
                source = change.source,
                onOpen = onOpen,
                onShare = onShare,
                onCopyLink = onCopyLink,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChangeActions(
    source: ChangeSource,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
) {
    val openLabel = stringResource(source.openLabelResId)
    val shareLabel = stringResource(R.string.change_share)
    val copyLinkLabel = stringResource(R.string.change_copy_link)

    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(menuState = it) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }

                Button(
                    onClick = onOpen,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(interactionSource),
                    interactionSource = interactionSource,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )

                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

                    Text(text = openLabel)
                }
            },
            menuContent = { menuState ->
                ActionMenuItem(Icons.AutoMirrored.Filled.OpenInNew, openLabel) {
                    menuState.dismiss()
                    onOpen()
                }
            },
        )

        customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }

                ToolbarTooltip(shareLabel) {
                    FilledTonalIconButton(
                        onClick = onShare,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.animateWidth(interactionSource),
                        interactionSource = interactionSource,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = shareLabel,
                        )
                    }
                }
            },
            menuContent = { menuState ->
                ActionMenuItem(Icons.Filled.Share, shareLabel) {
                    menuState.dismiss()
                    onShare()
                }
            },
        )

        customItem(
            buttonGroupContent = {
                val interactionSource = remember { MutableInteractionSource() }

                ToolbarTooltip(copyLinkLabel) {
                    FilledTonalIconButton(
                        onClick = onCopyLink,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.animateWidth(interactionSource),
                        interactionSource = interactionSource,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = copyLinkLabel,
                        )
                    }
                }
            },
            menuContent = { menuState ->
                ActionMenuItem(Icons.Filled.ContentCopy, copyLinkLabel) {
                    menuState.dismiss()
                    onCopyLink()
                }
            },
        )
    }
}

/**
 * A detail of a change.
 *
 * @param icon The icon of the detail
 * @param labelResId The label of the detail
 * @param value The value of the detail
 */
private data class Detail(
    val icon: ImageVector,
    val labelResId: Int,
    val value: String,
)

private const val SHORT_SHA_LENGTH = 12
